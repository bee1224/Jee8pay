package com.jeequan.jeepay.pay.task;

import cn.hutool.core.date.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosedOrderAuditTaskTest {

    @Test
    void queriesAtMostThreeTimesAtOneTwentyFourAndFortyEightHours() {
        Date expired = DateUtil.parse("2026-09-30 10:00:00");
        // 第 1 次：到期 1 小時後
        assertFalse(ClosedOrderAuditTask.isDue(0, expired, DateUtil.parse("2026-09-30 10:59:00")));
        assertTrue(ClosedOrderAuditTask.isDue(0, expired, DateUtil.parse("2026-09-30 11:00:00")));
        // 第 2 次：到期 24 小時後
        assertFalse(ClosedOrderAuditTask.isDue(1, expired, DateUtil.parse("2026-10-01 09:00:00")));
        assertTrue(ClosedOrderAuditTask.isDue(1, expired, DateUtil.parse("2026-10-01 10:00:00")));
        // 第 3 次：到期 48 小時後；之後不再查
        assertTrue(ClosedOrderAuditTask.isDue(2, expired, DateUtil.parse("2026-10-02 10:00:00")));
        assertFalse(ClosedOrderAuditTask.isDue(3, expired, DateUtil.parse("2026-10-03 10:00:00")));
        assertFalse(ClosedOrderAuditTask.isDue(0, null, new Date()));
    }
}
