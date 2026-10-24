# Production 資源警戒

`deploy/jee8pay-v2-production/scripts/resource-monitor.sh` 由 `/etc/cron.d/jee8pay-resource-monitor` 每分鐘以 root 執行，2026-10-24 起於正式機運作。通知與 Daily Report 共用同一個 bot 與營運群組（token 讀自 `/opt/jee8pay-v2-production/secrets/telegram-bot-token`，經 stdin 交給 curl，不進 process list 或 log）。

## 監測項目

| 項目 | 計算方式 |
|---|---|
| 主機 CPU | `/proc/stat` 5 秒區間使用率，連續 3 分鐘 ≥ 門檻才警戒 |
| 主機記憶體 | `(MemTotal − MemAvailable) / MemTotal` |
| 主機 swap | 有設定 swap 時才檢查（目前正式機沒有 swap） |
| 磁碟空間、inode | 實體檔案系統（排除 tmpfs、overlay 等） |
| 容器記憶體 | `docker stats` 的 MemPerc，相對各容器 `mem_limit`，已扣除可回收的 page cache |

## 通知規則

- 門檻預設 70%（`THRESHOLD`），≥ 70% 時通知一次。
- 持續超標每 6 小時提醒一次（`REMIND_HOURS`）。
- 低於 65% 才通知「已回落」，避免在門檻附近反覆通知。
- 警戒與解除通知發出 24 小時後自動刪除（`AUTO_DELETE_HOURS`）：發送時把 message_id 記在 `sent` 檔，每次執行刪除到期的訊息；刪除失敗會在 47 小時內重試，超過就放棄（Telegram 只允許 bot 刪除 48 小時內的訊息）。服務看門狗的通知同樣 24 小時後刪除。
- 狀態檔：`/var/lib/jee8pay-resource-monitor/`；log：`/var/log/jee8pay-resource-monitor.log`（`/etc/logrotate.d/jee8pay-monitors` 每週輪替，保留 4 份）。

已知：測試機 mq-broker 原本 `mem_limit` 768m，實際用量約 670 MiB（heap 448 MiB + 非 heap 約 220 MiB）達 97%；2026-10-24 改為 `mem_limit: 1024m` 並加 `-XX:MaxDirectMemorySize=256m`（RocketMQ 預設 1g 會超過容器上限），與正式機一致，用量降為約 65%。

已知：mq-broker（RocketMQ，`mem_limit` 1 GiB）平常約 68%，接近門檻；若持續警戒，應評估調高其 `mem_limit` 或 JVM heap，而非放寬全域門檻。

## 測試機

測試機（server1.nnviopp.com）安裝同一支腳本（`/opt/jee8pay-v2-dev/scripts/`），主機差異寫在 `/etc/jee8pay-watchdog.conf`（repo：`deploy/jee8pay-v2-dev/config/jee8pay-watchdog.conf`）：監測 `jee8pay-v2-dev*` 與 `nnviopp-sandbox-edge` 容器、時區 Asia/Taipei、token 讀自 `/opt/jee8pay-v2-dev/secrets/telegram-bot-token`。通知發到同一個營運群組，以 `[主機名]` 區分。

## 停用

```bash
sudo rm /etc/cron.d/jee8pay-resource-monitor
```
