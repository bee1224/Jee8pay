package com.jeequan.jeepay.service.sign;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.MlDsaKit;
import com.jeequan.jeepay.service.impl.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 後台（manager、merchant 服務）代商戶呼叫支付服務的商戶 API，例如後台退款、團長的渠道測試（ADR-0013）。
 *
 * 取代原本的 jeepay-sdk-java（MD5）：平台不持有商戶私鑰，因此以平台內部金鑰簽名，
 * signType 為 PLATFORM-ML-DSA-65，支付服務以內部公鑰驗證；回應再以平台簽名公鑰驗證。
 */
@Slf4j
@Component
public class PayApiClient {

    @Autowired private SignKeyStore signKeyStore;
    @Autowired private SysConfigService sysConfigService;

    /** 支付服務的內部網址（容器網路內直連，例如 http://payment:9216）；未設定時沿用系統配置的 paySiteUrl **/
    @Value("${isys.pay-internal-url:}")
    private String payInternalUrl;

    /**
     * 呼叫商戶 API，成功時回傳 data；失敗（code 不為 0、網路錯誤、回應簽名不符）一律拋 BizException。
     *
     * @param path      例如 /api/pay/unifiedOrder
     * @param bizParams 業務參數（mchNo、appId 等），不需放 version、signType、reqTime、sign
     */
    public JSONObject call(String path, Map<String, Object> bizParams) {
        JSONObject req = signedRequest(bizParams);
        String baseUrl = baseUrl();
        String body;
        try {
            body = HttpRequest.post(baseUrl + path).body(req.toJSONString(), "application/json").timeout(20000).execute().body();
        } catch (Exception e) {
            log.error("呼叫支付服務失敗 path={}", path, e);
            throw new BizException("無法連線到支付服務，請稍後再試");
        }

        JSONObject res = JSON.parseObject(body);
        if (res == null || res.getIntValue("code") != 0) {
            throw new BizException(res == null ? "支付服務沒有回應" : res.getString("msg"));
        }
        JSONObject data = res.getJSONObject("data");
        if (data != null && !MlDsaKit.verify(data, res.getString("sign"), signKeyStore.platformPublicKey())) {
            throw new BizException("支付服務回應的簽名驗證失敗");
        }
        return data;
    }

    /**
     * 管理後台「API 測試」用：送出與 call 相同的請求，但不論成敗都把完整的請求、原始回應、
     * 回應驗簽結果與支付服務打到上游渠道的 HTTP 往來（已遮蔽密碼與 token）一併回傳，不拋業務錯誤。
     */
    public JSONObject callTraced(String path, Map<String, Object> bizParams) {
        JSONObject req = signedRequest(bizParams);
        String url = baseUrl() + path;
        String reqBody = req.toJSONString();
        JSONObject result = new JSONObject(true);
        result.put("method", "POST");
        result.put("url", url);
        result.put("requestHeaders", Map.of("Content-Type", "application/json"));
        result.put("requestBody", reqBody);
        result.put("canonical", MlDsaKit.canonical(req));

        long startedAt = System.currentTimeMillis();
        try (HttpResponse resp = HttpRequest.post(url).header(TRACE_HEADER, "1")
                .body(reqBody, "application/json").timeout(60000).execute()) {
            result.put("latencyMillis", System.currentTimeMillis() - startedAt);
            result.put("httpStatus", resp.getStatus());
            result.put("responseHeaders", Map.of("Content-Type", StringUtils.defaultString(resp.header("Content-Type"))));
            String body = resp.body();
            result.put("responseBody", body);
            String trace = resp.header(TRACE_RESULT_HEADER);
            result.put("channelTrace", StringUtils.isBlank(trace) ? new JSONArray()
                    : JSON.parseArray(new String(Base64.getDecoder().decode(trace), StandardCharsets.UTF_8)));

            JSONObject res = null;
            try {
                res = JSON.parseObject(body);
            } catch (Exception ignored) {
                result.put("responseSignCheck", "回應不是 JSON");
            }
            if (res != null) {
                JSONObject data = res.getJSONObject("data");
                if (data == null) {
                    result.put("responseSignCheck", "回應沒有 data，不需驗簽");
                } else {
                    result.put("responseSignCheck", MlDsaKit.verify(data, res.getString("sign"), signKeyStore.platformPublicKey())
                            ? "通過（以平台簽名公鑰驗證 data）" : "失敗：回應簽名與 data 不符");
                }
            }
        } catch (Exception e) {
            log.warn("API 測試呼叫支付服務失敗 path={}", path, e);
            result.put("latencyMillis", System.currentTimeMillis() - startedAt);
            result.put("error", "無法連線到支付服務：" + e.getMessage());
        }
        return result;
    }

    private static final String TRACE_HEADER = "X-Jeepay-Trace";
    private static final String TRACE_RESULT_HEADER = "X-Jeepay-Channel-Trace";

    private JSONObject signedRequest(Map<String, Object> bizParams) {
        JSONObject req = new JSONObject(true);
        req.putAll(bizParams);
        req.put("version", "1.0");
        req.put("signType", MlDsaKit.SIGN_TYPE_PLATFORM);
        req.put("reqTime", String.valueOf(System.currentTimeMillis()));
        req.put("sign", MlDsaKit.sign(req, signKeyStore.internalPrivateKey()));
        return req;
    }

    private String baseUrl() {
        return StringUtils.isNotBlank(payInternalUrl) ? payInternalUrl : sysConfigService.getDBApplicationConfig().getPaySiteUrl();
    }
}
