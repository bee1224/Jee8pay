package com.jeequan.jeepay.service.wallet;

import cn.hutool.core.date.DateUtil;
import com.jeequan.jeepay.core.entity.PayOrderFee;
import com.jeequan.jeepay.core.entity.WalletLedger;
import com.jeequan.jeepay.service.mapper.PayOrderFeeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 訂單分潤結算（ADR-0010）：依訂單手續費快照，把訂單金額拆入商戶、代理、渠道、平台錢包。
 *
 * - 只結算有快照、支付成功、且成功時間早於結算截止點（T+N，台北時間午夜）的訂單。
 * - 每筆訂單一個交易；先以條件更新把快照從「未結算」轉為「已結算」，搶不到就跳過，
 *   流水唯一鍵是第二道防線，保證重跑、併發都不會重複入帳。
 * - 已結算的訂單若之後全額退款，反向沖回同樣的金額；結算前就退款的標記為不結算。
 */
@Slf4j
@Service
public class SettlementService {

    private static final int BATCH = 200;

    @Autowired private PayOrderFeeMapper payOrderFeeMapper;
    @Autowired private WalletService walletService;
    @Autowired private WalletConfig walletConfig;

    private final TransactionTemplate tx;

    public SettlementService(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** 結算截止點：T+0 為現在；T+N 為 N-1 天前的台北午夜（例如 T+1 = 今天 00:00 之前成功的訂單）。 */
    public Date cutoff(Date now) {
        int days = walletConfig.settleDelayDays();
        return days == 0 ? now : DateUtil.offsetDay(DateUtil.beginOfDay(now), -(days - 1));
    }

    /** 執行一輪：結算到期訂單、沖回已退款訂單。回傳 [結算筆數, 沖回筆數, 不結算筆數]。 */
    public int[] runOnce() {
        int skipped = payOrderFeeMapper.skipRefundedBeforeSettle();
        int settled = 0;
        int reversed = 0;
        for (Map<String, Object> row : payOrderFeeMapper.listSettleDue(cutoff(new Date()), BATCH)) {
            if (process(String.valueOf(row.get("payOrderId")), (String) row.get("ifCode"), false)) {
                settled++;
            }
        }
        for (Map<String, Object> row : payOrderFeeMapper.listReverseDue(BATCH)) {
            if (process(String.valueOf(row.get("payOrderId")), (String) row.get("ifCode"), true)) {
                reversed++;
            }
        }
        if (settled + reversed + skipped > 0) {
            log.info("分潤結算完成 settled={} reversed={} skipped={}", settled, reversed, skipped);
        }
        return new int[]{settled, reversed, skipped};
    }

    private boolean process(String payOrderId, String ifCode, boolean reverse) {
        try {
            Boolean done = tx.execute(status -> {
                byte from = reverse ? PayOrderFee.SETTLE_DONE : PayOrderFee.SETTLE_PENDING;
                byte to = reverse ? PayOrderFee.SETTLE_REVERSED : PayOrderFee.SETTLE_DONE;
                if (payOrderFeeMapper.transitSettleState(payOrderId, from, to) != 1) {
                    return false;
                }
                PayOrderFee fee = payOrderFeeMapper.selectById(payOrderId);
                List<SettlementSplit.Share> shares = SettlementSplit.of(fee, ifCode);
                for (SettlementSplit.Share share : shares) {
                    long amount = reverse ? -share.getAmount() : share.getAmount();
                    walletService.post(share.getOwnerType(), share.getOwnerId(),
                            reverse ? WalletLedger.BIZ_ORDER_REVERSE : WalletLedger.BIZ_ORDER_SETTLE, payOrderId,
                            amount, 0, true, reverse ? "訂單退款沖回" : "訂單結算", null, "系統結算");
                }
                return true;
            });
            return Boolean.TRUE.equals(done);
        } catch (Exception e) {
            // 單筆失敗不影響其他訂單；交易已回滾，快照維持原狀態，下一輪重試
            log.error("分潤結算失敗 payOrderId={} reverse={}", payOrderId, reverse, e);
            return false;
        }
    }
}
