package com.jeequan.jeepay.pay.contract;

import com.jeequan.jeepay.core.utils.RegKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaiwanMobileContractTest {

    @Test
    void acceptsTaiwanMobileNumbers() {
        assertTrue(RegKit.isMobile("0912345678"));
        assertTrue(RegKit.isMobile("0987654321"));
    }

    @Test
    void rejectsChinaFormatAndMalformedNumbers() {
        assertFalse(RegKit.isMobile("13812345678"));
        assertFalse(RegKit.isMobile("091234567"));
        assertFalse(RegKit.isMobile("09123456789"));
        assertFalse(RegKit.isMobile("0812345678"));
        assertFalse(RegKit.isMobile("09-12345678"));
        assertFalse(RegKit.isMobile(null));
    }
}
