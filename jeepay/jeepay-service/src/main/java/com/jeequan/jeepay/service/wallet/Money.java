package com.jeequan.jeepay.service.wallet;

import com.jeequan.jeepay.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/** 畫面輸入的「元」轉「分」；格式錯誤或超過兩位小數時回覆可讀的錯誤，而不是系統異常。 */
public final class Money {

    private Money() {
    }

    public static long yuanToFen(String yuan) {
        if (StringUtils.isBlank(yuan)) {
            throw new BizException("請輸入金額");
        }
        BigDecimal v;
        try {
            v = new BigDecimal(yuan.trim());
        } catch (NumberFormatException e) {
            throw new BizException("金額格式不正確");
        }
        if (v.stripTrailingZeros().scale() > 2) {
            throw new BizException("金額最多兩位小數");
        }
        try {
            return v.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new BizException("金額超出範圍");
        }
    }
}
