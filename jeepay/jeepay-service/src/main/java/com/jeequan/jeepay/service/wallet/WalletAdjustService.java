package com.jeequan.jeepay.service.wallet;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.WalletAccount;
import com.jeequan.jeepay.core.entity.WalletAdjustReq;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.WalletAdjustReqMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/** 人工調帳（ADR-0010）：申請 → 另一人覆核 → 記帳。用於更正、補償等無法由訂單結算產生的異動。 */
@Service
public class WalletAdjustService extends ServiceImpl<WalletAdjustReqMapper, WalletAdjustReq> {

    @Autowired private WalletService walletService;

    public WalletAdjustReq request(Long accountId, long amount, String reason, Long uid, String name) {
        WalletAccount account = walletService.getById(accountId);
        if (account == null) {
            throw new BizException("帳戶不存在");
        }
        if (amount == 0) {
            throw new BizException("調整金額不可為 0");
        }
        if (StringUtils.isBlank(reason)) {
            throw new BizException("請填寫調帳原因");
        }
        WalletAdjustReq req = new WalletAdjustReq().setAccountId(accountId).setOwnerType(account.getOwnerType())
                .setOwnerId(account.getOwnerId()).setAmount(amount).setReason(StringUtils.abbreviate(reason.trim(), 128))
                .setState(WalletAdjustReq.STATE_PENDING).setRequesterUid(uid).setRequesterName(name);
        save(req);
        return req;
    }

    @Transactional
    public void approve(Long reqId, Long uid, String name, String remark) {
        WalletAdjustReq req = claim(reqId, uid, name, remark, WalletAdjustReq.STATE_APPROVED);
        walletService.post(req.getOwnerType(), req.getOwnerId(), WalletLedger.BIZ_ADJUST, "ADJ" + reqId,
                req.getAmount(), 0, true, "人工調帳：" + req.getReason() + "（申請 " + req.getRequesterName() + "）", uid, name);
    }

    @Transactional
    public void reject(Long reqId, Long uid, String name, String remark) {
        claim(reqId, uid, name, remark, WalletAdjustReq.STATE_REJECTED);
    }

    private WalletAdjustReq claim(Long reqId, Long uid, String name, String remark, byte state) {
        WalletAdjustReq req = getById(reqId);
        if (req == null) {
            throw new BizException("申請不存在");
        }
        if (Objects.equals(req.getRequesterUid(), uid)) {
            throw new BizException("不可覆核自己提出的申請");
        }
        boolean updated = update(new LambdaUpdateWrapper<WalletAdjustReq>().eq(WalletAdjustReq::getReqId, reqId)
                .eq(WalletAdjustReq::getState, WalletAdjustReq.STATE_PENDING)
                .set(WalletAdjustReq::getState, state).set(WalletAdjustReq::getReviewerUid, uid)
                .set(WalletAdjustReq::getReviewerName, name).set(WalletAdjustReq::getReviewRemark, remark)
                .set(WalletAdjustReq::getReviewedAt, new Date()));
        if (!updated) {
            throw new BizException("此申請已處理");
        }
        return req;
    }
}
