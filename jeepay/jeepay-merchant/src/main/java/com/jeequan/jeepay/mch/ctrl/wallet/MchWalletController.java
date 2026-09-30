package com.jeequan.jeepay.mch.ctrl.wallet;

import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mch.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.mapper.WalletLedgerMapper;
import com.jeequan.jeepay.service.wallet.WalletConfig;
import com.jeequan.jeepay.service.wallet.WalletService;
import com.jeequan.jeepay.service.wallet.WithdrawService;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 商戶錢包（ADR-0010）：訂單結算後的實收金額累積於此，可申請提現，由平台人工匯款。
 * 商戶號一律取自登入者，不接受前端指定。
 */
@Tag(name = "我的錢包")
@RestController
@RequestMapping("/api/mchWallet")
public class MchWalletController extends CommonCtrl {

    @Autowired private WalletService walletService;
    @Autowired private WalletLedgerMapper walletLedgerMapper;
    @Autowired private WithdrawService withdrawService;
    @Autowired private WalletConfig walletConfig;

    @Operation(summary = "錢包餘額與收款帳戶")
    @PreAuthorize("hasAuthority('ENT_MCH_WALLET')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiRes<JSONObject> wallet() {
        JSONObject result = (JSONObject) JSONObject.toJSON(walletService.getOrCreate(WalletAccount.OWNER_MCH, getCurrentMchNo()));
        // 提供畫面顯示的提現規則（元）
        result.put("withdrawMin", BigDecimal.valueOf(walletConfig.withdrawMinFen()).movePointLeft(2));
        result.put("withdrawMax", BigDecimal.valueOf(walletConfig.withdrawMaxFen()).movePointLeft(2));
        result.put("withdrawFee", BigDecimal.valueOf(walletConfig.withdrawFeeFen()).movePointLeft(2));
        result.put("settleDelayDays", walletConfig.settleDelayDays());
        return ApiRes.ok(result);
    }

    @Operation(summary = "錢包流水")
    @PreAuthorize("hasAuthority('ENT_MCH_WALLET')")
    @RequestMapping(value = "/ledger", method = RequestMethod.GET)
    public ApiPageRes<WalletLedger> ledger() {
        return ApiPageRes.pages(walletLedgerMapper.selectPage(getIPage(), WalletLedger.gw()
                .eq(WalletLedger::getOwnerType, WalletAccount.OWNER_MCH).eq(WalletLedger::getOwnerId, getCurrentMchNo())
                .orderByDesc(WalletLedger::getLedgerId)));
    }

    @Operation(summary = "提現紀錄")
    @PreAuthorize("hasAuthority('ENT_MCH_WALLET')")
    @RequestMapping(value = "/withdraws", method = RequestMethod.GET)
    public ApiPageRes<WithdrawOrder> withdraws() {
        return ApiPageRes.pages(withdrawService.page(getIPage(), WithdrawOrder.gw()
                .eq(WithdrawOrder::getOwnerType, WalletAccount.OWNER_MCH).eq(WithdrawOrder::getOwnerId, getCurrentMchNo())
                .orderByDesc(WithdrawOrder::getCreatedAt)));
    }

    @Operation(summary = "申請提現（amount 以元為單位；reqNo 為冪等鍵）")
    @PreAuthorize("hasAuthority('ENT_MCH_WALLET_WITHDRAW')")
    @MethodLog(remark = "商戶申請提現")
    @RequestMapping(value = "/withdraws", method = RequestMethod.POST)
    public ApiRes<WithdrawOrder> apply() {
        long fen = new BigDecimal(getValStringRequired("amount")).movePointRight(2).longValueExact();
        return ApiRes.ok(withdrawService.apply(WalletAccount.OWNER_MCH, getCurrentMchNo(), fen, getValStringRequired("reqNo"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "取消待審核的提現")
    @PreAuthorize("hasAuthority('ENT_MCH_WALLET_WITHDRAW')")
    @MethodLog(remark = "商戶取消提現")
    @RequestMapping(value = "/withdraws/{withdrawId}/cancel", method = RequestMethod.POST)
    public ApiRes cancel(@PathVariable("withdrawId") String withdrawId) {
        withdrawService.cancel(WalletAccount.OWNER_MCH, getCurrentMchNo(), withdrawId,
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "設定收款帳戶")
    @PreAuthorize("hasAuthority('ENT_MCH_WALLET_PAYOUT_EDIT')")
    @MethodLog(remark = "商戶設定收款帳戶")
    @RequestMapping(value = "/payoutAccount", method = RequestMethod.PUT)
    public ApiRes<WalletAccount> payoutAccount() {
        return ApiRes.ok(withdrawService.updatePayoutAccount(WalletAccount.OWNER_MCH, getCurrentMchNo(), getObject(WalletAccount.class)));
    }
}
