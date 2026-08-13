package com.jeequan.jeepay.pay.channel.ccat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 最小 CCAT Token / Collect HTTPS client。 */
@Component
public class CcatClient {

    static final String TEST_BASE_URL = "https://test.4128888card.com.tw/app/";
    static final String PRODUCTION_BASE_URL = "https://cocs.4128888card.com.tw/";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration TOKEN_EARLY_EXPIRY = Duration.ofSeconds(60);

    private final Transport transport;
    private final Clock clock;
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private final Map<String, Object> tokenLocks = new ConcurrentHashMap<>();

    @Autowired
    public CcatClient() {
        this(new JavaHttpTransport(), Clock.systemUTC());
    }

    CcatClient(Transport transport, Clock clock) {
        this.transport = transport;
        this.clock = clock;
    }

    public CollectResponse append(CcatNormalMchParams params, JSONObject payload) throws CcatException {
        return collect(params, payload);
    }

    public JSONObject query(CcatNormalMchParams params, String payOrderId) throws CcatException {
        return queryWithMetadata(params, payOrderId).getBody();
    }

    public CollectResponse queryWithMetadata(CcatNormalMchParams params, String payOrderId) throws CcatException {
        JSONObject payload = new JSONObject(true);
        payload.put("cmd", "CvsOrderQuery");
        payload.put("cust_id", requireConfig(params).getCustId());
        payload.put("cust_order_no", payOrderId);
        return collect(params, payload);
    }

    public static String resolveBaseUrl(String environment) throws CcatException {
        if (CcatNormalMchParams.ENVIRONMENT_TEST.equals(environment)) {
            return TEST_BASE_URL;
        }
        if (CcatNormalMchParams.ENVIRONMENT_PRODUCTION.equals(environment)) {
            return PRODUCTION_BASE_URL;
        }
        throw new CcatException(ErrorType.CONFIGURATION, "CCAT environment must be TEST or PRODUCTION");
    }

    private CollectResponse collect(CcatNormalMchParams params, JSONObject payload) throws CcatException {
        long startedAt = System.nanoTime();
        CcatNormalMchParams checked = requireConfig(params);
        String baseUrl = resolveBaseUrl(checked.getEnvironment());
        String token = token(checked, baseUrl);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Authorization", "Bearer " + token);

        TransportResponse response;
        try {
            response = transport.post(baseUrl + "api/Collect", headers, payload.toJSONString(), REQUEST_TIMEOUT);
        } catch (IOException e) {
            throw new CcatException(ErrorType.AMBIGUOUS, "CCAT Collect transport failure", e,
                    null, elapsedMillis(startedAt), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CcatException(ErrorType.AMBIGUOUS, "CCAT Collect interrupted", e,
                    null, elapsedMillis(startedAt), null);
        }

        long latencyMillis = elapsedMillis(startedAt);
        JSONObject providerFields = parseProviderFields(response.body);
        if (response.statusCode == 401 || response.statusCode == 403) {
            throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Collect authentication failed", null,
                    response.statusCode, latencyMillis, providerFields);
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
            ErrorType type = isDeterministicHttpRejection(response.statusCode)
                    ? ErrorType.BUSINESS : ErrorType.AMBIGUOUS;
            throw new CcatException(type, "CCAT Collect HTTP " + response.statusCode, null,
                    response.statusCode, latencyMillis, providerFields);
        }
        try {
            return new CollectResponse(
                    parseJson(response.body, ErrorType.MALFORMED, "CCAT Collect response is malformed"),
                    response.statusCode, latencyMillis);
        } catch (CcatException e) {
            throw new CcatException(e.getType(), e.getMessage(), e,
                    response.statusCode, latencyMillis, providerFields);
        }
    }

    private String token(CcatNormalMchParams params, String baseUrl) throws CcatException {
        String cacheKey = baseUrl + '|' + params.getCustId();
        CachedToken cached = tokenCache.get(cacheKey);
        Instant now = clock.instant();
        if (cached != null && now.isBefore(cached.usableUntil)) {
            return cached.value;
        }

        Object lock = tokenLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        synchronized (lock) {
            cached = tokenCache.get(cacheKey);
            now = clock.instant();
            if (cached != null && now.isBefore(cached.usableUntil)) {
                return cached.value;
            }

            String form = "grant_type=password&username=" + encode(params.getCustId())
                    + "&password=" + encode(params.getApiPassword());
            Map<String, String> headers = Collections.singletonMap(
                    "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            TransportResponse response;
            try {
                response = transport.post(baseUrl + "Token", headers, form, REQUEST_TIMEOUT);
            } catch (IOException e) {
                throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Token transport failure", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Token interrupted", e);
            }

            if (response.statusCode < 200 || response.statusCode >= 300) {
                throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Token HTTP " + response.statusCode);
            }
            JSONObject body = parseJson(response.body, ErrorType.AUTHENTICATION, "CCAT Token response is malformed");
            String accessToken = body.getString("access_token");
            String expires = body.getString(".expires");
            if (StringUtils.isAnyBlank(accessToken, expires) || StringUtils.isNotBlank(body.getString("error"))) {
                throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Token response rejected");
            }

            Instant expiresAt;
            try {
                expiresAt = ZonedDateTime.parse(expires, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            } catch (DateTimeParseException e) {
                throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Token expiry is malformed", e);
            }
            Instant usableUntil = expiresAt.minus(TOKEN_EARLY_EXPIRY);
            if (!usableUntil.isAfter(now)) {
                throw new CcatException(ErrorType.AUTHENTICATION, "CCAT Token is already expired");
            }
            tokenCache.put(cacheKey, new CachedToken(accessToken, usableUntil));
            return accessToken;
        }
    }

    private static CcatNormalMchParams requireConfig(CcatNormalMchParams params) throws CcatException {
        if (params == null || StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw new CcatException(ErrorType.CONFIGURATION, "CCAT merchant configuration is incomplete");
        }
        resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static JSONObject parseJson(String body, ErrorType errorType, String message) throws CcatException {
        if (StringUtils.isBlank(body)) {
            throw new CcatException(errorType, message);
        }
        try {
            JSONObject result = JSON.parseObject(body);
            if (result == null) {
                throw new JSONException("null JSON object");
            }
            return result;
        } catch (JSONException e) {
            throw new CcatException(errorType, message, e);
        }
    }

    private static JSONObject parseProviderFields(String body) {
        try {
            JSONObject source = JSON.parseObject(body);
            if (source == null) {
                return null;
            }
            JSONObject allowed = new JSONObject(true);
            copyIfPresent(source, allowed, "status");
            copyIfPresent(source, allowed, "process_code");
            copyIfPresent(source, allowed, "result_code");
            copyIfPresent(source, allowed, "code");
            copyIfPresent(source, allowed, "msg");
            copyIfPresent(source, allowed, "message");
            copyIfPresent(source, allowed, "error");
            copyIfPresent(source, allowed, "error_description");
            copyIfPresent(source, allowed, "trans_id");
            copyIfPresent(source, allowed, "transaction_id");
            return allowed;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void copyIfPresent(JSONObject source, JSONObject target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private static boolean isDeterministicHttpRejection(int statusCode) {
        return statusCode >= 400 && statusCode < 500
                && statusCode != 408 && statusCode != 409 && statusCode != 425 && statusCode != 429;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public enum ErrorType {
        CONFIGURATION,
        AUTHENTICATION,
        BUSINESS,
        AMBIGUOUS,
        MALFORMED
    }

    public static class CcatException extends Exception {
        private final ErrorType type;
        private final Integer httpStatus;
        private final Long latencyMillis;
        private final JSONObject providerFields;

        public CcatException(ErrorType type, String message) {
            this(type, message, null, null, null, null);
        }

        public CcatException(ErrorType type, String message, Throwable cause) {
            this(type, message, cause, null, null, null);
        }

        public CcatException(ErrorType type, String message, Throwable cause, Integer httpStatus,
                             Long latencyMillis, JSONObject providerFields) {
            super(message, cause);
            this.type = type;
            this.httpStatus = httpStatus;
            this.latencyMillis = latencyMillis;
            this.providerFields = providerFields;
        }

        public ErrorType getType() {
            return type;
        }

        public Integer getHttpStatus() {
            return httpStatus;
        }

        public Long getLatencyMillis() {
            return latencyMillis;
        }

        public JSONObject getProviderFields() {
            return providerFields;
        }
    }

    public static final class CollectResponse {
        private final JSONObject body;
        private final int httpStatus;
        private final long latencyMillis;

        public CollectResponse(JSONObject body, int httpStatus, long latencyMillis) {
            this.body = body;
            this.httpStatus = httpStatus;
            this.latencyMillis = latencyMillis;
        }

        public JSONObject getBody() {
            return body;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public long getLatencyMillis() {
            return latencyMillis;
        }
    }

    interface Transport {
        TransportResponse post(String url, Map<String, String> headers, String body, Duration timeout)
                throws IOException, InterruptedException;
    }

    static final class TransportResponse {
        private final int statusCode;
        private final String body;

        TransportResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static final class CachedToken {
        private final String value;
        private final Instant usableUntil;

        private CachedToken(String value, Instant usableUntil) {
            this.value = value;
            this.usableUntil = usableUntil;
        }
    }

    private static final class JavaHttpTransport implements Transport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        @Override
        public TransportResponse post(String url, Map<String, String> headers, String body, Duration timeout)
                throws IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            headers.forEach(builder::header);
            HttpResponse<String> response = client.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new TransportResponse(response.statusCode(), response.body());
        }
    }
}
