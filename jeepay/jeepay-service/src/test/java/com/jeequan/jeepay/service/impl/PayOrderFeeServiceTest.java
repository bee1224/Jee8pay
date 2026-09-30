package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSON;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.PayOrderFee;
import com.jeequan.jeepay.service.fee.FeeWaterfall;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PayOrderFeeServiceTest {

    private static final AgentInfo SENIOR = new AgentInfo().setAgentNo("A_SR").setAgentLevel(AgentInfo.LEVEL_SENIOR).setState((byte) 1);
    private static final AgentInfo AGENT = new AgentInfo().setAgentNo("A_AG").setAgentLevel(AgentInfo.LEVEL_AGENT)
            .setParentAgentNo("A_SR").setState((byte) 1);

    @Test
    void buildCopiesLayerFeesAgentChainAndFlagsExcess() {
        PayOrder order = new PayOrder().setPayOrderId("P1").setMchNo("M1").setWayCode("RYO_IBON")
                .setAmount(100_000L).setMchFeeAmount(2_500L);
        List<FeeRule> rules = List.of(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.002", 0),
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_CHANNEL, "0.010", 500),
                rule(FeeRule.TARGET_AGENT, "A_SR", FeeRule.LAYER_SR_AGENT, "0.003", 0),
                rule(FeeRule.TARGET_AGENT, "A_AG", FeeRule.LAYER_AGENT, "0.005", 100));
        FeeRuleService.AgentChain chain = new FeeRuleService.AgentChain(AGENT, SENIOR, "A_REF");

        PayOrderFee fee = PayOrderFeeService.build(order, chain,
                FeeWaterfall.compute(FeeWaterfall.resolve("M1", AGENT, SENIOR, rules), order.getAmount()));

        assertEquals(200L, fee.getPlatformFee());
        assertEquals(1500L, fee.getChannelFee());
        assertEquals(300L, fee.getSrAgentFee());
        assertEquals(600L, fee.getAgentFee());
        assertEquals(2600L, fee.getTotalFee());
        assertEquals("A_AG", fee.getAgentNo());
        assertEquals("A_SR", fee.getSrAgentNo());
        assertEquals("A_REF", fee.getReferrerAgentNo());
        // 四層合計 26 元 > 商戶手續費 25 元 → 標記需檢查
        assertEquals((byte) 1, fee.getExceedsMchFee());
        assertEquals(4, JSON.parseArray(fee.getDetail()).size());
        assertEquals("AGENT:A_AG", JSON.parseArray(fee.getDetail()).getJSONObject(3).getString("source"));
    }

    @Test
    void unboundMerchantSnapshotHasNoAgents() {
        PayOrder order = new PayOrder().setPayOrderId("P2").setMchNo("M1").setWayCode("RYO_IBON").setAmount(10_000L);
        FeeRuleService.AgentChain chain = new FeeRuleService.AgentChain(null, null, null);

        PayOrderFee fee = PayOrderFeeService.build(order, chain, FeeWaterfall.compute(FeeWaterfall.resolve("M1", null, null,
                List.of(rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.01", 0))), order.getAmount()));

        assertNull(fee.getAgentNo());
        assertNull(fee.getSrAgentNo());
        assertEquals(100L, fee.getTotalFee());
        assertEquals(0L, fee.getMchFeeAmount());
        assertEquals((byte) 1, fee.getExceedsMchFee());
    }

    private static FeeRule rule(String targetType, String targetId, String layer, String rate, long fixed) {
        return new FeeRule().setWayCode("RYO_IBON").setTargetType(targetType).setTargetId(targetId)
                .setLayer(layer).setRate(new BigDecimal(rate)).setFixedAmount(fixed).setState((byte) 1);
    }
}
