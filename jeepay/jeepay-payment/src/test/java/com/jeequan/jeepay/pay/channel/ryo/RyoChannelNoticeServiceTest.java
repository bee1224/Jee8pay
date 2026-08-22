package com.jeequan.jeepay.pay.channel.ryo;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.ResponseException;
import com.jeequan.jeepay.core.model.params.ryo.RyoNormalMchParams;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.service.impl.PayOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RyoChannelNoticeServiceTest {

    private RyoClient client;
    private PayOrderService payOrderService;
    private RyoMchParamsResolver paramsResolver;
    private RyoChannelNoticeService service;
    private RyoNormalMchParams params;
    private MchAppConfigContext context;
    private PayOrder payOrder;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(RyoClient.class);
        payOrderService = mock(PayOrderService.class);
        paramsResolver = mock(RyoMchParamsResolver.class);
        service = new RyoChannelNoticeService(
                new RyoPayOrderQueryService(client, paramsResolver), payOrderService, paramsResolver);

        params = new RyoNormalMchParams();
        params.setEnvironment(RyoNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");
        context = new MchAppConfigContext();
        context.setMchNo("M-TEST");
        context.setAppId("APP-TEST");
        when(paramsResolver.resolve(context)).thenReturn(params);

        payOrder = new PayOrder();
        payOrder.setPayOrderId("P202608120000000001");
        payOrder.setAmount(10_000L);
        payOrder.setState(PayOrder.STATE_ING);
    }

    @Test
    void validPaidApnReturnsSuccessForNativeTransition() throws Exception {
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("7"));
        RyoChannelNoticeService.RyoNoticePayload payload = payload("test-user", "TX-001",
                payOrder.getPayOrderId(), "101", "B", "101");

        ChannelRetMsg result = service.doNotice(new MockHttpServletRequest(), payload, payOrder, context,
                RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);

        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, result.getChannelState());
        assertEquals("TX-001", result.getChannelOrderId());
        assertEquals("OK", result.getResponseEntity().getBody());
        assertEquals("text/plain", result.getResponseEntity().getHeaders().getContentType().toString());
    }

    @Test
    void validDuplicatePaidApnIsFullyRevalidatedAndAcked() throws Exception {
        payOrder.setState(PayOrder.STATE_SUCCESS);
        payOrder.setChannelOrderNo("TX-001");
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("8"));

        ChannelRetMsg result = service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-001", payOrder.getPayOrderId(), "101", "B", "101"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);

        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, result.getChannelState());
        assertEquals("OK", result.getResponseEntity().getBody());
        verify(client, times(1)).query(params, payOrder.getPayOrderId());
        verifyNoInteractions(payOrderService);
    }

    @Test
    void invalidChecksumIsRejectedBeforeProviderQuery() throws Exception {
        JSONObject body = apnBody("test-user", "TX-001", payOrder.getPayOrderId(), "101", "B", "101");
        body.put("checksum", "00000000000000000000000000000000");

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                RyoChannelNoticeService.RyoNoticePayload.parse(body.toJSONString()), payOrder, context,
                RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
        verifyNoInteractions(client);
    }

    @Test
    void wrongAccountIsRejectedBeforeProviderQuery() {
        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("other-account", "TX-001", payOrder.getPayOrderId(), "101", "B", "101"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
        verifyNoInteractions(client);
    }

    @Test
    void wrongLocalOrderIsRejectedBeforeProviderQuery() {
        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-001", "P-OTHER", "101", "B", "101"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
        verifyNoInteractions(client);
    }

    @Test
    void wrongProviderTransactionIsRejected() throws Exception {
        payOrder.setChannelOrderNo("TX-EXPECTED");
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("7"));

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-OTHER", payOrder.getPayOrderId(), "101", "B", "101"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    @Test
    void wrongApnAmountIsRejected() throws Exception {
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("7"));

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-001", payOrder.getPayOrderId(), "102", "B", "101"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    @Test
    void wrongPaidAmountIsRejected() throws Exception {
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("7"));

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-001", payOrder.getPayOrderId(), "101", "B", "100"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    @Test
    void unknownApnStatusAndUnknownQueryStateDoNotTransition() throws Exception {
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("2"));

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-001", payOrder.getPayOrderId(), "101", "Z", null),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    @Test
    void malformedPayloadAndWrongContentTypeAreRejected() {
        MockHttpServletRequest malformed = new MockHttpServletRequest("POST", "/api/pay/notify/ryo");
        malformed.setContentType("application/json");
        malformed.setContent("{bad-json".getBytes());
        assertThrows(ResponseException.class, () -> service.parseParams(malformed, null,
                RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));

        MockHttpServletRequest wrongType = new MockHttpServletRequest("POST", "/api/pay/notify/ryo");
        wrongType.setContentType("application/x-www-form-urlencoded");
        assertThrows(ResponseException.class, () -> service.parseParams(wrongType, null,
                RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    @Test
    void concurrentDuplicateLoserAcksOnlyExactCommittedTerminalIdentity() throws Exception {
        MockHttpServletRequest request = requestWithBody(
                apnBody("test-user", "TX-001", payOrder.getPayOrderId(), "101", "B", "101"));
        service.parseParams(request, null, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);

        PayOrder committed = new PayOrder();
        committed.setPayOrderId(payOrder.getPayOrderId());
        committed.setState(PayOrder.STATE_SUCCESS);
        committed.setChannelOrderNo("TX-001");
        when(payOrderService.getById(payOrder.getPayOrderId())).thenReturn(committed);

        ResponseEntity response = service.doNotifyOrderStateUpdateFail(request);
        assertEquals("OK", response.getBody());

        committed.setChannelOrderNo("TX-OTHER");
        assertEquals("ERROR", service.doNotifyOrderStateUpdateFail(request).getBody());
    }

    @Test
    void parseParamsUsesCanonicalLocalOrderKeyAndSanitizedPayloadString() throws Exception {
        MockHttpServletRequest request = requestWithBody(
                apnBody("test-user", "TX-001", payOrder.getPayOrderId(), "101", "B", "101"));

        org.apache.commons.lang3.tuple.MutablePair<String, Object> parsed = service.parseParams(
                request, null, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);

        assertEquals(payOrder.getPayOrderId(), parsed.getLeft());
        assertFalse(parsed.getRight().toString().contains("checksum"));
        assertFalse(parsed.getRight().toString().contains("pay_amount"));
    }

    @Test
    void closedOrderWithPaidApnReturnsConfirmSuccessForReopen() throws Exception {
        payOrder.setState(PayOrder.STATE_CLOSED);
        payOrder.setChannelOrderNo("TX-REOPEN");
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("4"));

        ChannelRetMsg result = service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-REOPEN", payOrder.getPayOrderId(), "101", "B", "101"),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);

        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, result.getChannelState());
        assertEquals("OK", result.getResponseEntity().getBody());
        verifyNoInteractions(payOrderService);
    }

    @Test
    void closedOrderWithWaitingApnIsRejected() throws Exception {
        payOrder.setState(PayOrder.STATE_CLOSED);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("3"));

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-REOPEN", payOrder.getPayOrderId(), "101", "A", null),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    @Test
    void closedOrderWithExpiredApnIsRejected() throws Exception {
        payOrder.setState(PayOrder.STATE_CLOSED);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("6"));

        assertThrows(ResponseException.class, () -> service.doNotice(new MockHttpServletRequest(),
                payload("test-user", "TX-REOPEN", payOrder.getPayOrderId(), "101", "D", null),
                payOrder, context, RyoChannelNoticeService.NoticeTypeEnum.DO_NOTIFY));
    }

    private RyoChannelNoticeService.RyoNoticePayload payload(String apiId, String transId, String orderNo,
                                                               String amount, String status, String payAmount) {
        return RyoChannelNoticeService.RyoNoticePayload.parse(
                apnBody(apiId, transId, orderNo, amount, status, payAmount).toJSONString());
    }

    private JSONObject apnBody(String apiId, String transId, String orderNo, String amount,
                               String status, String payAmount) {
        String nonce = "1234567890";
        JSONObject body = new JSONObject(true);
        body.put("api_id", apiId);
        body.put("trans_id", transId);
        body.put("order_no", orderNo);
        body.put("amount", amount);
        body.put("status", status);
        body.put("payment_code", 2);
        body.put("nonce", nonce);
        body.put("checksum", RyoKit.checksum(apiId, transId, amount, status, nonce));
        if (payAmount != null) {
            body.put("pay_date", "2026-08-12 12:00:00");
            body.put("pay_amount", payAmount);
        }
        return body;
    }

    private JSONObject queryResponse(String processCode) {
        JSONObject response = new JSONObject(true);
        response.put("status", "OK");
        response.put("cust_id", "test-user");
        response.put("cust_order_no", payOrder.getPayOrderId());
        response.put("order_amount", 100);
        response.put("bill_amount", 101);
        response.put("pay_amount", ("4".equals(processCode) || "7".equals(processCode)
                || "8".equals(processCode)) ? 101 : 0);
        response.put("process_code", processCode);
        return response;
    }

    private static MockHttpServletRequest requestWithBody(JSONObject body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pay/notify/ryo");
        request.setContentType("application/json");
        request.setContent(body.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }
}
