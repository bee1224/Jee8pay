package com.jeequan.jeepay.pay.contract;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.utils.JeepayKit;
import com.jeequan.jeepay.pay.mq.PayOrderMchNotifyMQReceiver;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantUatContractTest {

    private static final String SECRET = "SYNTHETIC_SECRET_ONLY_FOR_D01_VECTOR_20260813";
    private HttpServer callbackServer;

    @AfterEach
    void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
        }
    }

    @Test
    void createVectorMatchesNativeJeepaySignature() {
        JSONObject request = new JSONObject(true);
        request.put("version", "1.0");
        request.put("signType", "MD5");
        request.put("reqTime", "1786581000000");
        request.put("mchNo", "M_SYNTHETIC_UAT");
        request.put("appId", "APP_SYNTHETIC_UAT");
        request.put("mchOrderNo", "UAT-SYNTH-0001");
        request.put("wayCode", "RYO_IBON");
        request.put("amount", 4000L);
        request.put("currency", "TWD");
        request.put("subject", "RYO ibon UAT");
        request.put("body", "Synthetic test vector");
        request.put("notifyUrl", "https://merchant.example.test/callback/jeepay");
        request.put("expiredTime", 604800);
        request.put("channelExtra", "{\"payerName\":\"王小明\",\"payerPostcode\":\"100\","
                + "\"payerAddress\":\"台北市測試路1號\",\"payerMobile\":\"0900000000\","
                + "\"payerEmail\":\"uat@example.test\"}");

        String expected = "A4E1A33781D36672F66999C99283DADD";
        assertEquals(expected, JeepayKit.getSign(request, SECRET));

        request.put("amount", 4100L);
        assertNotEquals(expected, JeepayKit.getSign(request, SECRET));
        request.put("amount", 4000L);
        request.put("mchOrderNo", "UAT-SYNTH-MODIFIED");
        assertNotEquals(expected, JeepayKit.getSign(request, SECRET));
    }

    @Test
    void notifyVectorMatchesNativeJeepaySignatureAndRejectsWrongSecret() {
        JSONObject payload = notifyPayload();
        String expected = "8370535A47F225E2986FA91BAF9BCC4C";

        assertEquals(expected, JeepayKit.getSign(payload, SECRET));
        assertNotEquals(expected, JeepayKit.getSign(payload, "WRONG_SYNTHETIC_SECRET"));

        payload.put("amount", 4100L);
        assertNotEquals(expected, JeepayKit.getSign(payload, SECRET));
        payload.put("amount", 4000L);
        payload.put("payOrderId", "P_SYNTHETIC_MODIFIED");
        assertNotEquals(expected, JeepayKit.getSign(payload, SECRET));
    }

    @Test
    void duplicateLogicalNotifyDoesNotCreateOrSendAnotherRecord() {
        MchNotifyRecordService recordService = mock(MchNotifyRecordService.class);
        ConfigContextQueryService configService = mock(ConfigContextQueryService.class);
        IMQSender mqSender = mock(IMQSender.class);
        PayMchNotifyService service = new PayMchNotifyService();
        ReflectionTestUtils.setField(service, "mchNotifyRecordService", recordService);
        ReflectionTestUtils.setField(service, "configContextQueryService", configService);
        ReflectionTestUtils.setField(service, "mqSender", mqSender);

        PayOrder order = new PayOrder();
        order.setPayOrderId("P_SYNTHETIC_0001");
        order.setNotifyUrl("https://merchant.example.test/callback/jeepay");
        when(recordService.findByPayOrder(order.getPayOrderId())).thenReturn(new MchNotifyRecord());

        service.payOrderNotify(order);

        verify(recordService, never()).save(any());
        verifyNoInteractions(configService, mqSender);
    }

    @Test
    void exactSuccessAckMarksNotifyCompleteWithoutRetry() throws Exception {
        callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        callbackServer.createContext("/notify", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] response = "SUCCESS".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        callbackServer.start();

        MchNotifyRecordService recordService = mock(MchNotifyRecordService.class);
        PayOrderService payOrderService = mock(PayOrderService.class);
        IMQSender mqSender = mock(IMQSender.class);
        PayOrderMchNotifyMQReceiver receiver = new PayOrderMchNotifyMQReceiver();
        ReflectionTestUtils.setField(receiver, "mchNotifyRecordService", recordService);
        ReflectionTestUtils.setField(receiver, "payOrderService", payOrderService);
        ReflectionTestUtils.setField(receiver, "mqSender", mqSender);

        MchNotifyRecord record = new MchNotifyRecord();
        record.setNotifyId(1001L);
        record.setOrderId("P_SYNTHETIC_0001");
        record.setOrderType(MchNotifyRecord.TYPE_PAY_ORDER);
        record.setNotifyUrl("http://127.0.0.1:" + callbackServer.getAddress().getPort()
                + "/notify?payOrderId=P_SYNTHETIC_0001");
        record.setNotifyCount(0);
        record.setNotifyCountLimit(6);
        record.setState(MchNotifyRecord.STATE_ING);
        when(recordService.getById(1001L)).thenReturn(record);

        receiver.receive(new com.jeequan.jeepay.components.mq.model.PayOrderMchNotifyMQ.MsgPayload(1001L));

        verify(payOrderService).updateNotifySent("P_SYNTHETIC_0001");
        verify(recordService).updateNotifyResult(1001L, MchNotifyRecord.STATE_SUCCESS, "SUCCESS");
        verifyNoInteractions(mqSender);
    }

    private static JSONObject notifyPayload() {
        JSONObject payload = new JSONObject(true);
        payload.put("payOrderId", "P_SYNTHETIC_0001");
        payload.put("mchNo", "M_SYNTHETIC_UAT");
        payload.put("appId", "APP_SYNTHETIC_UAT");
        payload.put("mchOrderNo", "UAT-SYNTH-0001");
        payload.put("ifCode", "ryo");
        payload.put("wayCode", "RYO_IBON");
        payload.put("amount", 4000L);
        payload.put("currency", "TWD");
        payload.put("state", 2);
        payload.put("clientIp", "203.0.113.10");
        payload.put("subject", "RYO ibon UAT");
        payload.put("body", "Synthetic test vector");
        payload.put("channelOrderNo", "SYNTHETIC_PROVIDER_REFERENCE");
        payload.put("extParam", "TRACE-SYNTHETIC");
        payload.put("successTime", 1786581600000L);
        payload.put("createdAt", 1786581000000L);
        payload.put("reqTime", 1786581601000L);
        return payload;
    }
}
