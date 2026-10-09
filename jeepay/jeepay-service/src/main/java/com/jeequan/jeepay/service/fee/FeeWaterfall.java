package com.jeequan.jeepay.service.fee;

import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 四層手續費瀑布（ADR-0009）的純計算核心：不接觸 DB，給定規則與代理鏈即可解析與試算。
 * 第二階段的訂單費率快照直接沿用本類別，確保「設定頁試算」與「實際扣費」同一套邏輯。
 */
public final class FeeWaterfall {

    /** 計算順序即顯示順序 */
    public static final List<String> LAYERS = Collections.unmodifiableList(List.of(
            FeeRule.LAYER_PLATFORM, FeeRule.LAYER_CHANNEL, FeeRule.LAYER_SR_AGENT, FeeRule.LAYER_AGENT));

    private FeeWaterfall() {
    }

    /** 單層解析結果；source 說明採用了哪一條規則（DEFAULT／AGENT:代理號／MCH／NONE）。 */
    public static final class LayerRule {
        private final String layer;
        private final BigDecimal rate;
        private final long fixedAmount;
        private final String source;

        LayerRule(String layer, BigDecimal rate, long fixedAmount, String source) {
            this.layer = layer;
            this.rate = rate;
            this.fixedAmount = fixedAmount;
            this.source = source;
        }

        public String getLayer() { return layer; }
        public BigDecimal getRate() { return rate; }
        public long getFixedAmount() { return fixedAmount; }
        public String getSource() { return source; }
    }

    /** 單層試算結果（金額單位：分） */
    public static final class LayerFee {
        private final LayerRule rule;
        private final long fee;

        LayerFee(LayerRule rule, long fee) {
            this.rule = rule;
            this.fee = fee;
        }

        public String getLayer() { return rule.getLayer(); }
        public BigDecimal getRate() { return rule.getRate(); }
        public long getFixedAmount() { return rule.getFixedAmount(); }
        public String getSource() { return rule.getSource(); }
        public long getFee() { return fee; }
    }

    /** 整筆試算結果：totalFee 超過 amount 時 valid=false，不得用於結算。 */
    public static final class Breakdown {
        private final long amount;
        private final List<LayerFee> layers;
        private final long totalFee;

        Breakdown(long amount, List<LayerFee> layers) {
            this.amount = amount;
            this.layers = Collections.unmodifiableList(layers);
            this.totalFee = layers.stream().mapToLong(LayerFee::getFee).sum();
        }

        public long getAmount() { return amount; }
        public List<LayerFee> getLayers() { return layers; }
        public long getTotalFee() { return totalFee; }
        public long getNetAmount() { return amount - totalFee; }
        public boolean isValid() { return totalFee <= amount; }
    }

    /**
     * 解析某商戶在某支付方式下的四層費率。
     *
     * @param mchNo       商戶號
     * @param directAgent 商戶的直屬代理（可為 null：未綁定代理則團長費／隊長費為 0）
     * @param seniorAgent 直屬代理所屬的團長；直屬代理本身即團長時與 directAgent 相同
     * @param rules       該支付方式下已啟用的規則（DEFAULT、相關代理、該商戶的 MCH 覆寫）
     */
    public static List<LayerRule> resolve(String mchNo, AgentInfo directAgent, AgentInfo seniorAgent, List<FeeRule> rules) {
        List<LayerRule> result = new ArrayList<>(LAYERS.size());
        boolean hasSenior = seniorAgent != null;
        boolean hasLevel2Agent = directAgent != null && Objects.equals(directAgent.getAgentLevel(), AgentInfo.LEVEL_AGENT);
        for (String layer : LAYERS) {
            // 代理層沒有對應的收款代理時一律為 0，即使商戶有覆寫也不收（避免收了無人可分的費用）
            if ((FeeRule.LAYER_SR_AGENT.equals(layer) && !hasSenior) || (FeeRule.LAYER_AGENT.equals(layer) && !hasLevel2Agent)) {
                result.add(new LayerRule(layer, BigDecimal.ZERO, 0L, "NONE"));
                continue;
            }
            FeeRule mchOverride = find(rules, FeeRule.TARGET_MCH, mchNo, layer);
            if (mchOverride != null) {
                result.add(toLayerRule(mchOverride, FeeRule.TARGET_MCH));
                continue;
            }
            FeeRule base = null;
            String source = "NONE";
            if (FeeRule.LAYER_PLATFORM.equals(layer) || FeeRule.LAYER_CHANNEL.equals(layer)) {
                base = find(rules, FeeRule.TARGET_DEFAULT, "", layer);
                source = FeeRule.TARGET_DEFAULT;
            } else if (FeeRule.LAYER_SR_AGENT.equals(layer)) {
                base = find(rules, FeeRule.TARGET_AGENT, seniorAgent.getAgentNo(), layer);
                source = FeeRule.TARGET_AGENT + ":" + seniorAgent.getAgentNo();
            } else if (FeeRule.LAYER_AGENT.equals(layer)) {
                base = find(rules, FeeRule.TARGET_AGENT, directAgent.getAgentNo(), layer);
                source = FeeRule.TARGET_AGENT + ":" + directAgent.getAgentNo();
            }
            result.add(base == null ? new LayerRule(layer, BigDecimal.ZERO, 0L, "NONE") : toLayerRule(base, source));
        }
        return result;
    }

    /** 試算：每層手續費 = 金額 × 費率（四捨五入到分）＋ 單筆固定金額。 */
    public static Breakdown compute(List<LayerRule> resolved, long amount) {
        if (amount < 0) {
            throw new BizException("試算金額不可為負數");
        }
        List<LayerFee> fees = new ArrayList<>(resolved.size());
        for (LayerRule rule : resolved) {
            long percentPart = rule.getRate().multiply(BigDecimal.valueOf(amount))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            fees.add(new LayerFee(rule, Math.addExact(percentPart, rule.getFixedAmount())));
        }
        return new Breakdown(amount, fees);
    }

    /**
     * 規則本身的合法性檢查（不含對象是否存在）。
     * 平臺費／渠道費只允許 DEFAULT 或 MCH；團長費／隊長費只允許 AGENT 或 MCH。
     */
    public static void validateRule(FeeRule rule) {
        if (rule == null || isBlank(rule.getWayCode()) || isBlank(rule.getTargetType()) || isBlank(rule.getLayer())) {
            throw new BizException("支付方式、對象類型與費率層皆為必填");
        }
        if (!LAYERS.contains(rule.getLayer())) {
            throw new BizException("不支援的費率層：" + rule.getLayer());
        }
        boolean platformLayer = FeeRule.LAYER_PLATFORM.equals(rule.getLayer()) || FeeRule.LAYER_CHANNEL.equals(rule.getLayer());
        switch (rule.getTargetType()) {
            case FeeRule.TARGET_DEFAULT:
                if (!platformLayer) {
                    throw new BizException("平台預設只能設定平臺費與渠道費");
                }
                if (!isBlank(rule.getTargetId())) {
                    throw new BizException("平台預設不可指定對象");
                }
                break;
            case FeeRule.TARGET_AGENT:
                if (platformLayer) {
                    throw new BizException("代理只能設定團長費或隊長費");
                }
                if (isBlank(rule.getTargetId())) {
                    throw new BizException("請指定代理");
                }
                break;
            case FeeRule.TARGET_MCH:
                if (isBlank(rule.getTargetId())) {
                    throw new BizException("請指定商戶");
                }
                break;
            default:
                throw new BizException("不支援的對象類型：" + rule.getTargetType());
        }
        BigDecimal rate = rule.getRate() == null ? BigDecimal.ZERO : rule.getRate();
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new BizException("費率必須介於 0 與 100% 之間");
        }
        if (rate.scale() > 6 && rate.stripTrailingZeros().scale() > 6) {
            throw new BizException("費率最多 6 位小數（0.0001%）");
        }
        if (rule.getFixedAmount() != null && rule.getFixedAmount() < 0) {
            throw new BizException("單筆固定金額不可為負數");
        }
    }

    /** 代理類規則須對應層級：團長費 ↔ 團長、隊長費 ↔ 隊長。 */
    public static void validateAgentLayer(String layer, AgentInfo agent) {
        if (agent == null) {
            throw new BizException("代理不存在");
        }
        byte expected = FeeRule.LAYER_SR_AGENT.equals(layer) ? AgentInfo.LEVEL_SENIOR : AgentInfo.LEVEL_AGENT;
        if (!Objects.equals(agent.getAgentLevel(), expected)) {
            throw new BizException(FeeRule.LAYER_SR_AGENT.equals(layer) ? "團長費只能設定在團長上" : "隊長費只能設定在隊長上");
        }
    }

    private static FeeRule find(List<FeeRule> rules, String targetType, String targetId, String layer) {
        for (FeeRule rule : rules) {
            if (targetType.equals(rule.getTargetType()) && layer.equals(rule.getLayer())
                    && Objects.equals(nullToEmpty(targetId), nullToEmpty(rule.getTargetId()))
                    && (rule.getState() == null || rule.getState() == 1)) {
                return rule;
            }
        }
        return null;
    }

    private static LayerRule toLayerRule(FeeRule rule, String source) {
        return new LayerRule(rule.getLayer(),
                rule.getRate() == null ? BigDecimal.ZERO : rule.getRate(),
                rule.getFixedAmount() == null ? 0L : rule.getFixedAmount(),
                source);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
