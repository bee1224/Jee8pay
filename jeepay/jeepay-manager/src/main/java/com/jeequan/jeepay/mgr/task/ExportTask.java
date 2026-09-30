package com.jeequan.jeepay.mgr.task;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.service.export.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 下載中心背景工作：每 10 秒處理排隊中的匯出；每天清除 7 天前的檔案。 */
@Slf4j
@Component
public class ExportTask {

    @Autowired private ExportService exportService;

    @Value("${isys.oss.file-root-path:/tmp}")
    private String fileRootPath;

    @Scheduled(initialDelay = 20_000, fixedDelay = 10_000)
    public void run() {
        try {
            // 一次最多處理 5 個，避免單一排程佔住太久
            for (int i = 0; i < 5 && exportService.runNext(CS.SYS_TYPE.MGR, fileRootPath + "/private"); i++) {
                // 繼續下一個
            }
        } catch (Exception e) {
            log.error("匯出排程執行失敗", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?", zone = "Asia/Taipei")
    public void purge() {
        exportService.purge(CS.SYS_TYPE.MGR, fileRootPath + "/private");
    }
}
