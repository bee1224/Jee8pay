package com.jeequan.jeepay.pay.channel.ryo.payway;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.ryo.RyoNormalMchParams;
import com.jeequan.jeepay.pay.channel.ryo.RyoClient;
import com.jeequan.jeepay.pay.channel.ryo.RyoClient.RyoException;
import com.jeequan.jeepay.pay.channel.ryo.RyoClient.CollectResponse;
import com.jeequan.jeepay.pay.channel.ryo.RyoIbonOrderRS;
import com.jeequan.jeepay.pay.channel.ryo.RyoMchParamsResolver;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class RyoIbonTest {

    private RyoClient client;
    private RyoMchParamsResolver paramsResolver;
    private RyoIbon ibon;
    private RyoNormalMchParams params;
    private PayOrder payOrder;
    private UnifiedOrderRQ request;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(RyoClient.class);
        paramsResolver = mock(RyoMchParamsResolver.class);
        ibon = new TestRyoIbon(client, paramsResolver);
        params = new RyoNormalMchParams();
        params.setEnvironment(RyoNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");
        when(paramsResolver.resolve(any(MchAppConfigContext.class))).thenReturn(params);

        payOrder = new PayOrder();
        payOrder.setPayOrderId("P202608120000000001");
        payOrder.setMchOrderNo("MCH-ORDER-001");
        payOrder.setAmount(10_000L);
        payOrder.setCurrency("TWD");
        payOrder.setSubject("Test order");
        payOrder.setBody("Synthetic fixture");
        payOrder.setExpiredTime(Date.from(Instant.parse("2026-08-19T00:00:00Z")));

        request = new UnifiedOrderRQ();
        request.setChannelExtra("{\"payerName\":\"Test User\",\"payerPostcode\":\"100\","
                + "\"payerAddress\":\"Test Address\",\"payerMobile\":\"0900000000\","
                + "\"payerEmail\":\"test@example.invalid\"}");
    }

    @Test
    void buildsOfficialIbonConstantsAmountAndStableIdentity() throws Exception {
        JSONObject payload = ibon.buildAppendRequest(request, payOrder, params, "https://merchant.invalid/notify");

        assertEquals("CvsOrderAppend", payload.getString("cmd"));
        assertEquals("test-user", payload.getString("cust_id"));
        assertEquals(payOrder.getPayOrderId(), payload.getString("cust_order_no"));
        assertEquals(100, payload.getLongValue("order_amount"));
        assertEquals(0, payload.getIntValue("payment_type"));
        assertEquals(2, payload.getIntValue("payment_acquirerType"));
        assertEquals("https://merchant.invalid/notify", payload.getString("apn_url"));
    }

    @Test
    void providerCreateSuccessProducesWaitingInstructions() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(response(successResponse(100)));
        MchAppConfigContext context = new MchAppConfigContext();
        context.getNormalMchParamsMap().put(CS.IF_CODE.RYO, params);

        RyoIbonOrderRS result = (RyoIbonOrderRS) ibon.pay(request, payOrder, context);

        assertEquals(ChannelRetMsg.ChannelState.WAITING, result.getChannelRetMsg().getChannelState());
        assertTrue(result.getChannelRetMsg().isNeedQuery());
        assertEquals("123456", result.getIbonCode());
        assertEquals("990", result.getIbonShopId());
        assertEquals(101, result.getBillAmount());
        assertTrue(result.buildPayData().contains("990123456"));
    }

    @Test
    void deterministicBusinessErrorDoesNotQueryOrRetry() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(response(error("amount rejected")));

        RyoException error = assertThrows(RyoException.class,
                () -> ibon.executeCreate(params, payOrder, new JSONObject()));

        assertEquals(RyoClient.ErrorType.BUSINESS, error.getType());
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        verify(client, never()).queryWithMetadata(any(), anyString());
    }

    @Test
    void deterministicBusinessErrorUsesNativeConfirmFailAndCannotBuildEmptyPayData() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(response(error("amount rejected")));
        MchAppConfigContext context = new MchAppConfigContext();
        context.getNormalMchParamsMap().put(CS.IF_CODE.RYO, params);

        RyoIbonOrderRS result = (RyoIbonOrderRS) ibon.pay(request, payOrder, context);

        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_FAIL, result.getChannelRetMsg().getChannelState());
        assertEquals("RYO_BUSINESS", result.getChannelRetMsg().getChannelErrCode());
        assertThrows(IllegalStateException.class, result::buildPayData);
    }

    @Test
    void authenticationErrorDoesNotQueryOrRetry() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new RyoException(RyoClient.ErrorType.AUTHENTICATION, "token rejected"));

        RyoException error = assertThrows(RyoException.class,
                () -> ibon.executeCreate(params, payOrder, new JSONObject()));

        assertEquals(RyoClient.ErrorType.AUTHENTICATION, error.getType());
        verify(client, never()).queryWithMetadata(any(), anyString());
        assertThrows(IllegalStateException.class, new RyoIbonOrderRS()::buildPayData);
    }

    @Test
    void timeoutThenQueryFoundRecoversWithoutSecondAppend() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new RyoException(RyoClient.ErrorType.AMBIGUOUS, "read timeout"));
        when(client.queryWithMetadata(params, payOrder.getPayOrderId()))
                .thenReturn(response(successResponse(100)));

        RyoIbon.CreateResult recovered = ibon.executeCreate(params, payOrder, new JSONObject());

        assertEquals("OK", recovered.body().getString("status"));
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        verify(client, times(1)).queryWithMetadata(params, payOrder.getPayOrderId());
    }

    @Test
    void timeoutThenExactNotFoundDoesNotRetryAppend() throws Exception {
        JSONObject sameRequest = new JSONObject();
        sameRequest.put("cust_order_no", payOrder.getPayOrderId());
        when(client.append(params, sameRequest)).thenThrow(
                new RyoException(RyoClient.ErrorType.AMBIGUOUS, "read timeout"));
        when(client.queryWithMetadata(params, payOrder.getPayOrderId()))
                .thenReturn(response(error(RyoIbon.QUERY_NOT_FOUND_MESSAGE)));

        RyoException error = assertThrows(RyoException.class,
                () -> ibon.executeCreate(params, payOrder, sameRequest));

        assertEquals(RyoClient.ErrorType.AMBIGUOUS, error.getType());
        assertEquals("QUERY_NOT_FOUND", error.getProviderFields().getString("reconciliation"));
        verify(client, times(1)).append(params, sameRequest);
        verify(client, times(1)).queryWithMetadata(params, payOrder.getPayOrderId());
    }

    @Test
    void ambiguousTransportReturnsUnknownWithReconciliationAndNoFakeInstruction(CapturedOutput output)
            throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new RyoException(RyoClient.ErrorType.AMBIGUOUS, "read timeout",
                        new IOException("socket reset"), null, 8L, null));
        when(client.queryWithMetadata(params, payOrder.getPayOrderId()))
                .thenReturn(response(error(RyoIbon.QUERY_NOT_FOUND_MESSAGE)));
        MchAppConfigContext context = new MchAppConfigContext();
        context.getNormalMchParamsMap().put(CS.IF_CODE.RYO, params);

        RyoIbonOrderRS result = (RyoIbonOrderRS) ibon.pay(request, payOrder, context);

        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, result.getChannelRetMsg().getChannelState());
        assertTrue(result.getChannelRetMsg().isNeedQuery());
        assertThrows(IllegalStateException.class, result::buildPayData);
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        assertTrue(output.getAll().contains("outcome=TRANSPORT_ERROR"));
        assertTrue(output.getAll().contains("reconciliation=QUERY_NOT_FOUND"));
    }

    @Test
    void sanitizedCreateLogContainsAllowlistedEvidenceWithoutSecrets(CapturedOutput output) throws Exception {
        JSONObject rejected = error("password=test-api-password Bearer secret-token cust_id=test-user");
        rejected.put("process_code", "E10");
        rejected.put("trans_id", "SAFE-REF-1");
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(response(rejected));
        MchAppConfigContext context = new MchAppConfigContext();
        context.getNormalMchParamsMap().put(CS.IF_CODE.RYO, params);

        ibon.pay(request, payOrder, context);

        String logs = output.getAll();
        assertTrue(logs.contains("event=RYO_CREATE"));
        assertTrue(logs.contains("outcome=REJECTED"));
        assertTrue(logs.contains("payOrderId=P202608120000000001"));
        assertTrue(logs.contains("amountTwd=100"));
        assertTrue(logs.contains("httpStatus=200"));
        assertTrue(logs.contains("providerCode=E10"));
        assertTrue(logs.contains("providerReference=SAFE-REF-1"));
        assertTrue(logs.contains("latencyMs=12"));
        assertFalse(logs.contains("test-api-password"));
        assertFalse(logs.contains("secret-token"));
        assertFalse(logs.contains("test-user"));
    }

    @Test
    void i07JsonQuotedCredentialVectorCannotEscapeStructuredLog(CapturedOutput output) throws Exception {
        JSONObject rejected = error("nested={\\\"Token\\\":\\\"I07_TOKEN_SECRET\\\","
                + "\\\"Authorization\\\":\\\"Bearer I07_AUTH_SECRET\\\"}");
        rejected.put("raw_request", "I07_RAW_BODY_SECRET");
        rejected.put("Token", "I07_ROOT_TOKEN_SECRET");
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(response(rejected));
        MchAppConfigContext context = new MchAppConfigContext();
        context.getNormalMchParamsMap().put(CS.IF_CODE.RYO, params);

        ibon.pay(request, payOrder, context);

        String logs = output.getAll();
        assertTrue(logs.contains("event=RYO_CREATE"));
        assertFalse(logs.contains("I07_TOKEN_SECRET"));
        assertFalse(logs.contains("I07_AUTH_SECRET"));
        assertFalse(logs.contains("I07_RAW_BODY_SECRET"));
        assertFalse(logs.contains("I07_ROOT_TOKEN_SECRET"));
    }

    @Test
    void recoveredResponseAmountMismatchIsRejected() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new RyoException(RyoClient.ErrorType.AMBIGUOUS, "read timeout"));
        when(client.queryWithMetadata(params, payOrder.getPayOrderId()))
                .thenReturn(response(successResponse(999)));

        RyoException error = assertThrows(RyoException.class,
                () -> ibon.executeCreate(params, payOrder, new JSONObject()));

        assertEquals(RyoClient.ErrorType.MALFORMED, error.getType());
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
    }

    @Test
    void malformedProviderResponseFailsClosedAfterQueryRecoveryFailure() throws Exception {
        JSONObject malformed = new JSONObject();
        malformed.put("status", "OK");
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(response(malformed));
        when(client.queryWithMetadata(params, payOrder.getPayOrderId())).thenThrow(
                new RyoException(RyoClient.ErrorType.AMBIGUOUS, "query timeout"));

        assertThrows(RyoException.class, () -> ibon.executeCreate(params, payOrder, new JSONObject()));
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        verify(client, times(1)).queryWithMetadata(params, payOrder.getPayOrderId());
    }

    private JSONObject successResponse(long orderAmount) {
        JSONObject response = new JSONObject(true);
        response.put("status", "OK");
        response.put("cust_id", "test-user");
        response.put("cust_order_no", payOrder.getPayOrderId());
        response.put("order_amount", orderAmount);
        response.put("bill_amount", orderAmount + 1);
        response.put("ibon_shopid", "990");
        response.put("ibon_code", "123456");
        response.put("expire_date", "2026-08-19");
        return response;
    }

    private static JSONObject error(String message) {
        JSONObject response = new JSONObject();
        response.put("status", "ERROR");
        response.put("msg", message);
        return response;
    }

    private static CollectResponse response(JSONObject body) {
        return new CollectResponse(body, 200, 12);
    }

    private static final class TestRyoIbon extends RyoIbon {
        private TestRyoIbon(RyoClient client, RyoMchParamsResolver paramsResolver) {
            super(client, paramsResolver);
        }

        @Override
        protected String getNotifyUrl() {
            return "https://merchant.invalid/api/pay/notify/ryo";
        }
    }
}
