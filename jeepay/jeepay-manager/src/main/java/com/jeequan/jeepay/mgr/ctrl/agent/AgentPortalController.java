package com.jeequan.jeepay.mgr.ctrl.agent;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.service.impl.AgentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 代理後台（ADR-0009 第三階段），全部唯讀。
 * 代理號一律取自登入者的 belong_info_id，不接受前端指定，避免越權查看其他代理資料。
 */
@Tag(name = "代理後台")
@RestController
@RequestMapping("/api/agentPortal")
public class AgentPortalController extends AgentBaseCtrl {

    @Autowired private AgentPortalService agentPortalService;

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
}
