package com.jeequan.jeepay.pay.channel.ccat;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CcatKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, CcatKit.toCcatTwdAmount(100));
        assertEquals(100, CcatKit.toCcatTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, CcatKit.toCcatTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> CcatKit.toCcatTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> CcatKit.toCcatTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> CcatKit.toCcatTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, CcatKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> CcatKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> CcatKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> CcatKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                CcatKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(CcatKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = CcatKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(CcatKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(CcatKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(CcatKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(CcatKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(CcatKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, CcatKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, CcatKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, CcatKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, CcatKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, CcatKit.mapProcessCode("999"));
    }
}
