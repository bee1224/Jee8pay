# CCAT Provider Design

## Status

```text
JEE-C03 = PASS-WITH-DEBT
CCAT_CONTRACT_PHASE = COMPLETE
CCAT_RUNTIME_GATE = OPEN
CCAT_RUNTIME_IMPLEMENTATION = COMPLETE
CCAT_OFFLINE_ACCEPTANCE = PASS
CCAT_DEVELOPMENT_DEPLOYMENT = PASS
CCAT_LIVE_E2E = PASS
CCAT_PRODUCTION_CANDIDATE = DEPLOYED / ACCEPTED
CCAT_PRODUCTION_VALIDATION = NOT STARTED
V1_CUTOVER = NOT STARTED
NEXT = USER-SELECTED JEE-D01 OR LATER JEE-M01
```

Merchant-authenticated official specification `多元支付平台-WEBAPI介面規格(V1.28.1)` closes the C02 amount、status、APN identity and Create-idempotency blockers. P04 implements the design below through JeePay's native Provider extension points. JEE-E02 subsequently completed Development deployment and one real TWD 40 CCAT ibon E2E. JEE-E04 deployed the same accepted artifact as an isolated Platform Production Candidate; credential/config binding、public callback activation and pilot transaction remain explicit later gates.

## Scope

- Provider：CCAT／黑貓 PAY
- `ifCode`：`ccat`
- `wayCode`：`CCAT_IBON`
- Capability：Create ibon payment、Provider Query、Payment Notify / APN。

Non-goals：Refund、Transfer/Payout、Division、Channel User、Close Order、COCS、credit card、ATM、other CCAT products、custom Provider UI、secret-management refactor、JeePay payment-domain redesign。

## Source of Truth

Evidence priority：merchant-authenticated CCAT specification → official specification → official SDK → official implementation → observed behavior → prior V1 implementation → inference。

The normative source is the 97-page V1.28.1 specification, system update date `2026-05-08`. Relevant sections are p.9–10、p.14–27、p.35–39、p.94 and p.97. Detailed field/page provenance、the complete process-code table、31-item DoR and V1 drift are in [`contract-evidence.md`](contract-evidence.md).

V1 is prior behavior only. The later V1 mapping of `process_code=7/8` to pending contradicts V1.28.1 and must not be preserved.

## JeePay Extension Contract

```text
MchPayPassage.ifCode
→ Spring bean ccatPaymentService
→ PaywayUtil
→ payway/CcatIbon
```

Upstream query remains `PayOrderReissue → ChannelOrderReissueService → ccatPayOrderQueryService`. Merchant `QueryOrderController` remains local-`PayOrder` only. APN is routed by `ChannelNoticeController` to `ccatChannelNoticeService`; JeePay Core owns state transition and merchant notification.

No RED core modification is required or proposed.

## Endpoint Contract

### Token

| Item | Contract |
| --- | --- |
| Method/path | `POST /Token` |
| Content type | `application/x-www-form-urlencoded` |
| Fields | `grant_type=password`, `username`, `password` |
| Identity | Token `username` and Collect `cust_id` are the same customer financial code |
| Response | `access_token`, `token_type`, `expires_in`, `userName`, `.issued`, `.expires` |
| Expiry | use `.expires`; specification explicitly makes it authoritative |
| Collect auth | `Authorization: Bearer <access_token>` |

P04 should use a process-local token cache keyed by `environment + custId`, expire early relative to `.expires`, synchronize refresh per key and never log tokens. Cross-instance reuse and automatic retry-on-401 are optional optimization debt; a failed authenticated request must fail closed.

### Create ibon Payment

| Item | Contract |
| --- | --- |
| Method/path/content | `POST /api/Collect`, JSON, Bearer |
| Command | `cmd=CvsOrderAppend` |
| Account/order key | `cust_id + cust_order_no`; order number unique under the account |
| ibon constants | `payment_type=0`, `payment_acquirerType=2` |
| Amount | whole TWD dollars, no decimal places |
| Required business fields | p.15–17 field table, including expiry、payer data and `order_detail`; `apn_url` may use configured default |
| Response | same fields as Query; `status=OK/ERROR` |
| Payment artifact | `ibon_shopid`, `ibon_code`, `expire_date`, `bill_amount`, optional `short_url` |
| Create semantic | `OK` means instructions issued; PayOrder remains waiting, not paid |
| Duplicate | reject the already-uploaded `cust_order_no` |

Create `status=OK` plus valid ibon instructions maps to `ChannelRetMsg.WAITING` / `PayOrder.STATE_ING`. A deterministic validation/auth/business error is an API error, not proof of payment failure. Transport ambiguity remains `UNKNOWN` until Query recovery completes.

### Query

| Item | Contract |
| --- | --- |
| Method/path/content | `POST /api/Collect`, JSON, Bearer |
| Command | `cmd=CvsOrderQuery` |
| Lookup | `cust_id + cust_order_no` |
| Amounts | `order_amount`, `bill_amount`, `pay_amount` |
| State | `process_code`, authoritative table in V1.28.1 appendix 1 |
| Not found | `status=ERROR` plus documented `找不到此筆代繳資訊` message |

State mapping:

| process_code | Meaning | Adapter result |
| --- | --- | --- |
| `0`, `1`, `3` | pre-payment / waiting | `WAITING` |
| `2` | appears only in official sample; appendix omits meaning | `UNKNOWN`, no state transition, re-query |
| `4` | payer paid | `CONFIRM_SUCCESS` |
| `5` | cancelled slip | `CONFIRM_FAIL` |
| `6` | expired slip | `CONFIRM_FAIL` |
| `7`, `8` | paid; payout scheduled/completed | `CONFIRM_SUCCESS` |

Codes outside this table do not transition an ibon PayOrder. `13+` are card lifecycle values outside Phase 1.

## Credential Schema

The authenticated specification removes the previous duplicate account fields：Token `username` and Collect `cust_id` are the same value. Phase-1 `CcatNormalMchParams` should therefore contain only:

| Field | Required | Sensitive | Purpose |
| --- | --- | --- | --- |
| `environment` | YES | NO | Select test or production base URL; no implicit production default |
| `custId` | YES | account identifier | Token `username`、Create/Query `cust_id`、APN `api_id` comparison |
| `apiPassword` | YES | YES | Token request password |

Do not add `apiUsername` as a second independent identity and do not add a callback secret: V1.28.1 defines neither. COCS-specific hash material is outside ibon scope. `apiPassword` is stored through existing `PayInterfaceConfig.if_params`, masked by `deSenData()` and never hard-coded or logged. Credential at-rest protection remains separate `TD-001` debt.

## Identifier Mapping

| CCAT | JeePay | Rule |
| --- | --- | --- |
| Token `username` / Collect `cust_id` / APN `api_id` | `CcatNormalMchParams.custId` | exact account equality |
| `cust_order_no` / APN `order_no` | `PayOrder.payOrderId` | stable Provider order key; use the native 20-character platform ID |
| APN `trans_id` | `PayOrder.channelOrderNo` | unique payment-slip ID; bind only after full APN validation and authenticated Query |
| merchant order number | `PayOrder.mchOrderNo` | local merchant-facing identity only; do not send as Provider-scoped key |
| `ibon_shopid`、`ibon_code`、expiry、`short_url` | existing payment response data | merchant-facing instructions; no second order table |

`trans_id` is not returned by Query, so it never authorizes a transition alone. A first APN `trans_id` may be stored only after account/order/checksum/amount validation and authenticated Query confirms the same CCAT order's state. Later callbacks must match stored `channelOrderNo`; a mismatch fails closed.

## Amount Mapping

JeePay `UnifiedOrderRQ.amount` and `PayOrder.amount` are `Long` minor units. CCAT `order_amount` is whole TWD dollars and rejects decimals.

```text
require PayOrder.amount % 100 == 0
ccatOrderAmount = PayOrder.amount / 100
require Query.order_amount * 100 == PayOrder.amount
require APN.amount == Query.bill_amount
for successful full payment:
    require APN.pay_amount == Query.pay_amount == Query.bill_amount
```

The official p.25 ibon example has `order_amount != bill_amount == pay_amount`; an external payer fee means APN paid amount must not be compared directly with `PayOrder.amount`. Query is the cross-surface authority. Parse all values as exact scale-zero integer/`BigDecimal`; accept APN `pay_amount` as JSON number or digit string because V1.28.1 itself shows both. Never use float/double or rounding.

The global ibon bill cap is `20,000 元` including externally added fee. Account-specific bounds remain Provider validation; P04 must not invent a fixed minimum.

## Create Mapping and Recovery

### Request

| JeePay source | CCAT field | Conversion |
| --- | --- | --- |
| constant | `cmd` | `CvsOrderAppend` |
| config | `cust_id` | `custId` |
| `PayOrder.payOrderId` | `cust_order_no` | direct stable string |
| `PayOrder.amount` | `order_amount` | exact cents ÷ 100 |
| provider policy | `expire_date` | `yyyy-MM-dd` within CCAT-configured range |
| `PayOrder.subject/body` | `order_detail` | required, bounded to official length |
| generated callback URL | `apn_url` | HTTPS or use Provider-configured default |
| `channelExtra` | payer fields | parse provider-specific bounded JSON without shared-core field additions |
| constants | payment fields | `payment_type=0`, `payment_acquirerType=2` |

### Ambiguous failure algorithm

```text
Append once with stable key
→ OK: validate and persist response
→ deterministic business error: fail without blind retry
→ ambiguous response: Query stable key
   → found: validate and recover Create-equivalent result
   → exact not-found: bounded Append retry with identical key
      → duplicate rejection: Query again
      → other ambiguity: stop and reconcile later
```

Never generate a new `cust_order_no`. The official duplicate rejection makes same-key retry duplicate-safe; Query-first preserves recoverability and avoids unnecessary retries.

## APN / Callback

Target route:

```text
CCAT → ChannelNoticeController → ccatChannelNoticeService
→ authenticated CvsOrderQuery → PayOrderProcessService
→ PayMchNotifyService → merchant
```

### Parse and validate

1. Require `POST` JSON and the ibon safety fields：`api_id`、`trans_id`、`order_no`、`amount`、`status`、`payment_code=2`、`nonce`、`checksum`; paid state also requires `pay_amount/pay_date`.
2. Resolve local order by `order_no == PayOrder.payOrderId`.
3. Load that order's CCAT config and require `api_id == custId`.
4. Verify the 32-hex checksum in constant time.
5. Query CCAT with the same account and order key; validate Query `cust_order_no` and exact amount relationships.
6. Bind/compare `trans_id` through `channelOrderNo` only after Query confirmation.
7. Map only the official Query `process_code`; APN `status` is a hint that must agree with Query.
8. Return CCAT ACK through `ChannelRetMsg.responseEntity`; Core retains state transition and merchant-notify ownership.

### Checksum

```text
canonical = api_id + ":" + trans_id + ":" + amount + ":" + status + ":" + nonce
checksum = lowercase hexadecimal MD5(UTF-8 bytes of canonical)
```

The checksum is secretless and provides integrity only. It is not callback-origin authentication. The account-scoped Bearer Query is the authorization boundary. No `callbackChecksumSecret` is added.

### ACK, retry and replay

- Normative success body：exact uppercase plain text `OK`.
- V1.28.1 sends immediately, then every 15 minutes, at most three times per status; `OK` stops retry.
- HTTP status and response content type are not normatively specified; HTTP 200 + `text/plain` matches official implementation and prior V1.
- Invalid callbacks fail closed and never receive `OK`.
- Duplicate/replayed valid APNs are fully revalidated and queried again.
- JeePay's conditional pending-to-terminal transition and unique merchant-notify record absorb duplicate state/notify effects.
- For the concurrent loser, Provider-specific `doNotifyOrderStateUpdateFail()` may return `OK` only after re-reading an exact matching committed terminal state and `channelOrderNo`; unexplained update failure is not normalized.
- Nonce has a documented 10-character time/random shape but no TTL; do not add a replay cache that pretends to supply protocol freshness.

## Error Mapping

| Category | Handling |
| --- | --- |
| Create `OK` with valid ibon artifact | `WAITING` |
| Query official paid code | `CONFIRM_SUCCESS` |
| Query official cancellation/expiry | `CONFIRM_FAIL` |
| sample-only code `2` / unknown code | `UNKNOWN`; no transition |
| deterministic Create validation/auth error | API error; no payment-failure assertion |
| timeout/reset/HTTP ambiguity | Query stable key; use bounded same-key recovery |
| malformed/impossible response | `UNKNOWN`, safe redacted logging, no transition |

## Class Responsibilities

- `CcatPaymentService`：generic provider dispatch only.
- `payway/CcatIbon`：validate ibon inputs、map Create、return waiting instructions; never mark issuance as paid.
- `CcatPayOrderQueryService`：authenticated Query、identity/amount validation and official state mapping; no direct DB transition.
- `CcatChannelNoticeService`：parse APN、verify account/checksum/order/amount/transaction、query authoritative state and build exact ACK; no replacement MQ/state machine.
- `CcatClient`：small shared Token/Collect HTTPS client、expiry and error normalization; TLS certificate verification mandatory.
- `CcatKit`：optional small pure checksum helper only; no general framework.

## SDK and HTTP Decision

Use direct HTTPS through a minimal `CcatClient`. Official SDKs are C# and PHP and remain protocol evidence, not Java dependencies. Reuse an existing JeePay/Spring/Hutool HTTP dependency; add no new client library and never disable TLS verification.

## PayInterface / PayWay Configuration

`configPageType = 1`; generic JSON form is sufficient. `CUSTOM_VUE_REQUIRED = NO`.

| Entity | Design |
| --- | --- |
| PayInterface | `ifCode=ccat`, normal merchant mode, ISV disabled, `configPageType=1`, `wayCodes=[{"wayCode":"CCAT_IBON"}]` |
| PayWay | `wayCode=CCAT_IBON`, display name `黑貓 PAY ibon 繳款` |
| Config | `environment`, `custId`, masked `apiPassword` |
| icon/color | optional non-blocking metadata; do not copy unapproved remote assets |

## Security Model

| Threat | Adapter treatment |
| --- | --- |
| forged APN | exact account/order/checksum validation plus account-scoped authenticated Query |
| amount tampering | local ↔ Query `order_amount`; APN ↔ Query bill/paid amounts |
| account mismatch | APN `api_id == custId`; Query uses same `custId`/token account |
| transaction mismatch | first validated `trans_id` binds `channelOrderNo`; later mismatch fails closed |
| replay/duplicate | revalidate、Query、native atomic transition/notify dedup、exact duplicate ACK |
| Create timeout | stable key、Query-first、same-key bounded retry |
| credential exposure | `deSenData()`、redacted logging、no bearer/API password in fixtures/docs |
| TLS tampering | HTTPS and certificate validation only |

Allowed logs：`payOrderId`、validated `channelOrderNo`、mapped state、safe Provider error category、HTTP status and latency。Forbidden：API password、bearer token、raw `if_params`、full callback/request payload and any real credential.

## P04 Test Matrix

| Capability | Required cases |
| --- | --- |
| Create | code issued/waiting、whole-dollar rejection、external-fee response、duplicate rejection、business/auth failure、malformed response、timeout found/not-found/race recovery |
| Query | codes `0/1/3/4/5/6/7/8`、sample-only `2` no-transition、unknown code、not found、amount/account/order mismatch、timeout/malformed response |
| APN | valid `A/B/C/D/E` with Query agreement、invalid checksum/account/order/amount/transaction、JSON number/string paid amount、duplicate、concurrent duplicate、replay、unknown state、Query failure |

Fixtures must be dummy-only. No production Provider call、real order/payment/APN or real credential is authorized by this design.

## Implementation Blueprint

Candidate GREEN files for P04:

```text
jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/
├── CcatPaymentService.java
├── CcatPayOrderQueryService.java
├── CcatChannelNoticeService.java
├── CcatClient.java
└── payway/CcatIbon.java

jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/model/params/ccat/
└── CcatNormalMchParams.java
```

Expected GREEN edits：`CS.IF_CODE.CCAT`、`CS.PAY_WAY.CCAT_IBON` and approved PayInterface/PayWay seed/config data. Explicitly do not modify `PayOrder`、`AbstractPayOrderController`、`ChannelNoticeController`、`PayOrderProcessService`、`PayMchNotifyService`、merchant notification MQ/retry、authentication or RBAC.

The merchant-facing structured ibon instruction shape remains a potential YELLOW response representation question. First reuse existing `CommonPayDataRS.payUrl` for `short_url` and the smallest existing pay-data convention; any shared response-model change requires separate code evidence and review, not a RED core change.

## Non-Blocking Debt

- `process_code=2` sample/appendix inconsistency：always no transition until clarified.
- ACK HTTP status/content type and invalid-response wire format are not normative.
- Nonce freshness TTL is unspecified; Query/idempotency remains authoritative.
- Token lifetime prose/table differ; `.expires` is explicitly authoritative.
- Cross-instance token reuse、display metadata and optional fields are not correctness blockers.
- Production Candidate credential/config binding、public callback activation and pilot validation remain separate later gates.

## Definition of Ready

The 31-item table in `contract-evidence.md` has `28 READY`、`3 NONBLOCKING_PARTIAL`、`0 BLOCKED` and `0 runtime CONFLICT`. Amount、payment state、order/account/transaction identity、checksum、callback amount validation、ACK body、duplicate handling and ambiguous Create recovery no longer require guesses.

```text
B1_AMOUNT = RESOLVED
B2_PROCESS_CODE = RESOLVED
B3_APN_IDENTITY = RESOLVED
B4_APN_RETRY_REPLAY = RESOLVED
B5_CREATE_IDEMPOTENCY = RESOLVED
CCAT_RUNTIME_GATE = OPEN
CCAT_RUNTIME_IMPLEMENTATION = COMPLETE
CCAT_OFFLINE_ACCEPTANCE = PASS
CCAT_DEVELOPMENT_DEPLOYMENT = PASS
CCAT_LIVE_E2E = PASS
CCAT_PRODUCTION_CANDIDATE = DEPLOYED_AND_ACCEPTED
CCAT_PRODUCTION_VALIDATION = NOT_STARTED
V1_CUTOVER = NOT_STARTED
```
