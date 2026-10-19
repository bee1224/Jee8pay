package com.jeequan.jeepay.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.SysRole;
import com.jeequan.jeepay.core.entity.SysRoleEntRela;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.entity.SysUserAuth;
import com.jeequan.jeepay.core.entity.SysUserRoleRela;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.WalletAccountMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

/** 代理資訊服務（ADR-0009）：團長 → 隊長 兩層，上級建立後不可更改以保持物化路徑一致。 */
@Service
public class AgentInfoService extends ServiceImpl<AgentInfoMapper, AgentInfo> {

    @Autowired private AgentMchRelaService agentMchRelaService;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private SysUserService sysUserService;
    @Autowired private SysRoleService sysRoleService;
    @Autowired private SysUserAuthService sysUserAuthService;
    @Autowired private SysUserRoleRelaService sysUserRoleRelaService;
    @Autowired private SysRoleEntRelaService sysRoleEntRelaService;
    @Autowired private com.jeequan.jeepay.service.mapper.MchInfoMapper mchInfoMapper;
    @Autowired private WalletAccountMapper walletAccountMapper;
    @Autowired private com.jeequan.jeepay.service.mapper.ChannelAccountAgentMapper channelAccountAgentMapper;
    @Autowired private com.jeequan.jeepay.service.mapper.ChannelAccountScopeMapper channelAccountScopeMapper;

    @Transactional
    public AgentInfo create(AgentInfo agent, Long operatorUid, String operatorName) {
        if (agent == null || StringUtils.isBlank(agent.getAgentName())) {
            throw new BizException("代理名稱為必填");
        }
        Byte level = agent.getAgentLevel();
        String parentPath = "/";
        if (Objects.equals(level, AgentInfo.LEVEL_SENIOR)) {
            agent.setParentAgentNo(null);
        } else if (Objects.equals(level, AgentInfo.LEVEL_AGENT)) {
            AgentInfo parent = StringUtils.isBlank(agent.getParentAgentNo()) ? null : getById(agent.getParentAgentNo());
            if (parent == null || !Objects.equals(parent.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
                throw new BizException("隊長的上級必須是團長");
            }
            if (!Objects.equals(parent.getState(), (byte) 1)) {
                throw new BizException("上級團長已停用");
            }
            parentPath = parent.getAgentPath();
        } else {
            throw new BizException("代理層級必須為 1（團長）或 2（隊長）");
        }

        String agentNo = "A" + new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + RandomStringUtils.randomNumeric(3);
        agent.setAgentNo(agentNo);
        agent.setAgentPath(parentPath + agentNo + "/");
        agent.setState(agent.getState() == null ? (byte) 1 : agent.getState());
        agent.setCreatedUid(operatorUid);
        agent.setCreatedBy(operatorName);
        if (!save(agent)) {
            throw new BizException("新增代理失敗");
        }
        return agent;
    }

    /** 只允許修改基本資料與狀態；層級、上級、路徑不可變。 */
    public void updateBasic(String agentNo, AgentInfo changes) {
        AgentInfo current = getById(agentNo);
        if (current == null) {
            throw new BizException("代理不存在");
        }
        AgentInfo update = new AgentInfo();
        update.setAgentNo(agentNo);
        update.setAgentName(changes.getAgentName());
        update.setContactName(changes.getContactName());
        update.setContactTel(changes.getContactTel());
        update.setContactEmail(changes.getContactEmail());
        update.setState(changes.getState());
        update.setRemark(changes.getRemark());
        if (!updateById(update)) {
            throw new BizException("更新代理失敗");
        }
    }

    /**
     * 刪除代理。商戶、隊長、渠道、錢包餘額任一還在就拒絕；登入帳號（主帳號與子帳號）隨代理一起刪除。
     * 回傳被刪除的登入帳號 ID，由呼叫端清掉這些帳號的登入狀態。
     */
    @Transactional
    public List<Long> removeAgent(String agentNo, Long operatorUid, String operatorName) {
        AgentInfo agent = getById(agentNo);
        if (agent == null) {
            throw new BizException("代理不存在");
        }
        if (Objects.equals(agent.getIsHouse(), (byte) 1)) {
            throw new BizException("平台直屬不可刪除");
        }
        if (channelAccountAgentMapper.selectCount(com.jeequan.jeepay.core.entity.ChannelAccountAgent.gw()
                .eq(com.jeequan.jeepay.core.entity.ChannelAccountAgent::getSrAgentNo, agentNo)) > 0) {
            throw new BizException("該代理仍有渠道帳號，請先刪除或收回");
        }
        // 自動移除會讓限定範圍變回「全部可用」，因此要求先人工調整
        if (channelAccountScopeMapper.selectCount(com.jeequan.jeepay.core.entity.ChannelAccountScope.gw()
                .eq(com.jeequan.jeepay.core.entity.ChannelAccountScope::getAgentNo, agentNo)) > 0) {
            throw new BizException("該隊長仍被指定為渠道的使用對象，請先調整渠道的使用範圍");
        }
        if (count(AgentInfo.gw().eq(AgentInfo::getParentAgentNo, agentNo)) > 0) {
            throw new BizException("該代理下仍有隊長，不可刪除");
        }
        // 先清掉指向已刪除商戶的綁定（舊版刪商戶時沒有一併清除）
        List<String> boundMchNos = agentMchRelaService.list(AgentMchRela.gw().eq(AgentMchRela::getAgentNo, agentNo))
                .stream().map(AgentMchRela::getMchNo).collect(Collectors.toList());
        List<String> ghostMchNos = boundMchNos.stream().filter(no -> mchInfoMapper.selectById(no) == null).collect(Collectors.toList());
        if (!ghostMchNos.isEmpty()) {
            agentMchRelaService.remove(AgentMchRela.gw().eq(AgentMchRela::getAgentNo, agentNo).in(AgentMchRela::getMchNo, ghostMchNos));
        }
        if (boundMchNos.size() > ghostMchNos.size()) {
            throw new BizException("該代理仍有綁定的商戶，不可刪除");
        }
        // 錢包仍有餘額或提現處理中時不可刪除，否則款項會變成無主帳戶
        WalletAccount wallet = walletAccountMapper.selectOne(WalletAccount.gw()
                .eq(WalletAccount::getOwnerType, WalletAccount.OWNER_AGENT).eq(WalletAccount::getOwnerId, agentNo));
        if (wallet != null && (wallet.getBalance() != 0 || wallet.getFrozen() != 0)) {
            throw new BizException("該代理錢包仍有餘額或提現處理中，請先結清再刪除");
        }
        // 代理登入帳號的 belong_info_id 指向代理號，隨代理一起刪除，避免留下無主帳號
        List<SysUser> users = sysUserService.list(SysUser.gw().eq(SysUser::getSysType, CS.SYS_TYPE.MGR).eq(SysUser::getBelongInfoId, agentNo));
        if (!users.isEmpty()) {
            // 不用 SysUserService.removeUser：它以 removeById 刪使用者角色，而 t_sys_user_role_rela 沒有單一主鍵
            List<Long> userIds = users.stream().map(SysUser::getSysUserId).collect(Collectors.toList());
            sysUserAuthService.remove(SysUserAuth.gw().eq(SysUserAuth::getSysType, CS.SYS_TYPE.MGR).in(SysUserAuth::getUserId, userIds));
            sysUserRoleRelaService.remove(SysUserRoleRela.gw().in(SysUserRoleRela::getUserId, userIds));
            sysUserService.removeByIds(userIds);
        }
        // 子帳號的專屬角色（ROLE_AGSUB_*）只屬於這個代理，一併刪除
        List<String> staffRoleIds = sysRoleService.list(SysRole.gw().select(SysRole::getRoleId)
                        .eq(SysRole::getSysType, CS.SYS_TYPE.MGR).eq(SysRole::getBelongInfoId, agentNo)
                        .likeRight(SysRole::getRoleId, AgentStaffService.STAFF_ROLE_PREFIX))
                .stream().map(SysRole::getRoleId).collect(Collectors.toList());
        if (!staffRoleIds.isEmpty()) {
            sysRoleEntRelaService.remove(SysRoleEntRela.gw().in(SysRoleEntRela::getRoleId, staffRoleIds));
            sysRoleService.removeByIds(staffRoleIds);
        }
        List<FeeRule> rules = feeRuleService.list(FeeRule.gw()
                .eq(FeeRule::getTargetType, FeeRule.TARGET_AGENT).eq(FeeRule::getTargetId, agentNo));
        for (FeeRule rule : rules) {
            feeRuleService.removeRule(rule.getRuleId(), operatorUid, operatorName);
        }
        if (!removeById(agentNo)) {
            throw new BizException("刪除代理失敗");
        }
        return users.stream().map(SysUser::getSysUserId).collect(Collectors.toList());
    }

    /** 回傳商戶直屬代理所屬的團長（直屬代理本身即團長時回傳自己）。 */
    public AgentInfo seniorOf(AgentInfo directAgent) {
        if (directAgent == null) {
            return null;
        }
        if (Objects.equals(directAgent.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
            return directAgent;
        }
        return StringUtils.isBlank(directAgent.getParentAgentNo()) ? null : getById(directAgent.getParentAgentNo());
    }
}
