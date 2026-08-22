package com.jeequan.jeepay.pay.channel.jay;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JayKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, JayKit.toJayTwdAmount(100));
        assertEquals(10, JayKit.toJayTwdAmount(1_000));
        assertEquals(40, JayKit.toJayTwdAmount(4_000));
        assertEquals(100, JayKit.toJayTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, JayKit.toJayTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> JayKit.toJayTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> JayKit.toJayTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> JayKit.toJayTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, JayKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> JayKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> JayKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> JayKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                JayKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(JayKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = JayKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(JayKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(JayKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(JayKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(JayKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(JayKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, JayKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, JayKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, JayKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, JayKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, JayKit.mapProcessCode("999"));
    }
}
