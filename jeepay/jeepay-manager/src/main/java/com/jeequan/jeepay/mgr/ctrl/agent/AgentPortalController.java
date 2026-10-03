package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.PayWay;
import com.jeequan.jeepay.core.entity.SysLog;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.service.impl.AgentPortalService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.impl.PayWayService;
import com.jeequan.jeepay.service.mapper.WalletLedgerMapper;
import com.jeequan.jeepay.service.wallet.Money;
import com.jeequan.jeepay.service.wallet.WalletService;
import com.jeequan.jeepay.service.wallet.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 代理後台（ADR-0009 第三階段）。
 * 代理號一律取自登入者的 belong_info_id，不接受前端指定，避免越權查看其他代理資料。
 */
@Tag(name = "代理後台")
@RestController
@RequestMapping("/api/agentPortal")
public class AgentPortalController extends AgentBaseCtrl {

    @Autowired private AgentPortalService agentPortalService;
    @Autowired private WalletService walletService;
    @Autowired private PayWayService payWayService;
    @Autowired private WalletLedgerMapper walletLedgerMapper;
    @Autowired private WithdrawService withdrawService;
    @Autowired private PayOrderService payOrderService;

    private AgentInfo me() {
        return agentPortalService.requireAgent(getCurrentUser().getSysUser());
    }

    @Operation(summary = "我的代理資料")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public ApiRes<JSONObject> profile() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        AgentInfo me = me();
        JSONObject result = (JSONObject) JSONObject.toJSON(me);
        result.put("subAgents", Objects.equals(me.getAgentLevel(), AgentInfo.LEVEL_SENIOR) ? agentPortalService.subAgents(me) : null);
        // 代理沒有支付方式管理權限，這裡只提供設定下級費率時需要的代碼與名稱
        result.put("payWays", payWayService.list(PayWay.gw().select(PayWay::getWayCode, PayWay::getWayName)));
        return ApiRes.ok(result);
    }

    @Operation(summary = "轄區商戶")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/merchants", method = RequestMethod.GET)
    public ApiRes merchants() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(agentPortalService.merchants(me()));
    }

    @Operation(summary = "轄區費率（唯讀）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/feeRules", method = RequestMethod.GET)
    public ApiRes feeRules() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(agentPortalService.rules(me()));
    }

    @Operation(summary = "分潤統計")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/profits/summary", method = RequestMethod.GET)
    public ApiRes profitSummary() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(agentPortalService.profitSummary(me().getAgentNo(), startDate(), endDate()));
    }

    @Operation(summary = "分潤明細")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/profits", method = RequestMethod.GET)
    public ApiPageRes profits() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiPageRes.pages(agentPortalService.profitPage(getIPage(), me().getAgentNo(), startDate(), endDate()));
    }

    // ---------------- 錢包（ADR-0010）：代理號一律取自登入者，不接受前端指定 ----------------

    @Operation(summary = "我的錢包")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/wallet", method = RequestMethod.GET)
    public ApiRes<WalletAccount> wallet() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(walletService.getOrCreate(WalletAccount.OWNER_AGENT, me().getAgentNo()));
    }

    @Operation(summary = "我的錢包流水")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/wallet/ledger", method = RequestMethod.GET)
    public ApiPageRes<WalletLedger> walletLedger() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiPageRes.pages(walletLedgerMapper.selectPage(getIPage(), WalletLedger.gw()
                .eq(WalletLedger::getOwnerType, WalletAccount.OWNER_AGENT).eq(WalletLedger::getOwnerId, me().getAgentNo())
                .orderByDesc(WalletLedger::getLedgerId)));
    }

    @Operation(summary = "我的提現紀錄")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/wallet/withdraws", method = RequestMethod.GET)
    public ApiPageRes<WithdrawOrder> withdraws() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiPageRes.pages(withdrawService.page(getIPage(), WithdrawOrder.gw()
                .eq(WithdrawOrder::getOwnerType, WalletAccount.OWNER_AGENT).eq(WithdrawOrder::getOwnerId, me().getAgentNo())
                .orderByDesc(WithdrawOrder::getCreatedAt)));
    }

    @Operation(summary = "申請提現（amount 以元為單位；reqNo 為冪等鍵）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @MethodLog(remark = "代理申請提現")
    @RequestMapping(value = "/wallet/withdraws", method = RequestMethod.POST)
    public ApiRes<WithdrawOrder> applyWithdraw() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        long fen = Money.yuanToFen(getValString("amount"));
        return ApiRes.ok(withdrawService.apply(WalletAccount.OWNER_AGENT, me().getAgentNo(), fen, getValStringRequired("reqNo"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "取消待審核的提現")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @MethodLog(remark = "代理取消提現")
    @RequestMapping(value = "/wallet/withdraws/{withdrawId}/cancel", method = RequestMethod.POST)
    public ApiRes cancelWithdraw(@PathVariable("withdrawId") String withdrawId) {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        withdrawService.cancel(WalletAccount.OWNER_AGENT, me().getAgentNo(), withdrawId,
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "設定收款帳戶")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @MethodLog(remark = "代理設定收款帳戶")
    @RequestMapping(value = "/wallet/payoutAccount", method = RequestMethod.PUT)
    public ApiRes<WalletAccount> payoutAccount() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(withdrawService.updatePayoutAccount(WalletAccount.OWNER_AGENT, me().getAgentNo(), getObject(WalletAccount.class)));
    }

    @Operation(summary = "高級代理設定下級代理的代理費或推薦佣金")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_FEE_EDIT')")
    @MethodLog(remark = "代理設定下級費率")
    @RequestMapping(value = "/subAgentRules", method = RequestMethod.POST)
    public ApiRes subAgentRule() {
        requireAuthority("ENT_AGENT_PORTAL_FEE_EDIT");
        return ApiRes.ok(agentPortalService.saveSubAgentRule(me(), getObject(FeeRule.class),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    // ---------------- 家族樹權限：代理只操作自己這一支的後代 ----------------

    @Operation(summary = "轄區商戶的代收訂單（自己與下級代理直屬的商戶）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_ORDER')")
    @RequestMapping(value = "/orders", method = RequestMethod.GET)
    public ApiPageRes<PayOrder> orders() {
        requireAuthority("ENT_AGENT_PORTAL_ORDER");
        List<String> mchNos = agentPortalService.directMchNos(me());
        if (mchNos.isEmpty()) {
            return ApiPageRes.pages(getIPage());
        }
        // 商戶範圍由後端強制；前端傳入的 mchNo 只能在範圍內再縮小
        LambdaQueryWrapper<PayOrder> wrapper = PayOrder.gw().in(PayOrder::getMchNo, mchNos);
        return ApiPageRes.pages(payOrderService.listByPage(getIPage(), getObject(PayOrder.class), getReqParamJSON(), wrapper));
    }

    @Operation(summary = "高級代理新增下級代理並開通登入帳號")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_SUB_ADD')")
    @MethodLog(remark = "代理新增下級代理")
    @RequestMapping(value = "/subAgents", method = RequestMethod.POST)
    public ApiRes subAgentAdd() {
        requireAuthority("ENT_AGENT_PORTAL_SUB_ADD");
        return ApiRes.ok(agentPortalService.createSubAgent(me(), getObject(AgentInfo.class), getValString("loginUsername"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "代理新增商戶（直屬自己，或高級代理指定給下級代理）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_MCH_ADD')")
    @MethodLog(remark = "代理新增商戶")
    @RequestMapping(value = "/merchants", method = RequestMethod.POST)
    public ApiRes merchantAdd() {
        requireAuthority("ENT_AGENT_PORTAL_MCH_ADD");
        return ApiRes.ok(agentPortalService.createMerchant(me(), getObject(MchInfo.class), getValString("loginUsername"),
                getValString("agentNo"), getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "下級代理帳號的操作紀錄")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_OPLOG')")
    @RequestMapping(value = "/opLogs", method = RequestMethod.GET)
    public ApiPageRes<SysLog> opLogs() {
        requireAuthority("ENT_AGENT_PORTAL_OPLOG");
        return ApiPageRes.pages(agentPortalService.descendantLogs(me(), getIPage()));
    }
}
