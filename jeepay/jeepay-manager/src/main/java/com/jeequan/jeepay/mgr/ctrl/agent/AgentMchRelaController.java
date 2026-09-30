package com.jeequan.jeepay.mgr.ctrl.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.AgentMchRelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 商戶與代理綁定（ADR-0009）：直屬代理與推薦人分開設定。 */
@Tag(name = "商戶代理綁定")
@RestController
@RequestMapping("/api/agentMchRela")
public class AgentMchRelaController extends CommonCtrl {

    @Autowired private AgentMchRelaService agentMchRelaService;

    @Operation(summary = "綁定列表（可依代理或商戶篩選）")
    @PreAuthorize("hasAnyAuthority('ENT_MCH_AGENT_BIND', 'ENT_AGENT_LIST')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<AgentMchRela> list() {
        LambdaQueryWrapper<AgentMchRela> wrapper = AgentMchRela.gw();
        String mchNo = getValString("mchNo");
        String agentNo = getValString("agentNo");
        String referrerAgentNo = getValString("referrerAgentNo");
        if (StringUtils.isNotEmpty(mchNo)) {
            wrapper.eq(AgentMchRela::getMchNo, mchNo);
        }
        if (StringUtils.isNotEmpty(agentNo)) {
            wrapper.eq(AgentMchRela::getAgentNo, agentNo);
        }
        if (StringUtils.isNotEmpty(referrerAgentNo)) {
            wrapper.eq(AgentMchRela::getReferrerAgentNo, referrerAgentNo);
        }
        wrapper.orderByDesc(AgentMchRela::getUpdatedAt);
        IPage<AgentMchRela> pages = agentMchRelaService.page(getIPage(true), wrapper);
        return ApiPageRes.pages(pages);
    }

    @Operation(summary = "查詢商戶的代理綁定")
    @PreAuthorize("hasAuthority('ENT_MCH_AGENT_BIND')")
    @RequestMapping(value = "/{mchNo}", method = RequestMethod.GET)
    public ApiRes<AgentMchRela> detail(@PathVariable("mchNo") String mchNo) {
        return ApiRes.ok(agentMchRelaService.getById(mchNo));
    }

    @Operation(summary = "綁定或變更商戶的直屬代理與推薦人")
    @PreAuthorize("hasAuthority('ENT_MCH_AGENT_BIND')")
    @MethodLog(remark = "綁定商戶代理")
    @RequestMapping(value = "/{mchNo}", method = RequestMethod.PUT)
    public ApiRes bind(@PathVariable("mchNo") String mchNo) {
        agentMchRelaService.bind(mchNo, getValStringRequired("agentNo"), getValString("referrerAgentNo"),
                getCurrentUser().getSysUser().getSysUserId(), getCurrentUser().getSysUser().getRealname());
        return ApiRes.ok();
    }

    @Operation(summary = "解除商戶的代理綁定")
    @PreAuthorize("hasAuthority('ENT_MCH_AGENT_BIND')")
    @MethodLog(remark = "解除商戶代理綁定")
    @RequestMapping(value = "/{mchNo}", method = RequestMethod.DELETE)
    public ApiRes unbind(@PathVariable("mchNo") String mchNo) {
        agentMchRelaService.unbind(mchNo);
        return ApiRes.ok();
    }
}
