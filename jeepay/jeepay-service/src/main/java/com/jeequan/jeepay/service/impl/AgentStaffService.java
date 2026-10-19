package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.SysEntitlement;
import com.jeequan.jeepay.core.entity.SysRole;
import com.jeequan.jeepay.core.entity.SysRoleEntRela;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.entity.SysUserRoleRela;
import com.jeequan.jeepay.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代理的員工子帳號（比照競品「我的子帳號」）。
 *
 * 子帳號與主帳號同屬一個代理（belong_info_id 相同），差別只在角色：主帳號用固定角色
 * ROLE_AGENT_PORTAL／ROLE_AGENT_PORTAL_L2，子帳號每人一個專屬角色 ROLE_AGSUB_{sysUserId}，
 * 權限由主帳號逐項勾選，且只能從主帳號自己的權限裡挑。
 * 子帳號管理權限（ENT_AGENT_PORTAL_STAFF*）永遠不可授予子帳號，避免子帳號再開子帳號或替自己加權限。
 */
@Service
public class AgentStaffService {

    public static final String STAFF_ROLE_PREFIX = "ROLE_AGSUB_";
    /** 子帳號管理本身的權限，不可下放 */
    private static final String STAFF_ENT_PREFIX = "ENT_AGENT_PORTAL_STAFF";
    /** 任何子帳號都需要的基礎權限：個人中心、代理後台根選單與資料讀取（前端不列出，儲存時自動補上） */
    public static final List<String> BASE_ENTS = List.of("ENT_COMMONS", "ENT_C_USERINFO", "ENT_AGENT_PORTAL", "ENT_AGENT_PORTAL_VIEW");

    @Autowired private SysUserService sysUserService;
    @Autowired private SysUserAuthService sysUserAuthService;
    @Autowired private SysUserRoleRelaService sysUserRoleRelaService;
    @Autowired private SysRoleService sysRoleService;
    @Autowired private SysRoleEntRelaService sysRoleEntRelaService;
    @Autowired private SysEntitlementService sysEntitlementService;
    @Autowired private AgentPortalService agentPortalService;

    private static String roleIdOf(Long sysUserId) {
        return STAFF_ROLE_PREFIX + sysUserId;
    }

    /** 這個代理底下的子帳號 ID（以專屬角色判定，主帳號不在其中）。 */
    private Set<Long> staffUserIds(String agentNo) {
        List<String> roleIds = sysRoleService.list(SysRole.gw().select(SysRole::getRoleId)
                        .eq(SysRole::getSysType, CS.SYS_TYPE.MGR).eq(SysRole::getBelongInfoId, agentNo)
                        .likeRight(SysRole::getRoleId, STAFF_ROLE_PREFIX))
                .stream().map(SysRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return sysUserRoleRelaService.list(SysUserRoleRela.gw().in(SysUserRoleRela::getRoleId, roleIds))
                .stream().map(SysUserRoleRela::getUserId).collect(Collectors.toSet());
    }

    public List<JSONObject> list(AgentInfo me) {
        Set<Long> ids = staffUserIds(me.getAgentNo());
        List<JSONObject> result = new ArrayList<>();
        if (ids.isEmpty()) {
            return result;
        }
        Map<String, Long> entCounts = sysRoleEntRelaService.list(SysRoleEntRela.gw()
                        .in(SysRoleEntRela::getRoleId, ids.stream().map(AgentStaffService::roleIdOf).collect(Collectors.toList())))
                .stream().collect(Collectors.groupingBy(SysRoleEntRela::getRoleId, Collectors.counting()));
        sysUserService.list(SysUser.gw()
                        .select(SysUser::getSysUserId, SysUser::getLoginUsername, SysUser::getRealname, SysUser::getTelphone,
                                SysUser::getState, SysUser::getCreatedAt)
                        .eq(SysUser::getSysType, CS.SYS_TYPE.MGR).eq(SysUser::getBelongInfoId, me.getAgentNo())
                        .in(SysUser::getSysUserId, ids).orderByDesc(SysUser::getCreatedAt))
                .forEach(u -> {
                    JSONObject row = (JSONObject) JSONObject.toJSON(u);
                    if (AgentPortalService.isPlaceholderTel(u.getTelphone())) {
                        row.put("telphone", "");
                    }
                    row.put("entCount", entCounts.getOrDefault(roleIdOf(u.getSysUserId()), 0L));
                    result.add(row);
                });
        return result;
    }

    /** 建立子帳號：密碼為隨機 8 碼，只在這次回應出現；建立當下沒有任何權限，需另行設定。 */
    @Transactional
    public JSONObject create(AgentInfo me, String loginUsername, String realname, String telphone) {
        if (StringUtils.isBlank(loginUsername) || !loginUsername.matches(AgentPortalService.AGENT_LOGIN_PATTERN)) {
            throw new BizException(AgentPortalService.AGENT_LOGIN_HINT);
        }
        if (StringUtils.isBlank(realname)) {
            throw new BizException("請填寫名稱");
        }
        boolean hasTel = StringUtils.isNotBlank(telphone);
        if (hasTel && !telphone.matches("^09\\d{8}$")) {
            throw new BizException("手機號格式不正確（09 開頭 10 碼），不填也可以");
        }
        SysUser user = new SysUser();
        user.setLoginUsername(loginUsername);
        user.setRealname(realname);
        user.setTelphone(AgentPortalService.telOrPlaceholder(hasTel ? telphone : null));
        user.setSex(CS.SEX_UNKNOWN);
        user.setIsAdmin(CS.NO);
        user.setState(CS.YES);
        user.setBelongInfoId(me.getAgentNo());
        sysUserService.addSysUser(user, CS.SYS_TYPE.MGR);
        agentPortalService.dropPlaceholderTelLogin(user, CS.SYS_TYPE.MGR);

        SysRole role = new SysRole();
        role.setRoleId(roleIdOf(user.getSysUserId()));
        role.setRoleName("子帳號：" + loginUsername);
        role.setSysType(CS.SYS_TYPE.MGR);
        role.setBelongInfoId(me.getAgentNo());
        sysRoleService.save(role);
        sysUserService.saveUserRole(user.getSysUserId(), List.of(role.getRoleId()));

        // addSysUser 會先寫入系統預設密碼，這裡立即換成隨機密碼
        String password = AgentPortalService.randomPassword();
        sysUserAuthService.resetAuthInfo(user.getSysUserId(), null, null, password, CS.SYS_TYPE.MGR);
        JSONObject result = new JSONObject();
        result.put("sysUserId", user.getSysUserId());
        result.put("agentName", realname);
        result.put("loginUsername", loginUsername);
        result.put("initPassword", password);
        return result;
    }

    /** 主帳號可以下放的權限：自己固定角色擁有的，扣掉子帳號管理本身。 */
    public List<SysEntitlement> grantable(AgentInfo me) {
        List<String> entIds = sysRoleEntRelaService.list(SysRoleEntRela.gw()
                        .eq(SysRoleEntRela::getRoleId, AgentPortalService.roleOf(me)))
                .stream().map(SysRoleEntRela::getEntId)
                .filter(id -> !id.startsWith(STAFF_ENT_PREFIX)).collect(Collectors.toList());
        if (entIds.isEmpty()) {
            return List.of();
        }
        return sysEntitlementService.list(SysEntitlement.gw()
                .select(SysEntitlement::getEntId, SysEntitlement::getEntName, SysEntitlement::getEntType,
                        SysEntitlement::getPid, SysEntitlement::getEntSort)
                .eq(SysEntitlement::getSysType, CS.SYS_TYPE.MGR).eq(SysEntitlement::getState, CS.PUB_USABLE)
                .in(SysEntitlement::getEntId, entIds).orderByAsc(SysEntitlement::getEntSort));
    }

    public List<String> entIds(AgentInfo me, Long sysUserId) {
        requireStaff(me, sysUserId);
        return sysRoleEntRelaService.list(SysRoleEntRela.gw().eq(SysRoleEntRela::getRoleId, roleIdOf(sysUserId)))
                .stream().map(SysRoleEntRela::getEntId).collect(Collectors.toList());
    }

    /** 設定子帳號權限：只接受可下放範圍內的項目；有任何項目時自動補上基礎權限，否則登入後頁面讀不到資料。 */
    @Transactional
    public List<String> saveEntIds(AgentInfo me, Long sysUserId, List<String> requested) {
        requireStaff(me, sysUserId);
        Set<String> allowed = grantable(me).stream().map(SysEntitlement::getEntId).collect(Collectors.toSet());
        Set<String> picked = new LinkedHashSet<>();
        for (String entId : requested == null ? List.<String>of() : requested) {
            if (!allowed.contains(entId)) {
                throw new BizException("不可授予的權限：" + entId);
            }
            if (!BASE_ENTS.contains(entId)) {
                picked.add(entId);
            }
        }
        if (!picked.isEmpty()) {
            BASE_ENTS.stream().filter(allowed::contains).forEach(picked::add);
        }
        List<String> result = new ArrayList<>(picked);
        sysRoleEntRelaService.resetRela(roleIdOf(sysUserId), result);
        return result;
    }

    @Transactional
    public void setState(AgentInfo me, Long sysUserId, byte state) {
        SysUser user = requireStaff(me, sysUserId);
        SysUser update = new SysUser();
        update.setSysUserId(user.getSysUserId());
        update.setState(state == CS.YES ? CS.YES : CS.NO);
        sysUserService.updateById(update);
    }

    /** 重設子帳號密碼：新的隨機密碼只在這次回應出現。 */
    @Transactional
    public JSONObject resetPassword(AgentInfo me, Long sysUserId) {
        SysUser user = requireStaff(me, sysUserId);
        String password = AgentPortalService.randomPassword();
        sysUserAuthService.resetAuthInfo(user.getSysUserId(), null, null, password, CS.SYS_TYPE.MGR);
        JSONObject result = new JSONObject();
        result.put("agentName", user.getRealname());
        result.put("loginUsername", user.getLoginUsername());
        result.put("initPassword", password);
        return result;
    }

    /** 目標必須是「我這個代理」的子帳號；主帳號與別的代理的帳號一律拒絕。 */
    private SysUser requireStaff(AgentInfo me, Long sysUserId) {
        SysUser user = sysUserId == null ? null : sysUserService.getById(sysUserId);
        if (user == null || !CS.SYS_TYPE.MGR.equals(user.getSysType()) || !me.getAgentNo().equals(user.getBelongInfoId())
                || !staffUserIds(me.getAgentNo()).contains(sysUserId)) {
            throw new BizException("子帳號不存在");
        }
        return user;
    }
}
