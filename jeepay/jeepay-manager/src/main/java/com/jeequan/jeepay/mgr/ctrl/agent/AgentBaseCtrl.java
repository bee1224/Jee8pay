package com.jeequan.jeepay.mgr.ctrl.agent;

import cn.hutool.core.date.DateUtil;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Date;

/**
 * 代理與費率模組（ADR-0009）控制器共用基底。
 *
 * 注意：營運平台目前未啟用 Spring 方法層級安全（WebSecurityConfig 無 @EnableMethodSecurity），
 * 各控制器上的 @PreAuthorize 實際不生效（已記錄為安全債）。本模組涉及費率與分潤，
 * 因此每個端點另以 requireAuthority 在程式內檢查，與 @PreAuthorize 宣告一致；
 * 全域修正完成後這些檢查仍無害，可再評估移除。
 */
public abstract class AgentBaseCtrl extends CommonCtrl {

    /** 需具備任一權限，否則拋出權限錯誤。 */
    protected void requireAuthority(String... entIds) {
        boolean allowed = getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> Arrays.asList(entIds).contains(a));
        if (!allowed) {
            throw new BizException(ApiCodeEnum.SYS_PERMISSION_ERROR);
        }
    }

    protected Date startDate() {
        String v = getValString("startDate");
        return StringUtils.isBlank(v) ? null : DateUtil.beginOfDay(DateUtil.parseDate(v));
    }

    /** 迄日含當日，內部轉為隔日 00:00 的開區間。 */
    protected Date endDate() {
        String v = getValString("endDate");
        return StringUtils.isBlank(v) ? null : DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parseDate(v)), 1);
    }
}
