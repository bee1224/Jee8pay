# ADR-0008 — 移除中國／海外支付通道與其衍生功能

Status: Accepted
Date: 2026-09-30

## Context

Jee8pay 繼承上游 JeePay 的 alipay、wxpay、ysfpay、xxpay、plspay、pppay 六個通道，以及只服務這些通道的功能：收銀台（`jeepay-ui-cashier` 與內嵌的預編譯收銀台）、條碼自動辨識、取渠道用戶 ID、支付寶子商戶授權、商戶端支付測試／轉帳／分帳頁面。

台灣平台實際只使用黑貓 PAY 的四個 ibon 上游（RYO／JAY／CHI／JHD）。舊通道從未在台灣啟用，卻帶來以下成本：
- 大量 SDK 依賴與攻擊面。
- 共用設定 context 中的 SDK wrapper 快取。
- 貨幣不一致風險（TD-008）。
- 中國手機格式驗證擋住台灣門號（TD-003）。
- 一個會把網址參數直接輸出成 HTML 的端點（`CommonController`，XSS 風險）。

## Decision

完整移除上述六個通道，以及只為它們存在的程式碼、SDK 依賴、前端頁面、seed 資料與選單權限。平台的支付方式與介面定義只保留四個 `*_IBON` 上游。手機號驗證改為台灣格式。

同時移除未使用的元件實作：ActiveMQ、RabbitMQ、阿里雲 RocketMQ 與阿里雲 OSS。實際部署只用 RocketMQ 與本機儲存。

以下 JeePay 核心能力保留不刪：
- 轉帳、分帳、退款的 API 與資料表。Taiwan 目前未啟用，呼叫時依 UAT 文件回傳 fail-closed 錯誤。
- ISV（服務商）模型。它將作為多層代理的基礎（見 ADR-0009）。

## Decision Drivers

- 平台範圍明確只含黑貓 PAY ibon（README 的 Product Scope）。
- 縮小依賴與攻擊面，移除已知 XSS 風險端點。
- 消除 TD-003、TD-008 的根源，而不是逐一修補不會啟用的程式碼。
- 讓管理與商戶介面只呈現真正可用的功能。

## Options Considered

### Option A — 完整移除（採用）

刪除通道、SDK、前端、seed 與選單，保留核心能力的通用 API。需要修改少量共用檔案：`ConfigContextService`、`UnifiedOrderRQ`、`AbstractPayOrderController` 的收銀台分支。

### Option B — 保留程式碼，只停用 seed／選單

diff 最小，但 SDK 依賴、XSS 端點、wrapper 快取與貨幣風險都仍留在產物中，未來還可能被誤啟用。

### Option C — 維持現況

零風險，但持續承擔上述成本。

## Consequences

### Positive

- 付款模組移除數十個第三方函式庫，產物與攻擊面都縮小。
- 共用設定 context 不再持有中國 SDK wrapper。
- 介面只剩可用功能，並新增四上游一致性守門測試（`ProviderParityContractTest`）。

### Negative / Trade-offs

- 與上游 JeePay 的差異擴大，日後同步上游需要人工處理衝突。
- 若將來真的要接中國通道，必須重新引入，不能再只是開啟設定。
- 使用已移除 wayCode（例如 `ALI_JSAPI`）的請求，錯誤訊息改為「不支援的支付方式」，UAT 文件已同步。
- 教訓：`jeepay/conf/devCommons/config/application.yml` 看似範例，實為 pom 打包進 JAR 的共用設定。清理時必須連建置設定一起檢查，部署前必須做啟動驗證。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../debt/technical-debt-register.md`](../debt/technical-debt-register.md)（TD-003、TD-008）
- [`../operations/ccat-v2-development.md`](../operations/ccat-v2-development.md)（2026-09-29 測試環境部署紀錄）
- [`ADR-0002-native-provider-extension-contract.md`](ADR-0002-native-provider-extension-contract.md)
