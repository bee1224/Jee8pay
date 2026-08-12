# CCAT ibon Contract Evidence

Status: `JEE-C03 — PASS-WITH-DEBT`
Closure session: `JEE-C03`
Closure date: `2026-08-12`

```text
VERDICT = PASS-WITH-DEBT
CCAT_CONTRACT_PHASE = COMPLETE
CCAT_RUNTIME_GATE = OPEN

B1_AMOUNT = RESOLVED
B2_PROCESS_CODE = RESOLVED
B3_APN_IDENTITY = RESOLVED
B4_APN_RETRY_REPLAY = RESOLVED
B5_CREATE_IDEMPOTENCY = RESOLVED

REAL_SECRETS_EXPOSED = 0
REAL_SECRETS_WRITTEN = 0
JEE_C03_RUNTIME_SOURCE_MODIFICATIONS = NONE
CCAT_RUNTIME_IMPLEMENTATION = COMPLETE
CCAT_OFFLINE_ACCEPTANCE = PASS
CCAT_LIVE_E2E = NOT STARTED
CCAT_PRODUCTION_VALIDATION = NOT STARTED
```

本文件只記錄 CCAT／黑貓 PAY ibon CVS Phase 1 的 Token、Create、Provider Query 與 APN contract。Credit card、COCS、refund、payout、transfer 與其他 payment products 不在本階段。JEE-C03 沒有登入契約會員 portal、沒有呼叫 Provider API，也沒有使用任何 credential value。

## Evidence Authority

| Source | Version / provenance | Classification | Scope |
| --- | --- | --- | --- |
| `多元支付平台-WEBAPI介面規格(V1.28.1) (1).pdf` | system version `1.28.1`; updated `2026-05-08`; 97 pages | `MERCHANT_AUTHENTICATED_OFFICIAL_SPEC` | Normative Token、CVS Create/Query、APN、status、duplicate/error contract |
| `ccatpay/PHP_SDK` | `60e66bb679d015f883d80651d46e92864eebcb2f` | `OFFICIAL_SDK` | Token、Create/Query request surface |
| `ccatpay/NET_SDK` | `0383d1fa36a9b329beec42db7936aa5b82371cd9` | `OFFICIAL_SDK` | Request/response models |
| `ccatpay/ccat-for-woocommerce` | `5f3a4357c8c42114676a29acb3b6f1ab18dfcbf3` | `OFFICIAL_IMPLEMENTATION` | ibon constants、APN checksum、observed ACK |
| Development V1 source traces | C01/C02 recorded hashes | `PRIOR_IMPLEMENTATION` | Prior behavior and drift comparison only |
| JeePay source at `12852640445e2d7c1cdda55f8b4a1ad9620c8419` | actual C03 baseline | Local runtime evidence | Amount unit、native Provider extension、state/notify idempotency |

Evidence priority：`MERCHANT_AUTHENTICATED_OFFICIAL_SPEC` → `OFFICIAL_SPEC` → `OFFICIAL_SDK` → `OFFICIAL_IMPLEMENTATION` → `OBSERVED_BEHAVIOR` → `PRIOR_IMPLEMENTATION` → `INFERENCE`。V1 implementation 不覆蓋 V1.28.1 authenticated specification。

Relevant official sections reviewed：p.9「格式說明／網址」、p.10「取得 Token 驗証碼」、p.14–18「CVS／契客新增訂單」、p.19–22「訂單查詢」、p.23–27「訂單日期區間查詢」（cross-surface amount examples）、p.35–39「APN 主動通知」、p.94「訂單程序狀態一覽表」、p.97 `pay_route` appendix。關鍵表格已同時檢視 extracted text 與原始頁面 rendering。

## Final Contract Matrix

```text
TOKEN = CONFIRMED
CREATE = CONFIRMED
AMOUNT = CONFIRMED
IDENTIFIERS = CONFIRMED
QUERY = CONFIRMED
PROCESS_CODE = CONFIRMED_WITH_SAFE_UNKNOWN_2
APN_PAYLOAD = CONFIRMED
APN_ORDER_KEY = CONFIRMED
APN_PROVIDER_TX_BINDING = CONFIRMED
APN_ACCOUNT_BINDING = CONFIRMED
CHECKSUM_AUTH = CONFIRMED_INTEGRITY_ONLY
ACK = CONFIRMED_BODY_WITH_NONBLOCKING_HTTP_METADATA_DEBT
APN_DUPLICATE = CONFIRMED
APN_RETRY = CONFIRMED
APN_REPLAY = RESOLVED_BY_REVALIDATION_QUERY_AND_NATIVE_IDEMPOTENCY
CREATE_TIMEOUT_RECOVERY = CONFIRMED
CREATE_IDEMPOTENCY = CONFIRMED
```

## B1 — Amount Contract

### Official CCAT fields

| Surface | Field | Type / format | Unit and rule | Provenance |
| --- | --- | --- | --- | --- |
| Create | `order_amount` | JSON `number`; integer only | whole TWD dollars (`元`); decimal places rejected | p.15 field table；p.17 abnormal items 13–16 |
| Create response / Query | `order_amount` | JSON `number` | original order amount, whole TWD dollars | p.19 Query response table |
| Create response / Query | `bill_amount` | JSON `number` | payer-facing bill amount including externally added fee | p.19 Query response table |
| Query | `pay_amount` | JSON `number`, `0`/`NULL` before payment | actual paid amount, whole TWD dollars | p.20 Query response table；p.24–26 examples |
| APN | `amount` | JSON `number` | payment-slip amount, whole TWD dollars | p.35–36 APN table |
| APN | `pay_amount` | table says JSON `number`; sample encodes a digit string | actual paid amount, whole TWD dollars | p.37 table；p.38 sample |

The official specification describes the ibon cap as `20,000 元`, including any externally added handling fee (p.14). It does not publish one universal minimum because Create validation messages express account/service-dependent bounds. P04 must treat Provider rejection as authoritative for configured min/max and must not pre-invent a fixed minimum.

JeePay source is definitive on the platform side：`UnifiedOrderRQ.amount` and `PayOrder.amount` are `Long` values in minor units (`分`). Therefore:

```text
JEEPAY_AMOUNT_UNIT = integer minor units; TWD 100 = NT$1
CCAT_CREATE_AMOUNT_UNIT = whole TWD dollars; JSON number; scale 0
CCAT_QUERY_AMOUNT_UNIT = whole TWD dollars for order_amount/bill_amount/pay_amount
CCAT_APN_AMOUNT_UNIT = whole TWD dollars for amount/pay_amount
CONVERSION_RULE = require jeepayAmount % 100 == 0; ccatOrderAmount = jeepayAmount / 100 exactly
AMOUNT_COMPARISON_RULE = never compare APN pay_amount directly to local order_amount when an external fee exists; authenticate Query, require Query.order_amount * 100 == PayOrder.amount, require APN.amount == Query.bill_amount, and for paid state require APN.pay_amount == Query.pay_amount == Query.bill_amount
```

The p.25 official ibon example demonstrates why this distinction is mandatory：`order_amount` and `bill_amount/pay_amount` may differ when the payer-facing bill includes an external amount. Conversion and comparison use integer/`BigDecimal` exact arithmetic only. For APN `pay_amount`, P04 may accept the two representations shown by the same official document—JSON number or an all-digit string—then normalize to an exact scale-zero value; float/double and rounding are prohibited.

```text
B1_AMOUNT = RESOLVED
```

## B2 — `process_code` Contract

The runtime-relevant CVS values in authenticated specification appendix 1 are complete below. Terminal means terminal for the payment state; settlement may continue after payment success.

| process_code | Official Meaning | Payment Meaning | Terminal? | JeePay Mapping | Evidence |
| --- | --- | --- | --- | --- | --- |
| `0` | payment slip pending merchant confirmation | pre-payment | No | `WAITING` | p.94 appendix 1 |
| `1` | waiting to send payment notification | pre-payment | No | `WAITING` | p.94 appendix 1 |
| `2` | omitted from appendix; appears only in Query sample | `UNKNOWN` | Unknown | `NO_STATE_TRANSITION`; log and re-query | p.21 sample vs p.94 omission |
| `3` | waiting for payer | unpaid | No | `WAITING` | p.94 appendix 1 |
| `4` | payer has paid | paid | Yes | `SUCCESS` | p.94 appendix 1 |
| `5` | payment slip cancelled | closed/cancelled | Yes | `CLOSED` → JeePay `CONFIRM_FAIL` | p.94 appendix 1 |
| `6` | payment slip expired | closed/expired | Yes | `CLOSED` → JeePay `CONFIRM_FAIL` | p.94 appendix 1 |
| `7` | payout to merchant scheduled; reconciliation in progress | paid; settlement pending | Yes for payment | `SUCCESS` | p.94 appendix 1 |
| `8` | payout to merchant completed | paid; settlement completed | Yes | `SUCCESS` | p.94 appendix 1 |

Codes `13+` in the appendix are COCS/card lifecycle values and are outside ibon Phase 1. An unknown code never becomes success or failure by inference.

```text
PROCESS_CODE_7 = paid; scheduled payout; SUCCESS
PROCESS_CODE_8 = paid; payout completed; SUCCESS
V1_PROCESS_CODE_7_MAPPING = earlier V1 SUCCESS; later V1 WAITING
V1_PROCESS_CODE_8_MAPPING = earlier V1 SUCCESS; later V1 WAITING
V1_PROCESS_CODE_DRIFT = YES; the later V1 mapping for 7/8 contradicts the authenticated specification
OFFICIAL_CONTRACT_CONFLICT = Query sample p.21 contains code 2 while appendix p.94 omits code 2
B2_PROCESS_CODE = RESOLVED
```

The code-2 documentation conflict is non-blocking because `NO_STATE_TRANSITION` is a safe exhaustive runtime branch; P04 does not guess payment state and can continue reconciliation through Query.

## B3 — APN Identity and Security

### Transport and payload

```text
APN_METHOD = POST
APN_CONTENT_TYPE = application/json
APN_FIELDS = api_id, trans_id, order_no, amount, expire_time, status, payment_code, payment_detail, memo, create_time, modify_time, nonce, checksum, pay_date, pay_amount, optional invoice fields
```

The APN table does not contain a separate required/optional column. P04 must require the safety-critical ibon subset：`api_id`、`trans_id`、`order_no`、`amount`、`status`、`payment_code=2`、`nonce`、`checksum`; a paid callback also requires `pay_amount` and `pay_date`. Invoice fields remain ignored outside the enabled invoice feature. Evidence：p.35–37「APN 主動通知」。

### Four-layer identity binding

| Layer | Official / local identity | Binding rule |
| --- | --- | --- |
| A. Configured CCAT account | `username` / `cust_id` | p.10 explicitly states Token `username` and Create `cust_id` are the same customer financial code; store once as configured `custId` plus API password |
| B. Create outbound | `cust_id + cust_order_no` | `cust_id = configured custId`; `cust_order_no = PayOrder.payOrderId` and must remain stable |
| C. Incoming APN | `api_id + order_no + trans_id` | `api_id` is the assigned customer financial code; `order_no` is the merchant order number; `trans_id` is unique per payment slip |
| D. Local JeePay order | `payOrderId + channelOrderNo` | locate by `order_no == payOrderId`; bind the first fully validated `trans_id` to `channelOrderNo`; subsequent callbacks must match |

`PayOrder.mchOrderNo` remains merchant-facing and is not sent as CCAT's provider-scoped stable key. No second Provider transaction table or state machine is needed.

```text
APN_ORDER_BINDING = order_no ↔ outbound cust_order_no ↔ PayOrder.payOrderId
APN_PROVIDER_TX_BINDING = trans_id is the unique payment-slip transaction ID; first bind only after account/order/amount/checksum validation and authenticated Query confirmation, then require equality with PayOrder.channelOrderNo
APN_ACCOUNT_BINDING = compare APN api_id to configured custId; independently authenticate current state by Query using the same account-scoped Bearer token and cust_id
LOCAL_ORDER_BINDING = PayOrder.payOrderId
PROVIDER_TRANSACTION_BINDING = PayOrder.channelOrderNo
B3_APN_IDENTITY = RESOLVED
```

Query does not echo `trans_id`. Therefore `trans_id` alone never authorizes a state change: APN is an untrusted hint, and the account-scoped authenticated `CvsOrderQuery` for the same `cust_id + cust_order_no` must confirm order amount and an official paid `process_code`. A different first/subsequent `trans_id` is not auto-normalized; it fails closed for controlled reconciliation.

### Checksum

Authenticated specification p.37 defines:

```text
canonical = api_id + ":" + trans_id + ":" + amount + ":" + status + ":" + nonce
checksum = MD5(UTF-8 bytes of canonical), 32 hexadecimal characters
```

Field order and colon separators are normative. The document-wide encoding is UTF-8 (p.9); the official sample emits lowercase hexadecimal (p.38–39). Case sensitivity is not explicitly stated, so P04 should produce lowercase and compare a validated 32-hex input in constant time after case normalization. `nonce` is 10 characters composed from time plus four random digits; no TTL is specified.

This checksum has no merchant-held secret and provides message-integrity checking, not callback-origin authentication. Origin/state authorization comes from the account-scoped Bearer Query. The canonical expression matches official WooCommerce and both V1 traces：`MERCHANT_AUTHENTICATED_OFFICIAL_SPEC + PRIOR_IMPLEMENTATION MATCH`.

### APN status, amount and ACK

| APN status | Official meaning | Provider adapter action |
| --- | --- | --- |
| `A` | waiting for payer | Query; no success transition |
| `B` | paid | Query must confirm code `4`, `7`, or `8` and exact amount/identity before success |
| `C` | merchant cancellation | Query must confirm code `5`; map closed/fail |
| `D` | expired slip | Query must confirm code `6`; map closed/fail |
| `E` | payout scheduled | Query must confirm paid code `7` or `8`; success is already established |
| `I/J` | invoice notifications | outside payment-state scope; no PayOrder transition |

```text
AMOUNT_VERIFICATION = local amount ↔ Query.order_amount; APN.amount ↔ Query.bill_amount; APN.pay_amount ↔ Query.pay_amount ↔ Query.bill_amount for successful full payment
ACK_BODY = exact uppercase plain text OK
ACK_HTTP_STATUS = not specified by V1.28.1; HTTP 200 is OFFICIAL_IMPLEMENTATION + PRIOR_IMPLEMENTATION match
ACK_CONTENT_TYPE = not specified; text/plain is prior/official implementation behavior
INVALID_CALLBACK_BEHAVIOR = no normative HTTP status/body; fail closed and do not return OK
DUPLICATE_VALID_CALLBACK_ACK = OK after complete revalidation and exact committed-state check
```

Evidence：p.35 transport/Return table and p.36–37 payload; p.35 says an `OK` body stops further sends. No actual Provider callback was executed.

## B4 — Retry, Duplicate and Replay Regression

Authenticated specification p.35 confirms one immediate APN, then every 15 minutes, at most three sends per status code; any plain-text `OK` stops resend. p.39 also documents a manual APN resend control. This strengthens but does not replace the C02 safety design:

1. Revalidate every callback's account、order、transaction、checksum and amounts.
2. Use authenticated Query as the authoritative state.
3. Reuse JeePay's conditional `STATE_ING` transition and unique `(order_id, order_type)` merchant-notify record.
4. For concurrent duplicate callbacks, return `OK` from the Provider-specific update-fail hook only after re-reading exactly the same committed terminal state and `channelOrderNo`.
5. Treat replay as another untrusted reconciliation hint; no nonce TTL is assumed.

```text
B4_APN_RETRY_REPLAY = RESOLVED
```

## B5 — Create Ambiguous Failure and Idempotency

Authenticated specification establishes all necessary guarantees:

- `cust_order_no` must be unique under the same `cust_id` (p.15).
- A duplicate `CvsOrderAppend` is rejected：the Create abnormal list states that an already-uploaded merchant order number cannot be uploaded again (p.17).
- `CvsOrderQuery` looks up `cust_id + cust_order_no` and returns the same response fields as Create (p.17、p.19–20).
- Query not-found is the documented `ERROR` condition `找不到此筆代繳資訊` (p.22). No machine-readable error code is provided.

```text
ORDER_KEY = configured custId + PayOrder.payOrderId as cust_order_no
ORDER_KEY_UNIQUENESS_SCOPE = one cust_order_no under one cust_id
CREATE_ORDER_KEY_UNIQUENESS = CONFIRMED
CREATE_DUPLICATE_BEHAVIOR = REJECT DUPLICATE; never creates a second order under the same key
QUERY_AFTER_TIMEOUT_SUPPORTED = YES; Query uses the stable key and returns Create-equivalent fields
QUERY_NOT_FOUND_SEMANTICS = status ERROR + documented not-found message; no numeric error code
AUTOMATIC_CREATE_RETRY_SAFE = YES, only as a bounded same-key retry after Query not-found; never use a new key
AMBIGUOUS_FAILURE_RECOVERY = Query first; found recovers; exact not-found permits same-key Append; duplicate response triggers Query again
B5_CREATE_IDEMPOTENCY = RESOLVED
```

Recommended P04 algorithm:

```text
Append once with stable cust_id + cust_order_no
→ normal OK: validate and persist Provider result
→ deterministic business error: do not retry unchanged invalid request
→ ambiguous transport/HTTP result: Query stable key
   → found: validate account/order/order_amount and recover original Provider result
   → exact documented not-found: bounded Append retry with the identical key
      → OK: persist result
      → duplicate rejection: Query again and recover
      → other error/ambiguity: stop automatic retry and reconcile later
```

| Failure | May CCAT Have Received Request? | Contract-supported safe action | Automatic retry? |
| --- | ---: | --- | --- |
| DNS/connect failure proven before send | No; otherwise unknown | Query stable key; on exact not-found retry same key | Bounded same-key only |
| connection reset | Yes | Query; found recover; exact not-found same-key retry | Bounded same-key only |
| read timeout | Yes | Query; found recover; exact not-found same-key retry | Bounded same-key only |
| HTTP error | Yes | Query first; do not infer failure from HTTP alone | Only exact not-found + same key |
| business error | Request processed | Fix/reject according to documented error; do not blindly retry | No, unless a later Query proves not-found and request is otherwise valid |
| duplicate order | Yes; original key exists | Query original; never generate a replacement key | No additional blind Append |
| query not found | No record found at query time | Same-key Append is duplicate-safe because Provider rejects an existing key | Bounded same-key retry |

Network receipt classification is protocol inference; the duplicate safety conclusion is grounded in the official stable-key uniqueness and rejection rules. A timeout is never treated as proof of failure.

## 31-Item Runtime Definition of Ready

Status vocabulary：`READY`、`NONBLOCKING_PARTIAL`、`BLOCKED`、`CONFLICT`。

| # | Readiness item | Result | Evidence / treatment |
| ---: | --- | --- | --- |
| 1 | Token endpoint | READY | p.10 `POST /Token` |
| 2 | Token authentication fields | READY | p.10 password grant、username、password |
| 3 | Token response semantics | READY | p.10; use `.expires` as explicitly stated authoritative expiry |
| 4 | Create endpoint | READY | p.15 `POST /api/Collect` |
| 5 | Create required fields | READY | p.15–17 field tables |
| 6 | ibon constants | READY | `payment_type=0`, `payment_acquirerType=2`, p.15 |
| 7 | amount mapping | READY | whole TWD ↔ exact JeePay cents, p.14–20 |
| 8 | outbound order identity | READY | stable unique `cust_id + cust_order_no`, p.15 |
| 9 | Provider transaction identity | READY | APN `trans_id` unique per payment slip, p.35 |
| 10 | ibon payment code | READY | response/query ibon fields, p.19 |
| 11 | WAITING semantics | READY | code `3` and issuance-before-payment, p.94 |
| 12 | Query endpoint | READY | p.19 |
| 13 | Query key | READY | `cust_id + cust_order_no`, p.19 |
| 14 | Query amount | READY | p.19–20 |
| 15 | process_code state mapping | NONBLOCKING_PARTIAL | all appendix CVS codes mapped; sample-only code `2` fail-closes with no transition |
| 16 | APN method | READY | p.35 POST |
| 17 | APN content type/payload | READY | p.35–37 JSON field table |
| 18 | local order identity | READY | APN `order_no` ↔ outbound merchant order key |
| 19 | Provider transaction binding | READY | unique `trans_id`; bind after authenticated Query |
| 20 | account binding | READY | `username=cust_id`; APN `api_id` account code; account-scoped Query |
| 21 | checksum/auth | READY | MD5 integrity + Bearer-authenticated Query, p.10、p.37 |
| 22 | amount verification | READY | local/Query/APN cross-surface comparison above |
| 23 | ACK | NONBLOCKING_PARTIAL | body `OK` confirmed; HTTP status/content type unspecified normatively |
| 24 | duplicate semantics | READY | official resend contract + native JeePay idempotency |
| 25 | retry semantics | READY | immediate + 15-minute + three-per-status, p.35 |
| 26 | replay/residual-risk treatment | NONBLOCKING_PARTIAL | nonce shape confirmed but TTL absent; revalidation/Query/idempotency is safe |
| 27 | Create timeout recovery | READY | stable Query returns Create-equivalent response |
| 28 | duplicate Create behavior | READY | duplicate rejected, p.17 |
| 29 | retry/idempotency strategy | READY | query-first bounded same-key retry |
| 30 | JeePay native Provider mapping | READY | native SPI/fields suffice |
| 31 | no RED core modification required | READY | Provider-specific GREEN path; no RED change evidenced |

```text
READY = 28
NONBLOCKING_PARTIAL = 3 (#15, #23, #26)
BLOCKED = 0
CONFLICT = 0
```

The code-2 source inconsistency is recorded as `OFFICIAL_CONTRACT_CONFLICT`, but the readiness item is non-blocking rather than runtime `CONFLICT` because P04 has a complete conservative branch that performs no state transition.

## Official vs V1 Drift

| Area | Authenticated V1.28.1 | V1 behavior | Result |
| --- | --- | --- | --- |
| Amount | whole `元`, no decimals; order/bill/paid amount are distinct | whole-TWD conversion | `MATCH`, but V1 must preserve external-fee distinction |
| `process_code=2` | sample-only, absent from appendix | later V1 pending | `OFFICIAL_CONTRACT_CONFLICT`; V1 meaning remains unverified |
| `process_code=7` | paid, payout scheduled | earlier success; later pending | later variant `PRIOR_IMPLEMENTATION_DRIFT` |
| `process_code=8` | paid, payout completed | earlier success; later pending | later variant `PRIOR_IMPLEMENTATION_DRIFT` |
| APN account | account code plus account-scoped Query | later V1 compares configured API ID | concept matches; V2 follows authenticated naming/binding |
| Query not found | exact official error text | V1 substring matching | V1 is broader/brittle; V2 uses exact official condition |
| Create retry | duplicate same-key rejected | query-before-retry | safe only with the authenticated same-key algorithm above |

```text
PRIOR_IMPLEMENTATION_DRIFT = process_code 7/8 in later V1; broad Query not-found matching
```

## Remaining Blocking Unknowns

```text
NONE
```

## Non-Blocking Debt

- `process_code=2` appears in a Query sample but has no appendix meaning; keep `NO_STATE_TRANSITION` and seek written CCAT clarification when convenient.
- V1.28.1 specifies ACK body but not response HTTP status/content type or invalid-callback response.
- Nonce uniqueness construction is documented, but freshness lifetime and replay window are not.
- Token prose says three hours while the response table describes a default duration differently; use `.expires`, which the prose explicitly makes authoritative.
- Exact account-specific Create minimum/maximum below the global ibon cap is configuration-dependent and remains Provider-validation behavior.
- Token cross-instance reuse/revocation and retry-on-401 optimization are not needed for correctness.
- Optional metadata、presentation shape、unused payment methods and all non-ibon capabilities remain outside Phase 1.

## Controlled Live Validation Required

```text
CONTROLLED_LIVE_VALIDATION_REQUIRED_FOR_RUNTIME_GATE = NONE
```

Later environment/E2E work should verify sandbox/prod credential binding、exact ACK transport behavior and representative whole-TWD/external-fee vectors, but these checks do not block P04 implementation and were not executed in C03.

## Runtime Gate

```text
VERDICT = PASS-WITH-DEBT
CCAT_RUNTIME_GATE = OPEN
CCAT_CANONICAL_CONTRADICTIONS = 0
RUNTIME_SOURCE_MODIFICATIONS = CCAT_PROVIDER_ONLY
CCAT_RUNTIME_IMPLEMENTATION = COMPLETE
CCAT_OFFLINE_ACCEPTANCE = PASS
```

## Next Session

```text
NEXT = JEE-E02 CCAT Development Environment Binding & Controlled E2E
```

P04 implemented Create、Provider Query and APN on JeePay's native Provider Extension Contract. Offline acceptance passed with 46 CCAT-specific tests and full backend compile/test/package; no live Provider call or production validation was performed. The three nonblocking contract debts remain open.
