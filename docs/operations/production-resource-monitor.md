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
- 警戒與解除通知發出 24 小時後自動刪除（`AUTO_DELETE_HOURS`）：發送時把 message_id 記在 `sent` 檔，每次執行刪除到期的訊息；刪除失敗會在 47 小時內重試，超過就放棄（Telegram 只允許 bot 刪除 48 小時內的訊息）。edge 看門狗的停機通知不會自動刪除，保留作事件紀錄。
- 狀態檔：`/var/lib/jee8pay-resource-monitor/`；log：`/var/log/jee8pay-resource-monitor.log`（`/etc/logrotate.d/jee8pay-monitors` 每週輪替，保留 4 份）。

已知：mq-broker（RocketMQ，`mem_limit` 1 GiB）平常約 68%，接近門檻；若持續警戒，應評估調高其 `mem_limit` 或 JVM heap，而非放寬全域門檻。

## 停用

```bash
sudo rm /etc/cron.d/jee8pay-resource-monitor
```
