package com.jeequan.jeepay.pay.channel.chi;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.chi.ChiNormalMchParams;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.channel.chi.ChiClient.ChiException;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChiPayOrderQueryServiceTest {

    private ChiClient client;
    private ChiMchParamsResolver paramsResolver;
    private ChiPayOrderQueryService service;
    private ChiNormalMchParams params;
    private PayOrder payOrder;

    @BeforeEach
    void setUp() {
        client = mock(ChiClient.class);
        paramsResolver = mock(ChiMchParamsResolver.class);
        service = new ChiPayOrderQueryService(client, paramsResolver);
        params = new ChiNormalMchParams();
        params.setEnvironment(ChiNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");

        payOrder = new PayOrder();
        payOrder.setPayOrderId("P202608120000000001");
        payOrder.setAmount(10_000L);
        payOrder.setChannelOrderNo("TX-001");
    }

    @Test
    void publicQueryLoadsParamsThroughNativeResolverWhenContextMapIsEmpty() throws Exception {
        MchAppConfigContext context = new MchAppConfigContext();
        context.setMchNo("M-TEST");
        context.setAppId("APP-TEST");
        when(paramsResolver.resolve(context)).thenReturn(params);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response("3"));

        assertEquals(ChannelRetMsg.ChannelState.WAITING, service.query(payOrder, context).getChannelState());
        verify(paramsResolver).resolve(context);
    }

    @Test
    void mapsProcessCodeSevenToSuccess() throws Exception {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, query("7").getState());
    }

    @Test
    void mapsProcessCodeEightToSuccess() throws Exception {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, query("8").getState());
    }

    @Test
    void mapsProcessCodeTwoToNoTransition() throws Exception {
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, query("2").getState());
    }

    @Test
    void mapsUnknownProcessCodeToNoTransition() throws Exception {
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, query("999").getState());
    }

    @Test
    void mapsWaitingAndClosedCodes() throws Exception {
        assertEquals(ChannelRetMsg.ChannelState.WAITING, query("3").getState());
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_FAIL, query("5").getState());
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_FAIL, query("6").getState());
    }

    @Test
    void rejectsLocalOrderAmountMismatch() throws Exception {
        JSONObject response = response("7");
        response.put("order_amount", 99);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response);

        assertThrows(ChiException.class, () -> service.queryValidated(payOrder, params));
    }

    @Test
    void rejectsPaidAmountMismatch() throws Exception {
        JSONObject response = response("7");
        response.put("pay_amount", 100);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response);

        assertThrows(ChiException.class, () -> service.queryValidated(payOrder, params));
    }

    @Test
    void rejectsOrderIdentityMismatch() throws Exception {
        JSONObject response = response("7");
        response.put("cust_order_no", "P-OTHER");
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response);

        assertThrows(ChiException.class, () -> service.queryValidated(payOrder, params));
    }

    @Test
    void rejectsProviderTransactionMismatchWhenQueryEchoesIt() throws Exception {
        JSONObject response = response("7");
        response.put("trans_id", "TX-OTHER");
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response);

        assertThrows(ChiException.class, () -> service.queryValidated(payOrder, params));
    }

    @Test
    void providerErrorReturnsNoTransition() throws Exception {
        JSONObject response = new JSONObject();
        response.put("status", "ERROR");
        response.put("msg", "query rejected");
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response);

        assertThrows(ChiException.class, () -> service.queryValidated(payOrder, params));
    }

    private ChiPayOrderQueryService.ValidatedQuery query(String processCode) throws Exception {
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(response(processCode));
        return service.queryValidated(payOrder, params);
    }

    private JSONObject response(String processCode) {
        JSONObject response = new JSONObject(true);
        response.put("status", "OK");
        response.put("cust_id", "test-user");
        response.put("cust_order_no", payOrder.getPayOrderId());
        response.put("order_amount", 100);
        response.put("bill_amount", 101);
        response.put("pay_amount", isPaid(processCode) ? 101 : 0);
        response.put("process_code", processCode);
        return response;
    }

    private static boolean isPaid(String processCode) {
        return "4".equals(processCode) || "7".equals(processCode) || "8".equals(processCode);
    }
}
