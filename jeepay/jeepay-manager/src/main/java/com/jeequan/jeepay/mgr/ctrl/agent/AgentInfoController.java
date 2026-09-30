package com.jeequan.jeepay.mgr.ctrl.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.impl.AgentInfoService;
import com.jeequan.jeepay.service.impl.AgentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 代理管理（ADR-0009）：高級代理 → 一般代理 兩層，獨立於服務商。 */
@Tag(name = "代理管理")
@RestController
@RequestMapping("/api/agentInfo")
public class AgentInfoController extends AgentBaseCtrl {

    @Autowired private AgentInfoService agentInfoService;
    @Autowired private AgentPortalService agentPortalService;

    /**
     * 代理登入開關，預設關閉。營運平台的 @PreAuthorize 目前未生效（TD-015），
     * 開通代理帳號等於讓代理可呼叫全部平台 API；須先修正全域權限檢查並驗證後才可開啟。
     */
    @Value("${isys.agent-portal.login-enabled:false}")
    private boolean agentLoginEnabled;

    @Operation(summary = "代理列表")
    @PreAuthorize("hasAnyAuthority('ENT_AGENT_LIST', 'ENT_FEE_RULE_LIST', 'ENT_MCH_AGENT_BIND')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<AgentInfo> list() {
        requireAuthority("ENT_AGENT_LIST", "ENT_FEE_RULE_LIST", "ENT_MCH_AGENT_BIND");
        AgentInfo query = getObject(AgentInfo.class);
        LambdaQueryWrapper<AgentInfo> wrapper = AgentInfo.gw();
        if (StringUtils.isNotEmpty(query.getAgentNo())) {
            wrapper.eq(AgentInfo::getAgentNo, query.getAgentNo());
        }
        if (StringUtils.isNotEmpty(query.getAgentName())) {
            wrapper.like(AgentInfo::getAgentName, query.getAgentName());
        }
        if (query.getAgentLevel() != null) {
            wrapper.eq(AgentInfo::getAgentLevel, query.getAgentLevel());
        }
        if (StringUtils.isNotEmpty(query.getParentAgentNo())) {
            wrapper.eq(AgentInfo::getParentAgentNo, query.getParentAgentNo());
        }
        if (query.getState() != null) {
            wrapper.eq(AgentInfo::getState, query.getState());
        }
        wrapper.orderByAsc(AgentInfo::getAgentPath);
        IPage<AgentInfo> pages = agentInfoService.page(getIPage(true), wrapper);
        return ApiPageRes.pages(pages);
    }

    @Operation(summary = "新增代理")
    @PreAuthorize("hasAuthority('ENT_AGENT_INFO_ADD')")
    @MethodLog(remark = "新增代理")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes add() {
        requireAuthority("ENT_AGENT_INFO_ADD");
        AgentInfo agent = getObject(AgentInfo.class);
        agentInfoService.create(agent, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok(agent);
    }

    @Operation(summary = "代理詳情")
    @PreAuthorize("hasAnyAuthority('ENT_AGENT_INFO_VIEW', 'ENT_AGENT_INFO_EDIT')")
    @RequestMapping(value = "/{agentNo}", method = RequestMethod.GET)
    public ApiRes<AgentInfo> detail(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_INFO_VIEW", "ENT_AGENT_INFO_EDIT");
        AgentInfo agent = agentInfoService.getById(agentNo);
        if (agent == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        return ApiRes.ok(agent);
    }

    @Operation(summary = "更新代理（僅基本資料與狀態，層級與上級不可變）")
    @PreAuthorize("hasAuthority('ENT_AGENT_INFO_EDIT')")
    @MethodLog(remark = "更新代理")
    @RequestMapping(value = "/{agentNo}", method = RequestMethod.PUT)
    public ApiRes update(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_INFO_EDIT");
        agentInfoService.updateBasic(agentNo, getObject(AgentInfo.class));
        return ApiRes.ok();
    }

    @Operation(summary = "刪除代理")
    @PreAuthorize("hasAuthority('ENT_AGENT_INFO_DEL')")
    @MethodLog(remark = "刪除代理")
    @RequestMapping(value = "/{agentNo}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_INFO_DEL");
        agentInfoService.removeAgent(agentNo, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "代理登入帳號列表")
    @PreAuthorize("hasAuthority('ENT_AGENT_ACCOUNT')")
    @RequestMapping(value = "/{agentNo}/accounts", method = RequestMethod.GET)
    public ApiRes accounts(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_ACCOUNT");
        return ApiRes.ok(agentPortalService.accounts(agentNo));
    }

    @Operation(summary = "開通代理登入帳號（預設密碼，首次登入後自行修改）")
    @PreAuthorize("hasAuthority('ENT_AGENT_ACCOUNT')")
    @MethodLog(remark = "開通代理登入帳號")
    @RequestMapping(value = "/{agentNo}/accounts", method = RequestMethod.POST)
    public ApiRes createAccount(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_ACCOUNT");
        if (!agentLoginEnabled) {
            throw new BizException("代理登入尚未開放：需先修正營運平台權限檢查（TD-015），否則代理帳號可存取平台全部資料");
        }
        SysUser user = agentPortalService.createAccount(agentNo, getObject(SysUser.class));
        return ApiRes.ok(user.getSysUserId());
    }

    @Operation(summary = "代理分潤統計")
    @PreAuthorize("hasAuthority('ENT_AGENT_PROFIT')")
    @RequestMapping(value = "/{agentNo}/profits/summary", method = RequestMethod.GET)
    public ApiRes profitSummary(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_PROFIT");
        return ApiRes.ok(agentPortalService.profitSummary(agentNo, startDate(), endDate()));
    }

    @Operation(summary = "代理分潤明細")
    @PreAuthorize("hasAuthority('ENT_AGENT_PROFIT')")
    @RequestMapping(value = "/{agentNo}/profits", method = RequestMethod.GET)
    public ApiPageRes profits(@PathVariable("agentNo") String agentNo) {
        requireAuthority("ENT_AGENT_PROFIT");
        return ApiPageRes.pages(agentPortalService.profitPage(getIPage(), agentNo, startDate(), endDate()));
    }
}
