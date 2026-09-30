package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeRuleLog;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.fee.FeeWaterfall;
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

/**
 * 四層手續費規則（ADR-0009 第一階段：僅設定面與試算，不影響下單與結算）。
 * 平臺費／渠道費為平台鎖定層：除了進入 API 的權限，還必須持有 ENT_FEE_RULE_PLATFORM_EDIT，
 * 在後端擋下，不只靠前端隱藏欄位。
 */
@Tag(name = "費率設定")
@RestController
@RequestMapping("/api/feeRules")
public class FeeRuleController extends CommonCtrl {

    private static final String PLATFORM_EDIT = "ENT_FEE_RULE_PLATFORM_EDIT";

    @Autowired private FeeRuleService feeRuleService;
    @Autowired private FeeRuleLogMapper feeRuleLogMapper;

    @Operation(summary = "費率規則列表")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_LIST')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<FeeRule> list() {
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
        FeeRule rule = getObject(FeeRule.class);
        requireLayerPermission(rule.getLayer());
        return ApiRes.ok(feeRuleService.saveRule(rule,
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "刪除費率規則")
    @PreAuthorize("hasAnyAuthority('ENT_FEE_RULE_EDIT', 'ENT_FEE_RULE_PLATFORM_EDIT')")
    @MethodLog(remark = "刪除費率規則")
    @RequestMapping(value = "/{ruleId}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("ruleId") Long ruleId) {
        FeeRule rule = feeRuleService.getById(ruleId);
        if (rule == null) {
            throw new BizException("費率規則不存在");
        }
        requireLayerPermission(rule.getLayer());
        feeRuleService.removeRule(ruleId, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "試算：某商戶在某支付方式下，指定金額（分）的四層手續費")
    @PreAuthorize("hasAuthority('ENT_FEE_RULE_LIST')")
    @RequestMapping(value = "/preview", method = RequestMethod.GET)
    public ApiRes<JSONObject> preview() {
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
