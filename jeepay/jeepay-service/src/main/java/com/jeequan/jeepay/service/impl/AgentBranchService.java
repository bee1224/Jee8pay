package com.jeequan.jeepay.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.AgentMchRela;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeTemplate;
import com.jeequan.jeepay.core.entity.FeeTemplateItem;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.RiskBlacklist;
import com.jeequan.jeepay.core.entity.SysLog;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.entity.WayRoute;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.AgentMchRelaMapper;
import com.jeequan.jeepay.service.mapper.WalletLedgerMapper;
import com.jeequan.jeepay.service.route.WayRouteService;
import com.jeequan.jeepay.service.wallet.RiskBlacklistService;
import com.jeequan.jeepay.service.wallet.WalletService;
import com.jeequan.jeepay.service.wallet.WithdrawService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 家族樹權限（爺爺＝平台、爸爸＝團長、兒子＝隊長）：代理對自己這一支後代的管理操作。
 *
 * 每個方法都先由登入者的代理路徑推導出範圍（{@link Scope}），再檢查操作對象是否在範圍內；
 * 前端傳入的代理號、商戶號只能在範圍內選擇，不能擴大範圍。平臺費與渠道費一律不在這裡開放。
 */
@Service
public class AgentBranchService {

    private static final int REPORT_MAX_DAYS = 93;
    private static final Set<String> AGENT_LAYERS = Set.of(FeeRule.LAYER_SR_AGENT, FeeRule.LAYER_AGENT);

    @Autowired private AgentPortalService agentPortalService;
    @Autowired private AgentInfoMapper agentInfoMapper;
    @Autowired private AgentMchRelaMapper agentMchRelaMapper;
    @Autowired private AgentMchRelaService agentMchRelaService;
    @Autowired private MchInfoService mchInfoService;
    @Autowired private WalletService walletService;
    @Autowired private WalletLedgerMapper walletLedgerMapper;
    @Autowired private WithdrawService withdrawService;
    @Autowired private PayOrderService payOrderService;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private FeeTemplateService feeTemplateService;
    @Autowired private WayRouteService wayRouteService;
    @Autowired private RiskBlacklistService riskBlacklistService;
    @Autowired private SysUserService sysUserService;
    @Autowired private SysUserAuthService sysUserAuthService;
    @Autowired private SysLogService sysLogService;

    /** 登入代理看得到的範圍：自己與後代代理，以及這些代理直屬的商戶。 */
    public static final class Scope {
        public final AgentInfo me;
        public final List<AgentInfo> agents;
        public final Set<String> agentNos;
        public final List<AgentMchRela> relas;
        public final Set<String> mchNos;

        Scope(AgentInfo me, List<AgentInfo> agents, List<AgentMchRela> relas) {
            this.me = me;
            this.agents = agents;
            this.agentNos = agents.stream().map(AgentInfo::getAgentNo).collect(Collectors.toCollection(LinkedHashSet::new));
            this.relas = relas;
            this.mchNos = relas.stream().map(AgentMchRela::getMchNo).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        /** 後代代理（不含自己）。 */
        public Set<String> descendantAgentNos() {
            Set<String> result = new LinkedHashSet<>(agentNos);
            result.remove(me.getAgentNo());
            return result;
        }

        boolean ownsWallet(String ownerType, String ownerId) {
            return (WalletAccount.OWNER_MCH.equals(ownerType) && mchNos.contains(ownerId))
                    || (WalletAccount.OWNER_AGENT.equals(ownerType) && descendantAgentNos().contains(ownerId));
        }
    }

    public Scope scope(AgentInfo me) {
        List<AgentInfo> agents = agentPortalService.jurisdiction(me);
        List<AgentMchRela> relas = agentMchRelaMapper.selectList(AgentMchRela.gw()
                .in(AgentMchRela::getAgentNo, agents.stream().map(AgentInfo::getAgentNo).collect(Collectors.toList())));
        return new Scope(me, agents, relas);
    }

    private static void requireSenior(AgentInfo me, String what) {
        if (!Objects.equals(me.getAgentLevel(), AgentInfo.LEVEL_SENIOR)) {
            throw new BizException("只有團長可以" + what);
        }
    }

    private static String operator(AgentInfo me, String name) {
        return name + "（代理 " + me.getAgentNo() + "）";
    }

    // ---------------- 旗下錢包與流水 ----------------

    /** 後代代理與轄下商戶的錢包餘額；尚未建立錢包的以 0 顯示。 */
    public List<JSONObject> wallets(AgentInfo me) {
        Scope scope = scope(me);
        List<JSONObject> result = new ArrayList<>();
        for (AgentInfo agent : scope.agents) {
            if (!agent.getAgentNo().equals(me.getAgentNo())) {
                result.add(walletRow(WalletAccount.OWNER_AGENT, agent.getAgentNo(), agent.getAgentName()));
            }
        }
        if (!scope.mchNos.isEmpty()) {
            Map<String, MchInfo> mchMap = mchInfoService.listByIds(scope.mchNos).stream()
                    .collect(Collectors.toMap(MchInfo::getMchNo, m -> m));
            for (String mchNo : scope.mchNos) {
                MchInfo mch = mchMap.get(mchNo);
                result.add(walletRow(WalletAccount.OWNER_MCH, mchNo, mch == null ? mchNo : mch.getMchShortName()));
            }
        }
        return result;
    }

    private JSONObject walletRow(String ownerType, String ownerId, String name) {
        WalletAccount account = walletService.find(ownerType, ownerId);
        JSONObject row = new JSONObject(true);
        row.put("ownerType", ownerType);
        row.put("ownerId", ownerId);
        row.put("ownerName", name);
        row.put("balance", account == null ? 0L : account.getBalance());
        row.put("frozen", account == null ? 0L : account.getFrozen());
        row.put("manualFrozen", account == null ? 0L : manualFrozen(ownerType, ownerId));
        return row;
    }

    public IPage<WalletLedger> ledger(AgentInfo me, IPage<WalletLedger> page, String ownerType, String ownerId, Date start, Date end) {
        Scope scope = scope(me);
        Set<String> agentNos = scope.descendantAgentNos();
        LambdaQueryWrapper<WalletLedger> wrapper = WalletLedger.gw();
        if (StringUtils.isNotBlank(ownerId)) {
            if (!scope.ownsWallet(ownerType, ownerId)) {
                throw new BizException("只能查看自己旗下的錢包");
            }
            wrapper.eq(WalletLedger::getOwnerType, ownerType).eq(WalletLedger::getOwnerId, ownerId);
        } else if (scope.mchNos.isEmpty() && agentNos.isEmpty()) {
            return page;
        } else {
            wrapper.and(w -> {
                if (!scope.mchNos.isEmpty()) {
                    w.or(x -> x.eq(WalletLedger::getOwnerType, WalletAccount.OWNER_MCH).in(WalletLedger::getOwnerId, scope.mchNos));
                }
                if (!agentNos.isEmpty()) {
                    w.or(x -> x.eq(WalletLedger::getOwnerType, WalletAccount.OWNER_AGENT).in(WalletLedger::getOwnerId, agentNos));
                }
            });
        }
        wrapper.ge(start != null, WalletLedger::getCreatedAt, start).lt(end != null, WalletLedger::getCreatedAt, end)
                .orderByDesc(WalletLedger::getLedgerId);
        return walletLedgerMapper.selectPage(page, wrapper);
    }

    // ---------------- 凍結／解凍 ----------------

    /** 人工凍結尚未解凍的金額（與提現凍結分開計算）。 */
    public long manualFrozen(String ownerType, String ownerId) {
        return walletLedgerMapper.selectList(WalletLedger.gw().select(WalletLedger::getFrozenChange)
                        .eq(WalletLedger::getOwnerType, ownerType).eq(WalletLedger::getOwnerId, ownerId)
                        .in(WalletLedger::getBizType, WalletLedger.BIZ_FREEZE, WalletLedger.BIZ_UNFREEZE))
                .stream().mapToLong(l -> l.getFrozenChange() == null ? 0L : l.getFrozenChange()).sum();
    }

    /** 凍結：可用餘額移到凍結，不改變總額；不可超過可用餘額。 */
    @Transactional
    public void freeze(AgentInfo me, String ownerType, String ownerId, long amount, String remark, Long uid, String name) {
        checkFreezeTarget(me, ownerType, ownerId, amount, remark);
        walletService.post(ownerType, ownerId, WalletLedger.BIZ_FREEZE, freezeBizId("F"), -amount, amount, false,
                "代理凍結：" + remark.trim(), uid, operator(me, name));
    }

    /** 解凍：只能解開人工凍結的部分，不會動到提現申請凍結的金額。 */
    @Transactional
    public void unfreeze(AgentInfo me, String ownerType, String ownerId, long amount, String remark, Long uid, String name) {
        checkFreezeTarget(me, ownerType, ownerId, amount, remark);
        if (amount > manualFrozen(ownerType, ownerId)) {
            throw new BizException("解凍金額超過人工凍結中的金額");
        }
        walletService.post(ownerType, ownerId, WalletLedger.BIZ_UNFREEZE, freezeBizId("U"), amount, -amount, false,
                "代理解凍：" + remark.trim(), uid, operator(me, name));
    }

    private void checkFreezeTarget(AgentInfo me, String ownerType, String ownerId, long amount, String remark) {
        requireSenior(me, "凍結或解凍旗下資金");
        if (!scope(me).ownsWallet(ownerType, ownerId)) {
            throw new BizException("只能操作自己旗下的錢包");
        }
        if (amount <= 0) {
            throw new BizException("金額必須大於 0");
        }
        if (StringUtils.isBlank(remark)) {
            throw new BizException("請填寫原因");
        }
    }

    private static String freezeBizId(String prefix) {
        return prefix + DateUtil.format(new Date(), "yyMMddHHmmss") + RandomStringUtils.randomNumeric(4);
    }

    // ---------------- 商戶歸屬與帳號 ----------------

    /** 團長更換轄下商戶的直屬代理；新的直屬代理必須在自己這一支裡。 */
    @Transactional
    public void rebind(AgentInfo me, String mchNo, String agentNo, Long uid, String name) {
        requireSenior(me, "更換商戶的直屬代理");
        Scope scope = scope(me);
        if (!scope.mchNos.contains(mchNo)) {
            throw new BizException("只能調整自己旗下的商戶");
        }
        if (!scope.agentNos.contains(agentNo)) {
            throw new BizException("直屬代理必須是自己或自己的旗下代理");
        }
        agentMchRelaService.bind(mchNo, agentNo, uid, operator(me, name));
    }

    /** 重設轄下商戶登入帳號的密碼為新的隨機 8 碼，只在回應出現一次。 */
    @Transactional
    public JSONObject resetMerchantPassword(AgentInfo me, String mchNo) {
        if (!scope(me).mchNos.contains(mchNo)) {
            throw new BizException("只能操作自己旗下的商戶");
        }
        MchInfo mch = mchInfoService.getById(mchNo);
        SysUser user = mch == null || mch.getInitUserId() == null ? null : sysUserService.getById(mch.getInitUserId());
        if (user == null) {
            throw new BizException("找不到商戶的登入帳號");
        }
        String password = AgentPortalService.randomPassword();
        sysUserAuthService.resetAuthInfo(user.getSysUserId(), null, null, password, CS.SYS_TYPE.MCH);
        JSONObject result = new JSONObject();
        result.put("agentName", mch.getMchName());
        result.put("loginUsername", user.getLoginUsername());
        result.put("initPassword", password);
        return result;
    }

    // ---------------- 統計報表 ----------------

    /** 轄下商戶的代收統計：依商戶、依代理、依日期。起迄未給時為近 7 天，最長 93 天。 */
    public JSONObject report(AgentInfo me, Date start, Date end) {
        Date to = end == null ? DateUtil.offsetDay(DateUtil.beginOfDay(new Date()), 1) : end;
        Date from = start == null ? DateUtil.offsetDay(to, -7) : start;
        if (!from.before(to) || DateUtil.betweenDay(from, to, false) > REPORT_MAX_DAYS) {
            throw new BizException("日期區間不正確，最長可查 " + REPORT_MAX_DAYS + " 天");
        }
        Scope scope = scope(me);
        Map<String, JSONObject> byMch = new LinkedHashMap<>();
        Map<String, JSONObject> byAgent = new LinkedHashMap<>();
        Map<String, JSONObject> byDay = new TreeMap<>();
        JSONObject total = bucket();
        if (!scope.mchNos.isEmpty()) {
            Map<String, String> agentOfMch = scope.relas.stream().collect(Collectors.toMap(AgentMchRela::getMchNo, AgentMchRela::getAgentNo));
            Map<String, String> agentNames = scope.agents.stream().collect(Collectors.toMap(AgentInfo::getAgentNo, AgentInfo::getAgentName));
            Map<String, String> mchNames = mchInfoService.listByIds(scope.mchNos).stream()
                    .collect(Collectors.toMap(MchInfo::getMchNo, MchInfo::getMchShortName));
            List<Map<String, Object>> rows = payOrderService.listMaps(new QueryWrapper<PayOrder>()
                    .select("mch_no AS mchNo", "DATE_FORMAT(created_at, '%Y-%m-%d') AS day", "COUNT(*) AS total",
                            "SUM(CASE WHEN state = 2 THEN 1 ELSE 0 END) AS paid",
                            "SUM(CASE WHEN state = 2 THEN amount ELSE 0 END) AS paidAmount",
                            "SUM(CASE WHEN state = 2 THEN mch_fee_amount ELSE 0 END) AS feeAmount")
                    .in("mch_no", scope.mchNos).ge("created_at", from).lt("created_at", to)
                    .groupBy("mch_no", "DATE_FORMAT(created_at, '%Y-%m-%d')"));
            for (Map<String, Object> r : rows) {
                String mchNo = String.valueOf(r.get("mchNo"));
                String agentNo = agentOfMch.get(mchNo);
                add(byMch.computeIfAbsent(mchNo, k -> named(k, mchNames.get(k))), r);
                add(byAgent.computeIfAbsent(agentNo, k -> named(k, agentNames.get(k))), r);
                add(byDay.computeIfAbsent(String.valueOf(r.get("day")), k -> named(k, null)), r);
                add(total, r);
            }
        }
        JSONObject result = new JSONObject(true);
        result.put("startDate", DateUtil.formatDate(from));
        result.put("endDate", DateUtil.formatDate(DateUtil.offsetDay(to, -1)));
        result.put("total", total);
        result.put("byAgent", byAgent.values());
        result.put("byMch", byMch.values());
        result.put("byDay", byDay.values());
        return result;
    }

    private static JSONObject bucket() {
        JSONObject b = new JSONObject(true);
        b.put("total", 0L);
        b.put("paid", 0L);
        b.put("paidAmount", 0L);
        b.put("feeAmount", 0L);
        return b;
    }

    private static JSONObject named(String id, String name) {
        JSONObject b = new JSONObject(true);
        b.put("id", id);
        b.put("name", name);
        b.putAll(bucket());
        return b;
    }

    private static void add(JSONObject bucket, Map<String, Object> row) {
        for (String key : new String[]{"total", "paid", "paidAmount", "feeAmount"}) {
            Object v = row.get(key);
            bucket.put(key, bucket.getLongValue(key) + (v == null ? 0L : ((Number) v).longValue()));
        }
    }

    // ---------------- 黑名單（以團長為範圍，旗下共用） ----------------

    public List<RiskBlacklist> blacklist(AgentInfo me) {
        requireSenior(me, "管理黑名單");
        return riskBlacklistService.list(RiskBlacklist.gw().eq(RiskBlacklist::getScope, me.getAgentNo())
                .orderByDesc(RiskBlacklist::getId));
    }

    public RiskBlacklist addBlacklist(AgentInfo me, RiskBlacklist input, Long uid, String name) {
        requireSenior(me, "管理黑名單");
        input.setScope(me.getAgentNo());
        return riskBlacklistService.add(input, uid, operator(me, name));
    }

    public void removeBlacklist(AgentInfo me, Long id) {
        requireSenior(me, "管理黑名單");
        RiskBlacklist row = riskBlacklistService.getById(id);
        if (row == null || !me.getAgentNo().equals(row.getScope())) {
            throw new BizException("黑名單不存在");
        }
        riskBlacklistService.removeById(id);
    }

    // ---------------- 費率（團長費、隊長費；不含平臺費與渠道費） ----------------

    /** 自己這一支的代理費率，加上對轄下商戶的覆寫。 */
    public List<FeeRule> feeRules(AgentInfo me) {
        Scope scope = scope(me);
        return feeRuleService.list(FeeRule.gw().in(FeeRule::getLayer, AGENT_LAYERS).and(w -> {
            w.or(x -> x.eq(FeeRule::getTargetType, FeeRule.TARGET_AGENT).in(FeeRule::getTargetId, scope.agentNos));
            if (!scope.mchNos.isEmpty()) {
                w.or(x -> x.eq(FeeRule::getTargetType, FeeRule.TARGET_MCH).in(FeeRule::getTargetId, scope.mchNos));
            }
        }).orderByAsc(FeeRule::getWayCode, FeeRule::getTargetType, FeeRule::getTargetId, FeeRule::getLayer));
    }

    /**
     * 團長設定：自己的團長費、旗下代理的隊長費，或對轄下商戶逐一覆寫這兩層。
     * 儲存後以 1000 元試算轄下所有商戶；任何商戶各層合計超過其商戶費率就整筆回滾。
     */
    @Transactional
    public FeeRule saveFeeRule(AgentInfo me, FeeRule input, Long uid, String name) {
        requireSenior(me, "設定費率");
        Scope scope = scope(me);
        String layer = input.getLayer();
        if (!AGENT_LAYERS.contains(layer)) {
            throw new BizException("代理只能設定團長費與隊長費");
        }
        String targetId = StringUtils.trimToEmpty(input.getTargetId());
        if (FeeRule.TARGET_AGENT.equals(input.getTargetType())) {
            if (!scope.agentNos.contains(targetId)) {
                throw new BizException("只能設定自己或自己的旗下代理");
            }
        } else if (FeeRule.TARGET_MCH.equals(input.getTargetType())) {
            if (!scope.mchNos.contains(targetId)) {
                throw new BizException("只能覆寫自己旗下商戶的費率");
            }
        } else {
            throw new BizException("設定對象只能是代理或商戶");
        }
        FeeRule saved = feeRuleService.saveRule(input, uid, operator(me, name));
        rejectIfOverMerchantRate(input.getWayCode(), scope.mchNos);
        return saved;
    }

    private void rejectIfOverMerchantRate(String wayCode, Set<String> mchNos) {
        List<JSONObject> violations = feeRuleService.riskCheck(wayCode, 100_000L, mchNos);
        if (!violations.isEmpty()) {
            throw new BizException("設定後有 " + violations.size() + " 個商戶的手續費合計會超過商戶費率（例如 "
                    + violations.get(0).getString("mchNo") + "），請調低費率");
        }
    }

    public List<JSONObject> feeTemplates() {
        List<JSONObject> result = new ArrayList<>();
        for (FeeTemplate template : feeTemplateService.list()) {
            JSONObject row = (JSONObject) JSONObject.toJSON(template);
            row.put("items", feeTemplateService.items(template.getTemplateId()));
            result.add(row);
        }
        return result;
    }

    /** 把平台建立的費率範本套用到自己旗下的代理或商戶；套用後同樣做超收檢查。 */
    @Transactional
    public JSONObject applyTemplate(AgentInfo me, Long templateId, String targetType, List<String> targetIds, Long uid, String name) {
        requireSenior(me, "套用費率範本");
        Scope scope = scope(me);
        Set<String> allowed = FeeRule.TARGET_AGENT.equals(targetType) ? scope.agentNos
                : FeeRule.TARGET_MCH.equals(targetType) ? scope.mchNos : Set.of();
        if (targetIds == null || targetIds.isEmpty() || !allowed.containsAll(targetIds)) {
            throw new BizException("套用對象必須是自己旗下的代理或商戶");
        }
        JSONObject result = feeTemplateService.apply(templateId, targetType, targetIds, uid, operator(me, name));
        for (String wayCode : feeTemplateService.items(templateId).stream().map(FeeTemplateItem::getWayCode).collect(Collectors.toSet())) {
            rejectIfOverMerchantRate(wayCode, scope.mchNos);
        }
        return result;
    }

    // ---------------- 通道路由（只能設定指定給轄下商戶的規則） ----------------

    public List<WayRoute> routes(AgentInfo me) {
        Scope scope = scope(me);
        return scope.mchNos.isEmpty() ? new ArrayList<>() : wayRouteService.list(WayRoute.gw()
                .in(WayRoute::getMchNo, scope.mchNos).orderByAsc(WayRoute::getAliasWayCode, WayRoute::getMchNo, WayRoute::getMinAmount));
    }

    public WayRoute saveRoute(AgentInfo me, WayRoute input, String name) {
        requireSenior(me, "設定通道路由");
        Scope scope = scope(me);
        if (StringUtils.isBlank(input.getMchNo()) || !scope.mchNos.contains(input.getMchNo().trim())) {
            throw new BizException("路由規則必須指定自己旗下的商戶");
        }
        if (input.getRouteId() != null) {
            requireOwnRoute(scope, input.getRouteId());
        }
        return wayRouteService.saveRoute(input, operator(me, name));
    }

    public void removeRoute(AgentInfo me, Long routeId) {
        requireSenior(me, "設定通道路由");
        requireOwnRoute(scope(me), routeId);
        wayRouteService.removeById(routeId);
    }

    private void requireOwnRoute(Scope scope, Long routeId) {
        WayRoute existing = wayRouteService.getById(routeId);
        if (existing == null || !scope.mchNos.contains(existing.getMchNo())) {
            throw new BizException("路由規則不存在");
        }
    }

    // ---------------- 提現審核（代理可同意或駁回；撥款仍由平台執行） ----------------

    public IPage<WithdrawOrder> withdraws(AgentInfo me, IPage<WithdrawOrder> page, Byte state) {
        Scope scope = scope(me);
        Set<String> agentNos = scope.descendantAgentNos();
        if (scope.mchNos.isEmpty() && agentNos.isEmpty()) {
            return page;
        }
        return withdrawService.page(page, WithdrawOrder.gw().eq(state != null, WithdrawOrder::getState, state).and(w -> {
            if (!scope.mchNos.isEmpty()) {
                w.or(x -> x.eq(WithdrawOrder::getOwnerType, WalletAccount.OWNER_MCH).in(WithdrawOrder::getOwnerId, scope.mchNos));
            }
            if (!agentNos.isEmpty()) {
                w.or(x -> x.eq(WithdrawOrder::getOwnerType, WalletAccount.OWNER_AGENT).in(WithdrawOrder::getOwnerId, agentNos));
            }
        }).orderByDesc(WithdrawOrder::getCreatedAt));
    }

    private WithdrawOrder requireBranchWithdraw(AgentInfo me, String withdrawId) {
        requireSenior(me, "審核旗下提現");
        WithdrawOrder order = withdrawService.getById(withdrawId);
        if (order == null || !scope(me).ownsWallet(order.getOwnerType(), order.getOwnerId())) {
            throw new BizException("提現單不存在");
        }
        return order;
    }

    /** 代理同意：只在提現單上註記，讓平台審核時看得到；資金仍凍結，由平台撥款。 */
    public void approveWithdraw(AgentInfo me, String withdrawId, String name) {
        requireBranchWithdraw(me, withdrawId);
        boolean updated = withdrawService.update(new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getWithdrawId, withdrawId)
                .eq(WithdrawOrder::getState, WithdrawOrder.STATE_PENDING)
                .set(WithdrawOrder::getAgentApproveBy, StringUtils.abbreviate(operator(me, name), 64))
                .set(WithdrawOrder::getAgentApproveAt, new Date()));
        if (!updated) {
            throw new BizException("此提現單已處理");
        }
    }

    /** 代理駁回：提現單結案，凍結金額退回申請人的可用餘額。 */
    @Transactional
    public void rejectWithdraw(AgentInfo me, String withdrawId, String remark, Long uid, String name) {
        requireBranchWithdraw(me, withdrawId);
        if (StringUtils.isBlank(remark)) {
            throw new BizException("請填寫駁回原因");
        }
        withdrawService.reject(withdrawId, uid, StringUtils.abbreviate(operator(me, name), 64), remark.trim());
    }

    // ---------------- 白標（團長設定，自己與旗下代理登入後套用） ----------------

    public void saveBrand(AgentInfo me, Byte enabled, String title, String logo) {
        requireSenior(me, "設定品牌");
        String t = StringUtils.trimToNull(title);
        String l = StringUtils.trimToNull(logo);
        if (t != null && t.length() > 32) {
            throw new BizException("站台名稱最多 32 個字");
        }
        if (l != null && (l.length() > 255 || !(l.startsWith("/") || l.startsWith("http://") || l.startsWith("https://")))) {
            throw new BizException("Logo 位址不正確");
        }
        agentInfoMapper.update(null, new LambdaUpdateWrapper<AgentInfo>().eq(AgentInfo::getAgentNo, me.getAgentNo())
                .set(AgentInfo::getBrandEnabled, Objects.equals(enabled, CS.YES) ? CS.YES : CS.NO)
                .set(AgentInfo::getBrandTitle, t).set(AgentInfo::getBrandLogo, l));
    }

    /** 登入者適用的品牌：取自己這一支的團長；未啟用時回傳 null。 */
    public JSONObject effectiveBrand(AgentInfo me) {
        AgentInfo senior = Objects.equals(me.getAgentLevel(), AgentInfo.LEVEL_SENIOR) ? me
                : (me.getParentAgentNo() == null ? null : agentInfoMapper.selectById(me.getParentAgentNo()));
        if (senior == null || !Objects.equals(senior.getBrandEnabled(), CS.YES)) {
            return null;
        }
        JSONObject brand = new JSONObject();
        brand.put("title", senior.getBrandTitle());
        brand.put("logo", senior.getBrandLogo());
        return brand;
    }

    // ---------------- 登入紀錄 ----------------

    /** 自己最近 5 次登入的時間與 IP。 */
    public List<SysLog> recentLogins(Long userId) {
        return sysLogService.list(SysLog.gw().select(SysLog::getSysLogId, SysLog::getUserIp, SysLog::getCreatedAt)
                .eq(SysLog::getSysType, CS.SYS_TYPE.MGR).eq(SysLog::getUserId, userId).eq(SysLog::getMethodRemark, "登入認證")
                .orderByDesc(SysLog::getSysLogId).last("LIMIT 5"));
    }
}
