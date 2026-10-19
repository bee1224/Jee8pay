package com.jeequan.jeepay.pay.contract;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.utils.ChannelHttpTrace;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** API 測試的上游追蹤：未啟用時不記錄；啟用時密碼、Bearer token、access_token 必須遮蔽。 */
class ChannelHttpTraceTest {

    @Test
    void recordsNothingWhenInactive() {
        ChannelHttpTrace.record("RYO", "POST", "https://x/Token", Map.of(), "password=dummy", 200, "{}", 1, null);
        assertNull(ChannelHttpTrace.end());
    }

    @Test
    void masksCredentials() {
        ChannelHttpTrace.begin();
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer test-only-token");
            ChannelHttpTrace.record("RYO", "POST", "https://x/Token", Map.of(),
                    "grant_type=password&username=cust01&password=test-only-pass", 200,
                    "{\"access_token\":\"test-only-token\",\".expires\":\"x\"}", 5, null);
            ChannelHttpTrace.record("RYO", "POST", "https://x/api/Collect", headers,
                    "{\"cmd\":\"CvsOrderAppend\",\"cust_id\":\"cust01\"}", 200, "{\"status\":\"OK\"}", 7, null);
        } finally {
            JSONArray trace = ChannelHttpTrace.end();
            assertEquals(2, trace.size());
            String all = trace.toJSONString();
            assertFalse(all.contains("test-only-pass"));
            assertFalse(all.contains("test-only-token"));
            JSONObject token = trace.getJSONObject(0);
            assertTrue(token.getString("body").contains("username=cust01&password=****"));
            assertTrue(token.getString("responseBody").contains("\"access_token\":\"****\""));
            assertEquals("Bearer ****", trace.getJSONObject(1).getJSONObject("headers").getString("Authorization"));
            assertTrue(trace.getJSONObject(1).getString("body").contains("CvsOrderAppend"));
        }
        assertFalse(ChannelHttpTrace.active());
    }
}
