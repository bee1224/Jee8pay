package com.jeequan.jeepay.mgr.ctrl.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.RiskBlacklist;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.wallet.RiskBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 風控黑名單（平台維護；scope 可為 GLOBAL 或團長號）。 */
@Tag(name = "風控黑名單")
@RestController
@RequestMapping("/api/riskBlacklist")
public class RiskBlacklistController extends CommonCtrl {

    @Autowired private RiskBlacklistService riskBlacklistService;

    @Operation(summary = "黑名單列表")
    @PreAuthorize("hasAuthority('ENT_RISK_BLACKLIST')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<RiskBlacklist> list() {
        LambdaQueryWrapper<RiskBlacklist> w = RiskBlacklist.gw();
        String type = getValString("listType");
        String value = getValString("listValue");
        String scope = getValString("scope");
        w.eq(StringUtils.isNotBlank(type), RiskBlacklist::getListType, type);
        w.like(StringUtils.isNotBlank(value), RiskBlacklist::getListValue, value);
        w.eq(StringUtils.isNotBlank(scope), RiskBlacklist::getScope, scope);
        w.orderByDesc(RiskBlacklist::getId);
        return ApiPageRes.pages(riskBlacklistService.page(getIPage(), w));
    }

    @Operation(summary = "新增黑名單")
    @PreAuthorize("hasAuthority('ENT_RISK_BLACKLIST_EDIT')")
    @MethodLog(remark = "新增風控黑名單")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes add() {
        return ApiRes.ok(riskBlacklistService.add(getObject(RiskBlacklist.class),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "刪除黑名單")
    @PreAuthorize("hasAuthority('ENT_RISK_BLACKLIST_EDIT')")
    @MethodLog(remark = "刪除風控黑名單")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("id") Long id) {
        riskBlacklistService.removeById(id);
        return ApiRes.ok();
    }
}
