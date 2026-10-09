package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.PayOrderFee;
import com.jeequan.jeepay.service.fee.FeeWaterfall;
import com.jeequan.jeepay.service.mapper.PayOrderFeeMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 訂單四層手續費快照（ADR-0009 第二階段）。
 * 快照只做記錄與分潤統計，不改變 PayOrder 的 mchFeeRate／mchFeeAmount 與結算語意。
 */
@Slf4j
@Service
public class PayOrderFeeService extends ServiceImpl<PayOrderFeeMapper, PayOrderFee> {

    @Autowired private FeeRuleService feeRuleService;

    /**
     * 下單後寫入快照；已存在則不覆寫（快照一經建立即不可變）。
     * 任何例外只記錄錯誤、不往外拋：費率快照失敗不得阻斷商戶收款。
     */
    public void snapshotQuietly(PayOrder payOrder) {
        try {
            if (payOrder == null || StringUtils.isAnyBlank(payOrder.getPayOrderId(), payOrder.getMchNo(), payOrder.getWayCode())
                    || payOrder.getAmount() == null) {
                return;
            }
            if (getById(payOrder.getPayOrderId()) != null) {
                return;
            }
            FeeRuleService.AgentChain chain = feeRuleService.agentChainOf(payOrder.getMchNo());
            FeeWaterfall.Breakdown breakdown = FeeWaterfall.compute(
                    feeRuleService.resolve(payOrder.getMchNo(), payOrder.getWayCode(), chain), payOrder.getAmount());
            save(build(payOrder, chain, breakdown));
        } catch (Exception e) {
            log.error("訂單手續費快照寫入失敗 payOrderId={}，請以 t_pay_order_fee 缺漏檢查補登", payOrder == null ? null : payOrder.getPayOrderId(), e);
        }
    }

    /** 由訂單、代理鏈與試算結果組出快照；純函式，便於單元測試。 */
    public static PayOrderFee build(PayOrder payOrder, FeeRuleService.AgentChain chain, FeeWaterfall.Breakdown breakdown) {
        PayOrderFee fee = new PayOrderFee()
                .setPayOrderId(payOrder.getPayOrderId())
                .setMchNo(payOrder.getMchNo())
                .setWayCode(payOrder.getWayCode())
                .setAmount(payOrder.getAmount())
                .setMchFeeAmount(payOrder.getMchFeeAmount() == null ? 0L : payOrder.getMchFeeAmount())
                .setAgentNo(chain.getDirect() == null ? null : chain.getDirect().getAgentNo())
                .setSrAgentNo(chain.getSenior() == null ? null : chain.getSenior().getAgentNo())
                .setPlatformFee(0L).setChannelFee(0L).setSrAgentFee(0L).setAgentFee(0L)
                .setTotalFee(breakdown.getTotalFee());

        JSONArray detail = new JSONArray();
        for (FeeWaterfall.LayerFee layer : breakdown.getLayers()) {
            switch (layer.getLayer()) {
                case FeeRule.LAYER_PLATFORM: fee.setPlatformFee(layer.getFee()); break;
                case FeeRule.LAYER_CHANNEL: fee.setChannelFee(layer.getFee()); break;
                case FeeRule.LAYER_SR_AGENT: fee.setSrAgentFee(layer.getFee()); break;
                case FeeRule.LAYER_AGENT: fee.setAgentFee(layer.getFee()); break;
                default: break;
            }
            JSONObject item = new JSONObject(true);
            item.put("layer", layer.getLayer());
            item.put("rate", layer.getRate().stripTrailingZeros().toPlainString());
            item.put("fixedAmount", layer.getFixedAmount());
            item.put("fee", layer.getFee());
            item.put("source", layer.getSource());
            detail.add(item);
        }
        fee.setExceedsMchFee((byte) (breakdown.getTotalFee() > fee.getMchFeeAmount() ? 1 : 0));
        fee.setDetail(detail.toJSONString());
        return fee;
    }
}
