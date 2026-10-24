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
| 監控 | `scripts/edge-watchdog.sh` + `/etc/cron.d/jee8pay-edge-watchdog`（每分鐘與開機後 90 秒）。edge 停止時自動 `docker start`；停止、連續 3 分鐘不健康、恢復時以 Daily Report 同一個 bot 通知營運群組，只在狀態改變時發送。log：`/var/log/jee8pay-edge-watchdog.log`。 |

**注意**：正式機目前 `current/compose.yml` 的 edge 仍是 `unless-stopped`；下一次 release 帶入 repo 的 compose 前，若重建 edge，restart 政策會退回 `unless-stopped`。根因已修正，看門狗仍會兜底。

## 重開機後檢查

```bash
sudo docker ps -a --format '{{.Names}} {{.Status}}' | grep jee8pay-v2-production
sudo docker inspect -f '{{.HostConfig.RestartPolicy.Name}} {{.State.Status}} {{.State.Health.Status}}' jee8pay-v2-production-edge
sudo tail -5 /var/log/jee8pay-edge-watchdog.log
```

不要再對任何容器使用 `docker kill --signal ...` 做 reload；改用 `docker exec <container> <reload 指令>`。
