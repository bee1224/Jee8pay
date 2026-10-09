package com.jeequan.jeepay.service.wallet;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.core.entity.RiskBlacklist;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.entity.WithdrawOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.impl.FeeRuleService;
import com.jeequan.jeepay.service.mapper.AgentInfoMapper;
import com.jeequan.jeepay.service.mapper.WalletAccountMapper;
import com.jeequan.jeepay.service.mapper.WithdrawOrderMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 提現（ADR-0010）。目前沒有代付通道，所有提現都由平台人工匯款：
 * 申請 → 凍結 →（平台匯款後）標記已撥款 → 出帳；或駁回／申請人取消 → 解凍。
 * 風控：黑名單命中直接拒絕（留下已駁回紀錄）；單日次數、限制銀行、剛變更的收款帳戶只標記提示，交由審核人判斷。
 */
@Service
public class WithdrawService extends ServiceImpl<WithdrawOrderMapper, WithdrawOrder> {

    @Autowired private WalletService walletService;
    @Autowired private WalletAccountMapper walletAccountMapper;
    @Autowired private WalletConfig walletConfig;
    @Autowired private RiskBlacklistService riskBlacklistService;
    @Autowired private FeeRuleService feeRuleService;
    @Autowired private AgentInfoMapper agentInfoMapper;

    /** 設定收款帳戶；變更時間會記錄下來，24 小時內的提現標記 NEW_ACCOUNT。 */
    @Transactional
    public WalletAccount updatePayoutAccount(String ownerType, String ownerId, WalletAccount input) {
        String bankCode = StringUtils.trimToEmpty(input.getPayoutBankCode());
        String accountNo = normalizeAccountNo(input.getPayoutAccountNo());
        if (!bankCode.matches("\\d{3}")) {
            throw new BizException("銀行代碼須為 3 位數字");
        }
        if (!accountNo.matches("\\d{6,16}")) {
            throw new BizException("帳號須為 6～16 位數字");
        }
        if (StringUtils.isAnyBlank(input.getPayoutBankName(), input.getPayoutAccountName())) {
            throw new BizException("請填寫銀行名稱與戶名");
        }
        WalletAccount account = walletService.getOrCreate(ownerType, ownerId);
        walletService.update(new LambdaUpdateWrapper<WalletAccount>().eq(WalletAccount::getAccountId, account.getAccountId())
                .set(WalletAccount::getPayoutBankName, input.getPayoutBankName().trim())
                .set(WalletAccount::getPayoutBankCode, bankCode)
                .set(WalletAccount::getPayoutBranch, StringUtils.trimToNull(input.getPayoutBranch()))
                .set(WalletAccount::getPayoutAccountNo, accountNo)
                .set(WalletAccount::getPayoutAccountName, input.getPayoutAccountName().trim())
                .set(WalletAccount::getPayoutUpdatedAt, new Date()));
        return walletService.getById(account.getAccountId());
    }

    /**
     * 申請提現。reqNo 為申請端冪等鍵：同一擁有者重送同一 reqNo 會回傳原單，不會重複凍結。
     */
    @Transactional
    public WithdrawOrder apply(String ownerType, String ownerId, long amount, String reqNo, Long uid, String name) {
        if (StringUtils.isBlank(reqNo) || reqNo.length() > 64) {
            throw new BizException("缺少申請編號");
        }
        long min = walletConfig.withdrawMinFen();
        long max = walletConfig.withdrawMaxFen();
        long fee = walletConfig.withdrawFeeFen();
        if (amount < min || amount > max) {
            throw new BizException(String.format("單筆提現金額須介於 %s 與 %s 元之間", fen(min), fen(max)));
        }
        if (amount <= fee) {
            throw new BizException("提現金額須大於手續費 " + fen(fee) + " 元");
        }
        WalletAccount account = walletService.getOrCreate(ownerType, ownerId);
        WalletAccount locked = walletAccountMapper.lockById(account.getAccountId());
        // 冪等檢查放在鎖帳戶之後，並用鎖定讀取（FOR UPDATE）：REPEATABLE READ 下一般查詢讀的是交易開始時的快照，
        // 看不到並行申請剛提交的那一筆；鎖定讀取讀最新提交資料，後到的申請會直接回傳先完成的那一筆
        WithdrawOrder existing = getOne(WithdrawOrder.gw().eq(WithdrawOrder::getOwnerType, ownerType)
                .eq(WithdrawOrder::getOwnerId, ownerId).eq(WithdrawOrder::getReqNo, reqNo).last("FOR UPDATE"));
        if (existing != null) {
            return existing;
        }
        if (StringUtils.isAnyBlank(locked.getPayoutAccountNo(), locked.getPayoutBankCode(), locked.getPayoutAccountName())) {
            throw new BizException("請先設定收款帳戶");
        }
        if (locked.getBalance() < amount) {
            throw new BizException("可用餘額不足（可用 " + fen(locked.getBalance()) + " 元）");
        }

        WithdrawOrder order = new WithdrawOrder()
                .setWithdrawId("W" + DateUtil.format(new Date(), "yyMMddHHmmss") + RandomStringUtils.randomNumeric(4))
                .setOwnerType(ownerType).setOwnerId(ownerId).setReqNo(reqNo)
                .setAmount(amount).setFee(fee).setActualAmount(amount - fee)
                .setBankName(locked.getPayoutBankName()).setBankCode(locked.getPayoutBankCode())
                .setBranch(locked.getPayoutBranch()).setAccountNo(locked.getPayoutAccountNo())
                .setAccountName(locked.getPayoutAccountName())
                .setApplyUid(uid).setApplyName(name);

        String hit = riskBlacklistService.match(blacklistScopes(ownerType, ownerId), locked.getPayoutAccountNo(), locked.getPayoutAccountName());
        if (hit != null) {
            // 黑名單直接拒絕，不凍結，只留下紀錄供稽核
            order.setState(WithdrawOrder.STATE_REJECTED).setRiskFlags("BLACKLIST")
                    .setReviewerName("系統風控").setReviewRemark("收款帳戶命中黑名單（" + hit + "）").setReviewedAt(new Date());
            save(order);
            return order;
        }

        order.setState(WithdrawOrder.STATE_PENDING).setRiskFlags(riskFlags(locked));
        save(order);
        walletService.post(ownerType, ownerId, WalletLedger.BIZ_WITHDRAW_APPLY, order.getWithdrawId(),
                -amount, amount, false, "提現申請凍結", uid, name);
        return order;
    }

    /** 申請人取消（僅限待審核）。 */
    @Transactional
    public void cancel(String ownerType, String ownerId, String withdrawId, Long uid, String name) {
        WithdrawOrder order = getById(withdrawId);
        if (order == null || !ownerType.equals(order.getOwnerType()) || !ownerId.equals(order.getOwnerId())) {
            throw new BizException("提現單不存在");
        }
        close(order, WithdrawOrder.STATE_CANCELLED, uid, name, "申請人取消", null);
        walletService.post(ownerType, ownerId, WalletLedger.BIZ_WITHDRAW_RELEASE, withdrawId,
                order.getAmount(), -order.getAmount(), false, "取消提現解凍", uid, name);
    }

    @Transactional
    public void reject(String withdrawId, Long uid, String name, String remark) {
        if (StringUtils.isBlank(remark)) {
            throw new BizException("請填寫駁回原因");
        }
        WithdrawOrder order = require(withdrawId);
        close(order, WithdrawOrder.STATE_REJECTED, uid, name, remark, null);
        walletService.post(order.getOwnerType(), order.getOwnerId(), WalletLedger.BIZ_WITHDRAW_RELEASE, withdrawId,
                order.getAmount(), -order.getAmount(), false, "提現駁回解凍", uid, name);
    }

    /** 平台已完成人工匯款：凍結金額出帳，手續費記入平台帳戶。 */
    @Transactional
    public void markPaid(String withdrawId, Long uid, String name, String paidRef, String remark) {
        if (StringUtils.isBlank(paidRef)) {
            throw new BizException("請填寫匯款單號或交易序號");
        }
        WithdrawOrder order = require(withdrawId);
        if (Objects.equals(order.getApplyUid(), uid)) {
            throw new BizException("不可審核自己提出的提現");
        }
        close(order, WithdrawOrder.STATE_PAID, uid, name, remark, paidRef.trim());
        walletService.post(order.getOwnerType(), order.getOwnerId(), WalletLedger.BIZ_WITHDRAW_PAID, withdrawId,
                0, -order.getAmount(), false, "提現已撥款（實付 " + fen(order.getActualAmount()) + " 元）", uid, name);
        if (order.getFee() != null && order.getFee() > 0) {
            walletService.post(WalletAccount.OWNER_PLATFORM, WalletAccount.PLATFORM_ID, WalletLedger.BIZ_WITHDRAW_FEE, withdrawId,
                    order.getFee(), 0, true, "提現手續費", uid, name);
        }
    }

    /** 黑名單比對範圍：全平台＋擁有者所屬的團長（商戶取其代理鏈的團長；代理取自己或上級）。 */
    List<String> blacklistScopes(String ownerType, String ownerId) {
        List<String> scopes = new ArrayList<>();
        scopes.add(RiskBlacklist.SCOPE_GLOBAL);
        AgentInfo senior = null;
        if (WalletAccount.OWNER_MCH.equals(ownerType)) {
            senior = feeRuleService.agentChainOf(ownerId).getSenior();
        } else if (WalletAccount.OWNER_AGENT.equals(ownerType)) {
            AgentInfo agent = agentInfoMapper.selectById(ownerId);
            if (agent != null) {
                senior = Objects.equals(agent.getAgentLevel(), AgentInfo.LEVEL_SENIOR) ? agent
                        : (agent.getParentAgentNo() == null ? null : agentInfoMapper.selectById(agent.getParentAgentNo()));
            }
        }
        if (senior != null) {
            scopes.add(senior.getAgentNo());
        }
        return scopes;
    }

    private WithdrawOrder require(String withdrawId) {
        WithdrawOrder order = getById(withdrawId);
        if (order == null) {
            throw new BizException("提現單不存在");
        }
        return order;
    }

    /** 以「WHERE state=待審核」條件更新，防止重複審核或審核與取消同時發生。 */
    private void close(WithdrawOrder order, byte state, Long uid, String name, String remark, String paidRef) {
        boolean updated = update(new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getWithdrawId, order.getWithdrawId())
                .eq(WithdrawOrder::getState, WithdrawOrder.STATE_PENDING)
                .set(WithdrawOrder::getState, state)
                .set(WithdrawOrder::getReviewerUid, uid)
                .set(WithdrawOrder::getReviewerName, name)
                .set(WithdrawOrder::getReviewRemark, remark)
                .set(WithdrawOrder::getPaidRef, paidRef)
                .set(WithdrawOrder::getReviewedAt, new Date()));
        if (!updated) {
            throw new BizException("此提現單已處理");
        }
    }

    private String riskFlags(WalletAccount account) {
        List<String> flags = new ArrayList<>();
        long today = count(WithdrawOrder.gw().eq(WithdrawOrder::getAccountNo, account.getPayoutAccountNo())
                .in(WithdrawOrder::getState, WithdrawOrder.STATE_PENDING, WithdrawOrder.STATE_PAID)
                .ge(WithdrawOrder::getCreatedAt, DateUtil.beginOfDay(new Date())));
        if (today >= walletConfig.withdrawDailyLimitPerAccount()) {
            flags.add("DAILY_LIMIT");
        }
        if (walletConfig.restrictedBankCodes().contains(account.getPayoutBankCode())) {
            flags.add("RESTRICTED_BANK");
        }
        if (account.getPayoutUpdatedAt() != null && account.getPayoutUpdatedAt().after(DateUtil.offsetHour(new Date(), -24))) {
            flags.add("NEW_ACCOUNT");
        }
        return flags.isEmpty() ? null : String.join(",", flags);
    }

    static String normalizeAccountNo(String v) {
        return StringUtils.trimToEmpty(v).replaceAll("[\\s-]", "");
    }

    private static String fen(long v) {
        return java.math.BigDecimal.valueOf(v).movePointLeft(2).toPlainString();
    }
}
