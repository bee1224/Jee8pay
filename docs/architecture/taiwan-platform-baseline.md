# Taiwan Platform Baseline

> **STATUS: STALE（部分內容已被後續證據取代）**
> 本文是 JEE-B01（2026-08-12）的 audit snapshot。其中「CCAT amount 轉換仍 blocked」與「`channelOrderNo` / `trans_id` UNKNOWN」兩項已由 JEE-C03 的認證規格與 CCAT 實作取代，以 [`../providers/ccat/contract-evidence.md`](../providers/ccat/contract-evidence.md) 與 [`../providers/ccat/provider-design.md`](../providers/ccat/provider-design.md) 為準。其餘 currency / locale / terminology / timezone 的分類結論仍有效。更新本文件時請先比對 CCAT 契約文件，避免覆寫已驗證結論。

## Scope and classification

JEE-B01 audited tracked files for currency、locale/region、payment terminology、timezone、phone/identity/address assumptions and classified findings by runtime meaning. Raw candidate counts overlap and include Provider-specific、generated and upstream historical content；they are not blind-replacement targets.

Priority is P0 Taiwan user-facing semantics、P1 platform defaults、P2 generic business terminology、P3 Provider-specific preservation、P4 legacy debt. Public APIs、Java/DB identifiers、packages、Bean names and Provider protocol fields remain compatible.

## Taiwan terminology matrix

| Current | Context | Class | Taiwan Candidate | Action | Reason |
| --- | --- | --- | --- | --- | --- |
| `CNY` / `cny` default | Fresh schema and internal test/query defaults | PLATFORM_DEFAULT | `TWD` / `twd` | MIGRATE | Taiwan-created orders must not silently default to CNY |
| `￥` / `¥` | Generic Manager/Merchant amount display | PLATFORM_UI | `NT$` | MIGRATE | Remove renminbi display assumption |
| 人民币 / 人民幣 | Generic API/entity currency description | LANGUAGE_LOCALIZATION | 新臺幣 example or generic ISO 4217 description | MIGRATE | Default/example is Taiwan-facing; contract remains explicit |
| 商户 | Generic UI/API wording | LANGUAGE_LOCALIZATION | 商戶 | DEBT / screen-by-screen migration | Broad UI copy requires dedicated visual and workflow regression |
| 服务商 | Generic Taiwan business wording | LANGUAGE_LOCALIZATION | 服務商 | DEBT / screen-by-screen migration | Preserve internal identifiers such as `isvNo` |
| 支付接口 / 支付通道 / 渠道 | Generic business terminology | BUSINESS_SEMANTIC_REVIEW | 支付介接 / 支付管道 / Provider | REVIEW | Translation depends on operator meaning; no mass rename |
| 回调 / 补单 / 对账 / 分账 / 代付 | Generic operations | BUSINESS_SEMANTIC_REVIEW | 回呼 / 補單 / 對帳 / 分潤 / 代付 | REVIEW | Some terms are domain conventions and need product-owner confirmation |
| hard-coded CNY in WxPay/XxPay/YsfPay | China Provider adapters | PROVIDER_SPECIFIC | none | KEEP_AS_IS | Provider protocol owns its currency requirement |
| TWD generic order reaching a legacy CNY adapter | Legacy Provider enablement | BUSINESS_SEMANTIC_REVIEW | adapter-level currency validation | DEBT | Several upstream China adapters emit CNY without validating the generic order currency |
| CNY payloads in signed upstream API examples | Runtime markdown API documentation | BUSINESS_SEMANTIC_REVIEW | TWD examples with recomputed safe signatures | DEBT | Changing currency alone invalidates the static signature examples |
| Currency-less dashboard and division aggregates | Generic Manager/Merchant UI contracts | BUSINESS_SEMANTIC_REVIEW | Currency-aware aggregation | DEBT | Existing aggregate/division responses do not expose a currency dimension; the Taiwan UI displays the platform default without asserting a TWD-only API contract |
| `mchNo`, `ifCode`, `channelOrderNo`, packages/classes | Runtime/API/DB contract | INTERNAL_IDENTIFIER | none | KEEP_AS_IS | Compatibility boundary |
| Upstream README、SDK docs、built cashier artifacts | Imported history/artifacts | UPSTREAM_HISTORICAL | none | KEEP_AS_IS | Not Taiwan Workspace current truth |
| `^1\\d{10}$` mobile validation | Generic account/auth flows | BUSINESS_SEMANTIC_REVIEW | Taiwan-aware phone contract | DEBT | A direct replacement could lock out existing users |
| `zh_cn` in Knife4j | Third-party documentation UI setting | UNKNOWN | `zh-TW` if supported | DEFER | Support was not established; do not break API docs |
| `Asia/Shanghai` in service containers | Platform runtime | PLATFORM_DEFAULT | `Asia/Taipei` | MIGRATE | Correct IANA regional identity |

Canonical Taiwan-facing wording uses `商戶`、`服務商`、`付款`、`退款`、`轉帳`、`通知`、`新臺幣` and `NT$`. Source identifiers are not renamed solely for localization.

## Currency contract

`UnifiedOrderRQ.currency`、`RefundOrderRQ.currency` and `TransferOrderRQ.currency` are explicit, required strings. Controllers persist the supplied value; no generic validation restricts them to CNY. Therefore TWD can pass the generic Merchant API and signing/persistence path, but each Provider adapter must validate and map only its supported currency.

```text
PLATFORM_DEFAULT_CURRENCY = TWD
MERCHANT_API_CURRENCY = explicit required field retained
PLATFORM_TWD_ONLY = NO
```

Fresh-install SQL defaults use lowercase `twd`; Java/tooling examples use uppercase `TWD` where the existing context used uppercase. Existing database rows/defaults are not migrated by editing `init.sql` and require a separate physical migration. Upstream historical `jeepay/docs/sql/patch.sql` still creates the v1.6 transfer table with a CNY default; it is not a TWD migration and must not be used to claim existing-database readiness.

## Internal amount semantics

`UnifiedOrderRQ.amount`、`PayOrder.amount`、refund/transfer amounts and fee amounts use integer `Long` values in the smallest currency unit; code and schema comments call this unit `分`. Existing Provider mappings divide by 100 with decimal-safe logic when a major-unit protocol requires it. JEE-B01 does not change the money domain.

```text
JEEPAY_INTERNAL_AMOUNT_UNIT = integer smallest currency unit (分)
```

CCAT conversion remains blocked until official evidence defines `order_amount`、query and APN units/scale. Mapping and comparison must be integer/`BigDecimal` safe, never floating point.

## Locale, timezone and region

- Platform service-container timezone default: `Asia/Taipei`.
- Manager/Merchant component、date and time locale plus Manager/Merchant/Cashier source HTML language: `zh-TW`.
- Knife4j `zh_cn` remains classified `UNKNOWN` pending confirmed library support.
- DB/JVM host timezone and existing timestamp data require physical environment verification.
- China mobile regex and Simplified Chinese UI breadth are tracked as technical debt; no schema/authentication redesign is included.

## Transaction and identifier semantics

`ChannelRetMsg` distinguishes `WAITING`、`CONFIRM_SUCCESS`、`CONFIRM_FAIL`、`UNKNOWN`、`API_RET_ERROR` and `SYS_ERROR`. A payment artifact or ibon code generation is `WAITING`, not payment success. Provider ambiguity remains `UNKNOWN`; no CCAT-specific PayOrder state is introduced.

| Meaning | JeePay field | CCAT candidate |
| --- | --- | --- |
| Merchant Order ID | `mchOrderNo` | local merchant identity; not sent unless contract says so |
| JeePay PayOrder ID | `payOrderId` | stable `cust_order_no` candidate |
| Provider Outbound Order ID | existing `payOrderId` mapping | `cust_order_no` |
| Provider Transaction ID | `channelOrderNo` | `UNKNOWN`; do not assume `trans_id` |

No Provider-specific transaction table is justified by current evidence.
