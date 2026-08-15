package com.jeequan.jeepay.pay.channel.ccat;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.components.mq.model.PayOrderMchNotifyMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.pay.channel.IChannelNoticeService;
import com.jeequan.jeepay.pay.ctrl.payorder.ChannelNoticeController;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CcatChannelNoticeFlowTest {

    private CcatClient client;
    private PayOrderService payOrderService;
    private MchNotifyRecordService notifyRecordService;
    private ConfigContextQueryService configContextQueryService;
    private IMQSender mqSender;
    private CcatChannelNoticeService noticeService;
    private ChannelNoticeController controller;
    private CcatNormalMchParams params;
    private MchAppConfigContext context;
    private PayOrder payOrder;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(CcatClient.class);
        payOrderService = mock(PayOrderService.class);
        notifyRecordService = mock(MchNotifyRecordService.class);
        configContextQueryService = mock(ConfigContextQueryService.class);
        mqSender = mock(IMQSender.class);
        CcatMchParamsResolver paramsResolver = mock(CcatMchParamsResolver.class);

        params = new CcatNormalMchParams();
        params.setEnvironment(CcatNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");

        context = new MchAppConfigContext();
        context.setMchNo("M-TEST");
        context.setAppId("APP-TEST");
        context.getNormalMchParamsMap().put(CS.IF_CODE.CCAT, params);
        when(paramsResolver.resolve(context)).thenReturn(params);

        payOrder = new PayOrder();
        payOrder.setPayOrderId("P202608120000000001");
        payOrder.setMchNo("M-TEST");
        payOrder.setAppId("APP-TEST");
        payOrder.setMchOrderNo("MCH-ORDER-001");
        payOrder.setNotifyUrl("https://merchant.example.test/notify");
        payOrder.setAmount(10_000L);
        payOrder.setState(PayOrder.STATE_ING);

        noticeService = new CcatChannelNoticeService(
                new CcatPayOrderQueryService(client, paramsResolver), payOrderService, paramsResolver);

        PayMchNotifyService payMchNotifyService = new PayMchNotifyService();
        ReflectionTestUtils.setField(payMchNotifyService, "mchNotifyRecordService", notifyRecordService);
        ReflectionTestUtils.setField(payMchNotifyService, "configContextQueryService", configContextQueryService);
        ReflectionTestUtils.setField(payMchNotifyService, "mqSender", mqSender);

        PayOrderProcessService payOrderProcessService = new PayOrderProcessService();
        ReflectionTestUtils.setField(payOrderProcessService, "payOrderService", payOrderService);
        ReflectionTestUtils.setField(payOrderProcessService, "payMchNotifyService", payMchNotifyService);
        ReflectionTestUtils.setField(payOrderProcessService, "mqSender", mqSender);

        controller = new ChannelNoticeController();
        ReflectionTestUtils.setField(controller, "payOrderService", payOrderService);
        ReflectionTestUtils.setField(controller, "configContextQueryService", configContextQueryService);
        ReflectionTestUtils.setField(controller, "payMchNotifyService", payMchNotifyService);
        ReflectionTestUtils.setField(controller, "payOrderProcessService", payOrderProcessService);
        when(configContextQueryService.queryMchInfoAndAppInfo("M-TEST", "APP-TEST")).thenReturn(context);
    }

    @Test
    void e06StatusAProcessCode3KeepsIngWithoutChannelOrderOrMerchantNotify() throws Exception {
        when(payOrderService.getById(payOrder.getPayOrderId())).thenReturn(payOrder);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("3"));

        ResponseEntity response = invokeController(apnRequest("TX-E06-WAITING", "A", null));

        assertEquals("OK", response.getBody());
        assertEquals(PayOrder.STATE_ING, payOrder.getState());
        assertNull(payOrder.getChannelOrderNo());
        verify(payOrderService, never()).updateIng2Success(any(), any(), any());
        verify(payOrderService, never()).updateIng2Fail(any(), any(), any(), any(), any());
        verify(notifyRecordService, never()).findByPayOrder(any());
        verify(notifyRecordService, never()).save(any(MchNotifyRecord.class));
        verifyNoInteractions(mqSender);
    }

    @Test
    void terminalSuccessPreservesNativeTransitionChannelOrderAndMerchantNotify() throws Exception {
        PayOrder committed = terminalSuccessOrder("TX-E06-SUCCESS");
        when(payOrderService.getById(payOrder.getPayOrderId())).thenReturn(payOrder, committed);
        when(payOrderService.updateIng2Success(payOrder.getPayOrderId(), "TX-E06-SUCCESS", null)).thenReturn(true);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("7"));
        when(notifyRecordService.findByPayOrder(payOrder.getPayOrderId())).thenReturn(null);
        when(configContextQueryService.queryMchApp("M-TEST", "APP-TEST"))
                .thenReturn(new MchApp().setMchNo("M-TEST").setAppId("APP-TEST").setAppSecret("TEST_ONLY_SECRET"));
        when(notifyRecordService.save(any(MchNotifyRecord.class))).thenAnswer(invocation -> {
            invocation.<MchNotifyRecord>getArgument(0).setNotifyId(1001L);
            return true;
        });

        ResponseEntity response = invokeController(apnRequest("TX-E06-SUCCESS", "B", "101"));

        assertEquals("OK", response.getBody());
        assertEquals(PayOrder.STATE_SUCCESS, committed.getState());
        assertEquals("TX-E06-SUCCESS", committed.getChannelOrderNo());
        verify(payOrderService).updateIng2Success(payOrder.getPayOrderId(), "TX-E06-SUCCESS", null);
        verify(notifyRecordService).save(any(MchNotifyRecord.class));
        verify(mqSender).send(any(PayOrderMchNotifyMQ.class));
    }

    @Test
    void closedOrderPaidApnReopensToSuccessAndNotifies() throws Exception {
        payOrder.setState(PayOrder.STATE_CLOSED);
        payOrder.setChannelOrderNo("TX-CLOSED-REOPEN");
        PayOrder committed = terminalSuccessOrder("TX-CLOSED-REOPEN");
        when(payOrderService.getById(payOrder.getPayOrderId())).thenReturn(payOrder, committed);
        when(payOrderService.updateClosed2Success(payOrder.getPayOrderId(), "TX-CLOSED-REOPEN", null))
                .thenReturn(true);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("7"));
        when(notifyRecordService.findByPayOrder(payOrder.getPayOrderId())).thenReturn(null);
        when(configContextQueryService.queryMchApp("M-TEST", "APP-TEST"))
                .thenReturn(new MchApp().setMchNo("M-TEST").setAppId("APP-TEST").setAppSecret("TEST_ONLY_SECRET"));
        when(notifyRecordService.save(any(MchNotifyRecord.class))).thenAnswer(invocation -> {
            invocation.<MchNotifyRecord>getArgument(0).setNotifyId(2001L);
            return true;
        });

        ResponseEntity response = invokeController(apnRequest("TX-CLOSED-REOPEN", "B", "101"));

        assertEquals("OK", response.getBody());
        assertEquals(PayOrder.STATE_SUCCESS, committed.getState());
        verify(payOrderService).updateClosed2Success(payOrder.getPayOrderId(), "TX-CLOSED-REOPEN", null);
        verify(notifyRecordService).save(any(MchNotifyRecord.class));
        verify(mqSender).send(any(PayOrderMchNotifyMQ.class));
    }

    @Test
    void closedOrderWaitingApnStaysClosedWithoutTransitionOrNotify() throws Exception {
        payOrder.setState(PayOrder.STATE_CLOSED);
        when(payOrderService.getById(payOrder.getPayOrderId())).thenReturn(payOrder);
        when(client.query(params, payOrder.getPayOrderId())).thenReturn(queryResponse("3"));

        ResponseEntity response = invokeController(apnRequest("TX-CLOSED-WAITING", "A", null));

        assertEquals("ERROR", response.getBody());
        verify(payOrderService, never()).updateClosed2Success(any(), any(), any());
        verify(notifyRecordService, never()).save(any(MchNotifyRecord.class));
        verifyNoInteractions(mqSender);
    }

    private ResponseEntity invokeController(MockHttpServletRequest request) {
        try (MockedStatic<SpringBeansUtil> beans = mockStatic(SpringBeansUtil.class)) {
            beans.when(() -> SpringBeansUtil.getBean("ccatChannelNoticeService", IChannelNoticeService.class))
                    .thenReturn(noticeService);
            return controller.doNotify(request, CS.IF_CODE.CCAT, null);
        }
    }

    private MockHttpServletRequest apnRequest(String transId, String status, String payAmount) {
        String amount = "101";
        String nonce = "1234567890";
        JSONObject body = new JSONObject(true);
        body.put("api_id", "test-user");
        body.put("trans_id", transId);
        body.put("order_no", payOrder.getPayOrderId());
        body.put("amount", amount);
        body.put("status", status);
        body.put("payment_code", 2);
        body.put("nonce", nonce);
        body.put("checksum", CcatKit.checksum("test-user", transId, amount, status, nonce));
        if (payAmount != null) {
            body.put("pay_date", "2026-08-12 12:00:00");
            body.put("pay_amount", payAmount);
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pay/notify/ccat");
        request.setContentType("application/json");
        request.setContent(body.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }

    private JSONObject queryResponse(String processCode) {
        JSONObject response = new JSONObject(true);
        response.put("status", "OK");
        response.put("cust_id", "test-user");
        response.put("cust_order_no", payOrder.getPayOrderId());
        response.put("order_amount", 100);
        response.put("bill_amount", 101);
        response.put("pay_amount", "7".equals(processCode) ? 101 : 0);
        response.put("process_code", processCode);
        return response;
    }

    private PayOrder terminalSuccessOrder(String channelOrderNo) {
        PayOrder committed = new PayOrder();
        committed.setPayOrderId(payOrder.getPayOrderId());
        committed.setMchNo(payOrder.getMchNo());
        committed.setAppId(payOrder.getAppId());
        committed.setMchOrderNo(payOrder.getMchOrderNo());
        committed.setNotifyUrl(payOrder.getNotifyUrl());
        committed.setAmount(payOrder.getAmount());
        committed.setState(PayOrder.STATE_SUCCESS);
        committed.setChannelOrderNo(channelOrderNo);
        return committed;
    }
}
