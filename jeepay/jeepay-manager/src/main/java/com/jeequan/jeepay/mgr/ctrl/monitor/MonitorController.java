package com.jeequan.jeepay.mgr.ctrl.monitor;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.SysConfig;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上帝的流量監控：全站代收流量（每分鐘、每小時、各上游）、待處理積壓、系統資源，以及容量上限的使用率。
 * 容量上限只用來提醒（超過門檻變色），不會擋單；擋單屬於下單流程的變更，需另外確認。
 */
@Slf4j
@Tag(name = "流量監控")
@RestController
@RequestMapping("/api/monitor")
public class MonitorController extends CommonCtrl {

    /** 容量上限存在 t_sys_config（group_key=monitorCapacity），值為整數；0 表示不設上限 */
    public static final String KEY_PER_MINUTE = "monitorMaxOrdersPerMinute";
    public static final String KEY_DAILY_COUNT = "monitorMaxDailyOrders";
    public static final String KEY_DAILY_AMOUNT = "monitorMaxDailyAmount";
    /** 警示門檻（百分比） */
    public static final String KEY_WARN_PCT = "monitorWarnPercent";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired private PayOrderService payOrderService;
    @Autowired private MchNotifyRecordService mchNotifyRecordService;
    @Autowired private SysConfigService sysConfigService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Operation(summary = "全站流量與容量使用率")
    @PreAuthorize("hasAuthority('ENT_SYS_MONITOR')")
    @RequestMapping(value = "/traffic", method = RequestMethod.GET)
    public ApiRes traffic() {
        LocalDateTime now = LocalDateTime.now();
        String today = LocalDate.now().atStartOfDay().format(TS);
        String hourAgo = now.minusMinutes(60).format(TS);

        JSONObject res = new JSONObject();
        res.put("now", now.format(TS));

        // 最近 60 分鐘，每分鐘建單數與成功數
        List<Map<String, Object>> perMinute = payOrderService.listMaps(new QueryWrapper<PayOrder>()
                .select("DATE_FORMAT(created_at, '%H:%i') AS m", "COUNT(*) AS total", "SUM(state = 2) AS paid")
                .ge("created_at", hourAgo).groupBy("m").orderByAsc("m"));
        res.put("perMinute", perMinute);

        // 今天每小時
        List<Map<String, Object>> perHour = payOrderService.listMaps(new QueryWrapper<PayOrder>()
                .select("HOUR(created_at) AS h", "COUNT(*) AS total", "SUM(state = 2) AS paid", "SUM(CASE WHEN state = 2 THEN amount ELSE 0 END) AS paidAmount")
                .ge("created_at", today).groupBy("h").orderByAsc("h"));
        res.put("perHour", perHour);

        // 今天各上游
        List<Map<String, Object>> byChannel = payOrderService.listMaps(new QueryWrapper<PayOrder>()
                .select("IFNULL(if_code, '') AS ifCode", "COUNT(*) AS total", "SUM(state = 2) AS paid", "SUM(state = 3) AS failed",
                        "SUM(CASE WHEN state = 2 THEN amount ELSE 0 END) AS paidAmount")
                .ge("created_at", today).groupBy("ifCode").orderByDesc("total"));
        res.put("byChannel", byChannel);

        // 今天合計與尖峰
        Map<String, Object> totals = payOrderService.getMap(new QueryWrapper<PayOrder>()
                .select("COUNT(*) AS total", "IFNULL(SUM(state = 2), 0) AS paid", "IFNULL(SUM(state = 1), 0) AS waiting",
                        "IFNULL(SUM(CASE WHEN state = 2 THEN amount ELSE 0 END), 0) AS paidAmount")
                .ge("created_at", today));
        res.put("today", totals);
        Map<String, Object> peak = payOrderService.getMap(new QueryWrapper<PayOrder>()
                .select("DATE_FORMAT(created_at, '%H:%i') AS m", "COUNT(*) AS c")
                .ge("created_at", today).groupBy("m").orderByDesc("c").last("LIMIT 1"));
        res.put("peakMinute", peak);
        long lastMinute = payOrderService.count(PayOrder.gw().ge(PayOrder::getCreatedAt, java.sql.Timestamp.valueOf(now.minusMinutes(1))));
        res.put("lastMinute", lastMinute);

        // 積壓：通知商戶仍在重試的筆數
        res.put("notifyPending", mchNotifyRecordService.count(MchNotifyRecord.gw().eq(MchNotifyRecord::getState, MchNotifyRecord.STATE_ING)));

        // 系統資源（這台 manager 的 JVM、資料庫連線）
        JSONObject sys = new JSONObject();
        Runtime rt = Runtime.getRuntime();
        sys.put("jvmUsedMb", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        sys.put("jvmMaxMb", rt.maxMemory() / 1024 / 1024);
        try {
            sys.put("dbConnections", Long.parseLong(String.valueOf(jdbcTemplate.queryForMap("SHOW GLOBAL STATUS LIKE 'Threads_connected'").get("Value"))));
            sys.put("dbMaxConnections", Long.parseLong(String.valueOf(jdbcTemplate.queryForMap("SHOW VARIABLES LIKE 'max_connections'").get("Value"))));
        } catch (Exception e) {
            log.warn("讀取資料庫連線數失敗: {}", e.getMessage());
        }
        res.put("system", sys);

        res.put("capacity", capacity());
        return ApiRes.ok(res);
    }

    @Operation(summary = "修改容量上限（0 = 不設上限；只用於提醒，不會擋單）")
    @PreAuthorize("hasAuthority('ENT_SYS_MONITOR_EDIT')")
    @MethodLog(remark = "修改容量上限")
    @RequestMapping(value = "/capacity", method = RequestMethod.PUT)
    public ApiRes updateCapacity() {
        JSONObject p = getReqParamJSON();
        Map<String, Long> limits = new LinkedHashMap<>();
        for (String key : new String[]{KEY_PER_MINUTE, KEY_DAILY_COUNT, KEY_DAILY_AMOUNT, KEY_WARN_PCT}) {
            Long v = p.getLong(key);
            if (v == null) {
                continue;
            }
            if (v < 0 || (KEY_WARN_PCT.equals(key) && (v < 1 || v > 100))) {
                throw new BizException("數值不正確");
            }
            limits.put(key, v);
        }
        limits.forEach((k, v) -> {
            SysConfig c = new SysConfig();
            c.setConfigKey(k);
            c.setConfigVal(String.valueOf(v));
            sysConfigService.updateById(c);
        });
        return ApiRes.ok(capacity());
    }

    private JSONObject capacity() {
        JSONObject cap = new JSONObject();
        for (String key : new String[]{KEY_PER_MINUTE, KEY_DAILY_COUNT, KEY_DAILY_AMOUNT, KEY_WARN_PCT}) {
            SysConfig c = sysConfigService.getById(key);
            long v = 0;
            try {
                v = c == null ? 0 : Long.parseLong(c.getConfigVal().trim());
            } catch (NumberFormatException ignored) {
                // 設定值不是數字時視為未設定
            }
            cap.put(key, v);
        }
        if (cap.getLongValue(KEY_WARN_PCT) == 0) {
            cap.put(KEY_WARN_PCT, 80);
        }
        return cap;
    }
}
