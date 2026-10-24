# Production Edge 重開機復原

## 2026-10-24 停機事件

- **時間**：2026-10-24 17:05–18:28（Asia/Taipei），約 83 分鐘對外 80/443 中斷。最後一筆訂單 16:56，無訂單滯留。
- **觸發**：17:05:12 主機商由 hypervisor 關機（`qemu-ga: guest-shutdown called, mode: powerdown`、`hypervisor initiated shutdown`），無人登入、無事前通知。原因已寄信詢問 Namecheap（hostingsupport@namecheap.com），並要求日後重開機前預先通知。
- **擴大原因**：開機後 12 個 V2 容器中 11 個自動恢復，唯獨 `jee8pay-v2-production-edge` 未啟動，且 Docker 不留任何錯誤 log。

### 根因

1. 2026-10-22 20:46 certbot 續約，renew hook `scripts/sync-edge-certificate.sh` 以 `docker kill --signal HUP` 讓 nginx 重新載入憑證。
2. Docker 29.7.1 `daemon/kill.go`：只要不是在 daemon 關機中，對容器送**任何**訊號都會設 `HasBeenManuallyStopped = true` 並寫入磁碟；容器仍正常運作，所以平常看不出來。
3. 開機 restore 時，`unless-stopped` 對 `HasBeenManuallyStopped = true` 的容器回傳不重啟（`daemon/daemon.go` restore → `ShouldRestart()`），edge 被靜默略過。
4. 每分鐘的 `apply-uat-allowlist.sh` 對 edge `docker exec` 失敗 77 次，但沒有任何告警。

## 修正（2026-10-24 已套用）

| 項目 | 內容 |
|---|---|
| 根因 | `sync-edge-certificate.sh`（prod、dev）改用 `docker exec <edge> nginx -s reload`，不再 `docker kill`。舊檔備份為 `*.bak-<timestamp>`。 |
| 重啟政策 | prod edge 改為 `restart: always`：repo `compose.yml` 已改；執行中容器以 `docker update --restart=always` 套用，未重建。 |
| 監控 | 已由「服務看門狗」取代原 edge 看門狗，見下方〈服務看門狗與關機通知〉。 |

**注意**：2026-10-24 20:33 起正式機 `current`（release `169bb30d7eac-tgdel`）的 compose 已是 edge `restart: always`，與 repo 一致。

## 服務看門狗與關機通知

`scripts/service-watchdog.sh`（取代 `edge-watchdog.sh`），由 `/etc/cron.d/jee8pay-service-watchdog` 每分鐘及開機後 90 秒以 root 執行：

- **容器**：檢查 `current/compose.yml` 的全部服務（restart 政策為 `no` 的一次性工作如 `volume-init` 除外）。停止的容器依序自動 `docker start`（db、redis、mq-namesrv、mq-broker 先，edge 最後，其餘依 compose 順序）；連續 3 分鐘 unhealthy 告警；edge 另檢查 `162.0.233.203:443` 是否監聽。
- **主機服務**：docker、containerd、cron、ssh、fail2ban、ufw、qemu-guest-agent 未執行即告警。
- **重開機總結**：偵測到 `boot_id` 改變時發一則總結，含開機時間、上次是否正常關機、容器與主機服務的正常數量，以及自動啟動結果。
- **關機通知**：`systemd/jee8pay-shutdown-notify.service`（安裝於 `/etc/systemd/system/`，已 enable）。關機或重開機開始時執行 `service-watchdog.sh shutdown`，此時網路與 docker 尚未停止；通知含關機類型與 logind 記錄的來源（例如 `hypervisor initiated shutdown`），並留下正常關機標記。斷電或強制關機無法通知，開機總結會顯示「未偵測到正常關機通知」。
- 通知只在狀態改變時發送，24 小時後自動刪除（`AUTO_DELETE_HOURS`）；事件紀錄保留在 `/var/log/jee8pay-service-watchdog.log`。狀態檔：`/var/lib/jee8pay-service-watchdog/`。

關機通知尚未在正式機實際關機下驗證（避免為測試重開正式機），已驗證 unit 為 enabled/active 且排在 network-online、docker 之後（關機時先執行）。

## 重開機後檢查

```bash
sudo docker ps -a --format '{{.Names}} {{.Status}}' | grep jee8pay-v2-production
sudo docker inspect -f '{{.HostConfig.RestartPolicy.Name}} {{.State.Status}} {{.State.Health.Status}}' jee8pay-v2-production-edge
sudo tail -5 /var/log/jee8pay-service-watchdog.log
```

不要再對任何容器使用 `docker kill --signal ...` 做 reload；改用 `docker exec <container> <reload 指令>`。
