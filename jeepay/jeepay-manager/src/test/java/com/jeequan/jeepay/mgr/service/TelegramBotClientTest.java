package com.jeequan.jeepay.mgr.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramBotClientTest {

    @Test
    void sendMessagePostsEncodedFormAndAcceptsOkResponse() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        TelegramBotClient client = new TelegramBotClient("123:test-token", "-5211710664",
                (uri, body, timeout) -> {
                    assertTrue(uri.toString().endsWith("/sendMessage"));
                    requestBody.set(body);
                    return new TelegramBotClient.TransportResponse(
                            200, "{\"ok\":true,\"result\":{\"message_id\":321}}");
                });

        assertEquals(321L, client.sendMessage("SUM：NT$ 1,000"));
        assertTrue(requestBody.get().contains("chat_id=-5211710664"));
        assertTrue(requestBody.get().contains("text="));
    }

    @Test
    void sendMessageWithoutMessageIdIsRejected() {
        TelegramBotClient client = new TelegramBotClient("123:test-token", "-5211710664",
                (uri, body, timeout) -> new TelegramBotClient.TransportResponse(200, "{\"ok\":true}"));

        assertThrows(IllegalStateException.class, () -> client.sendMessage("test"));
    }

    @Test
    void deleteMessagePostsChatAndMessageId() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        TelegramBotClient client = new TelegramBotClient("123:test-token", "-5211710664",
                (uri, body, timeout) -> {
                    assertTrue(uri.toString().endsWith("/deleteMessage"));
                    requestBody.set(body);
                    return new TelegramBotClient.TransportResponse(200, "{\"ok\":true,\"result\":true}");
                });

        assertDoesNotThrow(() -> client.deleteMessage(321L));
        assertEquals("chat_id=-5211710664&message_id=321", requestBody.get());
    }

    @Test
    void rejectedDeleteDoesNotExposeToken() {
        String token = "123:test-secret";
        TelegramBotClient client = new TelegramBotClient(token, "-5211710664",
                (uri, body, timeout) -> new TelegramBotClient.TransportResponse(
                        400, "{\"ok\":false,\"description\":\"message can't be deleted\"}"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> client.deleteMessage(321L));
        assertFalse(error.getMessage().contains(token));
    }

    @Test
    void rejectedResponseDoesNotExposeToken() {
        String token = "123:test-secret";
        TelegramBotClient client = new TelegramBotClient(token, "-5211710664",
                (uri, body, timeout) -> new TelegramBotClient.TransportResponse(
                        200, "{\"ok\":false,\"description\":\"rejected\"}"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> client.sendMessage("test"));
        assertFalse(error.getMessage().contains(token));
    }

    @Test
    void transportFailureDoesNotExposeToken() {
        String token = "123:test-secret";
        TelegramBotClient client = new TelegramBotClient(token, "-5211710664",
                (uri, body, timeout) -> {
                    throw new IOException("request failed for " + uri);
                });

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> client.sendMessage("test"));
        assertFalse(error.getMessage().contains(token));
    }
}
