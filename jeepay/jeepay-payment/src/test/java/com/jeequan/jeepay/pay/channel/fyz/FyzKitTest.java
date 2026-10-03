package com.jeequan.jeepay.pay.channel.fyz;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FyzKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, FyzKit.toFyzTwdAmount(100));
        assertEquals(10, FyzKit.toFyzTwdAmount(1_000));
        assertEquals(40, FyzKit.toFyzTwdAmount(4_000));
        assertEquals(100, FyzKit.toFyzTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, FyzKit.toFyzTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> FyzKit.toFyzTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> FyzKit.toFyzTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> FyzKit.toFyzTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, FyzKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> FyzKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> FyzKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> FyzKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                FyzKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(FyzKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = FyzKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(FyzKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(FyzKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(FyzKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(FyzKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(FyzKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, FyzKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, FyzKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, FyzKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, FyzKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, FyzKit.mapProcessCode("999"));
    }
}
