package com.jeequan.jeepay.mgr.ctrl.payconfig;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.WayRoute;
import com.jeequan.jeepay.core.entity.WayRouteLog;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.mapper.WayRouteLogMapper;
import com.jeequan.jeepay.service.route.WayRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** 通道路由規則與決策紀錄（ADR-0011）。 */
@Tag(name = "通道路由")
@RestController
@RequestMapping("/api/wayRoutes")
public class WayRouteController extends CommonCtrl {

    @Autowired private WayRouteService wayRouteService;
    @Autowired private WayRouteLogMapper wayRouteLogMapper;

    @Operation(summary = "路由規則列表")
    @PreAuthorize("hasAuthority('ENT_WAY_ROUTE')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ApiPageRes<WayRoute> list() {
        LambdaQueryWrapper<WayRoute> w = WayRoute.gw();
        String alias = getValString("aliasWayCode");
        String mchNo = getValString("mchNo");
        w.eq(StringUtils.isNotBlank(alias), WayRoute::getAliasWayCode, alias);
        w.eq(StringUtils.isNotBlank(mchNo), WayRoute::getMchNo, mchNo);
        w.orderByAsc(WayRoute::getAliasWayCode, WayRoute::getMchNo).orderByDesc(WayRoute::getWeight);
        return ApiPageRes.pages(wayRouteService.page(getIPage(true), w));
    }

    @Operation(summary = "新增或修改路由規則")
    @PreAuthorize("hasAuthority('ENT_WAY_ROUTE_EDIT')")
    @MethodLog(remark = "儲存路由規則")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes save() {
        WayRoute input = getObject(WayRoute.class);
        input.setRouteId(getValLong("routeId"));
        return ApiRes.ok(wayRouteService.saveRoute(input, getCurrentUser().getSysUser().getRealname()));
    }

    @Operation(summary = "刪除路由規則")
    @PreAuthorize("hasAuthority('ENT_WAY_ROUTE_EDIT')")
    @MethodLog(remark = "刪除路由規則")
    @RequestMapping(value = "/{routeId}", method = RequestMethod.DELETE)
    public ApiRes delete(@PathVariable("routeId") Long routeId) {
        wayRouteService.removeById(routeId);
        return ApiRes.ok();
    }

    @Operation(summary = "路由決策紀錄")
    @PreAuthorize("hasAuthority('ENT_WAY_ROUTE')")
    @RequestMapping(value = "/logs", method = RequestMethod.GET)
    public ApiPageRes<WayRouteLog> logs() {
        LambdaQueryWrapper<WayRouteLog> w = WayRouteLog.gw();
        String mchNo = getValString("mchNo");
        String mchOrderNo = getValString("mchOrderNo");
        w.eq(StringUtils.isNotBlank(mchNo), WayRouteLog::getMchNo, mchNo);
        w.eq(StringUtils.isNotBlank(mchOrderNo), WayRouteLog::getMchOrderNo, mchOrderNo);
        w.orderByDesc(WayRouteLog::getLogId);
        return ApiPageRes.pages(wayRouteLogMapper.selectPage(getIPage(), w));
    }
}
