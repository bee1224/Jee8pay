package com.jeequan.jeepay.pay.channel.ccat;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient.CcatException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CcatClientTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void sendsFormTokenThenBearerJsonCollectAndCachesToken() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.responses.add(new CcatClient.TransportResponse(200,
                "{\"access_token\":\"synthetic-token\",\".expires\":\"Wed, 12 Aug 2026 03:00:00 GMT\"}"));
        transport.responses.add(new CcatClient.TransportResponse(200, "{\"status\":\"OK\"}"));
        transport.responses.add(new CcatClient.TransportResponse(200, "{\"status\":\"OK\"}"));
        CcatClient client = new CcatClient(transport, Clock.fixed(NOW, ZoneOffset.UTC));

        client.query(params(), "P202608120001");
        client.query(params(), "P202608120002");

        assertEquals(3, transport.requests.size());
        RecordedRequest token = transport.requests.get(0);
        assertEquals(CcatClient.TEST_BASE_URL + "Token", token.url);
        assertTrue(token.headers.get("Content-Type").startsWith("application/x-www-form-urlencoded"));
        assertEquals("grant_type=password&username=test-user&password=test-api-password", token.body);

        RecordedRequest collect = transport.requests.get(1);
        assertEquals(CcatClient.TEST_BASE_URL + "api/Collect", collect.url);
        assertEquals("Bearer synthetic-token", collect.headers.get("Authorization"));
        JSONObject payload = JSONObject.parseObject(collect.body);
        assertEquals("CvsOrderQuery", payload.getString("cmd"));
        assertEquals("test-user", payload.getString("cust_id"));
        assertEquals("P202608120001", payload.getString("cust_order_no"));
    }

    @Test
    void failsClosedForMissingOrUnknownEnvironment() {
        CcatNormalMchParams params = params();
        params.setEnvironment(null);
        CcatClient client = new CcatClient(new FakeTransport(), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(CcatException.class, () -> client.query(params, "P1"));
        assertThrows(CcatException.class, () -> CcatClient.resolveBaseUrl("LOCAL"));
    }

    @Test
    void rejectsMalformedTokenWithoutLeakingCredential() {
        FakeTransport transport = new FakeTransport();
        transport.responses.add(new CcatClient.TransportResponse(200, "{\"error\":\"invalid_grant\"}"));
        CcatClient client = new CcatClient(transport, Clock.fixed(NOW, ZoneOffset.UTC));

        CcatException error = assertThrows(CcatException.class, () -> client.query(params(), "P1"));
        assertFalse(error.getMessage().contains("test-api-password"));
    }

    @Test
    void classifiesCollectHttpFailureAsAmbiguous() {
        FakeTransport transport = new FakeTransport();
        transport.responses.add(new CcatClient.TransportResponse(200,
                "{\"access_token\":\"synthetic-token\",\".expires\":\"Wed, 12 Aug 2026 03:00:00 GMT\"}"));
        transport.responses.add(new CcatClient.TransportResponse(503, "temporarily unavailable"));
        CcatClient client = new CcatClient(transport, Clock.fixed(NOW, ZoneOffset.UTC));

        CcatException error = assertThrows(CcatException.class, () -> client.query(params(), "P1"));
        assertEquals(CcatClient.ErrorType.AMBIGUOUS, error.getType());
        assertEquals(503, error.getHttpStatus());
        assertNotNull(error.getLatencyMillis());
    }

    @Test
    void classifiesCollectHttpFourHundredAsDeterministicBusinessFailureWithAllowlistedFields() {
        FakeTransport transport = new FakeTransport();
        transport.responses.add(new CcatClient.TransportResponse(200,
                "{\"access_token\":\"synthetic-token\",\".expires\":\"Wed, 12 Aug 2026 03:00:00 GMT\"}"));
        transport.responses.add(new CcatClient.TransportResponse(400,
                "{\"status\":\"ERROR\",\"process_code\":\"E10\",\"msg\":\"rejected\","
                        + "\"cust_id\":\"must-not-be-retained\",\"access_token\":\"must-not-be-retained\"}"));
        CcatClient client = new CcatClient(transport, Clock.fixed(NOW, ZoneOffset.UTC));

        CcatException error = assertThrows(CcatException.class, () -> client.query(params(), "P1"));

        assertEquals(CcatClient.ErrorType.BUSINESS, error.getType());
        assertEquals(400, error.getHttpStatus());
        assertEquals("E10", error.getProviderFields().getString("process_code"));
        assertFalse(error.getProviderFields().containsKey("cust_id"));
        assertFalse(error.getProviderFields().containsKey("access_token"));
    }

    @Test
    void keepsConflictResponseAmbiguousBecauseProviderOrderMayAlreadyExist() {
        FakeTransport transport = new FakeTransport();
        transport.responses.add(new CcatClient.TransportResponse(200,
                "{\"access_token\":\"synthetic-token\",\".expires\":\"Wed, 12 Aug 2026 03:00:00 GMT\"}"));
        transport.responses.add(new CcatClient.TransportResponse(409,
                "{\"status\":\"ERROR\",\"msg\":\"duplicate order\"}"));
        CcatClient client = new CcatClient(transport, Clock.fixed(NOW, ZoneOffset.UTC));

        CcatException error = assertThrows(CcatException.class, () -> client.query(params(), "P1"));

        assertEquals(CcatClient.ErrorType.AMBIGUOUS, error.getType());
        assertEquals(409, error.getHttpStatus());
    }

    private static CcatNormalMchParams params() {
        CcatNormalMchParams params = new CcatNormalMchParams();
        params.setEnvironment(CcatNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");
        return params;
    }

    private static final class FakeTransport implements CcatClient.Transport {
        private final List<CcatClient.TransportResponse> responses = new ArrayList<>();
        private final List<RecordedRequest> requests = new ArrayList<>();

        @Override
        public CcatClient.TransportResponse post(String url, java.util.Map<String, String> headers,
                                                 String body, java.time.Duration timeout) {
            requests.add(new RecordedRequest(url, headers, body));
            return responses.remove(0);
        }
    }

    private static final class RecordedRequest {
        private final String url;
        private final java.util.Map<String, String> headers;
        private final String body;

        private RecordedRequest(String url, java.util.Map<String, String> headers, String body) {
            this.url = url;
            this.headers = headers;
            this.body = body;
        }
    }
}
