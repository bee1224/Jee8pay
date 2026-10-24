package com.jeequan.jeepay.mgr.task;

import com.jeequan.jeepay.mgr.service.TelegramBotClient;
import com.jeequan.jeepay.service.impl.PayOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramDailySummaryTaskTest {

    @Mock private PayOrderService payOrderService;
    @Mock private TelegramBotClient telegramBotClient;
    private final MapStore store = new MapStore();

    @Test
    void midnightSummaryUsesPreviousTaipeiDayAndFormatsFourProvidersAndSum() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T16:00:00Z"), TelegramDailySummaryTask.REPORT_ZONE);
        Map<String, Object> amounts = new HashMap<>();
        amounts.put("ryoAmount", 123_400L);
        amounts.put("chiAmount", 20_000_000L);
        amounts.put("jayAmount", 0L);
        amounts.put("jhdAmount", 6_600L);
        when(payOrderService.dailyProviderAmount(any(), any())).thenReturn(amounts);
        when(telegramBotClient.sendMessage(anyString())).thenReturn(321L);

        TelegramDailySummaryTask task = new TelegramDailySummaryTask(
                payOrderService, telegramBotClient, store, clock);
        task.sendPreviousDaySummary();

        ArgumentCaptor<Date> start = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> end = ArgumentCaptor.forClass(Date.class);
        verify(payOrderService).dailyProviderAmount(start.capture(), end.capture());
        assertEquals(Instant.parse("2026-09-14T16:00:00Z"), start.getValue().toInstant());
        assertEquals(Instant.parse("2026-09-15T16:00:00Z"), end.getValue().toInstant());
        verify(telegramBotClient).sendMessage("📊 2026-09-15 收款總結\n"
                + "RYO：NT$ 1,234\n"
                + "CHI：NT$ 200,000\n"
                + "JAY：NT$ 0\n"
                + "JHD：NT$ 66\n"
                + "SUM：NT$ 201,300");
        assertEquals(321L, store.get("telegramDailySummary:messageId:2026-09-15"));
    }

    @Test
    void storeFailureDoesNotFailSentSummary() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T16:00:00Z"), TelegramDailySummaryTask.REPORT_ZONE);
        when(payOrderService.dailyProviderAmount(any(), any())).thenReturn(new HashMap<>());
        when(telegramBotClient.sendMessage(anyString())).thenReturn(321L);
        TelegramDailySummaryTask.MessageIdStore broken = new MapStore() {
            @Override
            public void put(String key, long messageId) {
                throw new IllegalStateException("redis down");
            }
        };

        TelegramDailySummaryTask task = new TelegramDailySummaryTask(
                payOrderService, telegramBotClient, broken, clock);
        assertDoesNotThrow(task::sendPreviousDaySummary);
    }

    @Test
    void lateEveningDeletesSummarySentPreviousMidnight() {
        // 2026-09-17 23:30 台北：刪除 2026-09-16 00:00 送出的 2026-09-15 日報（約 47.5 小時）
        Clock clock = Clock.fixed(Instant.parse("2026-09-17T15:30:00Z"), TelegramDailySummaryTask.REPORT_ZONE);
        store.put("telegramDailySummary:messageId:2026-09-15", 321L);
        store.put("telegramDailySummary:messageId:2026-09-16", 322L);

        new TelegramDailySummaryTask(payOrderService, telegramBotClient, store, clock).deleteExpiredSummary();

        verify(telegramBotClient).deleteMessage(321L);
        verify(telegramBotClient, never()).deleteMessage(322L);
        assertNull(store.get("telegramDailySummary:messageId:2026-09-15"));
        assertEquals(322L, store.get("telegramDailySummary:messageId:2026-09-16"));
    }

    @Test
    void deleteFailureStillClearsKeyAndDoesNotThrow() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-17T15:30:00Z"), TelegramDailySummaryTask.REPORT_ZONE);
        store.put("telegramDailySummary:messageId:2026-09-15", 321L);
        doThrow(new IllegalStateException("Telegram Bot API rejected deleteMessage"))
                .when(telegramBotClient).deleteMessage(321L);

        TelegramDailySummaryTask task = new TelegramDailySummaryTask(
                payOrderService, telegramBotClient, store, clock);
        assertDoesNotThrow(task::deleteExpiredSummary);
        assertNull(store.get("telegramDailySummary:messageId:2026-09-15"));
    }

    @Test
    void nothingStoredMeansNoDelete() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-17T15:30:00Z"), TelegramDailySummaryTask.REPORT_ZONE);

        new TelegramDailySummaryTask(payOrderService, telegramBotClient, store, clock).deleteExpiredSummary();

        verifyNoInteractions(telegramBotClient);
    }

    private static class MapStore implements TelegramDailySummaryTask.MessageIdStore {
        private final Map<String, Long> values = new HashMap<>();

        @Override
        public void put(String key, long messageId) {
            values.put(key, messageId);
        }

        @Override
        public Long get(String key) {
            return values.get(key);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }
}
