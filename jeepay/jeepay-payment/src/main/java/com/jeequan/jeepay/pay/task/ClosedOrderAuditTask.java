package com.jeequan.jeepay.pay.task;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.PayOrderAudit;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.pay.channel.IPayOrderQueryService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.mapper.PayOrderAuditMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 已關閉訂單補查（技術債 C5，延伸 ADR-0007）。
 *
 * 本地到期關閉的訂單，付款人仍可能在上游有效期限內付款；若 APN 遺失，訂單會一直停在關閉、商戶不入帳。
 * 本任務對「到期後 72 小時內」的關閉訂單，只在到期後 1、24、48 小時各向上游查詢一次（最多 3 次，
 * 以旁表 t_pay_order_audit 記錄進度），查到已付款才以 ADR-0007 相同路徑轉回支付成功並通知商戶。
 * 查詢是唯讀，不會建立任何上游訂單。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "isys.closed-order-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClosedOrderAuditTask {

    /** 第 n 次補查距到期的最少時數 */
    static final int[] CHECKPOINT_HOURS = {1, 24, 48};
    private static final int WINDOW_HOURS = 72;

    @Autowired private PayOrderService payOrderService;
    @Autowired private PayOrderAuditMapper payOrderAuditMapper;
    @Autowired private ConfigContextQueryService configContextQueryService;
    @Autowired private PayOrderProcessService payOrderProcessService;

    @Scheduled(cron = "0 5/10 * * * ?")
    public void start() {
        Date now = new Date();
        var page = payOrderService.page(new Page<>(1, 200, false), PayOrder.gw()
                .eq(PayOrder::getState, PayOrder.STATE_CLOSED)
                .ge(PayOrder::getExpiredTime, DateUtil.offsetHour(now, -WINDOW_HOURS))
                .le(PayOrder::getExpiredTime, DateUtil.offsetHour(now, -CHECKPOINT_HOURS[0]))
                .orderByAsc(PayOrder::getExpiredTime));
        for (PayOrder order : page.getRecords()) {
            try {
                audit(order, now);
            } catch (Exception e) {
                log.error("關閉訂單補查失敗 payOrderId={}", order.getPayOrderId(), e);
            }
        }
    }

    /** 已查次數與到期時間決定這一輪是否該查。 */
    static boolean isDue(int auditCount, Date expiredTime, Date now) {
        if (auditCount >= CHECKPOINT_HOURS.length || expiredTime == null) {
            return false;
        }
        return !DateUtil.offsetHour(expiredTime, CHECKPOINT_HOURS[auditCount]).after(now);
    }

    private void audit(PayOrder order, Date now) {
        PayOrderAudit audit = payOrderAuditMapper.selectById(order.getPayOrderId());
        int count = audit == null ? 0 : audit.getAuditCount();
        if (!isDue(count, order.getExpiredTime(), now)) {
            return;
        }
        IPayOrderQueryService queryService = SpringBeansUtil.getBean(order.getIfCode() + "PayOrderQueryService", IPayOrderQueryService.class);
        if (queryService == null) {
            return;
        }
        MchAppConfigContext ctx = configContextQueryService.queryMchInfoAndAppInfo(order.getMchNo(), order.getAppId());
        ChannelRetMsg ret = null;
        try {
            ret = queryService.query(order, ctx);
        } catch (Exception e) {
            log.warn("關閉訂單補查：上游查詢失敗 payOrderId={}", order.getPayOrderId(), e);
        }
        boolean reopened = false;
        if (ret != null && ret.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS
                && payOrderService.updateClosed2Success(order.getPayOrderId(), ret.getChannelOrderId(), ret.getChannelUserId())) {
            // 與 ADR-0007 paid-APN 相同：轉回成功後走標準成功流程（商戶通知、分帳等）
            payOrderProcessService.confirmSuccess(order);
            reopened = true;
            log.warn("關閉訂單補查：上游已付款，訂單轉回支付成功 payOrderId={}", order.getPayOrderId());
        }
        PayOrderAudit row = new PayOrderAudit().setPayOrderId(order.getPayOrderId()).setAuditCount(count + 1)
                .setLastResult(ret == null ? "QUERY_FAILED" : String.valueOf(ret.getChannelState()))
                .setReopened((byte) (reopened ? 1 : 0)).setLastAuditAt(now);
        if (audit == null) {
            payOrderAuditMapper.insert(row);
        } else {
            payOrderAuditMapper.updateById(row);
        }
    }
}
