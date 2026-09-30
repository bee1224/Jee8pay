package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeTemplate;
import com.jeequan.jeepay.core.entity.FeeTemplateItem;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.fee.FeeWaterfall;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.FeeTemplateItemMapper;
import com.jeequan.jeepay.service.mapper.FeeTemplateMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 費率範本（ADR-0009 第四階段）：只含高代費／代理費，平臺層不走範本（平臺層須逐筆雙人覆核）。
 * 套用即逐筆呼叫 FeeRuleService.saveRule，沿用同一套檢查與變更紀錄；範本日後修改不回溯已套用的規則。
 */
@Service
public class FeeTemplateService extends ServiceImpl<FeeTemplateMapper, FeeTemplate> {

    @Autowired private FeeTemplateItemMapper itemMapper;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private AgentInfoMapper agentInfoMapper;

    public List<FeeTemplateItem> items(Long templateId) {
        return itemMapper.selectList(FeeTemplateItem.gw().eq(FeeTemplateItem::getTemplateId, templateId)
                .orderByAsc(FeeTemplateItem::getWayCode, FeeTemplateItem::getLayer));
    }

    /** 新增或整份覆寫範本（明細全刪後重建）。 */
    @Transactional
    public FeeTemplate saveTemplate(FeeTemplate input, List<FeeTemplateItem> items, Long uid, String name) {
        if (StringUtils.isBlank(input.getTemplateName())) {
            throw new BizException("範本名稱為必填");
        }
        if (items == null || items.isEmpty()) {
            throw new BizException("範本至少需要一筆費率");
        }
        Set<String> keys = new HashSet<>();
        for (FeeTemplateItem item : items) {
            if (FeeRuleService.isPlatformLayer(item.getLayer())) {
                throw new BizException("範本只能包含高代費、代理費與推薦佣金");
            }
            if (!keys.add(item.getWayCode() + "|" + item.getLayer())) {
                throw new BizException("範本中同一支付方式與費率層重複：" + item.getWayCode() + " " + item.getLayer());
            }
            // 以虛擬代理對象套用與正式規則相同的費率範圍檢查
            FeeWaterfall.validateRule(new FeeRule().setWayCode(item.getWayCode()).setTargetType(FeeRule.TARGET_AGENT)
                    .setTargetId("TEMPLATE").setLayer(item.getLayer())
                    .setRate(item.getRate()).setFixedAmount(item.getFixedAmount() == null ? 0L : item.getFixedAmount()));
        }
        FeeTemplate template = new FeeTemplate().setTemplateId(input.getTemplateId())
                .setTemplateName(input.getTemplateName().trim()).setRemark(input.getRemark())
                .setUpdatedUid(uid).setUpdatedBy(name);
        if (template.getTemplateId() == null) {
            save(template);
        } else {
            if (getById(template.getTemplateId()) == null) {
                throw new BizException("範本不存在");
            }
            updateById(template);
            itemMapper.delete(FeeTemplateItem.gw().eq(FeeTemplateItem::getTemplateId, template.getTemplateId()));
        }
        for (FeeTemplateItem item : items) {
            itemMapper.insert(new FeeTemplateItem().setTemplateId(template.getTemplateId()).setWayCode(item.getWayCode())
                    .setLayer(item.getLayer()).setRate(item.getRate())
                    .setFixedAmount(item.getFixedAmount() == null ? 0L : item.getFixedAmount()));
        }
        return template;
    }

    @Transactional
    public void removeTemplate(Long templateId) {
        itemMapper.delete(FeeTemplateItem.gw().eq(FeeTemplateItem::getTemplateId, templateId));
        if (!removeById(templateId)) {
            throw new BizException("範本不存在");
        }
    }

    /**
     * 套用到多個代理或商戶（同一交易）。代理對象只套用與其層級相符的明細
     * （高級代理取高代費、一般代理取代理費），其餘略過並計數回報。
     */
    @Transactional
    public JSONObject apply(Long templateId, String targetType, List<String> targetIds, Long uid, String name) {
        if (getById(templateId) == null) {
            throw new BizException("範本不存在");
        }
        if (!FeeRule.TARGET_AGENT.equals(targetType) && !FeeRule.TARGET_MCH.equals(targetType)) {
            throw new BizException("範本只能套用到代理或商戶");
        }
        if (targetIds == null || targetIds.isEmpty()) {
            throw new BizException("請選擇套用對象");
        }
        List<FeeTemplateItem> items = items(templateId);
        int applied = 0;
        int skipped = 0;
        for (String targetId : targetIds) {
            AgentInfo agent = FeeRule.TARGET_AGENT.equals(targetType) ? agentInfoMapper.selectById(targetId) : null;
            if (FeeRule.TARGET_AGENT.equals(targetType) && agent == null) {
                throw new BizException("代理不存在：" + targetId);
            }
            for (FeeTemplateItem item : items) {
                if (agent != null && !layerMatches(item.getLayer(), agent)) {
                    skipped++;
                    continue;
                }
                feeRuleService.saveRule(new FeeRule().setWayCode(item.getWayCode()).setTargetType(targetType)
                        .setTargetId(targetId).setLayer(item.getLayer()).setRate(item.getRate())
                        .setFixedAmount(item.getFixedAmount()), uid, name + "（範本 #" + templateId + "）");
                applied++;
            }
        }
        JSONObject result = new JSONObject();
        result.put("applied", applied);
        result.put("skipped", skipped);
        return result;
    }

    static boolean layerMatches(String layer, AgentInfo agent) {
        return FeeRule.LAYER_REFERRER.equals(layer)
                || (FeeRule.LAYER_SR_AGENT.equals(layer) && AgentInfo.LEVEL_SENIOR == agent.getAgentLevel())
                || (FeeRule.LAYER_AGENT.equals(layer) && AgentInfo.LEVEL_AGENT == agent.getAgentLevel());
    }
}
