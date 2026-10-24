# Telegram Daily Payment Summary

## Runtime contract

Production `manager` 每日 `00:00 Asia/Taipei` 統計前一個完整曆日，並向營運 Telegram 私人群組發送：

```text
📊 2026-09-15 收款總結
RYO：NT$ 1,234
CHI：NT$ 200,000
JAY：NT$ 0
JHD：NT$ 66
FYZ：NT$ 500
YUC：NT$ 700
SUM：NT$ 202,500
```

統計只讀 JeePay local `t_pay_order`，不呼叫 Provider。查詢口徑：

- `success_time >= 前日 00:00` 且 `< 今日 00:00`，時區 `Asia/Taipei`。
- `state = 2`（支付成功）。
- `currency = TWD`。
- `if_code` 為 `ryo`、`chi`、`jay`、`jhd`、`fyz`、`yuc`。
- `amount` 以分儲存，輸出為整數 TWD 並使用千分位逗號。

### 自動刪除

日報送出後約 47.5 小時自動刪除：每日 `23:30 Asia/Taipei` 刪除前天 `00:00` 送出的日報（reportDate = 今天 − 2）。Telegram 只允許 bot 刪除 48 小時內自己發的訊息，因此不能剛好在 48 小時刪除。

- 發送成功後，`message_id` 存入 Redis key `telegramDailySummary:messageId:<reportDate>`，TTL 3 天，所以 `manager` 重啟後仍能刪除。
- 刪除成功或失敗都會移除該 key，不會重試，因為超過 48 小時就再也刪不掉。
- Redis 寫入失敗只記 warn，日報照常送出，但該則不會被自動刪除。
- 若 `manager` 在 23:30 停機，該則日報不會被刪除，需要手動處理。

目前六個黑貓 PAY Provider 不提供 Refund capability，因此本報表不定義退款淨額；若未來加入退款，須另行確認 gross/net 口徑。

## Secret intake

Bot Token 不得寫入 repo、command line、chat 或 log。部署前由 operator 的 TTY 執行：

```bash
sudo /opt/jee8pay-v2-production/bin/populate-v2-telegram-bot-token
```

腳本將 Token 寫入 root-controlled `/opt/jee8pay-v2-production/secrets/telegram-bot-token`，檔案 owner 為 runtime uid `10001`、mode `0600`。Compose 將它掛載為 configtree key `telegram.daily-summary.bot-token`；群組 chat ID 由 Production Compose 設定。

## Deployment and verification

Token intake 完成後才可 recreate `manager`。啟動時若功能已啟用但 Token 或 chat ID 缺失，application 會 fail closed，不會以空值呼叫 Telegram。

驗證項目：

1. `manager` healthy，啟動 log 不含 Bot Token。
2. 次日 `00:00` 後出現 `Telegram daily payment summary sent for reportDate=...`。
3. Telegram 收到四家固定順序與最後一行 `SUM`。
4. DB 以同一個 `[start, end)` `success_time` 區間重算，四家及 `SUM` 相符。
5. 發送後第二天 `23:30` 後出現 `Telegram daily payment summary deleted for reportDate=...`，Telegram 群組中該則日報已消失。

目前沒有 catch-up 或 persistent delivery ledger；若 `manager` 在午夜停機，該日報不會自動補送。多實例部署前也須加入 single-run coordination，現行 Production Compose 僅有一個 `manager` instance。
