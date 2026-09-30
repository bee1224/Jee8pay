package com.jeequan.jeepay.service.wallet;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.WalletAccountMapper;
import com.jeequan.jeepay.service.mapper.WalletLedgerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 錢包記帳原語（ADR-0010）。所有餘額異動都必須經過 {@link #post}：
 * 先鎖帳戶列，再寫流水（唯一鍵保證冪等），最後以增量更新餘額。
 * 呼叫端負責外層交易；本方法要求已在交易中執行。
 */
@Service
public class WalletService extends ServiceImpl<WalletAccountMapper, WalletAccount> {

    @Autowired private WalletLedgerMapper ledgerMapper;

    /** 取得帳戶，不存在時建立（併發建立時以唯一鍵收斂）。 */
    public WalletAccount getOrCreate(String ownerType, String ownerId) {
        WalletAccount account = find(ownerType, ownerId);
        if (account != null) {
            return account;
        }
        try {
            WalletAccount created = new WalletAccount().setOwnerType(ownerType).setOwnerId(ownerId)
                    .setBalance(0L).setFrozen(0L).setTotalIn(0L).setTotalOut(0L);
            save(created);
            return created;
        } catch (DuplicateKeyException e) {
            return find(ownerType, ownerId);
        }
    }

    public WalletAccount find(String ownerType, String ownerId) {
        return getOne(WalletAccount.gw().eq(WalletAccount::getOwnerType, ownerType).eq(WalletAccount::getOwnerId, ownerId));
    }

    /**
     * 記帳一筆。
     *
     * @param availDelta   可用餘額變動（正入負出）
     * @param frozenDelta  凍結金額變動
     * @param allowNegative 是否允許可用餘額變成負數（結算沖回、人工調帳允許；提現凍結不允許）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WalletLedger post(String ownerType, String ownerId, String bizType, String bizId,
                             long availDelta, long frozenDelta, boolean allowNegative,
                             String remark, Long operatorUid, String operatorName) {
        WalletAccount account = getOrCreate(ownerType, ownerId);
        WalletAccount locked = baseMapper.lockById(account.getAccountId());
        long before = locked.getBalance();
        long after = before + availDelta;
        long frozenAfter = locked.getFrozen() + frozenDelta;
        if (!allowNegative && after < 0) {
            throw new BizException("可用餘額不足");
        }
        if (frozenAfter < 0) {
            throw new BizException("凍結金額不足，帳務狀態異常");
        }
        WalletLedger ledger = new WalletLedger().setAccountId(locked.getAccountId())
                .setOwnerType(ownerType).setOwnerId(ownerId).setBizType(bizType).setBizId(bizId)
                .setAmount(availDelta).setFrozenChange(frozenDelta)
                .setBalanceBefore(before).setBalanceAfter(after).setFrozenAfter(frozenAfter)
                .setRemark(remark).setOperatorUid(operatorUid).setOperatorName(operatorName);
        try {
            ledgerMapper.insert(ledger);
        } catch (DuplicateKeyException e) {
            throw new BizException("重複記帳：" + bizType + " " + bizId);
        }
        // 淨值變動（可用＋凍結）計入累計入／出帳；凍結與解凍只是分桶移動，不算入出
        long net = availDelta + frozenDelta;
        baseMapper.applyDelta(locked.getAccountId(), availDelta, frozenDelta, Math.max(net, 0), Math.max(-net, 0));
        return ledger;
    }
}
