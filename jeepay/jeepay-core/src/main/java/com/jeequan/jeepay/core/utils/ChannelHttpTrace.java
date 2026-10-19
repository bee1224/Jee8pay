package com.jeequan.jeepay.core.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 管理後台「API 測試」用：記錄同一個請求執行緒內，平台送給上游渠道的 HTTP 請求與回應。
 * 只有支付服務收到帶追蹤標頭的請求時才會啟用（是否帶回由 ChannelTraceFilter 判斷內部簽名）；平時 record 不做任何事。
 * 密碼、Bearer token、access_token 一律遮蔽後才記錄。
 */
public final class ChannelHttpTrace {

    private static final ThreadLocal<JSONArray> CURRENT = new ThreadLocal<>();
    private static final Pattern FORM_SECRET = Pattern.compile("(?i)((?:^|&)(?:password|client_secret)=)[^&]*");
    private static final Pattern JSON_SECRET = Pattern.compile("(?i)(\"(?:access_token|refresh_token|password|apiPassword)\"\\s*:\\s*\")[^\"]*");
    private static final int MAX_BODY = 8000;

    private ChannelHttpTrace() {
    }

    public static void begin() {
        CURRENT.set(new JSONArray());
    }

    /** 取出並結束本次追蹤；沒有啟用時回傳 null */
    public static JSONArray end() {
        JSONArray result = CURRENT.get();
        CURRENT.remove();
        return result;
    }

    public static boolean active() {
        return CURRENT.get() != null;
    }

    /**
     * @param status 上游 HTTP 狀態；連線失敗時傳 null 並在 error 說明
     */
    public static void record(String provider, String method, String url, Map<String, String> headers,
                              String body, Integer status, String responseBody, long latencyMillis, String error) {
        JSONArray list = CURRENT.get();
        if (list == null) {
            return;
        }
        Map<String, String> safeHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((k, v) -> safeHeaders.put(k, "authorization".equalsIgnoreCase(k) ? maskAuthorization(v) : v));
        }
        JSONObject item = new JSONObject(true);
        item.put("provider", provider);
        item.put("method", method);
        item.put("url", url);
        item.put("headers", safeHeaders);
        item.put("body", mask(body));
        item.put("status", status);
        item.put("responseBody", mask(responseBody));
        item.put("latencyMillis", latencyMillis);
        item.put("error", error);
        item.put("time", System.currentTimeMillis());
        list.add(item);
    }

    static String maskAuthorization(String value) {
        if (value == null) {
            return null;
        }
        int space = value.indexOf(' ');
        return (space > 0 ? value.substring(0, space + 1) : "") + "****";
    }

    static String mask(String body) {
        if (body == null) {
            return null;
        }
        String masked = FORM_SECRET.matcher(body).replaceAll("$1****");
        masked = JSON_SECRET.matcher(masked).replaceAll("$1****");
        return masked.length() <= MAX_BODY ? masked : masked.substring(0, MAX_BODY) + "…（已截斷）";
    }
}
