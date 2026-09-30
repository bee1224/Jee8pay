package com.jeequan.jeepay.service.wallet;

import com.jeequan.jeepay.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void convertsYuanToFenAndRejectsBadInput() {
        assertEquals(10_050L, Money.yuanToFen("100.5"));
        assertEquals(10_000L, Money.yuanToFen(" 100.00 "));
        assertEquals(-1_000L, Money.yuanToFen("-10"));
        assertEquals(12_300L, Money.yuanToFen("123.000"));
        assertThrows(BizException.class, () -> Money.yuanToFen("1.234"));
        assertThrows(BizException.class, () -> Money.yuanToFen("abc"));
        assertThrows(BizException.class, () -> Money.yuanToFen(""));
        assertThrows(BizException.class, () -> Money.yuanToFen("1e30"));
    }
}
