package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.AgentMchRelaMapper;
import com.jeequan.jeepay.service.mapper.PayOrderFeeMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 代理後台（ADR-0009 第三階段）。
 * 代理帳號是 sys_type=MGR、belong_info_id=代理號 的操作員，只綁定 ROLE_AGENT_PORTAL；
 * 資料範圍一律由「目前登入者的 belong_info_id → agent_path 前綴」決定，不信任前端傳入的代理號。
 */
@Service
public class AgentPortalService {

    /** 代理帳號固定角色（由 SQL seed，僅含代理後台權限） */
    public static final String ROLE_AGENT_PORTAL = "ROLE_AGENT_PORTAL";

    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private AgentMchRelaMapper agentMchRelaMapper;
    @Autowired private PayOrderFeeMapper payOrderFeeMapper;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private MchInfoService mchInfoService;
    @Autowired private SysUserService sysUserService;

    /** 由登入者歸屬取得代理；非代理帳號或代理已停用一律拒絕。 */
    public AgentInfo requireAgent(SysUser currentUser) {
        String agentNo = currentUser == null ? null : currentUser.getBelongInfoId();
        AgentInfo agent = StringUtils.isBlank(agentNo) || "0".equals(agentNo) ? null : agentInfoMapper.selectById(agentNo);
        if (agent == null) {
            throw new BizException("此頁僅供代理帳號使用");
        }
        if (agent.getState() == null || agent.getState() != CS.YES) {
            throw new BizException("代理已停用");
        }
        return agent;
    }

    /** 轄區內所有代理（含自己）：以物化路徑前綴比對。 */
    public List<AgentInfo> jurisdiction(AgentInfo me) {
        return agentInfoMapper.selectList(AgentInfo.gw().likeRight(AgentInfo::getAgentPath, me.getAgentPath())
                .orderByAsc(AgentInfo::getAgentPath));
    }

    public List<AgentInfo> subAgents(AgentInfo me) {
        return agentInfoMapper.selectList(AgentInfo.gw().eq(AgentInfo::getParentAgentNo, me.getAgentNo())
                .orderByAsc(AgentInfo::getAgentPath));
    }

    /** 轄區內直屬商戶＋自己推薦的商戶；relation 標示關係。 */
    public List<JSONObject> merchants(AgentInfo me) {
        List<String> agentNos = jurisdiction(me).stream().map(AgentInfo::getAgentNo).collect(Collectors.toList());
        List<AgentMchRela> relas = agentMchRelaMapper.selectList(AgentMchRela.gw()
                .in(AgentMchRela::getAgentNo, agentNos)
                .or().eq(AgentMchRela::getReferrerAgentNo, me.getAgentNo()));
        if (relas.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, MchInfo> mchMap = mchInfoService.listByIds(relas.stream().map(AgentMchRela::getMchNo).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(MchInfo::getMchNo, Function.identity()));
        List<JSONObject> result = new ArrayList<>(relas.size());
        for (AgentMchRela rela : relas) {
            MchInfo mch = mchMap.get(rela.getMchNo());
            JSONObject row = new JSONObject(true);
            row.put("mchNo", rela.getMchNo());
            row.put("mchName", mch == null ? null : mch.getMchShortName());
            row.put("mchState", mch == null ? null : mch.getState());
            row.put("agentNo", rela.getAgentNo());
            row.put("relation", agentNos.contains(rela.getAgentNo())
                    ? (me.getAgentNo().equals(rela.getAgentNo()) ? "直屬" : "下級代理")
                    : "推薦");
            result.add(row);
        }
        return result;
    }

    /** 轄區內代理的費率（唯讀）：自己的高代費／代理費與下級代理的代理費。 */
    public List<FeeRule> rules(AgentInfo me) {
        List<String> agentNos = jurisdiction(me).stream().map(AgentInfo::getAgentNo).collect(Collectors.toList());
        return feeRuleService.list(FeeRule.gw().eq(FeeRule::getTargetType, FeeRule.TARGET_AGENT)
                .in(FeeRule::getTargetId, agentNos)
                .orderByAsc(FeeRule::getWayCode, FeeRule::getTargetId, FeeRule::getLayer));
    }

    /**
     * 高級代理設定下級代理的代理費（或自己轄下代理的推薦佣金）。
     * 防呆：設定後，受影響商戶以 1000 元試算的各層合計不得超過其支付通道手續費，否則整筆回滾。
     */
    @Transactional
    public FeeRule saveSubAgentRule(AgentInfo me, FeeRule input, Long uid, String name) {
        if (!Objects.equals(me.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
            throw new BizException("只有高級代理可以設定下級代理的費率");
        }
        AgentInfo target = agentInfoMapper.selectById(input.getTargetId());
        if (target == null || !me.getAgentNo().equals(target.getParentAgentNo())) {
            throw new BizException("只能設定自己的下級代理");
        }
        if (!FeeRule.LAYER_AGENT.equals(input.getLayer()) && !FeeRule.LAYER_REFERRER.equals(input.getLayer())) {
            throw new BizException("代理後台只能設定代理費或推薦佣金");
        }
        input.setTargetType(FeeRule.TARGET_AGENT);
        FeeRule saved = feeRuleService.saveRule(input, uid, name + "（代理 " + me.getAgentNo() + "）");
        List<String> affected = agentMchRelaMapper.selectList(AgentMchRela.gw()
                        .eq(FeeRule.LAYER_AGENT.equals(input.getLayer()), AgentMchRela::getAgentNo, target.getAgentNo())
                        .eq(FeeRule.LAYER_REFERRER.equals(input.getLayer()), AgentMchRela::getReferrerAgentNo, target.getAgentNo()))
                .stream().map(AgentMchRela::getMchNo).collect(Collectors.toList());
        List<JSONObject> violations = feeRuleService.riskCheck(input.getWayCode(), 100_000L, affected);
        if (!violations.isEmpty()) {
            throw new BizException("設定後有 " + violations.size() + " 個商戶的手續費合計會超過商戶費率（例如 "
                    + violations.get(0).getString("mchNo") + "），請調低費率");
        }
        return saved;
    }

    public Map<String, Object> profitSummary(String agentNo, Date start, Date end) {
        return payOrderFeeMapper.sumAgentProfit(agentNo, start, end);
    }

    public IPage<Map<String, Object>> profitPage(IPage<?> page, String agentNo, Date start, Date end) {
        return payOrderFeeMapper.pageAgentProfit(page, agentNo, start, end);
    }

    /** 由平台為代理開通登入帳號；密碼為系統預設密碼，首次登入後應自行修改。 */
    @Transactional
    public SysUser createAccount(String agentNo, SysUser input) {
        AgentInfo agent = agentInfoMapper.selectById(agentNo);
        if (agent == null) {
            throw new BizException("代理不存在");
        }
        SysUser user = new SysUser();
        user.setLoginUsername(input.getLoginUsername());
        user.setRealname(input.getRealname());
        user.setTelphone(input.getTelphone());
        user.setSex(input.getSex() == null ? CS.SEX_UNKNOWN : input.getSex());
        user.setUserNo(StringUtils.defaultIfBlank(input.getUserNo(), null));
        user.setIsAdmin(CS.NO);
        user.setState(CS.YES);
        user.setBelongInfoId(agentNo);
        sysUserService.addSysUser(user, CS.SYS_TYPE.MGR);
        sysUserService.saveUserRole(user.getSysUserId(), List.of(ROLE_AGENT_PORTAL));
        return user;
    }

    public List<SysUser> accounts(String agentNo) {
        return sysUserService.list(SysUser.gw()
                .select(SysUser::getSysUserId, SysUser::getLoginUsername, SysUser::getRealname, SysUser::getTelphone,
                        SysUser::getState, SysUser::getCreatedAt)
                .eq(SysUser::getSysType, CS.SYS_TYPE.MGR).eq(SysUser::getBelongInfoId, agentNo));
    }
}
