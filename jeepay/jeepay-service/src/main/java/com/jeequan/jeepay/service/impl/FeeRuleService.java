package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeRuleLog;
import com.jeequan.jeepay.core.exception.BizException;
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
 * 四層手續費規則服務（ADR-0009 第一階段：僅設定面，不影響下單與結算）。
 * 為避免服務間循環依賴，代理與綁定資料直接經由 mapper 讀取。
 */
@Service
public class FeeRuleService extends ServiceImpl<FeeRuleMapper, FeeRule> {

    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private AgentMchRelaMapper agentMchRelaMapper;
    @Autowired private FeeRuleLogMapper feeRuleLogMapper;
    @Autowired private MchInfoService mchInfoService;

    /** 平臺費、渠道費屬平台鎖定層；呼叫端須另具平台層權限才可修改。 */
    public static boolean isPlatformLayer(String layer) {
        return FeeRule.LAYER_PLATFORM.equals(layer) || FeeRule.LAYER_CHANNEL.equals(layer);
    }

    /** 依（支付方式、對象、費率層）新增或覆蓋規則，並寫入變更紀錄。 */
    @Transactional
    public FeeRule saveRule(FeeRule input, Long operatorUid, String operatorName) {
        input.setTargetId(StringUtils.trimToEmpty(input.getTargetId()));
        input.setRate(input.getRate() == null ? BigDecimal.ZERO : input.getRate());
        input.setFixedAmount(input.getFixedAmount() == null ? 0L : input.getFixedAmount());
        input.setState(input.getState() == null ? (byte) 1 : input.getState());
        FeeWaterfall.validateRule(input);
        validateTarget(input);

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

    /** 解析某商戶在某支付方式下的四層費率（含來源說明）。 */
    public List<FeeWaterfall.LayerRule> resolveForMch(String mchNo, String wayCode) {
        if (mchInfoService.getById(mchNo) == null) {
            throw new BizException("商戶不存在");
        }
        AgentMchRela rela = agentMchRelaMapper.selectById(mchNo);
        AgentInfo direct = rela == null ? null : agentInfoMapper.selectById(rela.getAgentNo());
        AgentInfo senior = direct;
        if (direct != null && Objects.equals(direct.getAgentLevel(), AgentInfo.LEVEL_AGENT)) {
            senior = StringUtils.isBlank(direct.getParentAgentNo()) ? null : agentInfoMapper.selectById(direct.getParentAgentNo());
        }

        List<String> agentIds = new ArrayList<>();
        if (direct != null) {
            agentIds.add(direct.getAgentNo());
        }
        if (senior != null && !agentIds.contains(senior.getAgentNo())) {
            agentIds.add(senior.getAgentNo());
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
        return FeeWaterfall.resolve(mchNo, direct, senior, rules);
    }

    public FeeWaterfall.Breakdown preview(String mchNo, String wayCode, long amount) {
        return FeeWaterfall.compute(resolveForMch(mchNo, wayCode), amount);
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
