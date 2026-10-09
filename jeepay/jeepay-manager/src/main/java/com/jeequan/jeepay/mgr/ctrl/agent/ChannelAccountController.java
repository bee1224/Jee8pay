package com.jeequan.jeepay.mgr.ctrl.agent;

import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.ChannelAccount;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.impl.ChannelAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 渠道帳號（ADR-0012 第一階段）：只有上帝能建立、輸入金鑰與派發。團長的唯讀列表在 AgentPortalController。 */
@Tag(name = "渠道帳號")
@RestController
@RequestMapping("/api/channelAccounts")
public class ChannelAccountController extends AgentBaseCtrl {

    @Autowired private ChannelAccountService channelAccountService;

    @Operation(summary = "渠道帳號列表（不含金鑰）；srAgentNo 有值時只列出派發給該團長的帳號")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_LIST')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiRes list() {
        requireAuthority("ENT_CHANNEL_ACCOUNT_LIST");
        return ApiRes.ok(channelAccountService.listView(getValString("srAgentNo")));
    }

    @Operation(summary = "渠道帳號詳情（金鑰已遮罩）")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_EDIT')")
    @RequestMapping(value = "/{accountId}", method = RequestMethod.GET)
    public ApiRes detail(@PathVariable("accountId") String accountId) {
        requireAuthority("ENT_CHANNEL_ACCOUNT_EDIT");
        return ApiRes.ok(channelAccountService.detailWithMaskedParams(accountId));
    }

    @Operation(summary = "新增渠道帳號並派發給所屬團長")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_EDIT')")
    @MethodLog(remark = "新增渠道帳號")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes add() {
        requireAuthority("ENT_CHANNEL_ACCOUNT_EDIT");
        ChannelAccount account = channelAccountService.create(getObject(ChannelAccount.class), getValString("ifParams"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok(account);
    }

    @Operation(summary = "修改渠道帳號（名稱、狀態、備註、是否可共用、金鑰）")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_EDIT')")
    @MethodLog(remark = "修改渠道帳號")
    @RequestMapping(value = "/{accountId}", method = RequestMethod.PUT)
    public ApiRes update(@PathVariable("accountId") String accountId) {
        requireAuthority("ENT_CHANNEL_ACCOUNT_EDIT");
        channelAccountService.update(accountId, getObject(ChannelAccount.class), getValString("ifParams"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "刪除渠道帳號")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_EDIT')")
    @MethodLog(remark = "刪除渠道帳號")
    @RequestMapping(value = "/{accountId}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("accountId") String accountId) {
        requireAuthority("ENT_CHANNEL_ACCOUNT_EDIT");
        channelAccountService.removeAccount(accountId);
        return ApiRes.ok();
    }

    @Operation(summary = "把渠道帳號加派給另一位團長（帳號須已開啟共用）")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_EDIT')")
    @MethodLog(remark = "加派渠道帳號")
    @RequestMapping(value = "/{accountId}/agents", method = RequestMethod.POST)
    public ApiRes grant(@PathVariable("accountId") String accountId) {
        requireAuthority("ENT_CHANNEL_ACCOUNT_EDIT");
        channelAccountService.grant(accountId, getValStringRequired("srAgentNo"), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "收回加派的渠道帳號")
    @PreAuthorize("hasAuthority('ENT_CHANNEL_ACCOUNT_EDIT')")
    @MethodLog(remark = "收回渠道帳號")
    @RequestMapping(value = "/{accountId}/agents/{srAgentNo}", method = RequestMethod.DELETE)
    public ApiRes revoke(@PathVariable("accountId") String accountId, @PathVariable("srAgentNo") String srAgentNo) {
        requireAuthority("ENT_CHANNEL_ACCOUNT_EDIT");
        channelAccountService.revoke(accountId, srAgentNo);
        return ApiRes.ok();
    }
}
