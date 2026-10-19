package com.jeequan.jeepay.pay.contract;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.MlDsaKit;
import com.jeequan.jeepay.pay.mq.PayOrderMchNotifyMQReceiver;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.sign.SignKeyStore;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantUatContractTest {

    private HttpServer callbackServer;

    @AfterEach
    void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
        }
    }

    /** 測試用金鑰每次執行時產生，不放任何固定金鑰（ADR-0013） **/
    private static KeyPair newKeyPair() throws Exception {
        return KeyPairGenerator.getInstance("ML-DSA-65").generateKeyPair();
    }

    @Test
    void createCanonicalStringMatchesPublishedVector() {
        JSONObject request = createRequest();
        // 商戶文件公布的待簽字串範例：依參數名稱排序、去掉 sign、無結尾 &
        String expected = "amount=4000&appId=APP_SYNTHETIC_UAT&body=Synthetic test vector"
                + "&channelExtra={\"payerName\":\"王小明\",\"payerPostcode\":\"100\",\"payerAddress\":\"台北市測試路1號\","
                + "\"payerMobile\":\"0900000000\",\"payerEmail\":\"uat@example.test\"}"
                + "&currency=TWD&expiredTime=604800&mchNo=M_SYNTHETIC_UAT&mchOrderNo=UAT-SYNTH-0001"
                + "&notifyUrl=https://merchant.example.test/callback/jeepay&reqTime=1786581000000"
                + "&signType=ML-DSA-65&subject=RYO ibon UAT&version=1.0&wayCode=RYO_IBON";
        assertEquals(expected, MlDsaKit.canonical(request));

        request.put("sign", "IGNORED");
        request.put("clientIp", "");
        assertEquals(expected, MlDsaKit.canonical(request));
    }

    @Test
    void createSignatureVerifiesAndRejectsTamperingOrWrongKey() throws Exception {
        KeyPair merchant = newKeyPair();
        JSONObject request = createRequest();
        String sign = MlDsaKit.sign(request, merchant.getPrivate());
        PublicKey merchantPublic = MlDsaKit.parsePublicKey(MlDsaKit.toPem(merchant.getPublic()));

        assertTrue(MlDsaKit.verify(request, sign, merchantPublic));
        assertFalse(MlDsaKit.verify(request, sign, newKeyPair().getPublic()));
        request.put("amount", 4100L);
        assertFalse(MlDsaKit.verify(request, sign, merchantPublic));
        request.put("amount", 4000L);
        assertFalse(MlDsaKit.verify(request, "not-base64!", merchantPublic));
        assertFalse(MlDsaKit.verify(request, null, merchantPublic));
    }

    @Test
    void onlyMlDsa65PublicKeysAreAccepted() throws Exception {
        String ml44 = MlDsaKit.toPem(KeyPairGenerator.getInstance("ML-DSA-44").generateKeyPair().getPublic());
        assertThrows(BizException.class, () -> MlDsaKit.parsePublicKey(ml44));
        String rsa = MlDsaKit.toPem(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic());
        assertThrows(BizException.class, () -> MlDsaKit.parsePublicKey(rsa));
    }

    @Test
    void notifySignatureSurvivesUrlStorageAndFormPost() throws Exception {
        KeyPair platform = newKeyPair();
        SignKeyStore keyStore = new SignKeyStore();
        ReflectionTestUtils.setField(keyStore, "platformPrivateKey", platform.getPrivate());
        PayMchNotifyService service = new PayMchNotifyService();
        ReflectionTestUtils.setField(service, "signKeyStore", keyStore);

        PayOrder order = new PayOrder();
        order.setPayOrderId("P_SYNTHETIC_0001");
        order.setMchNo("M_SYNTHETIC_UAT");
        order.setAppId("APP_SYNTHETIC_UAT");
        order.setMchOrderNo("UAT-SYNTH-0001");
        order.setAmount(4000L);
        order.setCurrency("TWD");
        order.setState((byte) 2);
        order.setSubject("RYO ibon UAT + 特殊字元 &=?");
        order.setNotifyUrl("https://merchant.example.test/callback/jeepay");

        // 與 PayOrderMchNotifyMQReceiver 相同：從儲存的網址解出參數，再以表單 POST 給商戶
        String notifyUrl = service.createNotifyUrl(order);
        Map<String, String> posted = HttpUtil.decodeParamMap(notifyUrl, CharsetUtil.CHARSET_UTF_8);

        assertEquals(MlDsaKit.SIGN_TYPE, posted.get("signType"));
        assertEquals("RYO ibon UAT + 特殊字元 &=?", posted.get("subject"));
        assertTrue(MlDsaKit.verify(posted, posted.get("sign"), platform.getPublic()));
        posted.put("amount", "4100");
        assertFalse(MlDsaKit.verify(posted, posted.get("sign"), platform.getPublic()));
    }

    private static JSONObject createRequest() {
        JSONObject request = new JSONObject(true);
        request.put("version", "1.0");
        request.put("signType", "ML-DSA-65");
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
        return request;
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
}
