package com.jeequan.jeepay.mgr.ctrl.monitor;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.SysLoginDevice;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.AgentInfoService;
import com.jeequan.jeepay.service.impl.SysLoginDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登入裝置（上帝）：自己最近的登入，以及每位團長、隊長（含其操作員）最近一次登入的 IP、裝置與 GPS。
 * 只限平台帳號；代理即使被誤配這個權限也看不到。
 */
@Tag(name = "登入裝置")
@RestController
@RequestMapping("/api/loginDevices")
public class LoginDeviceController extends CommonCtrl {

    @Autowired private SysLoginDeviceService sysLoginDeviceService;
    @Autowired private AgentInfoService agentInfoService;

    @Operation(summary = "我的最近登入，與所有團長、隊長的最近一次登入")
    @PreAuthorize("hasAuthority('ENT_LOGIN_DEVICE')")
    @RequestMapping(value = "/overview", method = RequestMethod.GET)
    public ApiRes overview() {
        SysUser me = requirePlatformUser();
        // 每個代理只取最新一筆：先取各代理的最大 id，再一次撈出
        List<Object> latestIds = sysLoginDeviceService.listObjs(new QueryWrapper<SysLoginDevice>()
                .select("MAX(id) AS id").eq("sys_type", "MGR")
                .isNotNull("belong_info_id").ne("belong_info_id", "0").groupBy("belong_info_id"));
        Map<String, SysLoginDevice> latest = latestIds.isEmpty() ? Map.of()
                : sysLoginDeviceService.listByIds(latestIds.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysLoginDevice::getBelongInfoId, d -> d, (a, b) -> a.getId() > b.getId() ? a : b));

        List<JSONObject> agents = agentInfoService.list(AgentInfo.gw().orderByAsc(AgentInfo::getAgentLevel).orderByDesc(AgentInfo::getCreatedAt))
                .stream().map(a -> {
                    JSONObject o = new JSONObject(true);
                    o.put("agentNo", a.getAgentNo());
                    o.put("agentName", a.getAgentName());
                    o.put("agentLevel", a.getAgentLevel());
                    o.put("parentAgentNo", a.getParentAgentNo());
                    o.put("state", a.getState());
                    o.put("lastLogin", latest.get(a.getAgentNo()));
                    return o;
                }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mine", sysLoginDeviceService.recentByUser(me.getSysUserId(), 10));
        result.put("agents", agents);
        return ApiRes.ok(result);
    }

    @Operation(summary = "某位團長或隊長（含其操作員）最近 20 次登入")
    @PreAuthorize("hasAuthority('ENT_LOGIN_DEVICE')")
    @RequestMapping(value = "/agent", method = RequestMethod.GET)
    public ApiRes agentHistory() {
        requirePlatformUser();
        return ApiRes.ok(sysLoginDeviceService.recentByAgent(getValStringRequired("agentNo"), 20));
    }

    private SysUser requirePlatformUser() {
        SysUser me = getCurrentUser().getSysUser();
        if (StringUtils.isNotBlank(me.getBelongInfoId()) && !"0".equals(me.getBelongInfoId())) {
            throw new BizException("只有平台可以查看登入裝置");
        }
        return me;
    }
}
