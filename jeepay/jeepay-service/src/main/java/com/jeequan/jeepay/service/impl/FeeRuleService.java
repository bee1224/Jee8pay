package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeRuleLog;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.AmountUtil;
import com.jeequan.jeepay.service.fee.FeeWaterfall;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.AgentMchRelaMapper;
import com.jeequan.jeepay.service.mapper.FeeRuleLogMapper;
import com.jeequan.jeepay.service.mapper.FeeRuleMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 四層手續費規則服務（ADR-0009）：規則設定、解析與試算；下單快照見 PayOrderFeeService。
 * 為避免服務間循環依賴，代理與綁定資料直接經由 mapper 讀取。
 */
@Service
public class FeeRuleService extends ServiceImpl<FeeRuleMapper, FeeRule> {

    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private AgentMchRelaMapper agentMchRelaMapper;
    @Autowired private FeeRuleLogMapper feeRuleLogMapper;
    @Autowired private MchInfoService mchInfoService;
    @Autowired private MchPayPassageService mchPayPassageService;

    /** 平臺費、渠道費屬平台鎖定層；呼叫端須另具平台層權限才可修改。 */
    public static boolean isPlatformLayer(String layer) {
        return FeeRule.LAYER_PLATFORM.equals(layer) || FeeRule.LAYER_CHANNEL.equals(layer);
    }

    /** 依（支付方式、對象、費率層）新增或覆蓋規則，並寫入變更紀錄。 */
    @Transactional
    public FeeRule saveRule(FeeRule input, Long operatorUid, String operatorName) {
        normalizeAndValidate(input);

        FeeRule existing = getOne(FeeRule.gw()
                .eq(FeeRule::getWayCode, input.getWayCode())
                .eq(FeeRule::getTargetType, input.getTargetType())
                .eq(FeeRule::getTargetId, input.getTargetId())
                .eq(FeeRule::getLayer, input.getLayer()));

        FeeRule rule = new FeeRule()
                .setWayCode(input.getWayCode())
                .setTargetType(input.getTargetType())
                .setTargetId(input.getTargetId())
                .setLayer(input.getLayer())
                .setRate(input.getRate())
                .setFixedAmount(input.getFixedAmount())
                .setState(input.getState())
                .setUpdatedUid(operatorUid)
                .setUpdatedBy(operatorName);
        boolean ok;
        if (existing == null) {
            ok = save(rule);
        } else {
            rule.setRuleId(existing.getRuleId());
            ok = updateById(rule);
        }
        if (!ok) {
            throw new BizException("儲存費率規則失敗");
        }
        writeLog(rule, FeeRuleLog.ACTION_SAVE, existing == null ? null : snapshot(existing), snapshot(rule), operatorUid, operatorName);
        return rule;
    }

    @Transactional
    public void removeRule(Long ruleId, Long operatorUid, String operatorName) {
        FeeRule existing = getById(ruleId);
        if (existing == null) {
            throw new BizException("費率規則不存在");
        }
        if (!removeById(ruleId)) {
            throw new BizException("刪除費率規則失敗");
        }
        writeLog(existing, FeeRuleLog.ACTION_DELETE, snapshot(existing), null, operatorUid, operatorName);
    }

    /** 商戶當下的代理鏈：直屬代理、所屬高級代理（直屬即高級代理時相同）、推薦人代理號。 */
    public static final class AgentChain {
        private final AgentInfo direct;
        private final AgentInfo senior;
        private final AgentInfo referrer;

        public AgentChain(AgentInfo direct, AgentInfo senior, AgentInfo referrer) {
            this.direct = direct;
            this.senior = senior;
            this.referrer = referrer;
        }

        public AgentInfo getDirect() { return direct; }
        public AgentInfo getSenior() { return senior; }
        public AgentInfo getReferrer() { return referrer; }
        public String getReferrerAgentNo() { return referrer == null ? null : referrer.getAgentNo(); }
    }

    public AgentChain agentChainOf(String mchNo) {
        AgentMchRela rela = agentMchRelaMapper.selectById(mchNo);
        AgentInfo direct = rela == null ? null : agentInfoMapper.selectById(rela.getAgentNo());
        AgentInfo senior = direct;
        if (direct != null && Objects.equals(direct.getAgentLevel(), AgentInfo.LEVEL_AGENT)) {
            senior = StringUtils.isBlank(direct.getParentAgentNo()) ? null : agentInfoMapper.selectById(direct.getParentAgentNo());
        }
        String referrerNo = rela == null ? null : StringUtils.trimToNull(rela.getReferrerAgentNo());
        AgentInfo referrer = referrerNo == null ? null : agentInfoMapper.selectById(referrerNo);
        // 停用的推薦人不再分佣
        if (referrer != null && !Objects.equals(referrer.getState(), (byte) 1)) {
            referrer = null;
        }
        return new AgentChain(direct, senior, referrer);
    }

    /** 解析某商戶在某支付方式下的四層費率（含來源說明）。 */
    public List<FeeWaterfall.LayerRule> resolveForMch(String mchNo, String wayCode) {
        if (mchInfoService.getById(mchNo) == null) {
            throw new BizException("商戶不存在");
        }
        return resolve(mchNo, wayCode, agentChainOf(mchNo));
    }

    /** 以指定代理鏈解析；下單快照先取代理鏈再解析，確保快照記錄的代理與費率一致。 */
    public List<FeeWaterfall.LayerRule> resolve(String mchNo, String wayCode, AgentChain chain) {
        AgentInfo direct = chain.getDirect();
        AgentInfo senior = chain.getSenior();
        List<String> agentIds = new ArrayList<>();
        if (direct != null) {
            agentIds.add(direct.getAgentNo());
        }
        if (senior != null && !agentIds.contains(senior.getAgentNo())) {
            agentIds.add(senior.getAgentNo());
        }
        if (chain.getReferrer() != null && !agentIds.contains(chain.getReferrer().getAgentNo())) {
            agentIds.add(chain.getReferrer().getAgentNo());
        }
        List<FeeRule> rules = list(FeeRule.gw()
                .eq(FeeRule::getWayCode, wayCode)
                .eq(FeeRule::getState, (byte) 1)
                .and(w -> {
                    w.eq(FeeRule::getTargetType, FeeRule.TARGET_DEFAULT)
                            .or(x -> x.eq(FeeRule::getTargetType, FeeRule.TARGET_MCH).eq(FeeRule::getTargetId, mchNo));
                    if (!agentIds.isEmpty()) {
                        w.or(x -> x.eq(FeeRule::getTargetType, FeeRule.TARGET_AGENT).in(FeeRule::getTargetId, agentIds));
                    }
                }));
        return FeeWaterfall.resolve(mchNo, direct, senior, chain.getReferrer(), rules);
    }

    public FeeWaterfall.Breakdown preview(String mchNo, String wayCode, long amount) {
        return FeeWaterfall.compute(resolveForMch(mchNo, wayCode), amount);
    }

    /** 補預設值並檢查規則與對象；變更申請、範本與批次共用，確保與單筆儲存同一套檢查。 */
    void normalizeAndValidate(FeeRule input) {
        input.setTargetId(StringUtils.trimToEmpty(input.getTargetId()));
        input.setRate(input.getRate() == null ? BigDecimal.ZERO : input.getRate());
        input.setFixedAmount(input.getFixedAmount() == null ? 0L : input.getFixedAmount());
        input.setState(input.getState() == null ? (byte) 1 : input.getState());
        FeeWaterfall.validateRule(input);
        validateTarget(input);
    }

    /** 批次儲存代理層費率（同一交易，任一筆失敗全部回滾）；平臺層須逐筆走雙人覆核。 */
    @Transactional
    public int saveBatch(List<FeeRule> rules, Long operatorUid, String operatorName) {
        if (rules == null || rules.isEmpty()) {
            throw new BizException("沒有要儲存的費率");
        }
        for (FeeRule rule : rules) {
            if (isPlatformLayer(rule.getLayer())) {
                throw new BizException("平臺費與渠道費不可批次修改，請逐筆送出覆核");
            }
            saveRule(rule, operatorUid, operatorName);
        }
        return rules.size();
    }

    /**
     * 風險檢查：以參考金額試算，列出四層合計超過商戶手續費（支付通道費率）的商戶通道。
     * 合計超過代表平台／代理分到的比商戶付的還多，須調整費率。
     */
    public List<JSONObject> riskCheck(String wayCode, long referenceAmount) {
        return riskCheck(wayCode, referenceAmount, null);
    }

    /** 限定商戶範圍的風險檢查；mchNos 為 null 表示全部，空集合表示沒有需要檢查的商戶。 */
    public List<JSONObject> riskCheck(String wayCode, long referenceAmount, java.util.Collection<String> mchNos) {
        if (mchNos != null && mchNos.isEmpty()) {
            return new ArrayList<>();
        }
        List<MchPayPassage> passages = mchPayPassageService.list(MchPayPassage.gw()
                .eq(MchPayPassage::getState, (byte) 1)
                .eq(StringUtils.isNotBlank(wayCode), MchPayPassage::getWayCode, wayCode)
                .in(mchNos != null, MchPayPassage::getMchNo, mchNos)
                .orderByAsc(MchPayPassage::getMchNo, MchPayPassage::getWayCode));
        List<JSONObject> result = new ArrayList<>();
        for (MchPayPassage passage : passages) {
            FeeWaterfall.Breakdown b = FeeWaterfall.compute(
                    resolve(passage.getMchNo(), passage.getWayCode(), agentChainOf(passage.getMchNo())), referenceAmount);
            long mchFee = AmountUtil.calPercentageFee(referenceAmount, passage.getRate() == null ? BigDecimal.ZERO : passage.getRate());
            if (b.getTotalFee() > mchFee) {
                JSONObject row = new JSONObject(true);
                row.put("mchNo", passage.getMchNo());
                row.put("appId", passage.getAppId());
                row.put("wayCode", passage.getWayCode());
                row.put("mchRate", passage.getRate());
                row.put("mchFee", mchFee);
                row.put("totalFee", b.getTotalFee());
                row.put("layers", b.getLayers());
                result.add(row);
            }
        }
        return result;
    }

    private void validateTarget(FeeRule rule) {
        if (FeeRule.TARGET_AGENT.equals(rule.getTargetType())) {
            FeeWaterfall.validateAgentLayer(rule.getLayer(), agentInfoMapper.selectById(rule.getTargetId()));
        } else if (FeeRule.TARGET_MCH.equals(rule.getTargetType()) && mchInfoService.getById(rule.getTargetId()) == null) {
            throw new BizException("商戶不存在");
        }
    }

    private void writeLog(FeeRule key, String action, String before, String after, Long operatorUid, String operatorName) {
        feeRuleLogMapper.insert(new FeeRuleLog()
                .setWayCode(key.getWayCode())
                .setTargetType(key.getTargetType())
                .setTargetId(key.getTargetId())
                .setLayer(key.getLayer())
                .setAction(action)
                .setBeforeValue(before)
                .setAfterValue(after)
                .setOperatorUid(operatorUid)
                .setOperatorName(operatorName));
    }

    private static String snapshot(FeeRule rule) {
        JSONObject json = new JSONObject(true);
        json.put("rate", rule.getRate() == null ? null : rule.getRate().stripTrailingZeros().toPlainString());
        json.put("fixedAmount", rule.getFixedAmount());
        json.put("state", rule.getState());
        return json.toJSONString();
    }
}
