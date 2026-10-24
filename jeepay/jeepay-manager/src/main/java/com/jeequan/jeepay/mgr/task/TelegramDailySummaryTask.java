package com.jeequan.jeepay.mgr.task;

import com.jeequan.jeepay.core.cache.RedisUtil;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "telegram.daily-summary", name = "enabled", havingValue = "true")
public class TelegramDailySummaryTask {

    static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String MESSAGE_ID_KEY_PREFIX = "telegramDailySummary:messageId:";
    private static final long MESSAGE_ID_TTL_DAYS = 3;

    private final PayOrderService payOrderService;
    private final TelegramBotClient telegramBotClient;
    private final MessageIdStore messageIdStore;
    private final Clock clock;

    @Autowired
    public TelegramDailySummaryTask(PayOrderService payOrderService,
                                    TelegramBotClient telegramBotClient) {
        this(payOrderService, telegramBotClient, new RedisMessageIdStore(), Clock.system(REPORT_ZONE));
    }

    TelegramDailySummaryTask(PayOrderService payOrderService,
                             TelegramBotClient telegramBotClient,
                             MessageIdStore messageIdStore,
                             Clock clock) {
        this.payOrderService = payOrderService;
        this.telegramBotClient = telegramBotClient;
        this.messageIdStore = messageIdStore;
        this.clock = clock;
    }

    /** 日報 message_id 需跨 manager 重啟保存到刪除時間 */
    interface MessageIdStore {
        void put(String key, long messageId);

        Long get(String key);

        void remove(String key);
    }

    private static final class RedisMessageIdStore implements MessageIdStore {
        @Override
        public void put(String key, long messageId) {
            RedisUtil.setString(key, String.valueOf(messageId), MESSAGE_ID_TTL_DAYS, TimeUnit.DAYS);
        }

        @Override
        public Long get(String key) {
            String value = RedisUtil.getString(key);
            return value == null ? null : Long.valueOf(value);
        }

        @Override
        public void remove(String key) {
            RedisUtil.del(key);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Taipei")
    public void sendPreviousDaySummary() {
        LocalDate reportDate = LocalDate.now(clock).minusDays(1);
        Date start = Date.from(reportDate.atStartOfDay(REPORT_ZONE).toInstant());
        Date end = Date.from(reportDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant());
        Map<String, Object> amounts = payOrderService.dailyProviderAmount(start, end);

        long messageId = telegramBotClient.sendMessage(formatMessage(reportDate, amounts));
        log.info("Telegram daily payment summary sent for reportDate={}", reportDate);
        try {
            messageIdStore.put(messageIdKey(reportDate), messageId);
        } catch (RuntimeException e) {
            log.warn("Telegram daily summary messageId not stored, auto delete skipped for reportDate={}", reportDate, e);
        }
    }

    /**
     * Telegram 只允許 bot 刪除 48 小時內的訊息，故於 00:00 發送後第二天 23:30（約 47.5 小時）刪除。
     * 23:30 刪除的是前天 00:00 送出的日報，即 reportDate = 今天 - 2。
     */
    @Scheduled(cron = "0 30 23 * * ?", zone = "Asia/Taipei")
    public void deleteExpiredSummary() {
        LocalDate reportDate = LocalDate.now(clock).minusDays(2);
        String key = messageIdKey(reportDate);
        Long messageId = messageIdStore.get(key);
        if (messageId == null) {
            return;
        }
        try {
            telegramBotClient.deleteMessage(messageId);
            log.info("Telegram daily payment summary deleted for reportDate={}", reportDate);
        } catch (RuntimeException e) {
            log.warn("Telegram daily payment summary delete failed for reportDate={}: {}", reportDate, e.getMessage());
        } finally {
            // 超過 48 小時就再也刪不掉，不論成敗都不再重試
            messageIdStore.remove(key);
        }
    }

    static String messageIdKey(LocalDate reportDate) {
        return MESSAGE_ID_KEY_PREFIX + REPORT_DATE.format(reportDate);
    }

    static String formatMessage(LocalDate reportDate, Map<String, Object> amounts) {
        long ryo = cents(amounts, "ryoAmount");
        long chi = cents(amounts, "chiAmount");
        long jay = cents(amounts, "jayAmount");
        long jhd = cents(amounts, "jhdAmount");
        long fyz = cents(amounts, "fyzAmount");
        long yuc = cents(amounts, "yucAmount");
        long sum = Math.addExact(Math.addExact(Math.addExact(ryo, chi), Math.addExact(jay, jhd)), Math.addExact(fyz, yuc));

        return "📊 " + REPORT_DATE.format(reportDate) + " 收款總結\n"
                + "RYO：NT$ " + formatTwd(ryo) + "\n"
                + "CHI：NT$ " + formatTwd(chi) + "\n"
                + "JAY：NT$ " + formatTwd(jay) + "\n"
                + "JHD：NT$ " + formatTwd(jhd) + "\n"
                + "FYZ：NT$ " + formatTwd(fyz) + "\n"
                + "YUC：NT$ " + formatTwd(yuc) + "\n"
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
