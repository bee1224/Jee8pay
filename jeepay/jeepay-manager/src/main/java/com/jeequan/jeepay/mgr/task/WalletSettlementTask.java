package com.jeequan.jeepay.mgr.task;

import com.jeequan.jeepay.service.wallet.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 分潤結算排程（ADR-0010）：每 5 分鐘把到期（T+N）的成功訂單依快照入帳，並沖回已退款的訂單。
 * 結算本身冪等，重跑或與「立即結算」按鈕同時執行都不會重複入帳。
 * 多台營運平台同時部署時，可在其餘節點以 isys.wallet.settle-enabled=false 關閉。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "isys.wallet", name = "settle-enabled", havingValue = "true", matchIfMissing = true)
public class WalletSettlementTask {

    @Autowired private SettlementService settlementService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void run() {
        try {
            settlementService.runOnce();
        } catch (Exception e) {
            log.error("分潤結算排程執行失敗", e);
        }
    }
}
