# Technical Debt Register

Status vocabulary：`Open`、`In Progress`、`Resolved`、`Accepted`。

| ID | Debt | Severity | Status | Scope | Evidence | Exit Condition |
| --- | --- | --- | --- | --- | --- | --- |
| TD-001 | Provider credential at-rest protection | Unrated | Open | `t_pay_interface_config.if_params` | [`PayInterfaceConfig.ifParams`](../../jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/entity/PayInterfaceConfig.java#L82) is configuration JSON; the manager controller calls [`deSenData()`](../../jeepay/jeepay-manager/src/main/java/com/jeequan/jeepay/mgr/ctrl/merchant/MchPayInterfaceConfigController.java#L116) while preparing a response, which does not prove DB field-level encryption. No formal threat assessment is recorded. | Complete a separate security design, implementation, and verification of credential at-rest protection. |
| TD-002 | Taiwan zh-TW user-facing localization remains incomplete | Medium | Open | Manager、Merchant、Cashier UI and generic API messages | Semantic audit found broad Simplified Chinese presentation. JEE-B01 changes platform defaults and high-impact locale/currency surfaces only; identifiers and Provider contracts remain compatible. | Complete a dedicated screen-by-screen zh-TW content review with UI regression tests. |
| TD-003 | China mobile-number validation is embedded in generic user/account flows | High | Open | `RegKit.REG_MOBILE` and frontend form rules | Generic regex `^1\\d{10}$` rejects Taiwan mobile numbers; changing authentication/user identity behavior requires an explicit compatibility and migration plan. | Define a region-aware phone contract, migrate stored identities safely, and test manager/merchant authentication flows. |
| TD-004 | Physical Development/Production binding is incomplete | High | In Progress | Domains、hosts、DB、Redis/MQ、callback hosts、secret injection | JEE-E02 binds isolated Development runtime、V2-only CCAT `PRODUCTION` credentials and Development callback to `server1.nnviopp.com`. JEE-E04 binds an isolated `jee8pay-v2-production` Candidate to `server1.lp33ing.com` with fresh V2-only DB/Redis/MQ、loopback-only application ports、independent infrastructure secrets and Production environment identity; JEE-I05 independently accepts its provenance、health、isolation、rollback and V1 non-interference. Production CCAT credential/config intake and public callback activation remain Human Gates. | Provision an approved Production Merchant/Application CCAT config from the V2-only secret intake, activate and validate the reviewed candidate callback route, then complete Production pilot/cutover acceptance without fallback or V1 data reuse. |
| TD-005 | CCAT ibon safety-critical contract is incomplete | Blocking for CCAT runtime | Resolved | Create、Query、APN | JEE-C03 reviewed merchant-authenticated official specification V1.28.1. B1 amount、B2 runtime-safe state mapping、B3 account/order/transaction binding、B4 retry/replay and B5 duplicate/timeout recovery are resolved; the 31-item DoR has no blocking row and the Runtime Gate is open. Sample-only `process_code=2`、ACK HTTP metadata and nonce freshness remain explicitly non-blocking in the Provider evidence document. | Met by [`contract-evidence.md`](../providers/ccat/contract-evidence.md): authenticated page-level provenance、safe unknown-code handling、Query-authorized APN design and same-key Create recovery are recorded. |
| TD-006 | Dashboard and division aggregates are not currency-aware | Medium | Open | Manager/Merchant dashboards and division-record presentation | Aggregate responses and `PayOrderDivisionRecord` expose amounts without a currency dimension. Taiwan-facing UI can use the TWD platform default, but mixed-currency totals cannot be labeled reliably. | Define currency-aware aggregation and division presentation without changing the generic Merchant API into TWD-only. |
| TD-007 | Existing-database TWD migration is not supplied | High | Open | Existing deployments and `jeepay/docs/sql/patch.sql` | Fresh `init.sql` defaults are TWD, but the historical v1.6 transfer-table patch still creates a CNY default and no verified migration updates existing defaults or rows. | Design、review and execute an environment-specific migration with backup、rollback and post-migration currency verification. |
| TD-008 | Legacy China adapters do not consistently validate order currency | High | Open | WxPay、XxPay、YsfPay and other upstream China Provider paths | Some adapters emit a fixed CNY currency while generic orders retain an explicit currency; enabling them for a TWD order could create a local/provider currency mismatch. They are outside the active Taiwan roadmap. | Keep these Providers disabled for Taiwan until each adapter rejects unsupported currencies before any Provider call and has currency-mismatch regression tests. |
| TD-009 | Generic signing helper logs the signing preimage | High | Resolved | Merchant notification and other callers of `com.jeequan.jeepay.core.utils.JeepayKit.getSign` | The helper logs `signStr` at INFO after appending `key=<merchant app secret>`. E02 suppresses this specific logger in the V2 deployment configuration, but the source-level exposure remains. | Remove secret-bearing signing-preimage logging in a separately reviewed shared-core security change and add a runtime log regression test. |
| TD-010 | V2 callback edge binding is not restart-durable | Medium | Resolved | Development Sandbox edge runtime | JEE-N01 replaced the runtime-only mount/manual network attachment with a SHA-locked Compose overlay、durable read-only host config、stable-name transit declaration、`unless-stopped` and a healthcheck covering config、80/443 and both V2 ingresses. Two edge-only recreates and one Docker-managed PID 1 restart preserved config/network/routes; V1/V2 health remained intact. | Resolved by [`sandbox-edge-recovery.md`](../operations/sandbox-edge-recovery.md). Full host reboot smoke remains maintenance acceptance and is not claimed as tested. |
| TD-011 | CCAT runtime reads an empty Provider-param map when config cache is disabled | Blocking for CCAT live create/query/APN | Resolved | `CcatIbon`、`CcatPayOrderQueryService`、`CcatChannelNoticeService` with `isys.cache-config=false` | The CCAT adapter had bypassed the native cache-aware config query and read `MchAppConfigContext.normalMchParamsMap` directly. All three paths now use a Provider-local resolver backed by `ConfigContextQueryService.queryNormalMchParams`, preserving `t_pay_interface_config.if_params` as the single source of truth. Cache-enabled、cache-disabled、missing、malformed、incomplete、wrong-binding and no-secret-output regressions pass; full backend tests、compile and package pass. The fixed artifact is deployed to V2 with cache disabled, config validation PASS and V2 11/11 healthy. A fresh read-only review found no blocking issue. | Verified by 53/53 CCAT tests、57/57 backend tests、compile/package PASS、independent review PASS and deployed artifact SHA-256 `134c78229b7ac25a15e358ea3d4e1e7d284da526bea3577a43da18c70ddcd94c`. |
| TD-012 | No supported native entry point can resume an arbitrary existing INIT PayOrder through Provider Create | Nonblocking | Open | Generic operator recovery semantics | Core has a protected existing-order overload, but its only native caller is QR cashier and dispatches only `ALI_JSAPI` / `WX_JSAPI`; generic reissue accepts `ING` and performs Query. E02 intentionally leaves test PayOrder `P2087588849919840258` unchanged as a local test artifact with Provider Append 0. It does not block CCAT E2E using a separately authorized new native order. | No action for the E02 test artifact. Revisit only if a future business requirement explicitly needs generic operator recovery for arbitrary INIT orders; do not add CCAT-specific lifecycle behavior. |

本 register 不宣稱目前一定沒有其他 storage-layer protection；結論是「尚未證明有 field-level encryption」。JEE-G01 不修復此 debt。

## 2026-08-16 已解決（Resolved）

| ID | 債 | 修復 |
| --- | --- | --- |
| TD-009 | 簽名 preimage 含 secret log | `JeepayKit.getSign` 只記錄不含 key 的參數 |
| TD-011 | CCAT cache-disabled config | 已於 E02 解決（既有） |
| TD-010 | edge 非 restart-durable | 已於 N01 解決（既有） |
| C1 | 後端 API 訊息簡體 | 外層 API 訊息繁中化（payment/ccat/core RQ），同步更新黑箱與 UAT notice |
| C2 | merchant API reqTime 無 freshness | `ApiController.getRQByWithMchSign` 加入 5 分鐘視窗（超出回 9999 請求時間戳已過期） |
| C8 | api-v2-dev 已 proxied 但文件寫 DNS-only | UAT notice / platform-access 更新為 Cloudflare proxied 模式 |

## 2026-08-16 待辦（Deferred / 需另立任務）

| ID | 債 | 原因 |
| --- | --- | --- |
| TD-001 | if_params field-level encryption | 需安全設計 + ADR（不 hotfix） |
| TD-003 | 手機驗證 regex 拒台灣門號 | 涉及 auth 行為，需相容/遷移計畫 |
| TD-007 | 既有 DB TWD migration | 需 migration 設計 + 驗證 |
| TD-008 | 中國 adapter 不驗證 currency | 需逐 adapter 修 + regression |
| TD-012 | 無原生入口恢復任意 INIT 訂單 | 需需求/設計決策 |
| C5 | CCAT reconciliation 任務 | APN 遺失/Query 失敗仍會滯留；需定期稽核 CLOSED 訂單的排程任務 |
| A1 | repo artifacts/manifest 與部署脫節 | 已更新 repo compose tags；artifacts 為 gitignored 需另行治理 |
| A2 | 文件落後 runtime | 測試數已更新；I07R2-P05 驗收報告未補（runtime/ gitignored） |
| D1-D4 | 營運：MQ memory / reboot 未測 / V1 退休 / prod 空資料 | 營運項 |
| 商戶自助 / 代付人工流程 / 結算模型 | 業務功能 | 需設計 + ADR + 實作任務 |
