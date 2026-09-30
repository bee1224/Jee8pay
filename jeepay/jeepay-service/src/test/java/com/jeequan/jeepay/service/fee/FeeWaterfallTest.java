package com.jeequan.jeepay.service.fee;

import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.exception.BizException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeeWaterfallTest {

    private static final String WAY = "RYO_IBON";
    private static final AgentInfo SENIOR = agent("A_SR", AgentInfo.LEVEL_SENIOR, null);
    private static final AgentInfo AGENT = agent("A_AG", AgentInfo.LEVEL_AGENT, "A_SR");

    @Test
    void resolvesEachLayerFromItsOwner() {
        List<FeeRule> rules = List.of(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.002", 0),
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_CHANNEL, "0.010", 500),
                rule(FeeRule.TARGET_AGENT, "A_SR", FeeRule.LAYER_SR_AGENT, "0.003", 0),
                rule(FeeRule.TARGET_AGENT, "A_AG", FeeRule.LAYER_AGENT, "0.005", 100));

        List<FeeWaterfall.LayerRule> resolved = FeeWaterfall.resolve("M1", AGENT, SENIOR, rules);

        assertEquals(List.of("DEFAULT", "DEFAULT", "AGENT:A_SR", "AGENT:A_AG"),
                resolved.stream().map(FeeWaterfall.LayerRule::getSource).toList());
        FeeWaterfall.Breakdown b = FeeWaterfall.compute(resolved, 100_000);
        // 100000 分 × 0.2% = 200；× 1% + 500 = 1500；× 0.3% = 300；× 0.5% + 100 = 600
        assertEquals(List.of(200L, 1500L, 300L, 600L), b.getLayers().stream().map(FeeWaterfall.LayerFee::getFee).toList());
        assertEquals(2600L, b.getTotalFee());
        assertEquals(97_400L, b.getNetAmount());
        assertTrue(b.isValid());
    }

    @Test
    void merchantOverrideWinsForEveryLayer() {
        List<FeeRule> rules = List.of(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.002", 0),
                rule(FeeRule.TARGET_MCH, "M1", FeeRule.LAYER_PLATFORM, "0.001", 0),
                rule(FeeRule.TARGET_AGENT, "A_AG", FeeRule.LAYER_AGENT, "0.005", 0),
                rule(FeeRule.TARGET_MCH, "M1", FeeRule.LAYER_AGENT, "0.004", 0));

        List<FeeWaterfall.LayerRule> resolved = FeeWaterfall.resolve("M1", AGENT, SENIOR, rules);

        assertEquals("MCH", resolved.get(0).getSource());
        assertEquals(new BigDecimal("0.001"), resolved.get(0).getRate());
        assertEquals("MCH", resolved.get(3).getSource());
        assertEquals(new BigDecimal("0.004"), resolved.get(3).getRate());
    }

    @Test
    void merchantUnderSeniorDirectlyHasNoAgentLayer() {
        List<FeeRule> rules = List.of(
                rule(FeeRule.TARGET_AGENT, "A_SR", FeeRule.LAYER_SR_AGENT, "0.003", 0),
                rule(FeeRule.TARGET_AGENT, "A_SR", FeeRule.LAYER_AGENT, "0.009", 0));

        List<FeeWaterfall.LayerRule> resolved = FeeWaterfall.resolve("M1", SENIOR, SENIOR, rules);

        assertEquals("AGENT:A_SR", resolved.get(2).getSource());
        assertEquals("NONE", resolved.get(3).getSource());
        assertEquals(0, resolved.get(3).getRate().signum());
    }

    @Test
    void unboundMerchantPaysOnlyPlatformLayersAndDisabledRulesAreIgnored() {
        FeeRule disabled = rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_CHANNEL, "0.010", 0);
        disabled.setState((byte) 0);
        List<FeeRule> rules = List.of(rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.002", 0), disabled);

        List<FeeWaterfall.LayerRule> resolved = FeeWaterfall.resolve("M1", null, null, rules);

        assertEquals(List.of("DEFAULT", "NONE", "NONE", "NONE"),
                resolved.stream().map(FeeWaterfall.LayerRule::getSource).toList());
    }

    @Test
    void merchantOverrideOnAgentLayerIsIgnoredWithoutRecipient() {
        List<FeeRule> rules = List.of(
                rule(FeeRule.TARGET_MCH, "M1", FeeRule.LAYER_SR_AGENT, "0.003", 0),
                rule(FeeRule.TARGET_MCH, "M1", FeeRule.LAYER_AGENT, "0.004", 100));

        // 未綁定代理：兩個代理層都沒有收款人，覆寫不生效
        assertEquals(0L, FeeWaterfall.compute(FeeWaterfall.resolve("M1", null, null, rules), 100_000).getTotalFee());
        // 直屬高級代理：高代費生效，代理費仍無收款人
        List<FeeWaterfall.LayerRule> resolved = FeeWaterfall.resolve("M1", SENIOR, SENIOR, rules);
        assertEquals("MCH", resolved.get(2).getSource());
        assertEquals("NONE", resolved.get(3).getSource());
    }

    @Test
    void computeRoundsHalfUpPerLayerAndFlagsFeesAboveAmount() {
        List<FeeWaterfall.LayerRule> resolved = FeeWaterfall.resolve("M1", null, null,
                List.of(rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.015", 0)));
        // 4050 × 1.5% = 60.75 → 61
        assertEquals(61L, FeeWaterfall.compute(resolved, 4050).getTotalFee());

        List<FeeWaterfall.LayerRule> heavy = FeeWaterfall.resolve("M1", null, null,
                List.of(rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_CHANNEL, "0", 5000)));
        FeeWaterfall.Breakdown b = FeeWaterfall.compute(heavy, 4000);
        assertFalse(b.isValid());
        assertEquals(-1000L, b.getNetAmount());
    }

    @Test
    void validateRuleRejectsWrongLayerTargetCombinationsAndRanges() {
        assertThrows(BizException.class, () -> FeeWaterfall.validateRule(
                rule(FeeRule.TARGET_AGENT, "A_SR", FeeRule.LAYER_PLATFORM, "0.001", 0)));
        assertThrows(BizException.class, () -> FeeWaterfall.validateRule(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_AGENT, "0.001", 0)));
        assertThrows(BizException.class, () -> FeeWaterfall.validateRule(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "1", 0)));
        assertThrows(BizException.class, () -> FeeWaterfall.validateRule(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "-0.001", 0)));
        assertThrows(BizException.class, () -> FeeWaterfall.validateRule(
                rule(FeeRule.TARGET_MCH, "M1", FeeRule.LAYER_AGENT, "0.001", -1)));
        assertThrows(BizException.class, () -> FeeWaterfall.validateRule(
                rule(FeeRule.TARGET_DEFAULT, "", FeeRule.LAYER_PLATFORM, "0.0000001", 0)));
        FeeWaterfall.validateRule(rule(FeeRule.TARGET_MCH, "M1", FeeRule.LAYER_PLATFORM, "0.0025", 300));
    }

    @Test
    void validateAgentLayerRequiresMatchingLevel() {
        FeeWaterfall.validateAgentLayer(FeeRule.LAYER_SR_AGENT, SENIOR);
        FeeWaterfall.validateAgentLayer(FeeRule.LAYER_AGENT, AGENT);
        assertThrows(BizException.class, () -> FeeWaterfall.validateAgentLayer(FeeRule.LAYER_SR_AGENT, AGENT));
        assertThrows(BizException.class, () -> FeeWaterfall.validateAgentLayer(FeeRule.LAYER_AGENT, SENIOR));
        assertThrows(BizException.class, () -> FeeWaterfall.validateAgentLayer(FeeRule.LAYER_AGENT, null));
    }

    private static FeeRule rule(String targetType, String targetId, String layer, String rate, long fixed) {
        return new FeeRule().setWayCode(WAY).setTargetType(targetType).setTargetId(targetId)
                .setLayer(layer).setRate(new BigDecimal(rate)).setFixedAmount(fixed).setState((byte) 1);
    }

    private static AgentInfo agent(String no, byte level, String parent) {
        return new AgentInfo().setAgentNo(no).setAgentLevel(level).setParentAgentNo(parent).setState((byte) 1);
    }
}
