package com.jeequan.jeepay.service.wallet;

import com.jeequan.jeepay.core.entity.SysConfig;
import com.jeequan.jeepay.service.impl.SysConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 錢包與提現設定（t_sys_config 群組 walletConfig，於「系統管理 → 系統配置 → 錢包與提現」維護）。
 * 每次讀 DB，不走快取：設定量小，且修改後需立即生效。畫面上以「元」輸入，這裡轉為「分」。
 */
@Component
public class WalletConfig {

    public static final String GROUP = "walletConfig";

    @Autowired private SysConfigService sysConfigService;

    /** 結算延遲天數：0 = 成功即結算（T+0），1 = 隔日結算（T+1）。 */
    public int settleDelayDays() {
        return (int) Math.max(0, number("walletSettleDelayDays", 1));
    }

    public long withdrawMinFen() {
        return yuanToFen("withdrawMinAmount", 100);
    }

    public long withdrawMaxFen() {
        return yuanToFen("withdrawMaxAmount", 500000);
    }

    public long withdrawFeeFen() {
        return yuanToFen("withdrawFeeAmount", 0);
    }

    public int withdrawDailyLimitPerAccount() {
        return (int) Math.max(1, number("withdrawDailyLimitPerAccount", 3));
    }

    public List<String> restrictedBankCodes() {
        String v = value("withdrawRestrictedBankCodes");
        return StringUtils.isBlank(v) ? List.of()
                : Arrays.stream(v.split("[,，\\s]+")).map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    private long yuanToFen(String key, long defaultYuan) {
        String v = value(key);
        try {
            return StringUtils.isBlank(v) ? defaultYuan * 100 : new BigDecimal(v.trim()).movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return defaultYuan * 100;
        }
    }

    private long number(String key, long defaultValue) {
        String v = value(key);
        try {
            return StringUtils.isBlank(v) ? defaultValue : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String value(String key) {
        SysConfig config = sysConfigService.getById(key);
        return config == null ? null : config.getConfigVal();
    }
}
