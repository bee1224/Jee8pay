package com.jeequan.jeepay.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.FeeRule;
import com.jeequan.jeepay.core.entity.FeeRuleChangeReq;
import com.jeequan.jeepay.core.entity.FeeRuleLog;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.FeeRuleChangeReqMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/**
 * 平臺費／渠道費雙人覆核（ADR-0009 第四階段）。
 * 申請時即做完整檢查，避免無效申請進入佇列；核准時再檢查一次（期間商戶可能已被刪除）。
 * 狀態以「WHERE state=待覆核」條件更新，防止兩人同時核准造成重複寫入。
 */
@Service
public class FeeRuleChangeService extends ServiceImpl<FeeRuleChangeReqMapper, FeeRuleChangeReq> {

    @Autowired private FeeRuleService feeRuleService;

    @Transactional
    public FeeRuleChangeReq requestSave(FeeRule input, Long uid, String name) {
        if (!FeeRuleService.isPlatformLayer(input.getLayer())) {
            throw new BizException("只有平臺費與渠道費需要覆核");
        }
        feeRuleService.normalizeAndValidate(input);
        return submit(new FeeRuleChangeReq().setAction(FeeRuleLog.ACTION_SAVE)
                .setWayCode(input.getWayCode()).setTargetType(input.getTargetType()).setTargetId(input.getTargetId())
                .setLayer(input.getLayer()).setRate(input.getRate()).setFixedAmount(input.getFixedAmount()), uid, name);
    }

    @Transactional
    public FeeRuleChangeReq requestDelete(Long ruleId, Long uid, String name) {
        FeeRule rule = feeRuleService.getById(ruleId);
        if (rule == null) {
            throw new BizException("費率規則不存在");
        }
        if (!FeeRuleService.isPlatformLayer(rule.getLayer())) {
            throw new BizException("只有平臺費與渠道費需要覆核");
        }
        return submit(new FeeRuleChangeReq().setAction(FeeRuleLog.ACTION_DELETE)
                .setWayCode(rule.getWayCode()).setTargetType(rule.getTargetType()).setTargetId(rule.getTargetId())
                .setLayer(rule.getLayer()).setRate(rule.getRate()).setFixedAmount(rule.getFixedAmount()), uid, name);
    }

    private FeeRuleChangeReq submit(FeeRuleChangeReq req, Long uid, String name) {
        // 同一條規則同時只能有一張待覆核申請，避免核准順序不同導致結果難以預期
        if (count(FeeRuleChangeReq.gw().eq(FeeRuleChangeReq::getState, FeeRuleChangeReq.STATE_PENDING)
                .eq(FeeRuleChangeReq::getWayCode, req.getWayCode())
                .eq(FeeRuleChangeReq::getTargetType, req.getTargetType())
                .eq(FeeRuleChangeReq::getTargetId, req.getTargetId())
                .eq(FeeRuleChangeReq::getLayer, req.getLayer())) > 0) {
            throw new BizException("此規則已有待覆核的申請，請先處理");
        }
        req.setState(FeeRuleChangeReq.STATE_PENDING).setRequesterUid(uid).setRequesterName(name);
        save(req);
        return req;
    }

    @Transactional
    public void approve(Long reqId, Long uid, String name, String remark) {
        FeeRuleChangeReq req = claim(reqId, uid, name, remark, FeeRuleChangeReq.STATE_APPROVED);
        String operator = name + "（覆核；申請人 " + req.getRequesterName() + "）";
        if (FeeRuleLog.ACTION_DELETE.equals(req.getAction())) {
            FeeRule rule = feeRuleService.getOne(FeeRule.gw().eq(FeeRule::getWayCode, req.getWayCode())
                    .eq(FeeRule::getTargetType, req.getTargetType()).eq(FeeRule::getTargetId, req.getTargetId())
                    .eq(FeeRule::getLayer, req.getLayer()));
            if (rule == null) {
                throw new BizException("規則已不存在，請駁回此申請");
            }
            feeRuleService.removeRule(rule.getRuleId(), uid, operator);
        } else {
            feeRuleService.saveRule(new FeeRule().setWayCode(req.getWayCode()).setTargetType(req.getTargetType())
                    .setTargetId(req.getTargetId()).setLayer(req.getLayer()).setRate(req.getRate())
                    .setFixedAmount(req.getFixedAmount()), uid, operator);
        }
    }

    @Transactional
    public void reject(Long reqId, Long uid, String name, String remark) {
        claim(reqId, uid, name, remark, FeeRuleChangeReq.STATE_REJECTED);
    }

    private FeeRuleChangeReq claim(Long reqId, Long uid, String name, String remark, byte targetState) {
        FeeRuleChangeReq req = getById(reqId);
        if (req == null) {
            throw new BizException("申請不存在");
        }
        if (req.getState() != FeeRuleChangeReq.STATE_PENDING) {
            throw new BizException("此申請已處理");
        }
        if (Objects.equals(req.getRequesterUid(), uid)) {
            throw new BizException("不可覆核自己提出的申請");
        }
        boolean updated = update(new LambdaUpdateWrapper<FeeRuleChangeReq>()
                .eq(FeeRuleChangeReq::getReqId, reqId)
                .eq(FeeRuleChangeReq::getState, FeeRuleChangeReq.STATE_PENDING)
                .set(FeeRuleChangeReq::getState, targetState)
                .set(FeeRuleChangeReq::getReviewerUid, uid)
                .set(FeeRuleChangeReq::getReviewerName, name)
                .set(FeeRuleChangeReq::getReviewRemark, remark)
                .set(FeeRuleChangeReq::getReviewedAt, new Date()));
        if (!updated) {
            throw new BizException("此申請已處理");
        }
        return req;
    }
}
