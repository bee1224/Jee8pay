package com.jeequan.jeepay.pay.channel.ccat.payway;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient.CcatException;
import com.jeequan.jeepay.pay.channel.ccat.CcatIbonOrderRS;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CcatIbonTest {

    private CcatClient client;
    private CcatIbon ibon;
    private CcatNormalMchParams params;
    private PayOrder payOrder;
    private UnifiedOrderRQ request;

    @BeforeEach
    void setUp() {
        client = mock(CcatClient.class);
        ibon = new TestCcatIbon(client);
        params = new CcatNormalMchParams();
        params.setEnvironment(CcatNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");

        payOrder = new PayOrder();
        payOrder.setPayOrderId("P202608120000000001");
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
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(successResponse(100));
        MchAppConfigContext context = new MchAppConfigContext();
        context.getNormalMchParamsMap().put(CS.IF_CODE.CCAT, params);

        CcatIbonOrderRS result = (CcatIbonOrderRS) ibon.pay(request, payOrder, context);

        assertEquals(ChannelRetMsg.ChannelState.WAITING, result.getChannelRetMsg().getChannelState());
        assertTrue(result.getChannelRetMsg().isNeedQuery());
        assertEquals("123456", result.getIbonCode());
        assertEquals("990", result.getIbonShopId());
        assertEquals(101, result.getBillAmount());
        assertTrue(result.buildPayData().contains("990123456"));
    }

    @Test
    void deterministicBusinessErrorDoesNotQueryOrRetry() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(error("amount rejected"));

        CcatException error = assertThrows(CcatException.class,
                () -> ibon.executeCreate(params, payOrder, new JSONObject()));

        assertEquals(CcatClient.ErrorType.BUSINESS, error.getType());
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        verify(client, never()).query(any(), anyString());
    }

    @Test
    void authenticationErrorDoesNotQueryOrRetry() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new CcatException(CcatClient.ErrorType.AUTHENTICATION, "token rejected"));

        CcatException error = assertThrows(CcatException.class,
                () -> ibon.executeCreate(params, payOrder, new JSONObject()));

        assertEquals(CcatClient.ErrorType.AUTHENTICATION, error.getType());
        verify(client, never()).query(any(), anyString());
    }

    @Test
    void timeoutThenQueryFoundRecoversWithoutSecondAppend() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new CcatException(CcatClient.ErrorType.AMBIGUOUS, "read timeout"));
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(successResponse(100));

        JSONObject recovered = ibon.executeCreate(params, payOrder, new JSONObject());

        assertEquals("OK", recovered.getString("status"));
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        verify(client, times(1)).query(params, payOrder.getPayOrderId());
    }

    @Test
    void timeoutThenExactNotFoundRetriesAppendOnceWithSameRequest() throws Exception {
        JSONObject sameRequest = new JSONObject();
        sameRequest.put("cust_order_no", payOrder.getPayOrderId());
        when(client.append(params, sameRequest))
                .thenThrow(new CcatException(CcatClient.ErrorType.AMBIGUOUS, "read timeout"))
                .thenReturn(successResponse(100));
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(error(CcatIbon.QUERY_NOT_FOUND_MESSAGE));

        JSONObject recovered = ibon.executeCreate(params, payOrder, sameRequest);

        assertEquals("OK", recovered.getString("status"));
        verify(client, times(2)).append(params, sameRequest);
        verify(client, times(1)).query(params, payOrder.getPayOrderId());
    }

    @Test
    void retryRaceBusinessResponseQueriesAgainAndRecovers() throws Exception {
        when(client.append(eq(params), any(JSONObject.class)))
                .thenThrow(new CcatException(CcatClient.ErrorType.AMBIGUOUS, "read timeout"))
                .thenReturn(error("same-key order already exists"));
        when(client.query(params, payOrder.getPayOrderId()))
                .thenReturn(error(CcatIbon.QUERY_NOT_FOUND_MESSAGE))
                .thenReturn(successResponse(100));

        JSONObject recovered = ibon.executeCreate(params, payOrder, new JSONObject());

        assertEquals("OK", recovered.getString("status"));
        verify(client, times(2)).append(eq(params), any(JSONObject.class));
        verify(client, times(2)).query(params, payOrder.getPayOrderId());
    }

    @Test
    void retryTimeoutQueriesAgainAndRecoversWithoutThirdAppend() throws Exception {
        when(client.append(eq(params), any(JSONObject.class)))
                .thenThrow(new CcatException(CcatClient.ErrorType.AMBIGUOUS, "first timeout"))
                .thenThrow(new CcatException(CcatClient.ErrorType.AMBIGUOUS, "retry timeout"));
        when(client.query(params, payOrder.getPayOrderId()))
                .thenReturn(error(CcatIbon.QUERY_NOT_FOUND_MESSAGE))
                .thenReturn(successResponse(100));

        JSONObject recovered = ibon.executeCreate(params, payOrder, new JSONObject());

        assertEquals("OK", recovered.getString("status"));
        verify(client, times(2)).append(eq(params), any(JSONObject.class));
        verify(client, times(2)).query(params, payOrder.getPayOrderId());
    }

    @Test
    void recoveredResponseAmountMismatchIsRejected() throws Exception {
        when(client.append(eq(params), any(JSONObject.class))).thenThrow(
                new CcatException(CcatClient.ErrorType.AMBIGUOUS, "read timeout"));
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(successResponse(999));

        CcatException error = assertThrows(CcatException.class,
                () -> ibon.executeCreate(params, payOrder, new JSONObject()));

        assertEquals(CcatClient.ErrorType.MALFORMED, error.getType());
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
    }

    @Test
    void malformedProviderResponseFailsClosedAfterQueryRecoveryFailure() throws Exception {
        JSONObject malformed = new JSONObject();
        malformed.put("status", "OK");
        when(client.append(eq(params), any(JSONObject.class))).thenReturn(malformed);
        when(client.query(params, payOrder.getPayOrderId())).thenThrow(
                new CcatException(CcatClient.ErrorType.AMBIGUOUS, "query timeout"));

        assertThrows(CcatException.class, () -> ibon.executeCreate(params, payOrder, new JSONObject()));
        verify(client, times(1)).append(eq(params), any(JSONObject.class));
        verify(client, times(1)).query(params, payOrder.getPayOrderId());
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

    private static final class TestCcatIbon extends CcatIbon {
        private TestCcatIbon(CcatClient client) {
            super(client);
        }

        @Override
        protected String getNotifyUrl() {
            return "https://merchant.invalid/api/pay/notify/ccat";
        }
    }
}
