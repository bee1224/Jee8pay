package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.RiskBlacklist;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.entity.WayRoute;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.impl.AgentBranchService;
import com.jeequan.jeepay.service.impl.AgentPortalService;
import com.jeequan.jeepay.service.wallet.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代理後台：對自己這一支後代的管理（家族樹權限）。
 * 代理號一律取自登入者的 belong_info_id；範圍檢查都在 {@link AgentBranchService}。
 */
@Tag(name = "代理後台－旗下管理")
@RestController
@RequestMapping("/api/agentPortal/branch")
public class AgentBranchController extends AgentBaseCtrl {

    @Autowired private AgentPortalService agentPortalService;
    @Autowired private AgentBranchService branch;

    private AgentInfo me() {
        return agentPortalService.requireAgent(getCurrentUser().getSysUser());
    }

    private Long uid() {
        return getCurrentUser().getSysUser().getSysUserId();
    }

    private String realname() {
        return getCurrentUser().getSysUser().getRealname();
    }

    // ---------------- 旗下錢包 ----------------

    @Operation(summary = "旗下代理與商戶的錢包餘額")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_BRANCH_WALLET')")
    @RequestMapping(value = "/wallets", method = RequestMethod.GET)
    public ApiRes wallets() {
        requireAuthority("ENT_AGENT_PORTAL_BRANCH_WALLET");
        return ApiRes.ok(branch.wallets(me()));
    }

    @Operation(summary = "旗下錢包流水")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_BRANCH_WALLET')")
    @RequestMapping(value = "/ledger", method = RequestMethod.GET)
    public ApiPageRes<WalletLedger> ledger() {
        requireAuthority("ENT_AGENT_PORTAL_BRANCH_WALLET");
        return ApiPageRes.pages(branch.ledger(me(), getIPage(), getValString("ownerType"), getValString("ownerId"), startDate(), endDate()));
    }

    @Operation(summary = "凍結旗下資金（amount 以元為單位）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_FREEZE')")
    @MethodLog(remark = "代理凍結旗下資金")
    @RequestMapping(value = "/wallets/freeze", method = RequestMethod.POST)
    public ApiRes freeze() {
        requireAuthority("ENT_AGENT_PORTAL_FREEZE");
        branch.freeze(me(), getValStringRequired("ownerType"), getValStringRequired("ownerId"),
                Money.yuanToFen(getValString("amount")), getValString("remark"), uid(), realname());
        return ApiRes.ok();
    }

    @Operation(summary = "解凍旗下資金（只能解開人工凍結的部分）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_FREEZE')")
    @MethodLog(remark = "代理解凍旗下資金")
    @RequestMapping(value = "/wallets/unfreeze", method = RequestMethod.POST)
    public ApiRes unfreeze() {
        requireAuthority("ENT_AGENT_PORTAL_FREEZE");
        branch.unfreeze(me(), getValStringRequired("ownerType"), getValStringRequired("ownerId"),
                Money.yuanToFen(getValString("amount")), getValString("remark"), uid(), realname());
        return ApiRes.ok();
    }

    // ---------------- 商戶歸屬與帳號 ----------------

    @Operation(summary = "更換旗下商戶的直屬代理")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_MCH_EDIT')")
    @MethodLog(remark = "代理更換商戶歸屬")
    @RequestMapping(value = "/merchants/{mchNo}/binding", method = RequestMethod.PUT)
    public ApiRes rebind(@PathVariable("mchNo") String mchNo) {
        requireAuthority("ENT_AGENT_PORTAL_MCH_EDIT");
        branch.rebind(me(), mchNo, getValStringRequired("agentNo"), uid(), realname());
        return ApiRes.ok();
    }

    @Operation(summary = "重設旗下商戶的登入密碼（隨機，只在回應出現一次）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_MCH_EDIT')")
    @MethodLog(remark = "代理重設商戶密碼")
    @RequestMapping(value = "/merchants/{mchNo}/resetPassword", method = RequestMethod.POST)
    public ApiRes resetMerchantPassword(@PathVariable("mchNo") String mchNo) {
        requireAuthority("ENT_AGENT_PORTAL_MCH_EDIT");
        return ApiRes.ok(branch.resetMerchantPassword(me(), mchNo));
    }

    // ---------------- 統計報表 ----------------

    @Operation(summary = "旗下代收統計（依代理、商戶、日期）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_REPORT')")
    @RequestMapping(value = "/report", method = RequestMethod.GET)
    public ApiRes report() {
        requireAuthority("ENT_AGENT_PORTAL_REPORT");
        return ApiRes.ok(branch.report(me(), startDate(), endDate()));
    }

    // ---------------- 黑名單 ----------------

    @Operation(summary = "自己這一支共用的黑名單")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_BLACKLIST')")
    @RequestMapping(value = "/blacklist", method = RequestMethod.GET)
    public ApiRes blacklist() {
        requireAuthority("ENT_AGENT_PORTAL_BLACKLIST");
        return ApiRes.ok(branch.blacklist(me()));
    }

    @Operation(summary = "新增黑名單")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_BLACKLIST')")
    @MethodLog(remark = "代理新增黑名單")
    @RequestMapping(value = "/blacklist", method = RequestMethod.POST)
    public ApiRes blacklistAdd() {
        requireAuthority("ENT_AGENT_PORTAL_BLACKLIST");
        return ApiRes.ok(branch.addBlacklist(me(), getObject(RiskBlacklist.class), uid(), realname()));
    }

    @Operation(summary = "刪除黑名單")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_BLACKLIST')")
    @MethodLog(remark = "代理刪除黑名單")
    @RequestMapping(value = "/blacklist/{id}", method = RequestMethod.DELETE)
    public ApiRes blacklistRemove(@PathVariable("id") Long id) {
        requireAuthority("ENT_AGENT_PORTAL_BLACKLIST");
        branch.removeBlacklist(me(), id);
        return ApiRes.ok();
    }

    // ---------------- 費率 ----------------

    @Operation(summary = "自己這一支的代理層費率與商戶覆寫")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/feeRules", method = RequestMethod.GET)
    public ApiRes feeRules() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(branch.feeRules(me()));
    }

    @Operation(summary = "團長設定團長費、隊長費或商戶覆寫")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_FEE_EDIT')")
    @MethodLog(remark = "代理設定費率")
    @RequestMapping(value = "/feeRules", method = RequestMethod.POST)
    public ApiRes feeRuleSave() {
        requireAuthority("ENT_AGENT_PORTAL_FEE_EDIT");
        return ApiRes.ok(branch.saveFeeRule(me(), getObject(FeeRule.class), uid(), realname()));
    }

    @Operation(summary = "可套用的費率範本")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_FEE_EDIT')")
    @RequestMapping(value = "/feeTemplates", method = RequestMethod.GET)
    public ApiRes feeTemplates() {
        requireAuthority("ENT_AGENT_PORTAL_FEE_EDIT");
        return ApiRes.ok(branch.feeTemplates());
    }

    @Operation(summary = "套用費率範本到旗下代理或商戶")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_FEE_EDIT')")
    @MethodLog(remark = "代理套用費率範本")
    @RequestMapping(value = "/feeTemplates/{templateId}/apply", method = RequestMethod.POST)
    public ApiRes feeTemplateApply(@PathVariable("templateId") Long templateId) {
        requireAuthority("ENT_AGENT_PORTAL_FEE_EDIT");
        JSONObject params = getReqParamJSON();
        return ApiRes.ok(branch.applyTemplate(me(), templateId, params.getString("targetType"),
                params.getJSONArray("targetIds") == null ? null : params.getJSONArray("targetIds").toJavaList(String.class),
                uid(), realname()));
    }

    // ---------------- 通道路由 ----------------

    @Operation(summary = "旗下商戶的通道路由規則")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_ROUTE')")
    @RequestMapping(value = "/wayRoutes", method = RequestMethod.GET)
    public ApiRes routes() {
        requireAuthority("ENT_AGENT_PORTAL_ROUTE");
        return ApiRes.ok(branch.routes(me()));
    }

    @Operation(summary = "新增或修改路由規則")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_ROUTE')")
    @MethodLog(remark = "代理設定通道路由")
    @RequestMapping(value = "/wayRoutes", method = RequestMethod.POST)
    public ApiRes routeSave() {
        requireAuthority("ENT_AGENT_PORTAL_ROUTE");
        return ApiRes.ok(branch.saveRoute(me(), getObject(WayRoute.class), realname()));
    }

    @Operation(summary = "刪除路由規則")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_ROUTE')")
    @MethodLog(remark = "代理刪除通道路由")
    @RequestMapping(value = "/wayRoutes/{routeId}", method = RequestMethod.DELETE)
    public ApiRes routeRemove(@PathVariable("routeId") Long routeId) {
        requireAuthority("ENT_AGENT_PORTAL_ROUTE");
        branch.removeRoute(me(), routeId);
        return ApiRes.ok();
    }

    // ---------------- 提現審核 ----------------

    @Operation(summary = "旗下代理與商戶的提現單")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_WITHDRAW_AUDIT')")
    @RequestMapping(value = "/withdraws", method = RequestMethod.GET)
    public ApiPageRes<WithdrawOrder> withdraws() {
        requireAuthority("ENT_AGENT_PORTAL_WITHDRAW_AUDIT");
        return ApiPageRes.pages(branch.withdraws(me(), getIPage(), getValByte("state")));
    }

    @Operation(summary = "同意旗下提現（註記，撥款由平台執行）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_WITHDRAW_AUDIT')")
    @MethodLog(remark = "代理同意旗下提現")
    @RequestMapping(value = "/withdraws/{withdrawId}/approve", method = RequestMethod.POST)
    public ApiRes withdrawApprove(@PathVariable("withdrawId") String withdrawId) {
        requireAuthority("ENT_AGENT_PORTAL_WITHDRAW_AUDIT");
        branch.approveWithdraw(me(), withdrawId, realname());
        return ApiRes.ok();
    }

    @Operation(summary = "駁回旗下提現（凍結金額退回可用餘額）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_WITHDRAW_AUDIT')")
    @MethodLog(remark = "代理駁回旗下提現")
    @RequestMapping(value = "/withdraws/{withdrawId}/reject", method = RequestMethod.POST)
    public ApiRes withdrawReject(@PathVariable("withdrawId") String withdrawId) {
        requireAuthority("ENT_AGENT_PORTAL_WITHDRAW_AUDIT");
        branch.rejectWithdraw(me(), withdrawId, getValString("remark"), uid(), realname());
        return ApiRes.ok();
    }

    // ---------------- 品牌與登入紀錄 ----------------

    @Operation(summary = "登入者適用的品牌（未啟用時為空）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/brand", method = RequestMethod.GET)
    public ApiRes brand() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(branch.effectiveBrand(me()));
    }

    @Operation(summary = "團長設定品牌（站台名稱與 Logo）")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_BRAND')")
    @MethodLog(remark = "代理設定品牌")
    @RequestMapping(value = "/brand", method = RequestMethod.PUT)
    public ApiRes brandSave() {
        requireAuthority("ENT_AGENT_PORTAL_BRAND");
        branch.saveBrand(me(), getValByte("brandEnabled"), getValString("brandTitle"), getValString("brandLogo"));
        return ApiRes.ok();
    }

    @Operation(summary = "自己最近的登入紀錄")
    @PreAuthorize("hasAuthority('ENT_AGENT_PORTAL_VIEW')")
    @RequestMapping(value = "/loginLogs", method = RequestMethod.GET)
    public ApiRes loginLogs() {
        requireAuthority("ENT_AGENT_PORTAL_VIEW");
        return ApiRes.ok(branch.recentLogins(uid()));
    }
}
