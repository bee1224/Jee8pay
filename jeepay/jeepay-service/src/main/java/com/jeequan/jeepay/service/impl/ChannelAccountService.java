package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.ChannelAccount;
import com.jeequan.jeepay.core.entity.ChannelAccountAgent;
import com.jeequan.jeepay.core.entity.PayInterfaceConfig;
import com.jeequan.jeepay.core.entity.PayInterfaceDefine;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.utils.StringKit;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.ChannelAccountAgentMapper;
import com.jeequan.jeepay.service.mapper.ChannelAccountMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 渠道帳號（ADR-0012 第一階段）：只有上帝能建立、輸入金鑰與派發；團長只能看到派發給自己的帳號，看不到金鑰。
 * 本階段只建立帳號與派發關係，下單仍依商戶應用取金鑰。
 */
@Service
public class ChannelAccountService extends ServiceImpl<ChannelAccountMapper, ChannelAccount> {

    @Autowired private ChannelAccountAgentMapper channelAccountAgentMapper;
    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private PayInterfaceConfigService payInterfaceConfigService;
    @Autowired private PayInterfaceDefineService payInterfaceDefineService;

    /** 帳號列表（不含金鑰）；srAgentNo 有值時只回傳派發給該團長的帳號。每列附上被派發的團長。 */
    public List<ChannelAccount> listView(String srAgentNo) {
        List<ChannelAccountAgent> grants = channelAccountAgentMapper.selectList(ChannelAccountAgent.gw());
        List<ChannelAccount> accounts;
        if (StringUtils.isBlank(srAgentNo)) {
            accounts = list(ChannelAccount.gw().orderByDesc(ChannelAccount::getCreatedAt));
        } else {
            List<String> ids = grants.stream().filter(g -> srAgentNo.equals(g.getSrAgentNo()))
                    .map(ChannelAccountAgent::getAccountId).collect(Collectors.toList());
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            accounts = list(ChannelAccount.gw().in(ChannelAccount::getAccountId, ids).orderByDesc(ChannelAccount::getCreatedAt));
        }
        if (accounts.isEmpty()) {
            return accounts;
        }
        Map<String, String> agentNames = agentInfoMapper.selectList(AgentInfo.gw().eq(AgentInfo::getAgentLevel, AgentInfo.LEVEL_SENIOR))
                .stream().collect(Collectors.toMap(AgentInfo::getAgentNo, AgentInfo::getAgentName));
        Map<String, String> ifNames = payInterfaceDefineService.list()
                .stream().collect(Collectors.toMap(PayInterfaceDefine::getIfCode, PayInterfaceDefine::getIfName));
        for (ChannelAccount account : accounts) {
            List<JSONObject> assigned = new ArrayList<>();
            for (ChannelAccountAgent g : grants) {
                if (account.getAccountId().equals(g.getAccountId())) {
                    JSONObject row = new JSONObject();
                    row.put("srAgentNo", g.getSrAgentNo());
                    row.put("agentName", agentNames.get(g.getSrAgentNo()));
                    row.put("owner", g.getSrAgentNo().equals(account.getOwnerSrAgentNo()));
                    assigned.add(row);
                }
            }
            account.addExt("agents", assigned);
            account.addExt("ownerName", agentNames.get(account.getOwnerSrAgentNo()));
            account.addExt("ifName", ifNames.get(account.getIfCode()));
        }
        return accounts;
    }

    /** 團長看到的渠道列表：只有名稱、接口、狀態與是否為自己擁有，不含金鑰與其他團長。 */
    public List<JSONObject> listForAgent(String srAgentNo) {
        List<JSONObject> rows = new ArrayList<>();
        for (ChannelAccount account : listView(srAgentNo)) {
            JSONObject row = new JSONObject();
            row.put("accountId", account.getAccountId());
            row.put("accountName", account.getAccountName());
            row.put("ifCode", account.getIfCode());
            row.put("ifName", account.getExt().get("ifName"));
            row.put("state", account.getState());
            row.put("owned", srAgentNo.equals(account.getOwnerSrAgentNo()));
            rows.add(row);
        }
        return rows;
    }

    /** 單一帳號與遮罩後的金鑰（供上帝修改時顯示）。 */
    public ChannelAccount detailWithMaskedParams(String accountId) {
        ChannelAccount account = requireAccount(accountId);
        PayInterfaceConfig config = payInterfaceConfigService.getByInfoIdAndIfCode(CS.INFO_TYPE_CHANNEL_ACCOUNT, accountId, account.getIfCode());
        if (config != null && StringUtils.isNotBlank(config.getIfParams())) {
            NormalMchParams params = NormalMchParams.factory(account.getIfCode(), config.getIfParams());
            account.addExt("ifParams", params == null ? null : params.deSenData());
        }
        return account;
    }

    @Transactional
    public ChannelAccount create(ChannelAccount input, String ifParams, Long operatorUid, String operatorName) {
        if (input == null || StringUtils.isAnyBlank(input.getAccountName(), input.getIfCode(), input.getOwnerSrAgentNo())) {
            throw new BizException("帳號名稱、支付接口與所屬團長為必填");
        }
        PayInterfaceDefine define = payInterfaceDefineService.getById(input.getIfCode());
        if (define == null || !Objects.equals(define.getState(), CS.YES) || !Objects.equals(define.getIsMchMode(), CS.YES)) {
            throw new BizException("支付接口不存在或未啟用");
        }
        requireSenior(input.getOwnerSrAgentNo());
        requireValidParams(input.getIfCode(), ifParams);

        ChannelAccount account = new ChannelAccount()
                .setAccountId("CA" + new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + RandomStringUtils.randomNumeric(3))
                .setIfCode(input.getIfCode())
                .setAccountName(input.getAccountName().trim())
                .setOwnerSrAgentNo(input.getOwnerSrAgentNo())
                .setShareable(CS.NO)
                .setState(input.getState() == null ? CS.YES : input.getState())
                .setRemark(input.getRemark())
                .setCreatedUid(operatorUid)
                .setCreatedBy(operatorName);
        if (!save(account)) {
            throw new BizException("新增渠道帳號失敗");
        }
        PayInterfaceConfig config = new PayInterfaceConfig();
        config.setInfoType(CS.INFO_TYPE_CHANNEL_ACCOUNT);
        config.setInfoId(account.getAccountId());
        config.setIfCode(account.getIfCode());
        config.setIfParams(ifParams);
        config.setState(CS.YES);
        config.setCreatedUid(operatorUid);
        config.setCreatedBy(operatorName);
        config.setUpdatedUid(operatorUid);
        config.setUpdatedBy(operatorName);
        if (!payInterfaceConfigService.save(config)) {
            throw new BizException("儲存渠道金鑰失敗");
        }
        channelAccountAgentMapper.insert(new ChannelAccountAgent()
                .setAccountId(account.getAccountId()).setSrAgentNo(account.getOwnerSrAgentNo()).setCreatedBy(operatorName));
        return account;
    }

    /** 修改名稱、狀態、備註與是否可共用；ifParams 有值時與既有金鑰合併（未填的遮罩欄位保留原值）。接口與所屬團長不可變。 */
    @Transactional
    public void update(String accountId, ChannelAccount changes, String ifParams, Long operatorUid, String operatorName) {
        ChannelAccount current = requireAccount(accountId);
        if (Objects.equals(changes.getShareable(), CS.NO) && grantCount(accountId) > 1) {
            throw new BizException("此帳號已加派給其他團長，請先收回再關閉共用");
        }
        ChannelAccount update = new ChannelAccount().setAccountId(accountId)
                .setAccountName(StringUtils.trimToNull(changes.getAccountName()))
                .setState(changes.getState())
                .setShareable(changes.getShareable())
                .setRemark(changes.getRemark());
        if (!updateById(update)) {
            throw new BizException("更新渠道帳號失敗");
        }
        if (StringUtils.isNotBlank(ifParams)) {
            PayInterfaceConfig config = payInterfaceConfigService.getByInfoIdAndIfCode(CS.INFO_TYPE_CHANNEL_ACCOUNT, accountId, current.getIfCode());
            if (config == null) {
                throw new BizException("渠道金鑰不存在");
            }
            String merged = StringKit.marge(config.getIfParams(), ifParams);
            requireValidParams(current.getIfCode(), merged);
            PayInterfaceConfig configUpdate = new PayInterfaceConfig();
            configUpdate.setId(config.getId());
            configUpdate.setIfParams(merged);
            configUpdate.setUpdatedUid(operatorUid);
            configUpdate.setUpdatedBy(operatorName);
            if (!payInterfaceConfigService.updateById(configUpdate)) {
                throw new BizException("更新渠道金鑰失敗");
            }
        }
    }

    /** 加派給另一位團長：帳號必須先由上帝開啟共用。 */
    public void grant(String accountId, String srAgentNo, String operatorName) {
        ChannelAccount account = requireAccount(accountId);
        requireSenior(srAgentNo);
        if (!Objects.equals(account.getShareable(), CS.YES)) {
            throw new BizException("此帳號未開啟共用，不可加派給其他團長");
        }
        if (channelAccountAgentMapper.selectCount(ChannelAccountAgent.gw()
                .eq(ChannelAccountAgent::getAccountId, accountId).eq(ChannelAccountAgent::getSrAgentNo, srAgentNo)) > 0) {
            throw new BizException("該團長已可使用此帳號");
        }
        channelAccountAgentMapper.insert(new ChannelAccountAgent()
                .setAccountId(accountId).setSrAgentNo(srAgentNo).setCreatedBy(operatorName));
    }

    /** 收回加派；擁有者的那一列不可收回。 */
    public void revoke(String accountId, String srAgentNo) {
        ChannelAccount account = requireAccount(accountId);
        if (account.getOwnerSrAgentNo().equals(srAgentNo)) {
            throw new BizException("不可收回擁有者的使用權");
        }
        channelAccountAgentMapper.delete(ChannelAccountAgent.gw()
                .eq(ChannelAccountAgent::getAccountId, accountId).eq(ChannelAccountAgent::getSrAgentNo, srAgentNo));
    }

    @Transactional
    public void removeAccount(String accountId) {
        ChannelAccount account = requireAccount(accountId);
        if (grantCount(accountId) > 1) {
            throw new BizException("此帳號已加派給其他團長，請先收回再刪除");
        }
        channelAccountAgentMapper.delete(ChannelAccountAgent.gw().eq(ChannelAccountAgent::getAccountId, accountId));
        payInterfaceConfigService.remove(PayInterfaceConfig.gw()
                .eq(PayInterfaceConfig::getInfoType, CS.INFO_TYPE_CHANNEL_ACCOUNT)
                .eq(PayInterfaceConfig::getInfoId, accountId)
                .eq(PayInterfaceConfig::getIfCode, account.getIfCode()));
        if (!removeById(accountId)) {
            throw new BizException("刪除渠道帳號失敗");
        }
    }

    private long grantCount(String accountId) {
        return channelAccountAgentMapper.selectCount(ChannelAccountAgent.gw().eq(ChannelAccountAgent::getAccountId, accountId));
    }

    private ChannelAccount requireAccount(String accountId) {
        ChannelAccount account = getById(accountId);
        if (account == null) {
            throw new BizException("渠道帳號不存在");
        }
        return account;
    }

    private void requireSenior(String agentNo) {
        AgentInfo agent = StringUtils.isBlank(agentNo) ? null : agentInfoMapper.selectById(agentNo);
        if (agent == null || !Objects.equals(agent.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
            throw new BizException("渠道帳號只能屬於團長");
        }
        if (!Objects.equals(agent.getState(), (byte) 1)) {
            throw new BizException("該團長已停用");
        }
    }

    private void requireValidParams(String ifCode, String ifParams) {
        if (StringUtils.isBlank(ifParams)) {
            throw new BizException("請填寫渠道金鑰");
        }
        NormalMchParams params;
        try {
            params = NormalMchParams.factory(ifCode, ifParams);
        } catch (RuntimeException e) {
            params = null;
        }
        if (params == null) {
            throw new BizException("渠道金鑰格式不正確");
        }
    }
}
