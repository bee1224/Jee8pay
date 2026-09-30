package com.jeequan.jeepay.service.wallet;

import com.jeequan.jeepay.core.entity.PayOrderFee;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SettlementSplitTest {

    @Test
    void splitsWholeOrderAmountAcrossAllParties() {
        // 1000 元訂單、商戶手續費 30 元；平臺 2、渠道 15、高代 3、代理 6、推薦 1.5
        PayOrderFee fee = fee(100_000, 3_000).setChannelFee(1_500L).setSrAgentFee(300L).setAgentFee(600L).setReferrerFee(150L)
                .setSrAgentNo("A_SR").setAgentNo("A_AG").setReferrerAgentNo("A_REF");

        Map<String, Long> m = asMap(SettlementSplit.of(fee, "ryo"));

        assertEquals(97_000L, m.get("MCH:M1"));
        assertEquals(300L, m.get("AGENT:A_SR"));
        assertEquals(600L, m.get("AGENT:A_AG"));
        assertEquals(150L, m.get("AGENT:A_REF"));
        assertEquals(1_500L, m.get("CHANNEL:ryo"));
        // 平台 = 3000 - 1500 - 300 - 600 - 150 = 450（含平臺費 200 與未分配 250）
        assertEquals(450L, m.get("PLATFORM:PLATFORM"));
        assertEquals(100_000L, m.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void mergesSharesOfSameAgentAndSkipsZeroAndMissingOwners() {
        // 推薦人同時是高級代理：合併成一筆；無直屬一般代理
        PayOrderFee fee = fee(10_000, 200).setSrAgentFee(30L).setReferrerFee(20L).setAgentFee(0L)
                .setSrAgentNo("A_SR").setReferrerAgentNo("A_SR");

        List<SettlementSplit.Share> shares = SettlementSplit.of(fee, "jhd");
        Map<String, Long> m = asMap(shares);

        assertEquals(50L, m.get("AGENT:A_SR"));
        assertEquals(1, shares.stream().filter(s -> s.getOwnerId().equals("A_SR")).count());
        assertFalse(m.containsKey("CHANNEL:jhd"));
        assertEquals(10_000L, m.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void platformAbsorbsWhenLayersExceedMerchantFee() {
        PayOrderFee fee = fee(4_000, 8).setChannelFee(0L).setSrAgentFee(12L).setAgentFee(120L)
                .setSrAgentNo("A_SR").setAgentNo("A_AG");

        Map<String, Long> m = asMap(SettlementSplit.of(fee, "ryo"));

        assertEquals(-124L, m.get("PLATFORM:PLATFORM"));
        assertEquals(4_000L, m.values().stream().mapToLong(Long::longValue).sum());
    }

    private static PayOrderFee fee(long amount, long mchFee) {
        return new PayOrderFee().setPayOrderId("P1").setMchNo("M1").setAmount(amount).setMchFeeAmount(mchFee)
                .setPlatformFee(0L).setChannelFee(0L).setSrAgentFee(0L).setAgentFee(0L).setReferrerFee(0L);
    }

    private static Map<String, Long> asMap(List<SettlementSplit.Share> shares) {
        return shares.stream().collect(Collectors.toMap(s -> s.getOwnerType() + ":" + s.getOwnerId(), SettlementSplit.Share::getAmount));
    }
}
