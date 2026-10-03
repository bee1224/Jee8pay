package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.SysLog;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.AgentMchRelaMapper;
import com.jeequan.jeepay.service.mapper.PayOrderFeeMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
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
    /** 一般代理（第三代）帳號固定角色：沒有旗下代理與操作紀錄選單 */
    public static final String ROLE_AGENT_PORTAL_L2 = "ROLE_AGENT_PORTAL_L2";

    public static String roleOf(AgentInfo agent) {
        return Objects.equals(agent.getAgentLevel(), AgentInfo.LEVEL_SENIOR) ? ROLE_AGENT_PORTAL : ROLE_AGENT_PORTAL_L2;
    }

    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private AgentMchRelaMapper agentMchRelaMapper;
    @Autowired private PayOrderFeeMapper payOrderFeeMapper;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private MchInfoService mchInfoService;
    @Autowired private SysUserService sysUserService;
    @Autowired private SysUserAuthService sysUserAuthService;
    @Autowired private AgentInfoService agentInfoService;
    @Autowired private AgentMchRelaService agentMchRelaService;
    @Autowired private SysLogService sysLogService;

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
                    ? (me.getAgentNo().equals(rela.getAgentNo()) ? "直屬" : "旗下代理")
                    : "推薦");
            result.add(row);
        }
        return result;
    }

    /** 轄區內代理的費率（唯讀）：自己的高代費／代理費與旗下代理的代理費。 */
    public List<FeeRule> rules(AgentInfo me) {
        List<String> agentNos = jurisdiction(me).stream().map(AgentInfo::getAgentNo).collect(Collectors.toList());
        return feeRuleService.list(FeeRule.gw().eq(FeeRule::getTargetType, FeeRule.TARGET_AGENT)
                .in(FeeRule::getTargetId, agentNos)
                .orderByAsc(FeeRule::getWayCode, FeeRule::getTargetId, FeeRule::getLayer));
    }

    /**
     * 高級代理設定旗下代理的代理費（或自己轄下代理的推薦佣金）。
     * 防呆：設定後，受影響商戶以 1000 元試算的各層合計不得超過其支付通道手續費，否則整筆回滾。
     */
    @Transactional
    public FeeRule saveSubAgentRule(AgentInfo me, FeeRule input, Long uid, String name) {
        if (!Objects.equals(me.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
            throw new BizException("只有高級代理可以設定旗下代理的費率");
        }
        AgentInfo target = agentInfoMapper.selectById(input.getTargetId());
        if (target == null || !me.getAgentNo().equals(target.getParentAgentNo())) {
            throw new BizException("只能設定自己的旗下代理");
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

    private static final String PWD_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PWD_DIGITS = "23456789";
    private static final String PWD_SYMBOLS = "!@#$%&*?";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 產生 8 碼隨機初始密碼：至少各含一個英文字母、數字與特殊符號，排除易混淆字元（0/O、1/I）。
     * 登入密碼不分大小寫（UpperCasePasswordEncoder），因此字母一律用大寫。
     */
    public static String randomPassword() {
        String all = PWD_LETTERS + PWD_DIGITS + PWD_SYMBOLS;
        List<Character> chars = new ArrayList<>();
        chars.add(PWD_LETTERS.charAt(RANDOM.nextInt(PWD_LETTERS.length())));
        chars.add(PWD_DIGITS.charAt(RANDOM.nextInt(PWD_DIGITS.length())));
        chars.add(PWD_SYMBOLS.charAt(RANDOM.nextInt(PWD_SYMBOLS.length())));
        while (chars.size() < 8) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }

    /**
     * 開通代理登入帳號，密碼為隨機 8 碼。回傳 sysUserId、loginUsername 與 initPassword；
     * initPassword 只在這次回應出現，系統不保存明文。
     */
    @Transactional
    public JSONObject createAccount(String agentNo, SysUser input) {
        AgentInfo agent = agentInfoMapper.selectById(agentNo);
        if (agent == null) {
            throw new BizException("代理不存在");
        }
        if (StringUtils.isBlank(input.getLoginUsername()) || !input.getLoginUsername().matches("^[A-Za-z0-9_]{4,32}$")) {
            throw new BizException("登入帳號須為 4～32 碼英文、數字或底線");
        }
        if (StringUtils.isBlank(input.getTelphone()) || !input.getTelphone().matches("^09\\d{8}$")) {
            throw new BizException("請填寫正確的手機號（09 開頭 10 碼）");
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
        sysUserService.saveUserRole(user.getSysUserId(), List.of(roleOf(agent)));
        // addSysUser 會先寫入系統預設密碼，這裡立即換成隨機密碼
        String password = randomPassword();
        sysUserAuthService.resetAuthInfo(user.getSysUserId(), null, null, password, CS.SYS_TYPE.MGR);
        JSONObject result = new JSONObject();
        result.put("sysUserId", user.getSysUserId());
        result.put("loginUsername", user.getLoginUsername());
        result.put("initPassword", password);
        return result;
    }

    /** 新增代理並同時開通第一個登入帳號（同一交易；帳號建立失敗時代理也不會留下）。 */
    @Transactional
    public JSONObject createAgentWithAccount(AgentInfo agent, String loginUsername, Long operatorUid, String operatorName) {
        agentInfoService.create(agent, operatorUid, operatorName);
        SysUser input = new SysUser();
        input.setLoginUsername(loginUsername);
        input.setRealname(StringUtils.defaultIfBlank(agent.getContactName(), agent.getAgentName()));
        input.setTelphone(agent.getContactTel());
        JSONObject result = createAccount(agent.getAgentNo(), input);
        result.put("agentNo", agent.getAgentNo());
        result.put("agentName", agent.getAgentName());
        return result;
    }

    // ---------------- 家族樹權限：爺爺（平台）→ 爸爸（高級代理）→ 兒子（一般代理）→ 商戶 ----------------
    // 代理只能看到、操作自己這一支的後代；範圍一律由登入者的代理路徑推導，不接受前端指定。

    /** 轄區內代理（含自己）直屬的商戶號；不含只有推薦關係的商戶。 */
    public List<String> directMchNos(AgentInfo me) {
        List<String> agentNos = jurisdiction(me).stream().map(AgentInfo::getAgentNo).collect(Collectors.toList());
        return agentMchRelaMapper.selectList(AgentMchRela.gw().in(AgentMchRela::getAgentNo, agentNos))
                .stream().map(AgentMchRela::getMchNo).collect(Collectors.toList());
    }

    /** 高級代理新增自己的下級（一般）代理並開通登入帳號；層級與上級由後端決定。 */
    @Transactional
    public JSONObject createSubAgent(AgentInfo me, AgentInfo input, String loginUsername, Long uid, String name) {
        if (!Objects.equals(me.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
            throw new BizException("只有高級代理可以新增旗下代理");
        }
        AgentInfo agent = new AgentInfo();
        agent.setAgentName(input.getAgentName());
        agent.setContactName(input.getContactName());
        agent.setContactTel(input.getContactTel());
        agent.setContactEmail(input.getContactEmail());
        agent.setRemark(input.getRemark());
        agent.setAgentLevel(AgentInfo.LEVEL_AGENT);
        agent.setParentAgentNo(me.getAgentNo());
        agent.setState(CS.YES);
        return createAgentWithAccount(agent, loginUsername, uid, name + "（代理 " + me.getAgentNo() + "）");
    }

    /**
     * 代理新增商戶：商戶直屬於自己，或（高級代理）指定給自己的旗下代理。
     * 商戶登入帳號的密碼同樣改為隨機 8 碼，只在回應出現一次。支付通道與費率仍由平台設定。
     */
    @Transactional
    public JSONObject createMerchant(AgentInfo me, MchInfo input, String loginUsername, String targetAgentNo, Long uid, String name) {
        String ownerNo = StringUtils.defaultIfBlank(targetAgentNo, me.getAgentNo());
        if (jurisdiction(me).stream().noneMatch(a -> a.getAgentNo().equals(ownerNo))) {
            throw new BizException("只能把商戶建在自己或自己的旗下代理底下");
        }
        if (StringUtils.isAnyBlank(input.getMchName(), input.getMchShortName(), input.getContactName())) {
            throw new BizException("請填寫商戶名稱、商戶簡稱與聯絡人姓名");
        }
        if (StringUtils.isBlank(loginUsername) || !loginUsername.matches("^[A-Za-z0-9_]{4,32}$")) {
            throw new BizException("登入帳號須為 4～32 碼英文、數字或底線");
        }
        if (StringUtils.isBlank(input.getContactTel()) || !input.getContactTel().matches("^09\\d{8}$")) {
            throw new BizException("請填寫正確的手機號（09 開頭 10 碼）");
        }
        MchInfo mch = new MchInfo();
        mch.setMchNo("M" + System.currentTimeMillis() / 1000);
        mch.setMchName(input.getMchName());
        mch.setMchShortName(input.getMchShortName());
        mch.setContactName(input.getContactName());
        mch.setContactTel(input.getContactTel());
        mch.setContactEmail(input.getContactEmail());
        mch.setRemark(input.getRemark());
        mch.setType(CS.MCH_TYPE_NORMAL);
        mch.setState(CS.YES);
        mch.setCreatedUid(uid);
        mch.setCreatedBy(name + "（代理 " + me.getAgentNo() + "）");
        mchInfoService.addMch(mch, loginUsername);
        agentMchRelaService.bind(mch.getMchNo(), ownerNo, null, uid, name);
        String password = randomPassword();
        sysUserAuthService.resetAuthInfo(mchInfoService.getById(mch.getMchNo()).getInitUserId(), null, null, password, CS.SYS_TYPE.MCH);
        JSONObject result = new JSONObject();
        result.put("mchNo", mch.getMchNo());
        result.put("agentName", mch.getMchName());
        result.put("loginUsername", loginUsername);
        result.put("initPassword", password);
        return result;
    }

    /** 後代代理帳號的操作紀錄（不含自己）；只回傳時間、操作人、動作與 IP，不含請求與回應內容。 */
    public IPage<SysLog> descendantLogs(AgentInfo me, IPage<SysLog> page) {
        List<String> agentNos = jurisdiction(me).stream().map(AgentInfo::getAgentNo)
                .filter(no -> !no.equals(me.getAgentNo())).collect(Collectors.toList());
        List<Long> userIds = agentNos.isEmpty() ? Collections.emptyList() : sysUserService.list(SysUser.gw()
                        .select(SysUser::getSysUserId)
                        .eq(SysUser::getSysType, CS.SYS_TYPE.MGR).in(SysUser::getBelongInfoId, agentNos))
                .stream().map(SysUser::getSysUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return page;
        }
        return sysLogService.page(page, SysLog.gw()
                .select(SysLog::getSysLogId, SysLog::getUserId, SysLog::getUserName, SysLog::getUserIp,
                        SysLog::getMethodRemark, SysLog::getCreatedAt)
                .eq(SysLog::getSysType, CS.SYS_TYPE.MGR).in(SysLog::getUserId, userIds)
                .orderByDesc(SysLog::getSysLogId));
    }

    public List<SysUser> accounts(String agentNo) {
        return sysUserService.list(SysUser.gw()
                .select(SysUser::getSysUserId, SysUser::getLoginUsername, SysUser::getRealname, SysUser::getTelphone,
                        SysUser::getState, SysUser::getCreatedAt)
                .eq(SysUser::getSysType, CS.SYS_TYPE.MGR).eq(SysUser::getBelongInfoId, agentNo));
    }
}
