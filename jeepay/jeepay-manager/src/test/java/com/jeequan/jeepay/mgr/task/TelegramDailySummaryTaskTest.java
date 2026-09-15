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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramDailySummaryTaskTest {

    @Mock private PayOrderService payOrderService;
    @Mock private TelegramBotClient telegramBotClient;

    @Test
    void midnightSummaryUsesPreviousTaipeiDayAndFormatsFourProvidersAndSum() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T16:00:00Z"), TelegramDailySummaryTask.REPORT_ZONE);
        Map<String, Object> amounts = new HashMap<>();
        amounts.put("ryoAmount", 123_400L);
        amounts.put("chiAmount", 20_000_000L);
        amounts.put("jayAmount", 0L);
        amounts.put("jhdAmount", 6_600L);
        when(payOrderService.dailyProviderAmount(any(), any())).thenReturn(amounts);

        TelegramDailySummaryTask task = new TelegramDailySummaryTask(
                payOrderService, telegramBotClient, clock);
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
    }
}
