package com.jeequan.jeepay.mgr.ctrl.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.AgentInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 代理管理（ADR-0009）：高級代理 → 一般代理 兩層，獨立於服務商。 */
@Tag(name = "代理管理")
@RestController
@RequestMapping("/api/agentInfo")
public class AgentInfoController extends CommonCtrl {

    @Autowired private AgentInfoService agentInfoService;

    @Operation(summary = "代理列表")
    @PreAuthorize("hasAnyAuthority('ENT_AGENT_LIST', 'ENT_FEE_RULE_LIST', 'ENT_MCH_AGENT_BIND')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<AgentInfo> list() {
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
        AgentInfo agent = getObject(AgentInfo.class);
        agentInfoService.create(agent, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok(agent);
    }

    @Operation(summary = "代理詳情")
    @PreAuthorize("hasAnyAuthority('ENT_AGENT_INFO_VIEW', 'ENT_AGENT_INFO_EDIT')")
    @RequestMapping(value = "/{agentNo}", method = RequestMethod.GET)
    public ApiRes<AgentInfo> detail(@PathVariable("agentNo") String agentNo) {
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
        agentInfoService.updateBasic(agentNo, getObject(AgentInfo.class));
        return ApiRes.ok();
    }

    @Operation(summary = "刪除代理")
    @PreAuthorize("hasAuthority('ENT_AGENT_INFO_DEL')")
    @MethodLog(remark = "刪除代理")
    @RequestMapping(value = "/{agentNo}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("agentNo") String agentNo) {
        agentInfoService.removeAgent(agentNo, getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }
}
