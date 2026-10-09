# CCAT V2 Development Runtime

> **RENAME NOTE（2026-08-20）**：本文記錄 Development runtime 的歷史部署狀態。JeePay Provider 已由 `ccat` 改名為 `ryo`（`CCAT_IBON` → `RYO_IBON`），並新增 `jay` / `chi`；重新部署後 APN 路徑為 `/api/pay/notify/ryo`、`/api/pay/notify/jay`、`/api/pay/notify/chi`，secret 目錄改為 `secrets/ryo-provider`（及 jay/chi）。文中 `ccat-v2-dev.nnviopp.com` 域名與 `ccat-v2` 名稱保留為歷史命名。

> **V1 RETIREMENT（2026-08-23）**：dev VPS（`server1.nnviopp.com` / `159.198.40.128`）的 V1（Go `payment-service` 四方聚合支付）已**完整退役並清理**。現況：
> - V1 compose 專案全清：`nnviopp-sandbox`（`/opt/payment/payment-service-sandbox`）、`merchant-sandbox-sandbox`、孤兒 `nnviopp-production`（`/opt/payment/payment-service`）；`docker compose down` 全清 containers/networks，named volumes 已刪。
> - V1 DB 封存：`payment_sandbox`、`payment_production` → `state/v1-retirement-20260822-222906/db/*.sql.gz`；`/opt/payment` 整包封存 → `state/v1-retirement-20260822-222906/opt-payment-archive.tar.gz`（含 edge Dockerfile、baseline nginx.conf、edge image tar）。
> - V1 公開入口關閉：edge 改為**純 V2**（移除 sandbox-api/sandbox/merchant-sandbox server blocks，V1 hostnames 回 000/444）；V1 DNS records 已刪（`sandbox-api` / `sandbox` / `merchant-sandbox.nnviopp.com`）；cert SAN 收斂 3 個 V2 域名（`ccat-v2-dev` / `api-v2-dev` / `admin-v2-dev`）；孤兒 `nnviopp-production-edge` cert 與 `/etc/nnviopp-production` 已刪；`eth0:0` alias（159.198.42.146）已移除。
> - **edge 收編**：`deploy/jee8pay-v2-dev/edge/compose.edge.yaml`（standalone project `jee8pay-v2-dev-edge`，container 名沿用 `nnviopp-sandbox-edge`）取代 V1 專案內定義與 `public-callback/compose.edge-overlay.yaml` overlay；edge 只掛 `jee8pay-v2-dev-edge-transit` + `jee8pay-v2-dev-network`（transit 為 internal，port 發布需同時掛非 internal 的 v2 network）。reconcile/validate 已更新為純 V2 版本（無 overlay、無 V1 health 檢查、config 不得含 V1 參照）。
> - cert renewal 現以 V2 腳本 `/opt/jee8pay-v2-dev/scripts/sync-edge-certificate.sh` 為 renew/deploy hook（取代 `/opt/payment/.../sync-sandbox-edge-certificate.sh`）。
> - 完整 gap 與驗證見下文「V1 retirement（2026-08-23 盤點）」。

## 2026-10-09 改名、簡轉繁與 ADR-0012 第一階段（release `b93be3d-channel-p1`）

來源：分支 `test-env-overhaul` 的 `b93be3d` 加上尚未 commit 的工作目錄。正式環境未變更。2026-10-09 20:2x（台北時間）已部署：`current` 指向 `releases/b93be3d-channel-p1`；payment、manager、merchant 皆 healthy，manager 登入驗證碼 API 回 200，公開回呼 ryo／jay／chi／jhd 在 payment 重建後皆回 400（未 reload `callback-ingress`，TD-018 的修正首次以實際重建驗證）。渠道帳號 `CAHOUSERYO01`、`CAHOUSECHI01`、`CAHOUSEJAY01` 已建立並派發給 `A_HOUSE`，兩個既有商戶已歸到 `A_HOUSE`。回滾：把 `current` 指回 `releases/b93be3d-no-referrer` 後 `up -d --no-deps payment manager merchant manager-ui`，並依三個 SQL 檔尾的回滾段落還原。

| 項目 | 內容 |
| --- | --- |
| 更新服務 | payment、manager、merchant、manager-ui；DB、Redis、MQ、merchant-ui、callback-ingress 不動 |
| 改名 | 超級管理員 → 上帝、高級代理 → 團長、一般代理 → 隊長；費用層顯示為團長費、隊長費。代碼（`SR_AGENT`、`AGENT`、`agent_level`、角色 ID）不變 |
| 簡轉繁 | 後端回應訊息、操作日誌名稱與 `init.sql` 種子顯示名稱改為繁體；種子名稱以測試環境資料庫的人工翻譯為準。未轉：程式註解、Swagger 註記、log、`ChannelNoticeController`、`PayOrderProcessService`、`AbstractPayOrderController`（RED）與 `MchDivisionReceiverBindController` |
| ADR-0012 第一階段 | 新增 `t_channel_account`、`t_channel_account_agent`、`t_agent_info.is_house`；金鑰存 `t_pay_interface_config`（`info_type=4`）。既有商戶歸到 `A_HOUSE`（平台直屬），既有商戶應用金鑰複製成渠道帳號。下單仍依商戶應用取金鑰 |
| 選單 | 「代理管理」改為「團長管理／團長列表」，新增隱藏路由 `/agents/detail`（商戶、渠道、隊長）；「商戶管理」兩個選單改為隱藏路由（`MO`）；「服務商管理」停用；團長後台新增「渠道列表」 |
| 資源 | manager、merchant 的 `mem_limit` 由 384m 調為 512m（manager 於 2026-10-09 18:17 被 cgroup OOM 砍掉重啟一次） |
| DB | 依序執行 `20261009-rename-roles.sql`、`20261009-zh-tw-display-names.sql`、`20261009-channel-accounts.sql` |
| 驗證 | 後端 `mvn package` 全部測試通過；隔離環境（全新資料庫）`scripts/smoke-channel-accounts.py` 41/41 PASS，涵蓋金鑰遮罩、派發與共用規則、團長／隊長越權、搬遷腳本可重複執行。畫面未做瀏覽器逐頁驗證 |
| 已知限制 | 金鑰在第二階段切換前有兩份明文（商戶應用與渠道帳號），加重 TD-001 |

### 2026-10-09 追加：渠道管理選單（release `91dcb24-channel-menu`）

只重建 manager-ui，執行 `20261009-channel-menu.sql`。上帝新增「渠道管理 → 渠道列表」（`/channels`），列出全部團長的渠道帳號，可在此新增並指定所屬團長；後端沿用第一階段的 `/api/channelAccounts`。部署後 11 個容器皆 healthy。回滾：`current` 指回 `releases/b93be3d-channel-p1` 後重建 manager-ui，並刪除 SQL 檔尾所列兩筆選單。

### 2026-10-09 追加：大渠道選單與渠道使用範圍（release `91dcb24-channel-scope`）

重建 manager、manager-ui，執行 `20261009-channel-scope.sql`；另以 `20261009-channel-menu.sql` 末段把「支付介面」搬到「渠道管理」底下並改名「大渠道」（只改資料庫）。新增 `t_channel_account_scope` 與 `PUT /api/channelAccounts/{id}/scope`。隔離環境 `scripts/smoke-channel-accounts.py` 49/49 PASS；部署後 11 個容器皆 healthy。使用範圍在第二階段前只記錄與顯示，不影響下單。回滾：`current` 指回 `releases/91dcb24-channel-menu` 後重建 manager、manager-ui，並 `DROP TABLE t_channel_account_scope`。

## 2026-09-29 test-env-overhaul 部署

來源：分支 `test-env-overhaul`（`a3594721af26`；後端 JAR 建於 `e3cda9c`，其後僅前端與 SQL 變更）。正式環境未變更。

| 項目 | 內容 |
| --- | --- |
| Release | `/opt/jee8pay-v2-dev/releases/a3594721af26-overhaul`（`current` 指向此處） |
| 更新服務 | payment、manager、merchant、manager-ui、merchant-ui（映像標籤 `a3594721af26`）；其餘容器未重啟 |
| 移除 | `cashier` 容器與 `artifacts/ui-cashier`；健康容器數 11 → 10（`bin/validate-sandbox-edge` 已同步） |
| DB | 依序執行 `deploy/jee8pay-v2-dev/sql/` 的 `20260928-remove-china-providers.sql`、`20260929-add-jhd-definition.sql`、`20260929-planned-feature-entitlements.sql`、`20260929-wallet-placeholder-menus.sql`；執行前備份於 `state/overhaul-20260929/pre-cleanup-tables.sql.gz` |
| callback-ingress | 換上 repo 版 `config/callback-ingress.conf`（新增 `/api/pay/notify/jhd`），只重建該容器 |
| 驗證 | 隔離環境（`jee8pay-smoke`，驗完已銷毀）三服務啟動與路由冒煙 PASS；部署後 10/10 healthy、`validate-sandbox-edge` PASS、`run-d01-blackbox.py`（RYO_IBON）23 項 PASS、Provider Create 呼叫 0 次 |

事故紀錄：第一次部署時 payment 因誤刪 `jeepay/conf/devCommons/config/application.yml`（三個 pom 以 resource 打包進 JAR）而出現 bean 循環依賴啟動失敗，已回滾（payment 停擺約 6 分鐘），還原該檔（`e3cda9c`）並經隔離環境驗證後重新部署。

**2026-10-03 已套用（TD-014 Resolved）**：operator 執行 `scripts/apply-edge-jhd-route.sh`，edge 現行 SHA `7a393f33…`，公開 jhd 回呼回 400。以下為當時的說明與手動步驟，保留作紀錄。原文：公開 edge 的 jhd APN 路由（TD-014）。測試環境的 `ccat-v2-dev.nnviopp.com/api/pay/notify/jhd` 目前回 404（callback-ingress 已支援，edge 設定尚未更新）。reconcile 腳本需人工核准旗標，請由 operator 執行：

```bash
ssh -tt nnviopp-sandbox
D=/opt/jee8pay-v2-dev/merchant-uat; BIN=/opt/jee8pay-v2-dev/bin; BK=/opt/jee8pay-v2-dev/state/overhaul-20260929
sudo cp -p $D/nginx.proposed.conf $BK/nginx.proposed.conf.pre-jhd; sudo cp -p $D/prepare-edge-nginx.py $BK/prepare-edge-nginx.py.pre-jhd
sudo cp -p $BIN/reconcile-sandbox-edge $BK/; sudo cp -p $BIN/validate-sandbox-edge $BK/
# 將 repo 的 deploy/jee8pay-v2-dev/merchant-uat/prepare-edge-nginx.py 安裝為 $D/prepare-edge-nginx.py（root 0700）後：
sudo python3 $D/prepare-edge-nginx.py --origin-mode dns-only   # 預期 PROPOSED_SHA256=7a393f332a6830c932a4e51b1754165af2be652b61a31640edcfbd79ca328ea4
sudo chown root:10002 $D/nginx.proposed.conf && sudo chmod 0640 $D/nginx.proposed.conf
sudo sed -i "s/^readonly expected_config_sha=.*/readonly expected_config_sha=7a393f332a6830c932a4e51b1754165af2be652b61a31640edcfbd79ca328ea4/" $BIN/reconcile-sandbox-edge $BIN/validate-sandbox-edge
sudo env SANDBOX_EDGE_RECONCILE_APPROVED=YES $BIN/reconcile-sandbox-edge && sudo $BIN/validate-sandbox-edge
```

新舊設定的差異僅為新增 `location = /api/pay/notify/jhd`（`dns-only` 模式；現行 SHA `840afb1a…`）。回滾：還原上述備份後再執行一次 reconcile。

整體回滾（應用層）：將 `current` 指回 `releases/de94ab8fd19d-mgrui-20260929`，以其 compose 執行 `up -d --no-deps payment manager merchant manager-ui merchant-ui cashier`；DB 設定資料可由備份還原。

## 2026-09-30 test-env-overhaul 部署（ADR-0009 與頭像）

| Release | 內容 | 更新服務 | DB（`deploy/jee8pay-v2-dev/sql/`） |
| --- | --- | --- | --- |
| `a5378620627a-merchant-shells` | 刪除商戶平台空殼頁 | merchant、merchant-ui | `20260930-remove-merchant-shell-menus.sql` |
| `72e41d7be676-agent-fee-p1` | ADR-0009 第一階段：代理、商戶綁定、四層費率 | manager、manager-ui | `20260930-agent-fee-waterfall-phase1.sql` |
| `66e2ee8d7d2b-agent-fee-p2to4` | ADR-0009 第二～四階段：快照表、代理後台（登入預設關閉）、範本／批次／雙人覆核／風險檢查 | manager、manager-ui | `20260930-agent-fee-waterfall-phase2to4.sql` |
| `75a7f8941a1e-local-avatars` | 預設頭像改本地圖、修正兩個 UI 的 favicon／載入 logo 404 | manager、merchant、manager-ui、merchant-ui | `20260930-local-default-avatars.sql` |
| `d280fd030ffc-method-security` | 使用者核准：啟用 `@PreAuthorize`（TD-015）、下單寫入費率快照（TD-017）、代理登入預設開啟 | payment、manager、merchant、manager-ui | 無（`ENT_UAT_EDGE_ALLOWLIST` 測試環境已存在，只補 `init.sql`） |
| `79df3f7a1c10-wallet` | ADR-0010：錢包、T+N 結算、提現、風控黑名單、人工調帳；推薦佣金層；UI nginx 重新解析（TD-013） | payment、manager、merchant、兩個 UI | `20260930-wallet-settlement.sql` |
| `913befd87796-route-export` | ADR-0011：別名路由（IBON）與決策紀錄；兩個平台的下載中心（背景匯出） | payment、manager、merchant、兩個 UI | `20260930-way-route-export.sql` |
| `96b04e94998e-review-fixes` | 逆向驗證修正：seed 權限漂移、提現並行冪等、金額格式、操作者名稱長度、有餘額不可刪除、匯出參數白名單 | payment、manager、merchant | `20260930-seed-drift-entitlements.sql`（測試環境無變動） |
| `96b04e94998e-callback-resolver`（2026-10-03） | `callback-ingress.conf` 改用 Docker DNS 於請求時解析 payment（TD-018）；其餘檔案與 `96b04e94998e-review-fixes` 相同（hardlink 複製） | callback-ingress | 無 |
| `96b04e94998e-ui-brand-titles`（2026-10-03；取代同日的 `96b04e94998e-ui-branding`、`96b04e94998e-ui-brand-images`） | 移除上游「介面市場／Plus商業版」按鈕與支付介面頁的推銷橫幅；頁尾與載入畫面改為「三把扇科技」；換上品牌圖（主 logo、側欄展開／收合圖示、品牌文字、favicon、預設頭像、登入背景）；分頁標題改為「三把扇-營運平台」「三把扇-商戶平台」 | manager-ui、merchant-ui | 無 |
| `96b04e94998e-history-query-guard`（2026-10-03；取代同日的 `96b04e94998e-history-query`） | 代收查詢送出搜尋後才顯示列表與匯出，匯出加防呆（需先搜尋、條件未變更、有日期區間、筆數上限、確認視窗）；以「歷史查詢」（代收查詢、代付查詢佔位頁）取代「下載中心」；訂單管理只顯示今日訂單、不含搜尋；匯出與「匯出紀錄」併入各查詢頁；後端未變動 | manager-ui、merchant-ui | `20261003-history-query-menus.sql`（備份於 `state/history-query-*/pre-entitlement.sql`） |
| `96b04e94998e-agent-credential`（2026-10-03） | 新增代理時一併開通登入帳號（必填登入帳號與聯絡手機），密碼改為 8 碼隨機並以一次性視窗顯示（附複製按鈕）；「開通登入帳號」同樣改為隨機密碼；操作日誌遮蔽 `initPassword`。部署前於隔離環境 `jee8pay-smoke`（全新資料庫，驗完已銷毀）端到端 13 項 PASS | manager、manager-ui | 無 |
| `96b04e94998e-agent-portal-menus`（2026-10-03） | 代理後台由單頁分頁改為五個左側選單（錢包與提現、分潤、旗下商戶、下級代理、費率），代理帳號登入後直接顯示在第一層；平台帳號（含超管）不再顯示「代理後台」 | manager-ui | `20261003-agent-portal-menus.sql`（備份於 `state/agent-portal-menus-*/pre-entitlement.sql`） |
| `96b04e94998e-family-tree-p1`（2026-10-03） | 家族樹權限第一階段：代理後台新增「訂單」（只含自己與下級代理直屬商戶）、「操作紀錄」（下級代理帳號的操作）；高級代理可新增下級代理、代理可新增商戶（隨機一次性密碼）；一般代理改用獨立角色 `ROLE_AGENT_PORTAL_L2`。範圍一律由登入者的代理路徑推導。部署前於隔離環境 `jee8pay-smoke`（全新資料庫，含 merchant 服務，驗完已銷毀）端到端 31 項 PASS，含越權測試 | manager、manager-ui | `20261003-agent-family-tree.sql`（備份於 `state/family-tree-p1-*/pre-rbac.sql`） |
| `96b04e94998e-family-tree-p2`（2026-10-03） | 家族樹權限第二階段：超管代理列表新增家族樹卡片；代理後台新增旗下錢包（含凍結／解凍）、提現審核（同意註記／駁回，撥款仍由平台）、統計報表、通道路由、黑名單、品牌設定（白標）、登入紀錄；費率頁可設高代費、代理費、推薦佣金與商戶覆寫並套用範本；商戶可更換歸屬與重設密碼；「下級代理」改名「旗下代理」。新增欄位 `t_agent_info.brand_*`、`t_withdraw_order.agent_approve_*`。部署前於隔離環境端到端 95 項 PASS（第一階段回歸 31＋第二階段 64，含越權測試） | manager、manager-ui | `20261003-agent-family-tree-p2.sql`（備份於 `state/family-tree-p2-*/pre-p2.sql`） |
| `b93be3d-no-referrer`（2026-10-03） | 移除推薦佣金：費率回到四層（平臺、渠道、高代、代理），拿掉推薦人綁定、REFERRER 費率層、結算拆帳的推薦人份額與相關畫面；三個後端服務同版重建。部署前於隔離環境端到端 99 項 PASS（含四層結算入帳），部署後 `run-d01-blackbox.py`（RYO_IBON）PASS、訂單數不變、`validate-sandbox-edge` PASS、公開四條回呼路由回 400 | payment、manager、merchant、manager-ui | `20261003-remove-referrer-commission.sql`（三個服務上線後才執行；移除 `referrer_agent_no`、`referrer_fee` 欄位；備份於 `state/no-referrer-*/pre-no-referrer.sql`） |

- 每次部署前都在隔離環境 `jee8pay-smoke`（驗完即銷毀）做開機與端到端冒煙：第一階段 17 項、第二～四階段 33 項、頭像 6 項、權限與快照全面回歸 42 項，都通過（權限回歸只有一項是腳本在 log 輸出前就檢查的時序誤判）；部署後 10/10 healthy。`d280fd030ffc` 部署後，`run-d01-blackbox.py`（M_D01_EXTERNAL_UAT、RYO_IBON）23/23 PASS，訂單數不變。
- 權限相關資料表部署前的備份放在 `state/overhaul-20260929/pre-*.sql`。
- 平臺費／渠道費、人工調帳都是雙人覆核，但測試環境目前只有一個啟用中的營運平台超管；使用前須先建立第二個具覆核權限的帳號。
- 分潤結算排程每 5 分鐘執行，只結算有快照的新訂單；T+N 與提現限額在「系統管理 → 系統配置 → 錢包與提現」調整。
- 代理登入由 `isys.agent-portal.login-enabled` 控制；TD-015 在測試環境修正後預設開啟。正式環境仍未修正 TD-015。
- 測試環境尚未有新下單，因此 `t_pay_order_fee` 目前為 0 筆；下一筆經授權的真實下單會產生第一筆快照。

## Current binding

JEE-E02 binds source `1f313e776d03c2383adff5aa96b9aac9b78efedc` to Development VPS `server1.nnviopp.com` as Compose project `jee8pay-v2-dev`. The runtime is under `/opt/jee8pay-v2-dev/`; it does not use `/opt/payment/`, V1 databases, V1 volumes, V1 application networks, or public ports 80/443.

> 2026-08-23 更新：V1 已退役並從本 VPS 移除（`/opt/payment` 封存於 `state/v1-retirement-20260822-222906/`），上述隔離敘述為歷史。V2 是此 VPS 唯一的支付平台。

Runtime source and deployment inputs are in [`deploy/jee8pay-v2-dev/`](../../deploy/jee8pay-v2-dev/). Generated JAR/UI artifacts and local secrets are ignored. The deployed release records source and artifact checksums in `/opt/jee8pay-v2-dev/SOURCE` and `/opt/jee8pay-v2-dev/current/DEPLOYMENT-MANIFEST.sha256`.

## Topology and access

| Service | Internal endpoint | Host bind | Purpose |
| --- | --- | --- | --- |
| Payment | `payment:9216` | `127.0.0.1:19216` | Payment API and Provider callback runtime |
| Manager | `manager:9217` | `127.0.0.1:19217` | Manager backend |
| Merchant | `merchant:9218` | `127.0.0.1:19218` | Merchant backend and V2 test receiver |
| ~~Cashier~~ | — | — | 已移除（2026-09-28 `test-env-overhaul`）：收銀台只服務中國通道，對外無路由 |
| Manager UI | `manager-ui:80` | `127.0.0.1:19227` | Manager UI |
| Merchant UI | `merchant-ui:80` | `127.0.0.1:19228` | Merchant UI |
| Callback ingress | `jee8pay-v2-callback:8080` | none | Exact CCAT APN path only |
| DB / Redis / RocketMQ | Compose-internal only | none | V2-only state and messaging |

Use SSH forwarding for operator access:

```bash
ssh -N nnviopp-sandbox \
  -L 19216:127.0.0.1:19216 \
  -L 19226:127.0.0.1:19226 \
  -L 19227:127.0.0.1:19227 \
  -L 19228:127.0.0.1:19228
```

## Resource budget and staged start

Compose hard limits total 3264 MiB including the one-shot volume initializer and callback ingress. (V1 已退役，部署 gate 不再需要 V1 reserve；歷史要求「至少 1536 MiB host/V1 reserve」與「V1 health 保持 11/11」已失效。)

```bash
cd /opt/jee8pay-v2-dev/current
export V2_SECRET_DIR=/opt/jee8pay-v2-dev/secrets
docker compose -p jee8pay-v2-dev -f compose.yml up -d db redis mq-namesrv
docker compose -p jee8pay-v2-dev -f compose.yml up -d mq-broker
docker compose -p jee8pay-v2-dev -f compose.yml up -d payment
docker compose -p jee8pay-v2-dev -f compose.yml up -d manager merchant
docker compose -p jee8pay-v2-dev -f compose.yml up -d cashier manager-ui merchant-ui callback-ingress
```

The VPS CPU cannot run the repository's arm64 SWR MySQL/JRE images or current `mysql:8.0` requiring x86-64-v2. V2 therefore uses host-validated amd64 `mariadb:10.11` and official multi-arch `eclipse-temurin:17-jre-jammy`. This is artifact compatibility, not V1 database reuse.

## Secrets and Provider gate

Infrastructure/JWT/test-app secrets are V2-only files under `/opt/jee8pay-v2-dev/secrets`, mode `0600`; values never belong in Git or reports. App-readable secrets use fixed UID/GID `10001`; the DB root secret remains root-owned. CCAT credentials are bound through native `t_pay_interface_config.if_params` with exactly `environment`、`custId` and `apiPassword`.

Missing or ambiguous CCAT credentials leave the Provider gate closed. Never copy values from V1、conversation、logs、shell history or documentation. Token probing is limited to one non-transactional request only after environment/account scope and redaction are verified.

The V2-only intake is `/opt/jee8pay-v2-dev/secrets/ccat-provider/`, owned by `root:root` with mode `0700`. Its `environment`、`custId` and `apiPassword` files are mode `0600`. Populate or rotate it only with:

```bash
ssh -tt nnviopp-sandbox 'sudo -n /opt/jee8pay-v2-dev/bin/populate-v2-ccat-secret'
```

The root-only helper reads values from the TTY、suppresses password echo and confirmation、and never accepts a secret in argv or prints one. Select exactly `TEST` or `PRODUCTION`; a Development platform does not imply a CCAT environment. The intake is never mounted into an application container or committed to Git.

JEE-E02 validated a `PRODUCTION` intake、provisioned exactly one `APP_E02_CCAT_DEV / ccat` native config row and completed exactly one successful standalone Token authentication on 2026-08-13. The one-shot marker is `/opt/jee8pay-v2-dev/state/ccat-token-probe-attempted`; it prevents a second probe. Post-probe exact-value and token-pattern scans found no V2 log exposure. `t_pay_interface_config.if_params` at-rest protection remains TD-001.

The first authorized TWD 40 unified-order attempt created local PayOrder `P2087588849919840258` in `INIT` but did not reach CCAT. Root cause TD-011 was a Provider integration defect: Create、Query and APN read the context's cache-populated map instead of the native cache-aware `ConfigContextQueryService.queryNormalMchParams` source. A Provider-local resolver now uses that native query in all three paths, so both cache modes retain `t_pay_interface_config.if_params` as the single source of truth and missing/malformed/wrong-bound params fail closed.

TD-011 regression evidence is 53/53 CCAT tests、57/57 backend tests、compile/package PASS. V2 payment release `1f313e776d03c2383adff5aa96b9aac9b78efedc-td011-134c78229b7a` deploys artifact SHA-256 `134c78229b7ac25a15e358ea3d4e1e7d284da526bea3577a43da18c70ddcd94c`; its 169-entry manifest verifies completely. Runtime remains `isys.cache-config=false`、CCAT config validation PASS、V2 11/11 healthy and exact-secret/token log hits 0.

The old INIT order is intentionally retained unchanged as a test artifact. Do not recover、delete or update it and do not add lifecycle functionality for it. TD-012 remains nonblocking because native reissue does not automatically Create for INIT. A separately authorized new native order may proceed without treating the artifact as a runtime blocker.

After the TD-011 deployment and a fresh minimal preflight, E02 invoked the native Merchant unified-order endpoint exactly once for one newly authorized TWD 40 order. PayOrder `P2087602494821605377` reached native `ING` / WAITING and returned a valid ibon payment instruction expiring 2026-08-20. The root-only one-shot marker and response are under `/opt/jee8pay-v2-dev/state/ccat-authorized-new-order-*`; do not submit another Create.

The human payment completed on 2026-08-13. CCAT first sent status `A`, which passed validation and retained WAITING, then status `B`, which passed checksum、account、order、transaction、amount and authenticated Query reconciliation. Native `ChannelNoticeController` changed the order from `ING` to `SUCCESS`, stored Provider transaction reference `2026081300245913` and returned the CCAT `OK` ACK. Native Merchant Notify created exactly one record; its first MQ delivery received `SUCCESS` and required no retry. Final V1 and V2 health remained 11/11 each, and post-payment exact-secret/token log scans remained zero.

## Public callback binding

Bound callback URL:

```text
https://ccat-v2-dev.nnviopp.com/api/pay/notify/ccat
```

JEE-E02 applied the explicitly approved additive Sandbox control-plane delta on 2026-08-13. The hostname is a DNS-only A record to `159.198.40.128` with TTL 300. Certificate `nnviopp-sandbox-edge` contains the prior three names plus the new hostname. Only the exact APN path reaches `jee8pay-v2-callback:8080`; root and nested paths return `404`. Existing V1 host smoke remains `404 / 200 / 404`, both V1 and V2 remain 11/11 healthy, and the V1 edge was never stopped or recreated.

Applied evidence:

```text
edge/nginx.conf SHA256 = 10a4877269bae2e624e26554a166078bff441545a26ef87f26d1549f3fe1c3a4
compose.sandbox-edge.yaml SHA256 = 492531f744a3109d663d7094e7dfe526618fc0f4bb3a957a92865a0d513aceda
certificate SANs = ccat-v2-dev.nnviopp.com, merchant-sandbox.nnviopp.com, sandbox-api.nnviopp.com, sandbox.nnviopp.com
V2 upstream = jee8pay-v2-callback:8080
exact APN path = /api/pay/notify/ccat
public exact-path invalid probe = HTTP 400, same response as direct V2 transit probe
wrong-path probes = HTTP 404
V1 edge restart count = 0
```

The pre-apply DNS and routing plans both passed. Root-only snapshots of the absent DNS state, original V1 edge inputs, certificate/SANs, container state and checksums are under `/opt/jee8pay-v2-dev/state/public-callback-preapply-20260813/`. The prior V2 `paySiteUrl` is captured separately. No secret value is stored in this documentation.

P04 sends `apn_url` dynamically on each `CvsOrderAppend`: `CcatIbon.pay` builds the Append request with `getNotifyUrl()`, and the native URL is `DBApplicationConfig.paySiteUrl + /api/pay/notify/ccat`. V2-only `paySiteUrl` is now `https://ccat-v2-dev.nnviopp.com`; no CCAT contractual-member portal mutation is required.

The zero-stop runtime mount described above is historical E02 evidence. JEE-N01 recreated only the edge with the V2-owned Compose overlay、durable read-only final config and stable transit network, then passed edge-only recreate and Docker-managed restart regression. TD-010 is resolved. Current reconciliation and External readiness procedures are in [`sandbox-edge-recovery.md`](sandbox-edge-recovery.md).

Read-only validation（V1 hostnames 已退役，不再檢查）：

```bash
getent ahostsv4 ccat-v2-dev.nnviopp.com
openssl s_client -connect 159.198.40.128:443 -servername ccat-v2-dev.nnviopp.com </dev/null 2>/dev/null \
  | openssl x509 -noout -ext subjectAltName
curl -sS -o /dev/null -w '%{http_code}\n' https://ccat-v2-dev.nnviopp.com/
curl -sS -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' \
  --data '{}' https://ccat-v2-dev.nnviopp.com/api/pay/notify/ryo
```

> 下列 E02 時期 Rollback（restore three-SAN certificate / delete DNS record）為**退役前歷史程序**：V1 已退役，cert 現收斂 3 個 V2 域名，deploy/renew hook 為 `/opt/jee8pay-v2-dev/scripts/sync-edge-certificate.sh`；`rollback-v2-callback-edge-hot` 與 `manage-sandbox-ccat-v2-dns.sh rollback` 不再適用於現況。

Rollback, in reverse ownership order:

```bash
# Restore the V2-only origin first.
cd /opt/jee8pay-v2-dev/current
sudo docker compose -p jee8pay-v2-dev exec -T db sh -lc \
  'export MYSQL_PWD="$(cat /run/secrets/db-root-password)"; exec mariadb -uroot' <<'SQL'
UPDATE jee8pay_v2_dev.t_sys_config
SET config_val = 'http://127.0.0.1:9216'
WHERE config_key = 'paySiteUrl'
  AND config_val = 'https://ccat-v2-dev.nnviopp.com';
SQL

# Remove the hot config mount, gracefully reload the original config and detach transit.
sudo /opt/jee8pay-v2-dev/bin/rollback-v2-callback-edge-hot

# Restore the original three-SAN certificate.
sudo certbot certonly --non-interactive --cert-name nnviopp-sandbox-edge \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/nnviopp-sandbox/cloudflare-token.ini \
  --dns-cloudflare-propagation-seconds 30 \
  --pre-hook /bin/true --post-hook /bin/true \
  --deploy-hook /opt/payment/payment-service-sandbox/scripts/sync-sandbox-edge-certificate.sh \
  --force-renewal \
  -d merchant-sandbox.nnviopp.com \
  -d sandbox-api.nnviopp.com \
  -d sandbox.nnviopp.com

# Delete only the E02-created DNS record using the captured absent pre-state.
sudo env SANDBOX_CCAT_V2_DNS_ROLLBACK_APPROVED=YES \
  /opt/jee8pay-v2-dev/bin/manage-sandbox-ccat-v2-dns.sh \
  rollback /etc/nnviopp-sandbox/cloudflare-token.ini \
  /opt/jee8pay-v2-dev/state/ccat-v2-dns-backup.json
```

## V1 retirement（2026-08-23 盤點）

V1（Go `payment-service` 四方聚合支付）與 V2（JeePay Java）是兩套獨立平台。2026-08-23 完成 dev VPS 的 V1 完整退役與清理：

| 項目 | 狀態 |
| --- | --- |
| V1 公開入口（sandbox-api/sandbox/merchant-sandbox.nnviopp.com） | 已關閉（edge 移除 server blocks 回 000；DNS records 已刪，現 NXDOMAIN） |
| `nnviopp-sandbox` containers（api/admin/mysql/squid egress/dbeaver proxy） | 已停止；`docker compose -p nnviopp-sandbox down` 全清（networks/volumes 已刪） |
| `merchant-sandbox-sandbox`（V1 merchant receiver） | 已停止；compose down 全清 |
| 孤兒 `nnviopp-production`（api/admin/mysql/edge，PUBLIC_BASE_URL=api.nnviopp.com） | 已停止；compose down 全清；其 cert 已 `certbot delete`，`/etc/nnviopp-production` 已刪 |
| V1 DB | `payment_sandbox`、`payment_production` 封存於 `state/v1-retirement-20260822-222906/db/`（root-only） |
| `/opt/payment` | 整包封存 `state/v1-retirement-20260822-222906/opt-payment-archive.tar.gz`（208MB，含 edge Dockerfile、baseline nginx.conf、edge image tar）後刪除 |
| `nnviopp-sandbox-edge` | 由 V2 standalone compose 接管（`edge/compose.edge.yaml`，project `jee8pay-v2-dev-edge`）；純 V2 config（`nginx.proposed.conf` SHA `7a393f33…`，含 ryo/jay/chi/jhd 四條 APN route） |
| cert SAN | 收斂 3 個 V2 域名（ccat-v2-dev / api-v2-dev / admin-v2-dev）；renew/deploy hook → `/opt/jee8pay-v2-dev/scripts/sync-edge-certificate.sh` |
| `eth0:0` alias（159.198.42.146） | 已移除（interfaces 檔與 runtime 均清） |
| `/etc/nnviopp-sandbox` | 僅保留 `cloudflare-token.ini` + `edge-tls`；V1 檔案（payment-service.env、ccat-provider.env、admin creds 等）封存於 `state/v1-retirement-20260822-222906/etc-nnviopp-sandbox-archive/` |
| Docker 清理 | V1 images、舊 V2 release images、build cache、33 個孤兒 volumes 全清；舊 release dirs 移除（保留 current + 1f313e rollback 兩代） |

退役前快照：`state/v1-retirement-20260822-222906/`（DNS records、edge config、cert、containers/volumes/networks/images inventory、DB dumps、/opt/payment archive、V2 health baseline）。

後續維護注意：

- edge reconcile/validate：`/opt/jee8pay-v2-dev/bin/reconcile-sandbox-edge` / `validate-sandbox-edge`（需 `SANDBOX_EDGE_RECONCILE_APPROVED=YES`）；config 由 `merchant-uat/prepare-edge-nginx.py`（自足純 V2）生成，SHA 固定於腳本內。
- edge container 名沿用 `nnviopp-sandbox-edge`（cert deploy hook 參照），但專案已是 `jee8pay-v2-dev-edge`。
- V1 如需還原：`opt-payment-archive.tar.gz` + DB dumps 在 `state/v1-retirement-20260822-222906/`；V1 已不影響 V2 任何路由/資源。

## Merchant Notify receiver

V2 uses the native Merchant test receiver:

```text
http://merchant:9218/api/anon/paytestNotify/payOrder
```

It validates the native JeePay signature against the synthetic V2 app and returns exact `SUCCESS`. The deployment raises both JeePay signing-helper loggers to WARN because upstream INFO logging can include signing preimages; source-level remediation remains TD-009.

## Rollback and inspection

Rollback only the TD-011 payment artifact without touching V1 or V2 stateful services:

```bash
sudo ln -s /opt/jee8pay-v2-dev/releases/1f313e776d03c2383adff5aa96b9aac9b78efedc \
  /opt/jee8pay-v2-dev/current.rollback
sudo mv -T /opt/jee8pay-v2-dev/current.rollback /opt/jee8pay-v2-dev/current
cd /opt/jee8pay-v2-dev/current
export V2_SECRET_DIR=/opt/jee8pay-v2-dev/secrets
sudo -E docker compose -p jee8pay-v2-dev -f compose.yml up -d --no-deps payment
```

Stopping V2 preserves its volumes and does not touch V1:

```bash
cd /opt/jee8pay-v2-dev/current
export V2_SECRET_DIR=/opt/jee8pay-v2-dev/secrets
docker compose -p jee8pay-v2-dev -f compose.yml stop
```

Inspect without printing environment values:

```bash
docker compose -p jee8pay-v2-dev -f /opt/jee8pay-v2-dev/current/compose.yml ps
docker stats --no-stream --filter label=com.docker.compose.project=jee8pay-v2-dev
docker inspect --format '{{.Name}} restarts={{.RestartCount}} oom={{.State.OOMKilled}}' \
  $(docker ps -q --filter label=com.docker.compose.project=jee8pay-v2-dev)
```
