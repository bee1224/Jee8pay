package com.jeequan.jeepay.pay.channel.jhd;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JhdKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, JhdKit.toJhdTwdAmount(100));
        assertEquals(10, JhdKit.toJhdTwdAmount(1_000));
        assertEquals(40, JhdKit.toJhdTwdAmount(4_000));
        assertEquals(100, JhdKit.toJhdTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, JhdKit.toJhdTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> JhdKit.toJhdTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> JhdKit.toJhdTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> JhdKit.toJhdTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, JhdKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> JhdKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> JhdKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> JhdKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                JhdKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(JhdKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = JhdKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(JhdKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(JhdKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(JhdKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(JhdKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(JhdKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, JhdKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, JhdKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, JhdKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, JhdKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, JhdKit.mapProcessCode("999"));
    }
}
