package com.jeequan.jeepay.pay.channel.ryo;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RyoKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, RyoKit.toRyoTwdAmount(100));
        assertEquals(10, RyoKit.toRyoTwdAmount(1_000));
        assertEquals(40, RyoKit.toRyoTwdAmount(4_000));
        assertEquals(100, RyoKit.toRyoTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, RyoKit.toRyoTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> RyoKit.toRyoTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> RyoKit.toRyoTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> RyoKit.toRyoTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, RyoKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> RyoKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> RyoKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> RyoKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                RyoKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(RyoKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = RyoKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(RyoKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(RyoKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(RyoKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(RyoKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(RyoKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, RyoKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, RyoKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, RyoKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, RyoKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, RyoKit.mapProcessCode("999"));
    }
}
