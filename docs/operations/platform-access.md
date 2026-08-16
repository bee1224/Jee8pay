# Platform Access（營運平台存取）

日期：2026-08-17

## 公開網址（營運平台）

| 環境 | 網址 | 說明 |
| --- | --- | --- |
| 測試（V2 Development） | `https://admin-v2-dev.nnviopp.com` | nnviopp sandbox edge → V2 manager-ui；Cloudflare proxied；登入保護 |
| 正式（V2 Production Candidate） | `https://admin-v2.lp33ing.com` | 指向 V2 Production Candidate（已部署並驗證，見下） |

> **部署狀態（2026-08-16 已完成）**：正式網址的 DNS（proxied）與 TLS SAN 已就緒；V1 edge（`lp33ing-production-edge`）
> 以 bind-mount `/etc/lp33ing-production/edge-nginx.conf`（read-only）套用 admin-v2 路由，edge 已 recreate、
> `nginx -t` 通過、reload 後 `admin-v2.lp33ing.com` 實測回 V2 Manager UI（營運平台）。
> 此動作影響 live V1 基礎設施，後續改動需明確授權（步驟見 `docs/operations/ccat-v2-production-candidate.md` 的 edge 章節）。

### Production edge ↔ V2 network 契約（F01 修復）

V1 edge 的 compose 屬 V1 legacy infra（不在本 repo），但 admin-v2 路由依賴以下 runtime 契約：

1. `lp33ing-production-edge` 必須 attach `jee8pay-v2-production-network`（Docker embedded DNS 只解析同 network 名稱）。
2. edge config（`/etc/lp33ing-production/edge-nginx.conf`）必須含 `resolver 127.0.0.11` + `set $admin_ui manager-ui` 的 variable proxy_pass。
3. `admin-v2.lp33ing.com` 443 區塊只代理 `$admin_ui:80`，不直接掛到整個 application network。

驗證（edge recreate 後必須重跑）：`deploy/jee8pay-v2-production/scripts/verify-prod-edge-admin-route.sh`
（檢查 network attach、DNS 解析、resolver 語法、`nginx -t`、live title、V1 api/admin 無回歸）。

## 帳號

- 登入帳號：`jeepay`（Manager 超管，is_admin=1，兩個環境皆同）
- **密碼：不分大小寫**（系統以 toUpperCase 正規化）；實際值不寫入本 repo，以授權交付文件/營運密碼表為準
- 密碼驗證改動：`UpperCasePasswordEncoder`（`jeepay-service/.../service/utils/`），manager/merchant 登入共用；
  Merchant API 的 App Secret（MD5 簽名）**維持大小寫敏感**，不受影響

## 前端

- 三個前端（Manager/Merchant/Cashier）與 init.sql seed 已繁中化（zh-TW）。
- 測試環境已部署繁中 build；正式環境 UI images 已建置於 candidate release。
- 已部署的 `dist/index.html` 有 entry JS/CSS 的 `<link rel="preload">`（**build 後手動 patch 加入**，非 build pipeline 產出；
  source `index.html` 未含 — vite 2.9 對 build-time preload 路徑會 build 失敗）。fresh build 後需重新 patch 才會帶 preload。
- admin-v2 路由（dev edge → merchant-api-ingress → manager-ui；prod edge → manager-ui）已改用
  Docker 內建 DNS 動態解析（`resolver 127.0.0.11` + variable `proxy_pass`）：UI 容器重建（IP 變動）
  後**無需 reload edge/ingress**，最多 10–30 秒自動恢復。對應設定：
  `deploy/jee8pay-v2-dev/merchant-uat/nginx.conf`（ingress）與
  `deploy/jee8pay-v2-production/edge-nginx.conf`（prod edge）。
  若手動調整任一跳（dev edge → ingress、prod edge → manager-ui）的 upstream，仍需 reload 對應 nginx。

## 安全備註

- Manager/營運後台為高權限面，建議僅以登入（帳號+密碼+驗證碼）為閘；若需 IP allowlist 請另提供 IP 清單。

## UAT Edge 白名單（自助管理）

營運平台 → 系統管理 → **UAT Edge 白名單**（`/uatedge/allowlist`，權限 `ENT_UAT_EDGE_ALLOWLIST`）：

- 新增/刪除 IP（支援 IPv4、IPv6、CIDR）；儲存後約 **1 分鐘內由 host cron 自動套用**到
  `api-v2-dev.nnviopp.com` 的建單/查單，不需人工部署。
- Talend 兩台測試機（`34.92.245.74`、`34.92.52.162`）為系統保留 IP，頁面不可刪除。
- 頁面會顯示套用狀態（`status.txt`：APPLIED / FAILED + reload 結果）。

架構：

```text
Manager 後端 API（UatEdgeAllowlistController）
  → /opt/jee8pay-v2-dev/edge-allowlist/allowlist.json（manager 容器 bind mount，唯一來源）
  → host cron（apply-uat-allowlist.sh，每分鐘）比對後產生 uat.conf
  → edge 以 include /etc/nginx/allowlist/uat.conf 套用 + nginx -t + reload
```

涉及檔案：`jeepay/jeepay-manager/.../mgr/ctrl/uatedge/UatEdgeAllowlistController.java`、
`jeepay-ui/jeepay-ui-manager/src/views/uatedge/UatEdgeAllowlist.vue`、
`deploy/jee8pay-v2-dev/scripts/apply-uat-allowlist.sh`、
`deploy/jee8pay-v2-dev/merchant-uat/prepare-edge-nginx.py`（`include` 指令）。
reconcile（`reconcile-sandbox-edge.sh`）會驗證 allowlist 掛載與 Talend 保留 IP 存在。
此功能目前只在 Development 環境啟用；Production edge 若需白名單自助管理，走相同模式另立任務。
