# Merchant UAT Frontend Operator Map

本文件是 JEE-D01 的 read-only frontend／menu source mapping；沒有修改任何 frontend。實際 menu 由 `t_sys_entitlement` 動態產生，component mapping 位於各 UI 的 `src/config/appConfig.js`。

## Manager

| Operator task | Actual menu/page | D01 use |
| --- | --- | --- |
| Merchant | 商戶管理 → 商戶列表；route `/mch`，`MchListPage` | 檢查 `M_D01_EXTERNAL_UAT`、普通商戶、啟用狀態 |
| App | 商戶管理 → 應用列表；route `/apps`，`MchAppPage` | 檢查 `APP_D01_EXTERNAL_UAT`、啟用狀態；編輯畫面以 masked placeholder 顯示 App Secret |
| Provider config | 應用列表 → 支付配置 → 支付參數 | 檢查 App 的 `ryo` config 已啟用；敏感欄位由 params model 脫敏，不抄到文件 |
| Passage | 應用列表 → 支付配置 → 支付通道 | 檢查 `RYO_IBON → ryo` passage（JAY／CHI 同理）、rate、state |
| PayInterface | 支付配置 → 支付接口；route `/ifdefines` | 檢查 `ifCode=ryo` 定義與支援 wayCode |
| PayWay | 支付配置 → 支付方式；route `/payways` | 檢查 exact `RYO_IBON` casing/name |
| PayOrder | 訂單管理 → 支付訂單；component default `/payOrder` | 依 Merchant/App、`mchOrderNo` 或 `payOrderId` 查 WAITING／SUCCESS |
| Merchant Notify | 訂單管理 → 商戶通知；route `/notify` | 查 notify state/count/response；failed record 有 native 重發操作 |

`RYO_IBON` 沒有 custom Provider frontend；generic PayInterface config form 就是 canonical operator surface。

## Merchant

| Operator task | Actual menu/page | D01 relevance |
| --- | --- | --- |
| App / API credential | 商戶中心 → 應用管理；route `/apps` | 可看 App ID、編輯／rotate App Secret；detail API 只回 masked secret |
| Channel config | 應用管理 → 支付配置 | generic Provider config 與 passage 可見性取決於 entitlement；D01 credential 由我方 secure handoff 管理 |
| PayOrder | 訂單中心 → 訂單管理；component default `/payOrder` | 可查 Merchant 自己的 PayOrder 與狀態 |
| Notify status | 無獨立 Merchant Notify menu/page | Merchant UI 沒有 Manager 的 `/notify` list；由 Manager 或 API/logs 查 delivery |

D01 沒有建立 Merchant portal login credential；外部系統串接只需要 Merchant ID、App ID、App Secret，不需要登入 Merchant UI。

## Cashier

Cashier UI 的 actual routes 只有 WeChat、Alipay、YSF JSAPI；沒有 `RYO_IBON` page／route。`RYO_IBON` native UnifiedOrder 直接回 `payDataType=ryoIbon` 與 `payData`，不使用 JeePay Cashier URL。

Merchant UI 的 generic Pay Test modal 也只特別處理既有 QR／payurl types，沒有 `ryoIbon` renderer。因此本次外部 Merchant API flow 應自行 parse `payData` 顯示 payment code／expiry／可選 `shortUrl`，不依賴 JeePay frontend redesign。

## Source evidence

- Menu seed：`jeepay/docs/sql/init.sql` 的 Manager `ENT_MCH*`、`ENT_ORDER*`、`ENT_PC*` 與 Merchant `ENT_MCH_APP`／`ENT_PAY_ORDER`。
- Manager component map：`jeepay-ui/jeepay-ui-manager/src/config/appConfig.js`。
- Merchant component map：`jeepay-ui/jeepay-ui-merchant/src/config/appConfig.js`。
- Cashier routes：`jeepay-ui/jeepay-ui-cashier/src/router/index.js` 與 `src/config/index.js`。
- RYO response：`RyoIbonOrderRS`。
