package com.jeequan.jeepay.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.SysUser;
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
import java.util.Objects;

/** 代理資訊服務（ADR-0009）：高級代理 → 一般代理 兩層，上級建立後不可更改以保持物化路徑一致。 */
@Service
public class AgentInfoService extends ServiceImpl<AgentInfoMapper, AgentInfo> {

    @Autowired private AgentMchRelaService agentMchRelaService;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private SysUserService sysUserService;
    @Autowired private WalletAccountMapper walletAccountMapper;

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
                throw new BizException("一般代理的上級必須是高級代理");
            }
            if (!Objects.equals(parent.getState(), (byte) 1)) {
                throw new BizException("上級高級代理已停用");
            }
            parentPath = parent.getAgentPath();
        } else {
            throw new BizException("代理層級必須為 1（高級代理）或 2（一般代理）");
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

    @Transactional
    public void removeAgent(String agentNo, Long operatorUid, String operatorName) {
        AgentInfo agent = getById(agentNo);
        if (agent == null) {
            throw new BizException("代理不存在");
        }
        if (count(AgentInfo.gw().eq(AgentInfo::getParentAgentNo, agentNo)) > 0) {
            throw new BizException("該代理下仍有一般代理，不可刪除");
        }
        if (agentMchRelaService.count(AgentMchRela.gw().eq(AgentMchRela::getAgentNo, agentNo)) > 0) {
            throw new BizException("該代理仍有綁定的商戶，不可刪除");
        }
        if (agentMchRelaService.count(AgentMchRela.gw().eq(AgentMchRela::getReferrerAgentNo, agentNo)) > 0) {
            throw new BizException("該代理仍是商戶的推薦人，不可刪除");
        }
        // 錢包仍有餘額或提現處理中時不可刪除，否則款項會變成無主帳戶
        WalletAccount wallet = walletAccountMapper.selectOne(WalletAccount.gw()
                .eq(WalletAccount::getOwnerType, WalletAccount.OWNER_AGENT).eq(WalletAccount::getOwnerId, agentNo));
        if (wallet != null && (wallet.getBalance() != 0 || wallet.getFrozen() != 0)) {
            throw new BizException("該代理錢包仍有餘額或提現處理中，請先結清再刪除");
        }
        // 代理登入帳號的 belong_info_id 指向代理號，先刪帳號避免留下無主帳號
        if (sysUserService.count(SysUser.gw().eq(SysUser::getSysType, CS.SYS_TYPE.MGR).eq(SysUser::getBelongInfoId, agentNo)) > 0) {
            throw new BizException("該代理仍有登入帳號，請先至「系統管理 → 操作員」刪除");
        }
        List<FeeRule> rules = feeRuleService.list(FeeRule.gw()
                .eq(FeeRule::getTargetType, FeeRule.TARGET_AGENT).eq(FeeRule::getTargetId, agentNo));
        for (FeeRule rule : rules) {
            feeRuleService.removeRule(rule.getRuleId(), operatorUid, operatorName);
        }
        if (!removeById(agentNo)) {
            throw new BizException("刪除代理失敗");
        }
    }

    /** 回傳商戶直屬代理所屬的高級代理（直屬代理本身即高級代理時回傳自己）。 */
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
