# CCAT Provider Design

## Status

`BLOCKED-BY-CONTRACT-UNKNOWNS` — 本文件是 P03 設計成果，不是 implementation plan 的批准。

已確認 JeePay extension contract 與部分 CCAT request surface；但可公開取得的 CCAT official SDK／WooCommerce implementation 沒有提供可安全採用的 APN authentication contract、amount unit contract、完整 status/process-code table 或 create idempotency contract。P04 不得用猜測補齊這些欄位。

## Scope

- Provider：CCAT／黑貓 PAY
- `ifCode`：`ccat`
- `wayCode`：`CCAT_IBON`
- Capability：Create ibon payment、Provider Query、Payment Notify / APN。

Non-goals：Refund、Transfer/Payout、Division、Channel User、Close Order、COCS、DPH、ATM、其他 CCAT product、custom Provider UI、secret-management refactor、JeePay payment-domain redesign。

## Source of Truth

Evidence priority is: CCAT official specification supplied to the merchant, CCAT official SDK, CCAT official WooCommerce repository, then local runtime code. The first item is not available in this workspace. Official source is authoritative for what it explicitly documents; an implementation is not authority for omitted fields or undocumented semantics.

`DOCUMENTATION DRIFT`: `docs/providers/ccat/README.md` says no usable official specification exists. That remains true for a complete protocol specification, but CCAT's official download page now links to public SDKs and an official WooCommerce repository. This document does not edit the README because its `Unknowns` are still materially correct.

## Evidence Inventory

| Source | Type | Version / revision | Relevant capability | Authority |
| --- | --- | --- | --- | --- |
| `https://www.ccat.com.tw/Home/Download` | Official download page | accessed 2026-08-12 | Publishes C# SDK, PHP SDK, WooCommerce plugin | Primary index |
| `https://www.ccat.com.tw/Home/Pay24` | Official service page | accessed 2026-08-12 | Confirms ibon payment service exists | Primary product evidence |
| `ccatpay/NET_SDK` | Official SDK | `0383d1fa36a9b329beec42db7936aa5b82371cd9` | token, CVS create/query, response model | Primary implementation evidence |
| `ccatpay/PHP_SDK` | Official SDK | `60e66bb679d015f883d80651d46e92864eebcb2f` | token, `CvsOrderAppend`, `CvsOrderQuery` | Primary implementation evidence |
| `ccatpay/ccat-for-woocommerce` | Official implementation | `5f3a4357c8c42114676a29acb3b6f1ab18dfcbf3` | ibon create, token caching, APN handling | Official observed behavior; not a complete spec |
| `jeepay/jeepay-payment/...` | Local runtime code | workspace baseline | SPI, query/callback ownership, response persistence | Local runtime evidence |

The official repositories were inspected in `/tmp/ccat-p03-ukBxM5/`; no CCAT credentials were read or used. Relevant paths include:

- `PHP_SDK/src/pay/api/LoginClient.php`, `OrderClient.php`, `utils/CurlTool.php`, `model/CvsOrderAppendRequest.php`.
- `NET_SDK/.../Models/Shared/OrderModel.cs`, `Models/CreateOrder/CvsOrder/CvsOrderModel .cs`, `Models/ReturnData/ReturnOrder.cs`, `Models/ReturnData/ReturnToken.cs`.
- `ccat-for-woocommerce/includes/class-ccatpay-gateway-cvs-abstract.php`, `class-ccatpay-gateway-cvs-ibon.php`, `class-ccatpay-gateway-abstract.php`.

## JeePay Extension Contract

Runtime evidence establishes the following non-negotiable route:

```text
MchPayPassage.ifCode
→ Spring bean ccatPaymentService
→ PaywayUtil
→ payway/CcatIbon
```

`AbstractPayOrderController` resolves `${ifCode}PaymentService`; `PaywayUtil` derives `CcatIbon` from `CCAT_IBON`. Upstream query is `PayOrderReissue → ChannelOrderReissueService → ccatPayOrderQueryService`, while merchant `QueryOrderController` remains local-`PayOrder` only. APN is routed by `ChannelNoticeController` to `ccatChannelNoticeService`; JeePay core owns local transition and merchant notification.

No RED core change is evidenced or proposed.

## CCAT Endpoint Contract

### Authentication / Token

| Item | Evidence-backed value | Evidence |
| --- | --- | --- |
| Method | `POST` | PHP `LoginClient::getToken()`; WooCommerce `get_payment_api_token()` |
| Path | `token` relative to base URL | same |
| Request content type | `application/x-www-form-urlencoded` | WooCommerce implementation |
| Request fields | `grant_type=password`, `username`, `password` | PHP SDK and WooCommerce implementation |
| Response fields | `access_token`, `token_type`, `expires_in`, `.issued`, `.expires`; errors `error`, `error_description` | .NET `ReturnToken` |
| Auth on Collect | `Authorization: Bearer <access_token>` | PHP `CurlTool`, WooCommerce implementation |
| Test base URL | `https://test.4128888card.com.tw/app/` | official WooCommerce implementation |
| Production base URL | `https://cocs.4128888card.com.tw/` | official WooCommerce implementation |
| Token invalidation/error rules | `UNKNOWN` | No public normative specification found |

The PHP SDK uses an older test URL with `http`; P04 must use HTTPS only. The official WooCommerce implementation demonstrates distinct test and production URLs and credentials, so sandbox/test scope exists; exact contractual eligibility and onboarding requirements remain `UNKNOWN`.

### Create ibon Payment

| Item | Evidence-backed value | Evidence |
| --- | --- | --- |
| Method | `POST` | PHP SDK / WooCommerce |
| Path | `api/Collect` | PHP `ApiConst::Collect` |
| Content type | `application/json` | PHP `CurlTool`, WooCommerce |
| Command | `cmd=CvsOrderAppend` | PHP `OrderClient`, WooCommerce |
| ibon constants | `payment_type=0`, `payment_acquirerType=2` | official ibon WooCommerce class |
| Response envelope | `status` is `OK` or `ERROR`; `msg`; `cust_order_no` | .NET `ReturnBasic` |
| ibon response fields | `ibon_shopid`, `ibon_code`, `expire_date`, `bill_amount`, optional `short_url` | .NET `ReturnOrder`; WooCommerce |
| Create success semantic | `status=OK` means API processing success, not payment success | WooCommerce stores order as pending after `OK` |
| Provider transaction identifier | `UNKNOWN` | No distinct field is documented in official CVS response model |
| Error code contract | `UNKNOWN` beyond `status` and `msg` | SDK model |

### Query Payment

| Item | Evidence-backed value | Evidence |
| --- | --- | --- |
| Method/path/content type | `POST api/Collect` with JSON Bearer request | PHP SDK base client |
| Command | `cmd=CvsOrderQuery` | PHP `OrderClient` |
| Lookup fields | `cust_id`, `cust_order_no` | PHP SDK and .NET `OrderQueryModel` |
| Returned status fields | `status`, `msg`, `cust_order_no`, `process_code`, `order_amount`, `pay_date`, `process_code_update_time` | .NET return models |
| Paid process code | `UNKNOWN` | No public official status table was found; do not assume `4` |
| Not-found/error semantics | `UNKNOWN` | No public error/status table was found |

### APN / Callback

`apn_url` is a create field and the .NET SDK says a missing value uses the service-level default. Official WooCommerce registers a `POST` JSON endpoint and its code expects fields `api_id`, `trans_id`, `amount`, `status`, `nonce`, `checksum`; CVS handling then tries to locate by `order_no`. This is **observed official implementation behavior**, not a confirmed APN specification: `order_no` is not in that required-field list and no relationship of `trans_id`, `order_no`, and `cust_order_no` is documented there.

| APN item | Status |
| --- | --- |
| HTTP method and JSON payload | OBSERVED: `POST` JSON in official WooCommerce implementation |
| Required callback fields | `UNKNOWN`; observed code names six fields above but cannot prove completeness |
| Local order lookup field | `BLOCKING UNKNOWN` |
| API / merchant identity binding | `BLOCKING UNKNOWN` |
| Provider transaction identifier semantics | `BLOCKING UNKNOWN` |
| Signature/checksum protocol | `BLOCKING UNKNOWN`; see below |
| ACK HTTP/body/retry contract | `BLOCKING UNKNOWN`; observed plugin returns `200 OK`, but no normative proof |

## Credential Schema

The following is the maximum evidence-backed first schema. It deliberately does **not** invent a callback secret field.

| Field | Required | Sensitive | Purpose | Evidence |
| --- | --- | --- | --- | --- |
| `environment` | YES | NO | Select CCAT test or production base URL | official WooCommerce implementation |
| `apiUsername` | YES | NO / account identifier | Token request `username` | official PHP SDK |
| `apiPassword` | YES | YES | Token request `password` | official PHP SDK |
| `custId` | YES | NO | `CvsOrderAppend` / `CvsOrderQuery` contract customer identifier | official PHP/.NET SDK |
| `callbackChecksumSecret` | `UNKNOWN` | YES | Only add if merchant-only APN specification confirms it exists and names its source | No supporting specification |

`CcatNormalMchParams` must include only the confirmed four fields at first. `apiUsername` and `custId` must not be silently treated as identical: the SDK names them differently and their equality is not documented. Parameters are stored in existing `t_pay_interface_config.if_params`; no credential is hard-coded in adapter code. `KNOWN SECURITY DEBT — Provider credential at-rest protection` remains unchanged.

## Token Lifecycle

### Contract findings

Token is acquired with `grant_type=password`, `username`, and `password`; Collect calls present it as a Bearer token. The response has both relative (`expires_in`) and absolute (`.expires`) expiry fields. The public material does not define token revocation, cross-instance reuse, or retry-on-401 rules.

### Options

| Option | Correctness / concurrency | Multi-config / multi-instance | Complexity / recommendation |
| --- | --- | --- | --- |
| A. Acquire per API call | Correct but unnecessary login load; no shared expiry race | naturally isolated | Do not recommend when expiry is supplied |
| B. Process-local cache keyed by credential/config identity | Must expire early and synchronize refresh per key; loss on restart is safe | isolated per instance, duplicated token acquisition across instances | **Recommended for phase 1** |
| C. Shared Redis token cache | Requires distributed lock, TTL policy, cache-secret operational controls | handles multiple instances | Over-designed for three capabilities without documented token reuse guarantees |

P04 design: process-local cache key must distinguish `environment + apiUsername + custId` and cache only the opaque token with expiry minus a conservative skew. On token-auth response, evict that key and retry once only if the official specification later confirms this is safe. Do not log token values. Re-evaluate Option C only after deployment requires multiple application instances and CCAT confirms reuse semantics.

## Identifier Mapping

# IDENTIFIER MAP

| CCAT | JeePay | Direction | Notes |
| --- | --- | --- | --- |
| `username` | `CcatNormalMchParams.apiUsername` | config → token | Not proven identical to `cust_id` |
| `password` | `CcatNormalMchParams.apiPassword` | config → token | Sensitive |
| `cust_id` | `CcatNormalMchParams.custId` | config → create/query | Confirmed request field |
| `cust_order_no` | `PayOrder.payOrderId` | outbound / query / callback lookup candidate | **Recommended, but final APN field contract is blocking**; SDK requires uniqueness within a `cust_id` and JeePay `payOrderId` is the native internal unique ID |
| `trans_id` | `UNKNOWN` | APN | Do not map to `channelOrderNo` until CCAT defines it |
| CCAT provider transaction ID | `PayOrder.channelOrderNo` | response / APN / query | Reuse existing field if a distinct authoritative identifier is supplied; none is confirmed now |
| merchant `mchOrderNo` | no CCAT field in phase 1 | local only | Must not replace `payOrderId`; it is not proven unique per CCAT `cust_id` |
| `amount` / `order_amount` | `PayOrder.amount` | create/query/APN | Unit conversion is blocking unknown |
| `ibon_shopid` + `ibon_code` | merchant-facing `payData` | create response | concatenate only if official contract confirms the presentation format; WooCommerce does this as observed behavior |
| `expire_date` | merchant-facing `payData` | create response | no dedicated `PayOrder` field; do not force DB schema change |
| `nonce` | APN verification input | callback only | persistence/replay semantics unknown |

The choice of `payOrderId` for `cust_order_no` is compatible with current JeePay callback routing because the controller can either parse that field or require the callback URL `/api/pay/notify/ccat/{payOrderId}`. It remains conditional on CCAT confirming that its APN returns `cust_order_no` (or an equally reliable reference). A callback URL path alone is not sufficient evidence that CCAT preserves the suffix or does not alter it.

## Amount Mapping

JeePay `PayOrder.amount` is explicitly in cents. CCAT SDK declares `order_amount` as `decimal`; the official service page prices in NT dollars, but neither source defines the API unit, decimal scale, rounding, or callback/query formatting. Therefore:

```text
PayOrder.amount (cents) ↔ CCAT order_amount / callback amount = BLOCKING UNKNOWN
```

P04 must obtain a CCAT specification or non-production official test vector that states the unit. It must use integer/`BigDecimal` conversion and exact integer comparison, never floating point. APN acceptance must enforce `callback amount == expected local amount` only after that conversion is formally defined.

## Create Payment

Target flow after blockers clear:

```text
UnifiedOrderRQ → PayOrder → CcatPaymentService → CcatIbon
→ CcatClient token → CCAT CvsOrderAppend → CommonPayDataRS → UnifiedOrderRS
```

### Request Mapping

| JeePay source | CCAT field | Conversion | Required | Evidence |
| --- | --- | --- | --- | --- |
| config | `cmd` | constant `CvsOrderAppend` | YES | PHP SDK |
| config | `cust_id` | `CcatNormalMchParams.custId` | YES | PHP/.NET SDK |
| `PayOrder.payOrderId` | `cust_order_no` | direct string; must be unique per `cust_id` | YES | .NET SDK uniqueness comment |
| `PayOrder.amount` | `order_amount` | `BLOCKING UNKNOWN` | YES | amount field confirmed; unit unknown |
| provider expiry policy | `expire_date` | `yyyy-MM-dd` is observed; business policy/allowed range `UNKNOWN` | YES in PHP request model | PHP SDK |
| `PayOrder.subject` / `body` | `order_detail` | bounded text; length/encoding `UNKNOWN` | `UNKNOWN` | SDK model |
| generated notify URL | `apn_url` | HTTPS URL | `UNKNOWN` whether required | SDK model |
| `UnifiedOrderRQ` customer data | `payer_name`, `payer_postcode`, `payer_address`, `payer_mobile`, `payer_email` | source availability and requirement must be validated | `BLOCKING UNKNOWN` | SDK fields; WooCommerce requires them |
| constant | `payment_type` | `0` | YES | official ibon class |
| constant | `payment_acquirerType` | `2` | YES | official ibon class |

`payment_type=0` and `payment_acquirerType=2` are protocol constants, not credentials. Invoice and shipping fields are excluded from phase 1.

### Response and Merchant-facing Result

`status=OK` plus a parseable ibon payment artifact maps to `WAITING`, never success. Preferred existing response model is `CommonPayDataRS`: use `payUrl` only if CCAT returns a usable `short_url`; otherwise current generic output cannot represent a structured tuple `{ibonShopId, ibonCode, expireDate, billAmount}`. Returning only a concatenated code as `payData` would be undocumented and not self-describing.

This is a `YELLOW CANDIDATE`: first inspect whether a single `codeUrl` or an existing merchant convention can carry the complete ibon instructions. If not, propose the smallest shared `UnifiedOrderRS`/pay-data extension before implementation; do not modify it in P04 without a separate evidence-backed decision.

### Create State Mapping

| CCAT result | ChannelRetMsg | PayOrder | Rationale |
| --- | --- | --- | --- |
| `status=OK`, valid ibon artifact | `WAITING` | `STATE_ING` | code generation is not payment |
| documented immediate paid result | `CONFIRM_SUCCESS` | success | `UNKNOWN` whether possible for ibon |
| `status=ERROR` with deterministic invalid request/auth/business rejection | `API_RET_ERROR` at create; no payment failure assertion | remain pending/error handling per controller | Error taxonomy/status code details unknown |
| transport timeout / response lost | `UNKNOWN` | do not mark failed | create may have succeeded |
| malformed/impossible response | `UNKNOWN` | do not mark failed | cannot establish remote outcome |

## Query Payment

Fixed JeePay flow:

```text
PayOrderReissue → ChannelOrderReissueService → ccatPayOrderQueryService
→ CCAT CvsOrderQuery → ChannelRetMsg → JeePay transition
```

| JeePay | CCAT | Evidence |
| --- | --- | --- |
| config `custId` | `cust_id` | official SDK |
| `PayOrder.payOrderId` | `cust_order_no` | recommended unique mapping / official query field |
| Bearer token | Authorization header | official SDK |
| command constant | `cmd=CvsOrderQuery` | official SDK |

| CCAT status / `process_code` | Meaning | ChannelRetMsg | PayOrder behavior |
| --- | --- | --- | --- |
| documented paid code | paid | `CONFIRM_SUCCESS` | core updates success and saves `channelOrderNo` if supplied |
| documented unpaid / waiting code | unpaid | `WAITING` | no transition |
| documented expired/cancelled/failed code | terminal | see closed/expired decision | no guessing |
| response `ERROR`, not found, unknown code | `UNKNOWN` until official table | `UNKNOWN` / `API_RET_ERROR` depends on documented semantics | no terminal transition |

`process_code=4` is not used because no official source read in this session establishes its meaning. `STATUS MAPPING DECISION REQUIRED` is blocking.

## APN / Callback

Target route is:

```text
CCAT → /api/pay/notify/ccat[/payOrderId] → ChannelNoticeController
→ ccatChannelNoticeService → PayOrder → PayOrderProcessService
→ PayMchNotifyService → merchant
```

### `parseParams()`

Do not implement until CCAT confirms one reliable local identifier. Preferred order is: parse a documented callback `cust_order_no` that equals `PayOrder.payOrderId`; otherwise require CCAT's documented preservation of the URL `{payOrderId}` suffix. `trans_id` and `order_no` cannot be used without a specification tying them to create/query identifiers.

### Verification order

After the blocking contract is supplied, use the minimum provider-specific sequence:

1. Require documented HTTP method, content type, bounded parseable body, and required fields.
2. Obtain local order through the authoritative CCAT merchant-order reference.
3. Check callback account identity against the matching `CcatNormalMchParams` field.
4. Validate the documented checksum/signature using constant-time comparison where applicable.
5. Check exact converted amount, merchant order reference, and any documented provider transaction ID against local state.
6. Map only documented payment status; duplicate a valid terminal success must still receive the documented success ACK.
7. Return the CCAT-specific ACK through `ChannelRetMsg.responseEntity`; leave state change and merchant notification to core.

No `api_id` comparison, `trans_id` storage, nonce TTL, or replay store is safe to implement until their official meanings are supplied.

## Signature / Checksum

`BLOCKING UNKNOWN`.

The official WooCommerce code has incompatible observed approaches: one path computes MD5 over `api_id:trans_id:amount:status:nonce` with no secret; another uses `api_id:notify_time:cust_order_no:...` and a differently named `chk`. Neither proves a production APN contract and a checksum without a merchant-held secret would not provide origin authentication. Do not copy either implementation as security truth.

Required CCAT evidence: algorithm, canonical field list/order/separator, character encoding, case/hex format, secret/key source, whether nonce is signed, and one non-secret test vector. Safe test-vector design after receipt: use CCAT-provided public vector, or create a test-only dummy credential/vector whose expected hash is generated from the confirmed canonicalization; never use production material.

## State Mapping

`ChannelRetMsg.ChannelState` has no distinct CLOSED value.

| CCAT condition | Candidate | Decision |
| --- | --- | --- |
| payment code generated / unpaid | `WAITING` | confirmed design |
| paid | `CONFIRM_SUCCESS` | requires official paid status code |
| explicit terminal failure / cancellation / expiry | `CONFIRM_FAIL` | only after official process-code semantics confirm no later payment is possible |
| unknown / malformed / transport ambiguity | `UNKNOWN` | confirmed design |

### Closed / Expired

Options A (`CONFIRM_FAIL`), B (keep `WAITING` for local expiry), and C (core extension) cannot be selected without CCAT's expiration/cancellation semantics and JeePay deployment timeout policy. **Recommended provisional policy: Option B**, preserving `WAITING` for unverified codes; promote a documented irrevocable expiry/cancellation to Option A. Option C has no evidence and is rejected for phase 1. This is a blocking status-mapping unknown, not permission to leave expired orders permanently waiting.

## Error Mapping

| Category | Examples | ChannelRetMsg / handling |
| --- | --- | --- |
| Deterministic business failure | documented validation/rejection | `API_RET_ERROR` on create; `CONFIRM_FAIL` only when query/APN proves terminal payment failure |
| Waiting | code generated, documented unpaid/processing | `WAITING` |
| Transport failure | timeout, DNS, reset, TLS | `UNKNOWN`; retain order and reconcile through query |
| Ambiguous create | request may have reached CCAT, response lost | `UNKNOWN`; never create a second identifier |
| Malformed provider response | invalid JSON, missing mandatory field, impossible status/amount | `UNKNOWN` and safe logging |

## Retry / Ambiguous Results

The SDK documents `cust_order_no` uniqueness under `cust_id`, which supports using stable `PayOrder.payOrderId` for every attempt. It does **not** document create idempotency or duplicate-create response behavior. Therefore a transport timeout must not regenerate an ID or blindly resubmit. First query the same `cust_id + cust_order_no`; retry create only after CCAT documents idempotency/duplicate semantics. This is `HIGH-RISK UNKNOWN` and blocks P04's production-safe create behavior.

## Idempotency / Replay

JeePay already guards `STATE_ING` transition and merchant-notify deduplication through its existing processing path. It is not provider replay protection.

- Duplicate legitimate APN: after the callback contract is confirmed and validation still succeeds, core must not re-transition/re-notify; adapter should return the documented successful ACK.
- Replay attack: `nonce` exists only as observed code. Its uniqueness, timestamp binding, scope, and required persistence are `SECURITY DESIGN OPEN`.
- Do not add Redis replay state in P04 merely because Redis is available; decide after the official APN contract specifies replay semantics.

## Provider ACK

`BLOCKING UNKNOWN`. The official WooCommerce plugin returns `HTTP 200` and body `OK` for its own handler, but CCAT's specification, content type, case sensitivity, failure ACK, and retry rule are not public in evidence. P04 must fill this table only from the merchant specification:

| Situation | HTTP | Body |
| --- | ---: | --- |
| valid paid | UNKNOWN | UNKNOWN |
| duplicate valid paid | UNKNOWN | UNKNOWN |
| invalid checksum | UNKNOWN | UNKNOWN |
| order not found | UNKNOWN | UNKNOWN |
| amount mismatch | UNKNOWN | UNKNOWN |
| transient internal error | UNKNOWN | UNKNOWN |

## Class Responsibilities

- `CcatPaymentService`: `getIfCode`, `isSupport`, generic dispatch via `PaywayUtil`, and provider-independent precheck only.
- `payway/CcatIbon`: validate confirmed ibon inputs, map create request, call client, parse code/expiry, and return `ChannelRetMsg` plus existing pay-data response. It must not treat issuance as payment success.
- `CcatPayOrderQueryService`: query and map only CCAT response to `ChannelRetMsg`; no direct DB update or merchant callback.
- `CcatChannelNoticeService`: parse, verify confirmed identity/checksum/amount/transaction/status, and build CCAT ACK. It must not reimplement MQ or payment state transition.
- `CcatClient`: one small provider client for token acquisition, Collect HTTP, JSON/error normalization. This is justified because create and query share token, base URL, Bearer header, and transport handling.
- `CcatKit`: **not needed initially**. Add only if a confirmed checksum algorithm needs a small pure helper.

## SDK vs HTTP Decision

`RECOMMENDED INTEGRATION MODE = Direct HTTP via minimal CcatClient.` CCAT's official public SDKs are C# and PHP, not Java; importing either is impossible/inappropriate. They remain protocol evidence. Direct HTTP keeps dependencies unchanged and has less abstraction mismatch once the missing CCAT specification is supplied.

## HTTP Client Decision

Reuse existing dependency: Spring Boot web stack is present; Hutool is already used extensively in payment channels. P04 should select the existing project-standard client after inspecting the nearest simple JSON/Bearer provider and must add no dependency. Do not use the official PHP SDK's TLS-disabling behavior; TLS certificate verification is mandatory.

## Configuration Schema

`configPageType = 1`; generic JSON form is sufficient. `CUSTOM_VUE_REQUIRED = NO`.

Seed schema proposal (no SQL in P03):

| Field | Label | Input | Required | Mask | Default |
| --- | --- | --- | --- | --- | --- |
| `environment` | CCAT 環境 | radio `test,production` | YES | no | no implicit production default |
| `apiUsername` | API 登入帳號 | text | YES | no | none |
| `apiPassword` | API 密碼 | password/textarea | YES | yes (`star=1`) | none |
| `custId` | 契客代號 | text | YES | no | none |

The model must implement `deSenData()` masking `apiPassword`; do not display bearer tokens or raw `if_params` in logs. A callback secret field is intentionally absent until official evidence confirms it.

## PayInterface / PayWay Seed Design

| Entity | Design |
| --- | --- |
| PayInterface | `ifCode=ccat`, normal merchant mode enabled, ISV mode disabled, `configPageType=1`, `wayCodes=[{"wayCode":"CCAT_IBON"}]` |
| PayWay | `wayCode=CCAT_IBON`, display name `黑貓 PAY ibon 繳款` |
| icon / color / scene metadata | `UNKNOWN`; reuse no remote asset without approved source |

The actual seed location is `jeepay/docs/sql/init.sql`/approved migration location, but P03 writes no SQL.

## Security Model

| Threat | Existing JeePay protection | CCAT adapter responsibility | Open debt |
| --- | --- | --- | --- |
| forged APN | callback routing only | authenticated signature, identity binding, exact status verification | blocking APN spec |
| amount tampering | local order amount | exact documented unit conversion and comparison | blocking amount unit |
| `api_id` mismatch | none provider-specific | compare only after official field mapping | blocking identity semantics |
| transaction mismatch | `channelOrderNo` field exists | bind documented merchant/provider identifiers | blocking identifier semantics |
| replay | state guard / notify dedup | honor documented nonce/replay rule | security design open |
| duplicate legitimate APN | state guard / notify infrastructure | revalidate then success ACK | blocking ACK contract |
| create timeout ambiguity | reissue query infrastructure | stable order ID; query before retry | high-risk unknown |
| response tampering / TLS | HTTPS client stack | certificate verification; no token/secret log | no TLS bypass |
| credential exposure | existing config model | `deSenData`, redacted logs | KNOWN SECURITY DEBT — at-rest protection |
| accidental credential commit | repository rules | dummy-only fixtures, secret scan review | none |

## Logging Rules

Allowed: `payOrderId`, safe CCAT provider transaction ID once defined, `cust_order_no` where permitted, mapped state, HTTP result category/status, latency, and non-sensitive CCAT error code.

Forbidden: `apiPassword`, bearer token, callback secret, checksum input containing secret, raw credential JSON, private keys, and full sensitive request/callback payloads. Existing official WooCommerce test logging of raw APN body is not adopted.

## Test Matrix

No tests are written in P03.

| Capability | Unit | Adapter integration | Live verification boundary |
| --- | --- | --- | --- |
| Create | success/code generated; business/auth failure; missing mandatory field; malformed response; timeout; duplicate ambiguity | mock token + Collect contract | CCAT non-production only after all blockers resolved |
| Query | paid; unpaid; pending; failed; expired; not found; timeout; malformed response | mocked documented `process_code` table | CCAT non-production only |
| APN | valid paid; invalid signature; wrong identity; amount mismatch; wrong transaction; order not found; duplicate; replay; malformed body; unexpected status | raw HTTP fixture against `ChannelNoticeController` path | CCAT non-production callback only |

Unit and integration fixture values must be dummy-only. Production calls, real ibon orders, payment, real APN, and production secrets are prohibited in P03.

## Implementation Blueprint

### New Files Candidate

```text
jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/
├── CcatPaymentService.java
├── CcatPayOrderQueryService.java
├── CcatChannelNoticeService.java
├── CcatClient.java                 # justified shared token/HTTP/Collect layer
└── payway/
    └── CcatIbon.java

jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/model/params/ccat/
└── CcatNormalMchParams.java
```

Expected GREEN modifications after contract blockers clear: `CS.java` (`IF_CODE.CCAT` and `PAY_WAY.CCAT_IBON`) and approved PayInterface/PayWay seed location. A small pay-data response extension is a separate `YELLOW CANDIDATE`, not approved by this document.

### Files Explicitly Not To Modify

- `PayOrder`
- `AbstractPayOrderController`
- `ChannelNoticeController`
- `PayOrderProcessService`
- `PayMchNotifyService`
- merchant notification MQ/retry core
- authentication/RBAC

## Blocking Unknowns

1. Merchant-versioned official CCAT specification for `CvsOrderAppend`, including exact required fields, customer-data requirements, expiry limits, error codes, and amount unit/scale.
2. Official query `process_code` table, specifically paid, pending, cancelled, expired, failed, and not-found meanings.
3. APN specification: payload fields, reliable merchant-order reference, `api_id`/`trans_id` meanings, identity binding, signature/checksum algorithm/key, canonicalization, and safe vector.
4. APN ACK HTTP/body/content-type and retry semantics.
5. Create idempotency/duplicate behavior after response-lost timeout.
6. Merchant-facing ibon artifact presentation contract sufficient to decide whether existing `CommonPayDataRS` can represent it without a YELLOW response-model extension.

## Non-blocking Unknowns

- CCAT refund, transfer, division, channel user, close order, COCS, DPH, ATM, and other products.
- Final display icon, color, and optional seed metadata.
- Token revocation / cross-instance reuse semantics (process-local cache is safe provisional behavior).

## Definition of Ready

P04 is **not ready**. It becomes `READY FOR P04` only when the six blocking items above are answered by official CCAT evidence and these acceptance conditions can be checked:

1. Create endpoint, auth/token, required ibon fields, outbound order ID, amount unit, success/wait/error semantics, and ibon code response are confirmed.
2. Query endpoint, lookup ID, and paid plus all terminal/pending status mappings are confirmed.
3. APN payload, checksum, amount comparison, account identity binding, local order lookup, and acknowledgement are confirmed.
4. `CcatNormalMchParams` has a complete evidence-backed schema.
5. Direct HTTP/client decision is retained or revised with evidence; no RED core modification is required.

