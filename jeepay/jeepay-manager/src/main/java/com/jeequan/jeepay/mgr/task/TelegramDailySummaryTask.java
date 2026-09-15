package com.jeequan.jeepay.mgr.task;

import com.jeequan.jeepay.mgr.service.TelegramBotClient;
import com.jeequan.jeepay.service.impl.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "telegram.daily-summary", name = "enabled", havingValue = "true")
public class TelegramDailySummaryTask {

    static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PayOrderService payOrderService;
    private final TelegramBotClient telegramBotClient;
    private final Clock clock;

    @Autowired
    public TelegramDailySummaryTask(PayOrderService payOrderService,
                                    TelegramBotClient telegramBotClient) {
        this(payOrderService, telegramBotClient, Clock.system(REPORT_ZONE));
    }

    TelegramDailySummaryTask(PayOrderService payOrderService,
                             TelegramBotClient telegramBotClient,
                             Clock clock) {
        this.payOrderService = payOrderService;
        this.telegramBotClient = telegramBotClient;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Taipei")
    public void sendPreviousDaySummary() {
        LocalDate reportDate = LocalDate.now(clock).minusDays(1);
        Date start = Date.from(reportDate.atStartOfDay(REPORT_ZONE).toInstant());
        Date end = Date.from(reportDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant());
        Map<String, Object> amounts = payOrderService.dailyProviderAmount(start, end);

        telegramBotClient.sendMessage(formatMessage(reportDate, amounts));
        log.info("Telegram daily payment summary sent for reportDate={}", reportDate);
    }

    static String formatMessage(LocalDate reportDate, Map<String, Object> amounts) {
        long ryo = cents(amounts, "ryoAmount");
        long chi = cents(amounts, "chiAmount");
        long jay = cents(amounts, "jayAmount");
        long jhd = cents(amounts, "jhdAmount");
        long sum = Math.addExact(Math.addExact(ryo, chi), Math.addExact(jay, jhd));

        return "📊 " + REPORT_DATE.format(reportDate) + " 收款總結\n"
                + "RYO：NT$ " + formatTwd(ryo) + "\n"
                + "CHI：NT$ " + formatTwd(chi) + "\n"
                + "JAY：NT$ " + formatTwd(jay) + "\n"
                + "JHD：NT$ " + formatTwd(jhd) + "\n"
                + "SUM：NT$ " + formatTwd(sum);
    }

    private static long cents(Map<String, Object> amounts, String key) {
        if (amounts == null || amounts.get(key) == null) {
            return 0L;
        }
        Object value = amounts.get(key);
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
    }

    private static String formatTwd(long cents) {
        return NumberFormat.getIntegerInstance(Locale.US).format(cents / 100);
    }
}
