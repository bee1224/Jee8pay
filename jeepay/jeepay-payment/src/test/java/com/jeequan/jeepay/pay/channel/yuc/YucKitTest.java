package com.jeequan.jeepay.pay.channel.yuc;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YucKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, YucKit.toYucTwdAmount(100));
        assertEquals(10, YucKit.toYucTwdAmount(1_000));
        assertEquals(40, YucKit.toYucTwdAmount(4_000));
        assertEquals(100, YucKit.toYucTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, YucKit.toYucTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> YucKit.toYucTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> YucKit.toYucTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> YucKit.toYucTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, YucKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> YucKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> YucKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> YucKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                YucKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(YucKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = YucKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(YucKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(YucKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(YucKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(YucKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(YucKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, YucKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, YucKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, YucKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, YucKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, YucKit.mapProcessCode("999"));
    }
}
