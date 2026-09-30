package com.jeequan.jeepay.mgr.ctrl.wallet;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletAdjustReq;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.mapper.WalletLedgerMapper;
import com.jeequan.jeepay.service.wallet.SettlementService;
import com.jeequan.jeepay.service.wallet.WalletAdjustService;
import com.jeequan.jeepay.service.wallet.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** 錢包帳戶、流水、人工調帳、立即結算（ADR-0010）。 */
@Tag(name = "錢包")
@RestController
@RequestMapping("/api/wallet")
public class WalletController extends CommonCtrl {

    @Autowired private WalletService walletService;
    @Autowired private WalletLedgerMapper walletLedgerMapper;
    @Autowired private WalletAdjustService walletAdjustService;
    @Autowired private SettlementService settlementService;

    @Operation(summary = "錢包帳戶列表")
    @PreAuthorize("hasAuthority('ENT_WALLET_ACCOUNT')")
    @RequestMapping(value = "/accounts", method = RequestMethod.GET)
    public ApiPageRes<WalletAccount> accounts() {
        LambdaQueryWrapper<WalletAccount> w = WalletAccount.gw();
        String ownerType = getValString("ownerType");
        String ownerId = getValString("ownerId");
        w.eq(StringUtils.isNotBlank(ownerType), WalletAccount::getOwnerType, ownerType);
        w.like(StringUtils.isNotBlank(ownerId), WalletAccount::getOwnerId, ownerId);
        w.orderByAsc(WalletAccount::getOwnerType).orderByDesc(WalletAccount::getBalance);
        return ApiPageRes.pages(walletService.page(getIPage(true), w));
    }

    @Operation(summary = "帳戶彙總：各類帳戶的可用與凍結合計")
    @PreAuthorize("hasAuthority('ENT_WALLET_ACCOUNT')")
    @RequestMapping(value = "/summary", method = RequestMethod.GET)
    public ApiRes<JSONObject> summary() {
        JSONObject result = new JSONObject(true);
        for (String type : new String[]{WalletAccount.OWNER_PLATFORM, WalletAccount.OWNER_MCH, WalletAccount.OWNER_AGENT, WalletAccount.OWNER_CHANNEL}) {
            long balance = 0;
            long frozen = 0;
            for (WalletAccount a : walletService.list(WalletAccount.gw().eq(WalletAccount::getOwnerType, type))) {
                balance += a.getBalance();
                frozen += a.getFrozen();
            }
            JSONObject item = new JSONObject();
            item.put("balance", balance);
            item.put("frozen", frozen);
            result.put(type, item);
        }
        return ApiRes.ok(result);
    }

    @Operation(summary = "餘額流水")
    @PreAuthorize("hasAnyAuthority('ENT_WALLET_LEDGER', 'ENT_WALLET_ACCOUNT')")
    @RequestMapping(value = "/ledger", method = RequestMethod.GET)
    public ApiPageRes<WalletLedger> ledger() {
        LambdaQueryWrapper<WalletLedger> w = WalletLedger.gw();
        Long accountId = getValLong("accountId");
        String ownerType = getValString("ownerType");
        String ownerId = getValString("ownerId");
        String bizType = getValString("bizType");
        String bizId = getValString("bizId");
        String start = getValString("startDate");
        String end = getValString("endDate");
        w.eq(accountId != null, WalletLedger::getAccountId, accountId);
        w.eq(StringUtils.isNotBlank(ownerType), WalletLedger::getOwnerType, ownerType);
        w.eq(StringUtils.isNotBlank(ownerId), WalletLedger::getOwnerId, ownerId);
        w.eq(StringUtils.isNotBlank(bizType), WalletLedger::getBizType, bizType);
        w.eq(StringUtils.isNotBlank(bizId), WalletLedger::getBizId, bizId);
        w.ge(StringUtils.isNotBlank(start), WalletLedger::getCreatedAt, StringUtils.isBlank(start) ? null : DateUtil.beginOfDay(DateUtil.parseDate(start)));
        w.lt(StringUtils.isNotBlank(end), WalletLedger::getCreatedAt, StringUtils.isBlank(end) ? null : DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parseDate(end)), 1));
        w.orderByDesc(WalletLedger::getLedgerId);
        return ApiPageRes.pages(walletLedgerMapper.selectPage(getIPage(), w));
    }

    @Operation(summary = "人工調帳申請列表")
    @PreAuthorize("hasAnyAuthority('ENT_WALLET_ACCOUNT', 'ENT_WALLET_ADJUST_REVIEW')")
    @RequestMapping(value = "/adjusts", method = RequestMethod.GET)
    public ApiPageRes<WalletAdjustReq> adjusts() {
        Byte state = getValByte("state");
        return ApiPageRes.pages(walletAdjustService.page(getIPage(true), WalletAdjustReq.gw()
                .eq(state != null, WalletAdjustReq::getState, state).orderByDesc(WalletAdjustReq::getReqId)));
    }

    @Operation(summary = "提出人工調帳（amount 以元為單位，正為加、負為減）")
    @PreAuthorize("hasAuthority('ENT_WALLET_ADJUST')")
    @MethodLog(remark = "提出人工調帳")
    @RequestMapping(value = "/adjusts", method = RequestMethod.POST)
    public ApiRes requestAdjust() {
        long fen = new BigDecimal(getValStringRequired("amount")).movePointRight(2).longValueExact();
        return ApiRes.ok(walletAdjustService.request(getValLongRequired("accountId"), fen, getValString("reason"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "核准人工調帳（不可覆核自己的申請）")
    @PreAuthorize("hasAuthority('ENT_WALLET_ADJUST_REVIEW')")
    @MethodLog(remark = "核准人工調帳")
    @RequestMapping(value = "/adjusts/{reqId}/approve", method = RequestMethod.POST)
    public ApiRes approveAdjust(@PathVariable("reqId") Long reqId) {
        walletAdjustService.approve(reqId, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname(), getValString("remark"));
        return ApiRes.ok();
    }

    @Operation(summary = "駁回人工調帳")
    @PreAuthorize("hasAuthority('ENT_WALLET_ADJUST_REVIEW')")
    @MethodLog(remark = "駁回人工調帳")
    @RequestMapping(value = "/adjusts/{reqId}/reject", method = RequestMethod.POST)
    public ApiRes rejectAdjust(@PathVariable("reqId") Long reqId) {
        walletAdjustService.reject(reqId, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname(), getValString("remark"));
        return ApiRes.ok();
    }

    @Operation(summary = "立即執行一輪結算（與排程相同邏輯，冪等）")
    @PreAuthorize("hasAuthority('ENT_WALLET_SETTLE_RUN')")
    @MethodLog(remark = "立即結算")
    @RequestMapping(value = "/settle/run", method = RequestMethod.POST)
    public ApiRes<JSONObject> settleNow() {
        int[] r = settlementService.runOnce();
        JSONObject result = new JSONObject(true);
        result.put("settled", r[0]);
        result.put("reversed", r[1]);
        result.put("skipped", r[2]);
        return ApiRes.ok(result);
    }
}
