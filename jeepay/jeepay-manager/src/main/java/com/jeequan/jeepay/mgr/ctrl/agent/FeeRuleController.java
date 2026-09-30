package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeRuleChangeReq;
import com.jeequan.jeepay.core.entity.FeeRuleLog;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.fee.FeeWaterfall;
import com.jeequan.jeepay.service.impl.FeeRuleChangeService;
import com.jeequan.jeepay.service.impl.FeeRuleService;
import com.jeequan.jeepay.service.mapper.FeeRuleLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 四層手續費規則（ADR-0009）。
 * 平臺費／渠道費為平台鎖定層：須持有 ENT_FEE_RULE_PLATFORM_EDIT 才能提出變更，
 * 且變更一律經另一位具 ENT_FEE_RULE_REVIEW 者核准後才生效（雙人覆核），在後端擋下，不只靠前端。
 */
@Tag(name = "費率設定")
@RestController
@RequestMapping("/api/feeRules")
public class FeeRuleController extends AgentBaseCtrl {

    private static final String PLATFORM_EDIT = "ENT_FEE_RULE_PLATFORM_EDIT";

    @Autowired private FeeRuleService feeRuleService;
    @Autowired private FeeRuleChangeService feeRuleChangeService;
    @Autowired private FeeRuleLogMapper feeRuleLogMapper;

    @Operation(summary = "費率規則列表")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_LIST')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<FeeRule> list() {
        requireAuthority("ENT_FEE_RULE_LIST");
        LambdaQueryWrapper<FeeRule> wrapper = FeeRule.gw();
        String wayCode = getValString("wayCode");
        String targetType = getValString("targetType");
        String targetId = getValString("targetId");
        if (StringUtils.isNotEmpty(wayCode)) {
            wrapper.eq(FeeRule::getWayCode, wayCode);
        }
        if (StringUtils.isNotEmpty(targetType)) {
            wrapper.eq(FeeRule::getTargetType, targetType);
        }
        if (StringUtils.isNotEmpty(targetId)) {
            wrapper.eq(FeeRule::getTargetId, targetId);
        }
        wrapper.orderByAsc(FeeRule::getWayCode, FeeRule::getTargetType, FeeRule::getTargetId, FeeRule::getLayer);
        IPage<FeeRule> pages = feeRuleService.page(getIPage(true), wrapper);
        return ApiPageRes.pages(pages);
    }

    @Operation(summary = "新增或修改費率規則")
    @PreAuthorize("hasAnyAuthority('ENT_FEE_RULE_EDIT', 'ENT_FEE_RULE_PLATFORM_EDIT')")
    @MethodLog(remark = "儲存費率規則")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes save() {
        requireAuthority("ENT_FEE_RULE_EDIT", "ENT_FEE_RULE_PLATFORM_EDIT");
        FeeRule rule = getObject(FeeRule.class);
        requireLayerPermission(rule.getLayer());
        // 平臺層不直接生效：建立變更申請，由另一位具覆核權限者核准後才寫入
        if (FeeRuleService.isPlatformLayer(rule.getLayer())) {
            return ApiRes.ok(pending(feeRuleChangeService.requestSave(rule, uid(), uname())));
        }
        return ApiRes.ok(feeRuleService.saveRule(rule, uid(), uname()));
    }

    @Operation(summary = "刪除費率規則")
    @PreAuthorize("hasAnyAuthority('ENT_FEE_RULE_EDIT', 'ENT_FEE_RULE_PLATFORM_EDIT')")
    @MethodLog(remark = "刪除費率規則")
    @RequestMapping(value = "/{ruleId}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("ruleId") Long ruleId) {
        requireAuthority("ENT_FEE_RULE_EDIT", "ENT_FEE_RULE_PLATFORM_EDIT");
        FeeRule rule = feeRuleService.getById(ruleId);
        if (rule == null) {
            throw new BizException("費率規則不存在");
        }
        requireLayerPermission(rule.getLayer());
        if (FeeRuleService.isPlatformLayer(rule.getLayer())) {
            return ApiRes.ok(pending(feeRuleChangeService.requestDelete(ruleId, uid(), uname())));
        }
        feeRuleService.removeRule(ruleId, uid(), uname());
        return ApiRes.ok();
    }

    @Operation(summary = "批次儲存代理層費率（同一交易；平臺層不可批次）")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_BATCH')")
    @MethodLog(remark = "批次儲存費率規則")
    @RequestMapping(value = "/batch", method = RequestMethod.POST)
    public ApiRes batch() {
        requireAuthority("ENT_FEE_RULE_BATCH");
        List<FeeRule> rules = JSONArray.parseArray(getValStringRequired("rules"), FeeRule.class);
        for (FeeRule rule : rules) {
            requireLayerPermission(rule.getLayer());
        }
        return ApiRes.ok(feeRuleService.saveBatch(rules, uid(), uname()));
    }

    @Operation(summary = "風險檢查：四層合計超過商戶手續費的商戶通道")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_LIST')")
    @RequestMapping(value = "/risk", method = RequestMethod.GET)
    public ApiRes risk() {
        requireAuthority("ENT_FEE_RULE_LIST");
        Long amount = getValLong("amount");
        return ApiRes.ok(feeRuleService.riskCheck(getValString("wayCode"), amount == null || amount <= 0 ? 100_000L : amount));
    }

    @Operation(summary = "平臺層變更申請列表")
    @PreAuthorize("hasAnyAuthority('ENT_FEE_RULE_LIST', 'ENT_FEE_RULE_REVIEW')")
    @RequestMapping(value = "/changeReqs", method = RequestMethod.GET)
    public ApiPageRes<FeeRuleChangeReq> changeReqs() {
        requireAuthority("ENT_FEE_RULE_LIST", "ENT_FEE_RULE_REVIEW");
        LambdaQueryWrapper<FeeRuleChangeReq> wrapper = FeeRuleChangeReq.gw();
        Byte state = getValByte("state");
        if (state != null) {
            wrapper.eq(FeeRuleChangeReq::getState, state);
        }
        wrapper.orderByDesc(FeeRuleChangeReq::getReqId);
        return ApiPageRes.pages(feeRuleChangeService.page(getIPage(true), wrapper));
    }

    @Operation(summary = "核准平臺層變更申請（不可覆核自己的申請）")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_REVIEW')")
    @MethodLog(remark = "核准費率變更申請")
    @RequestMapping(value = "/changeReqs/{reqId}/approve", method = RequestMethod.POST)
    public ApiRes approve(@PathVariable("reqId") Long reqId) {
        requireAuthority("ENT_FEE_RULE_REVIEW");
        feeRuleChangeService.approve(reqId, uid(), uname(), getValString("remark"));
        return ApiRes.ok();
    }

    @Operation(summary = "駁回平臺層變更申請")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_REVIEW')")
    @MethodLog(remark = "駁回費率變更申請")
    @RequestMapping(value = "/changeReqs/{reqId}/reject", method = RequestMethod.POST)
    public ApiRes reject(@PathVariable("reqId") Long reqId) {
        requireAuthority("ENT_FEE_RULE_REVIEW");
        feeRuleChangeService.reject(reqId, uid(), uname(), getValString("remark"));
        return ApiRes.ok();
    }

    private Long uid() {
        return getCurrentUser().getSysUser().getSysUserId();
    }

    private String uname() {
        return getCurrentUser().getSysUser().getRealname();
    }

    private static JSONObject pending(FeeRuleChangeReq req) {
        JSONObject result = new JSONObject();
        result.put("pendingReview", true);
        result.put("reqId", req.getReqId());
        return result;
    }

    @Operation(summary = "試算：某商戶在某支付方式下，指定金額（分）的四層手續費")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_LIST')")
    @RequestMapping(value = "/preview", method = RequestMethod.GET)
    public ApiRes<JSONObject> preview() {
        requireAuthority("ENT_FEE_RULE_LIST");
        String mchNo = getValStringRequired("mchNo");
        String wayCode = getValStringRequired("wayCode");
        long amount = getValLongRequired("amount");
        FeeWaterfall.Breakdown breakdown = feeRuleService.preview(mchNo, wayCode, amount);

        JSONArray layers = new JSONArray();
        for (FeeWaterfall.LayerFee fee : breakdown.getLayers()) {
            JSONObject item = new JSONObject(true);
            item.put("layer", fee.getLayer());
            item.put("rate", fee.getRate().stripTrailingZeros().toPlainString());
            item.put("fixedAmount", fee.getFixedAmount());
            item.put("source", fee.getSource());
            item.put("fee", fee.getFee());
            layers.add(item);
        }
        JSONObject result = new JSONObject(true);
        result.put("mchNo", mchNo);
        result.put("wayCode", wayCode);
        result.put("amount", breakdown.getAmount());
        result.put("layers", layers);
        result.put("totalFee", breakdown.getTotalFee());
        result.put("netAmount", breakdown.getNetAmount());
        result.put("valid", breakdown.isValid());
        return ApiRes.ok(result);
    }

    @Operation(summary = "費率變更紀錄")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_LOG')")
    @RequestMapping(value = "/logs", method = RequestMethod.GET)
    public ApiPageRes<FeeRuleLog> logs() {
        requireAuthority("ENT_FEE_RULE_LOG");
        LambdaQueryWrapper<FeeRuleLog> wrapper = FeeRuleLog.gw();
        String wayCode = getValString("wayCode");
        if (StringUtils.isNotEmpty(wayCode)) {
            wrapper.eq(FeeRuleLog::getWayCode, wayCode);
        }
        wrapper.orderByDesc(FeeRuleLog::getLogId);
        IPage<FeeRuleLog> pages = feeRuleLogMapper.selectPage(getIPage(), wrapper);
        return ApiPageRes.pages(pages);
    }

    private void requireLayerPermission(String layer) {
        if (FeeRuleService.isPlatformLayer(layer)) {
            boolean allowed = getCurrentUser().getAuthorities().stream()
                    .anyMatch(authority -> PLATFORM_EDIT.equals(authority.getAuthority()));
            if (!allowed) {
                throw new BizException("平臺費與渠道費僅限平台管理者設定");
            }
        } else if (getCurrentUser().getAuthorities().stream()
                .noneMatch(authority -> "ENT_FEE_RULE_EDIT".equals(authority.getAuthority()))) {
            throw new BizException("無權限修改代理層費率");
        }
    }
}
