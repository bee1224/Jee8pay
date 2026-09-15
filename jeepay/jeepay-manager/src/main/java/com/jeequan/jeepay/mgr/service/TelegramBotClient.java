package com.jeequan.jeepay.mgr.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "telegram.daily-summary", name = "enabled", havingValue = "true")
public class TelegramBotClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final String botToken;
    private final String chatId;
    private final Transport transport;

    @Autowired
    public TelegramBotClient(@Value("${telegram.daily-summary.bot-token:}") String botToken,
                             @Value("${telegram.daily-summary.chat-id:}") String chatId) {
        this(botToken, chatId, new JavaHttpTransport());
    }

    TelegramBotClient(String botToken, String chatId, Transport transport) {
        if (StringUtils.isAnyBlank(botToken, chatId)) {
            throw new IllegalStateException("Telegram daily summary configuration is incomplete");
        }
        this.botToken = botToken;
        this.chatId = chatId;
        this.transport = transport;
    }

    public void sendMessage(String text) {
        String form = "chat_id=" + encode(chatId) + "&text=" + encode(text);
        TransportResponse response;
        try {
            response = transport.post(
                    URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"),
                    form,
                    REQUEST_TIMEOUT);
        } catch (IOException e) {
            throw new IllegalStateException("Telegram Bot API transport failure");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Telegram Bot API request interrupted");
        }

        JSONObject body;
        try {
            body = JSON.parseObject(response.body);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Telegram Bot API response is malformed");
        }
        if (response.statusCode < 200 || response.statusCode >= 300
                || body == null || !body.getBooleanValue("ok")) {
            throw new IllegalStateException("Telegram Bot API rejected the message");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface Transport {
        TransportResponse post(URI uri, String body, Duration timeout) throws IOException, InterruptedException;
    }

    static final class TransportResponse {
        private final int statusCode;
        private final String body;

        TransportResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static final class JavaHttpTransport implements Transport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        @Override
        public TransportResponse post(URI uri, String body, Duration timeout)
                throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new TransportResponse(response.statusCode(), response.body());
        }
    }
}
