package com.jeequan.jeepay.pay.channel.chi;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChiKitTest {

    @Test
    void convertsExactWholeTwdAmounts() {
        assertEquals(1, ChiKit.toChiTwdAmount(100));
        assertEquals(10, ChiKit.toChiTwdAmount(1_000));
        assertEquals(40, ChiKit.toChiTwdAmount(4_000));
        assertEquals(100, ChiKit.toChiTwdAmount(10_000));
        assertEquals(Long.MAX_VALUE / 100, ChiKit.toChiTwdAmount((Long.MAX_VALUE / 100) * 100));
    }

    @Test
    void rejectsNonDivisibleAmountWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> ChiKit.toChiTwdAmount(101));
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> ChiKit.toChiTwdAmount(0));
        assertThrows(IllegalArgumentException.class, () -> ChiKit.toChiTwdAmount(-100));
    }

    @Test
    void parsesOnlyScaleZeroProviderAmounts() {
        assertEquals(123, ChiKit.parseWholeTwd("123", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> ChiKit.parseWholeTwd("1.00", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> ChiKit.parseWholeTwd("1.5", "amount", false));
        assertThrows(IllegalArgumentException.class, () -> ChiKit.parseWholeTwd("-1", "amount", false));
    }

    @Test
    void computesKnownSyntheticChecksumVector() {
        assertEquals("081b9e76fc1a843fda2dbfa569069a34",
                ChiKit.checksum("test-account", "TX123", "101", "B", "1234567890"));
        assertTrue(ChiKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567890",
                "081B9E76FC1A843FDA2DBFA569069A34"));
    }

    @Test
    void rejectsChangedChecksumFieldsAndWrongCanonicalOrder() {
        String checksum = ChiKit.checksum("test-account", "TX123", "101", "B", "1234567890");
        assertFalse(ChiKit.verifyChecksum("test-account-2", "TX123", "101", "B", "1234567890", checksum));
        assertFalse(ChiKit.verifyChecksum("test-account", "TX124", "101", "B", "1234567890", checksum));
        assertFalse(ChiKit.verifyChecksum("test-account", "TX123", "102", "B", "1234567890", checksum));
        assertFalse(ChiKit.verifyChecksum("test-account", "TX123", "101", "A", "1234567890", checksum));
        assertFalse(ChiKit.verifyChecksum("test-account", "TX123", "101", "B", "1234567891", checksum));
        assertNotEquals(checksum, ChiKit.checksum("TX123", "test-account", "101", "B", "1234567890"));
    }

    @Test
    void mapsOfficialProcessCodesConservatively() {
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, ChiKit.mapProcessCode("7"));
        assertEquals(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS, ChiKit.mapProcessCode("8"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, ChiKit.mapProcessCode("2"));
        assertEquals(ChannelRetMsg.ChannelState.UNKNOWN, ChiKit.mapProcessCode("999"));
    }
}
