package com.jeequan.jeepay.pay.config;

import com.alibaba.fastjson.JSONArray;
import com.jeequan.jeepay.core.utils.ChannelHttpTrace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 管理後台「API 測試」：請求帶 X-Jeepay-Trace: 1 時，記錄這次請求打到上游渠道的 HTTP 往來，
 * 以 Base64 JSON 放在回應標頭 X-Jeepay-Channel-Trace 帶回；回應本文維持原樣。
 * 只有以平台內部金鑰簽名的請求（ApiController 驗簽通過後標記 INTERNAL_SIGNED_ATTR）才會帶回，
 * 外部商戶即使帶了標頭也拿不到任何追蹤內容。
 */
@Component
public class ChannelTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Jeepay-Trace";
    public static final String RESULT_HEADER = "X-Jeepay-Channel-Trace";
    public static final String INTERNAL_SIGNED_ATTR = "jeepay.internalSigned";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"1".equals(request.getHeader(TRACE_HEADER));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        ChannelHttpTrace.begin();
        try {
            chain.doFilter(request, wrapper);
        } finally {
            JSONArray trace = ChannelHttpTrace.end();
            if (trace != null && Boolean.TRUE.equals(request.getAttribute(INTERNAL_SIGNED_ATTR))) {
                wrapper.setHeader(RESULT_HEADER, Base64.getEncoder()
                        .encodeToString(trace.toJSONString().getBytes(StandardCharsets.UTF_8)));
            }
            wrapper.copyBodyToResponse();
        }
    }
}
