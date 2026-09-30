package com.jeequan.jeepay.mgr.ctrl.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.wallet.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 提現審核（ADR-0010）：平台完成人工匯款後標記已撥款，或駁回解凍。 */
@Tag(name = "提現審核")
@RestController
@RequestMapping("/api/withdraws")
public class WithdrawController extends CommonCtrl {

    @Autowired private WithdrawService withdrawService;

    @Operation(summary = "提現單列表")
    @PreAuthorize("hasAuthority('ENT_WALLET_WITHDRAW')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<WithdrawOrder> list() {
        LambdaQueryWrapper<WithdrawOrder> w = WithdrawOrder.gw();
        Byte state = getValByte("state");
        String ownerType = getValString("ownerType");
        String ownerId = getValString("ownerId");
        String withdrawId = getValString("withdrawId");
        w.eq(state != null, WithdrawOrder::getState, state);
        w.eq(StringUtils.isNotBlank(ownerType), WithdrawOrder::getOwnerType, ownerType);
        w.eq(StringUtils.isNotBlank(ownerId), WithdrawOrder::getOwnerId, ownerId);
        w.eq(StringUtils.isNotBlank(withdrawId), WithdrawOrder::getWithdrawId, withdrawId);
        w.orderByAsc(WithdrawOrder::getState).orderByDesc(WithdrawOrder::getCreatedAt);
        return ApiPageRes.pages(withdrawService.page(getIPage(), w));
    }

    @Operation(summary = "標記已撥款（需填匯款單號）")
    @PreAuthorize("hasAuthority('ENT_WALLET_WITHDRAW_REVIEW')")
    @MethodLog(remark = "提現標記已撥款")
    @RequestMapping(value = "/{withdrawId}/paid", method = RequestMethod.POST)
    public ApiRes paid(@PathVariable("withdrawId") String withdrawId) {
        withdrawService.markPaid(withdrawId, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname(),
                getValString("paidRef"), getValString("remark"));
        return ApiRes.ok();
    }

    @Operation(summary = "駁回提現（解凍）")
    @PreAuthorize("hasAuthority('ENT_WALLET_WITHDRAW_REVIEW')")
    @MethodLog(remark = "駁回提現")
    @RequestMapping(value = "/{withdrawId}/reject", method = RequestMethod.POST)
    public ApiRes reject(@PathVariable("withdrawId") String withdrawId) {
        withdrawService.reject(withdrawId, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname(),
                getValString("remark"));
        return ApiRes.ok();
    }
}
