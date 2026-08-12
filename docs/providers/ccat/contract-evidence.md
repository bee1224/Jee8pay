# CCAT ibon Contract Evidence

Status: `PARTIAL — BLOCKING UNKNOWNS`
Recovery session: `JEE-C01`
Recovery date: `2026-08-12`

```text
EVIDENCE_RECOVERY = PARTIAL
CCAT_RUNTIME_GATE = CLOSED
REAL_SECRETS_EXPOSED = 0
```

本文件只記錄 CCAT ibon Create、Provider Query 與 APN。證據標籤依序為 `OFFICIAL`、`OFFICIAL-SDK`、`OFFICIAL-IMPLEMENTATION`、`OBSERVED`、`PRIOR-IMPLEMENTATION`、`INFERENCE`。Implementation 只能證明該版本如何運作，不能替代未公開的 normative contract。

## Evidence Inventory

| Source | Revision / provenance | Classification | Scope |
| --- | --- | --- | --- |
| [CCAT download page](https://www.ccat.com.tw/Home/Download) | accessed 2026-08-12 | OFFICIAL | CCAT-owned SDK、WooCommerce repository 與登入後串接文件入口 |
| `ccatpay/PHP_SDK` | `60e66bb679d015f883d80651d46e92864eebcb2f` | OFFICIAL-SDK | Token、CVS Create/Query request surface |
| `ccatpay/NET_SDK` | `0383d1fa36a9b329beec42db7936aa5b82371cd9` | OFFICIAL-SDK | Request/response models、field comments、SDK guide |
| `ccatpay/ccat-for-woocommerce` | `5f3a4357c8c42114676a29acb3b6f1ab18dfcbf3` | OFFICIAL-IMPLEMENTATION | ibon constants、Create、APN checksum/status/ACK observed behavior |
| Development VPS `/opt/payment/payment-service` | no Git metadata; key file SHA-256 recorded below | PRIOR-IMPLEMENTATION | Earlier V1 client/service/notify/tests |
| Development VPS `/opt/payment/payment-service-sandbox` | no Git metadata; key file SHA-256 recorded below | PRIOR-IMPLEMENTATION | Later V1 client/service/notify/idempotency tests |
| Production VPS specified source roots | both roots absent | OBSERVED | `V1_PRODUCTION_SOURCE_ACCESS = UNAVAILABLE` |

Official repository SHAs were freshly verified from GitHub. The .NET SDK guide identifies itself as V1.1 dated 2022-08-31 and explicitly says the SDK wraps the separate `多元支付平台 WEBAPI 規格書`. The PHP README likewise directs API/error-code questions to the integration document or CCAT developers. The CCAT download page makes manuals available only after contract-member login. Therefore the public SDK is intentionally not the complete normative specification.

V1 was inspected read-only. No `.env`、raw container environment、credential value、live Provider call、payment、restart、deployment or data mutation was used. The V1 roots have no `.git`, so provenance uses paths, timestamps and SHA-256 instead of commit IDs:

| V1 file | UTC mtime | SHA-256 |
| --- | --- | --- |
| `payment-service/internal/provider/ccat/client.go` | 2026-08-04 14:33:30 | `b4f2513338f9975b9d6516bc532f6133196b415457c1273f1a7d697b27d068f7` |
| `payment-service/internal/provider/ccat/client_test.go` | 2026-08-04 14:33:12 | `003f44f587cd143683611a2643547018857e709c5f114a9917f8c6fe2db71eb3` |
| `payment-service-sandbox/internal/provider/ccat/client.go` | 2026-08-08 20:21:33 | `3780ae5aa82e67942d168067c208d4c66553904961096c022e8e512cdb4fbfb7` |
| `payment-service-sandbox/internal/provider/ccat/client_test.go` | 2026-08-08 20:21:33 | `f6fa67537401e8e96599e908912cafb1360b9b06f82e395e3ef07189e74d636e` |

## Contract Verdict

```text
TOKEN = PARTIAL
CREATE = PARTIAL
AMOUNT = PARTIAL
IDENTIFIERS = PARTIAL
QUERY = PARTIAL
PROCESS_CODE = CONFLICT
APN_PAYLOAD = PARTIAL
APN_ORDER_KEY = PARTIAL
APN_ACCOUNT_BINDING = UNKNOWN
CHECKSUM_AUTH = PARTIAL
ACK = PARTIAL
APN_RETRY = UNKNOWN
CREATE_IDEMPOTENCY = UNKNOWN
```

## Token Contract

| Item | Evidence-backed finding | Evidence |
| --- | --- | --- |
| Method/path | `POST {base}/Token` (`token` case differs among implementations) | OFFICIAL-SDK、OFFICIAL-IMPLEMENTATION、PRIOR-IMPLEMENTATION |
| Content type | `application/x-www-form-urlencoded` | OFFICIAL-IMPLEMENTATION、PRIOR-IMPLEMENTATION |
| Fields | `grant_type=password`、`username`、`password` | OFFICIAL-SDK、OFFICIAL-IMPLEMENTATION、PRIOR-IMPLEMENTATION |
| Response | `access_token`、`token_type`、`expires_in`、`.issued`、`.expires`; `error`、`error_description` | OFFICIAL-SDK |
| Collect auth | `Authorization: Bearer <token>` | OFFICIAL-SDK、OFFICIAL-IMPLEMENTATION、PRIOR-IMPLEMENTATION |
| V1 reuse | process-local cache; reuse until `.expires - 1 minute`; evict and retry once on HTTP 401 | PRIOR-IMPLEMENTATION |
| Normative reuse/revocation/401 policy | `UNKNOWN` | Public material omits lifecycle rules |

`TOKEN_CONTRACT = PARTIAL`. Protocol acquisition is confirmed; V1 cache/retry is prior behavior, not an official lifecycle guarantee. This unknown is non-blocking if V2 fails closed and avoids unsafe repeated business requests.

## Create / CvsOrderAppend

| Item | Finding | Evidence |
| --- | --- | --- |
| Method/path/content | `POST api/Collect`, JSON, Bearer | OFFICIAL-SDK、OFFICIAL-IMPLEMENTATION、PRIOR-IMPLEMENTATION |
| Command | `cmd=CvsOrderAppend` | OFFICIAL-SDK、OFFICIAL-IMPLEMENTATION、PRIOR-IMPLEMENTATION |
| SDK field surface | `cust_id`、`cust_order_no`、`order_amount`、`expire_date`、payer name/postcode/address/mobile/email、`payment_type`、`payment_acquirerType`、`apn_url`、`order_detail` | OFFICIAL-SDK |
| ibon constants | `payment_type=0`, `payment_acquirerType=2` | OFFICIAL-IMPLEMENTATION + PRIOR-IMPLEMENTATION MATCH |
| ibon limit | amount upper bound 20,000 including added handling fee | OFFICIAL-SDK comment |
| Result envelope | `status` (`OK`/`ERROR`)、`msg`、`cust_order_no` | OFFICIAL-SDK |
| Payment artifact | `ibon_shopid`、`ibon_code`、`expire_date`、`bill_amount`; implementation also accepts `short_url` | OFFICIAL-SDK、OFFICIAL-IMPLEMENTATION |
| Success semantic | `status=OK` leaves order pending and exposes payment instructions; it is not paid | OFFICIAL-IMPLEMENTATION + PRIOR-IMPLEMENTATION MATCH |
| Complete required/length/expiry/error rules | `UNKNOWN` | Normative integration document unavailable |

V1 sends every SDK core/payer field, whole-TWD `order_amount`, the two ibon constants and its notify URL. The later V1 validates Create response `cust_order_no` and `order_amount`; this is a sound prior safety pattern, not proof that every successful CCAT response contains both fields.

## Amount Contract

| Surface | Representation/evidence |
| --- | --- |
| JeePay `PayOrder.amount` | integer smallest currency unit (`Long`); TWD mapping is cents |
| Official SDK Create/Query model | `decimal? order_amount`; described as collection amount, ibon cap 20,000 |
| Official WooCommerce Create | passes the TWD order total directly; displays `bill_amount` as an integer |
| Earlier and later V1 | require whole positive TWD; send `AmountCents / 100`; parse Create/Query/APN integer values and multiply by 100 |
| APN/query normative unit/scale | `UNKNOWN` |

```text
JeePay amount cents = exact integer
CCAT V1 mapping = whole TWD integer = JeePay cents / 100
CCAT_AMOUNT_MAPPING = PARTIAL
```

The sources consistently point to major TWD units, but the public SDK types the value as decimal and does not define scale、rounding or whether Create、Query、`bill_amount`、APN `amount` and `pay_amount` share an identical representation. V2 must reject non-whole-TWD orders unless the merchant specification supplies a different exact rule. Conversion and comparison must use integer/`BigDecimal`, never float/double. Money correctness remains blocked until one normative CCAT statement or an official non-production vector confirms the cross-surface mapping.

## Identifier Contract

| Identity | V1 use | V2 mapping | Status |
| --- | --- | --- | --- |
| Merchant Order ID | `merchant_order_no`, local idempotency key | `PayOrder.mchOrderNo`, local only | CONFIRMED JeePay responsibility |
| JeePay PayOrder ID | V1 equivalent is generated platform `order.OrderNo` | `PayOrder.payOrderId` → `cust_order_no` | DESIGN DECISION, conditional on official length/charset acceptance |
| Provider outbound order ID | V1 `order.OrderNo` → `cust_order_no`; Query uses same value | reuse `PayOrder.payOrderId`; do not add DB table | OFFICIAL-SDK uniqueness + PRIOR behavior |
| CCAT transaction ID | V1 reads APN/Query `trans_id` into Provider trade number | `PayOrder.channelOrderNo` only after authoritative Query/APN confirms it | PARTIAL; SDK response model does not document it |

The SDK states `cust_order_no` must be unique under one `cust_id`. JeePay generates `payOrderId` as `P` plus a distributed numeric ID (normally 20 characters), which matches V1's later 20-character limit, but that limit itself is only prior implementation evidence. `mchOrderNo` must not be sent as the CCAT ID because its uniqueness domain is merchant-facing, not provider-config-facing.

## Query / CvsOrderQuery

| Item | Finding | Evidence |
| --- | --- | --- |
| Method/path/content | same JSON Bearer `POST api/Collect` | OFFICIAL-SDK、PRIOR-IMPLEMENTATION |
| Command | `CvsOrderQuery` | OFFICIAL-SDK、PRIOR-IMPLEMENTATION |
| Lookup | `cust_id` + `cust_order_no` | OFFICIAL-SDK、PRIOR-IMPLEMENTATION |
| Response surface | `status`、`msg`、`cust_order_no`、`order_amount`、`process_code`、`process_code_update_time`、`pay_date` | OFFICIAL-SDK |
| Additional V1 fields | optional `cust_id`、`api_id`、`trans_id`、`pay_amount` | PRIOR-IMPLEMENTATION only |
| Not found | substring match on `msg` for `找不到` / `not found` | PRIOR-IMPLEMENTATION; brittle and non-normative |

V1 query validates local order、amount and optional account/transaction fields before accepting state. V2 should retain that pattern, but cannot map payment state until the official `process_code` contract is obtained.

## process_code Contract

| process_code | Official Meaning | Terminal? | Payment Meaning | JeePay Mapping | Evidence |
| --- | --- | --- | --- | --- | --- |
| `0` | UNKNOWN | UNKNOWN | V1 later: pending | no transition | PRIOR-IMPLEMENTATION |
| `1` | UNKNOWN | UNKNOWN | V1 later: pending | no transition | PRIOR-IMPLEMENTATION |
| `2` | UNKNOWN | UNKNOWN | V1 later: pending; earlier V1 unsupported | no transition | PRIOR-IMPLEMENTATION DRIFT |
| `3` | UNKNOWN | UNKNOWN | V1 later: pending | no transition | PRIOR-IMPLEMENTATION |
| `4` | UNKNOWN | UNKNOWN | both V1 variants: paid | `CONFIRM_SUCCESS` only after official confirmation | PRIOR-IMPLEMENTATION |
| `5` | UNKNOWN | UNKNOWN | both V1 variants: failed | `CONFIRM_FAIL` only after official terminal confirmation | PRIOR-IMPLEMENTATION |
| `6` | UNKNOWN | UNKNOWN | both V1 variants: expired | `CONFIRM_FAIL` only after official irrevocability confirmation | PRIOR-IMPLEMENTATION |
| `7` | UNKNOWN | UNKNOWN | earlier V1: paid; later V1: pending | no transition | `EVIDENCE_CONFLICT` |
| `8` | UNKNOWN | UNKNOWN | earlier V1: paid; later V1: pending | no transition | `EVIDENCE_CONFLICT` |
| not found | UNKNOWN | UNKNOWN | V1 parses message text | no transition | PRIOR-IMPLEMENTATION |

`PROCESS_CODE = CONFLICT`. Public official source only names the field. The conflict for `7`/`8` exists between two deployed V1 source traces, so V1 cannot supply the missing status table.

## APN Contract

### Payload and status

The official WooCommerce ibon handler observes `POST` JSON. Its shared validation requires `api_id`、`trans_id`、`amount`、`status`、`nonce`、`checksum`; the CVS handler additionally reads `order_no`. Later V1 requires those six plus `pay_amount`、`payment_code=2` and `order_no` or `cust_order_no`, then performs authenticated `CvsOrderQuery` before accepting state.

| APN status | Official implementation meaning | V1 later mapping | V2 action |
| --- | --- | --- | --- |
| `A` | waiting for payer | pending | no success transition |
| `B` | payer paid | paid | accept only after authoritative Query confirms paid |
| `C` | merchant cancellation | failed | fail only after official/normative terminal semantics |
| `D` | expired payment slip | expired | fail only after official/normative irrevocability |
| `E` | scheduled remittance to merchant | pending | no payment-state transition |
| `I` / `J` | invoice notifications | outside phase-1 payment state | ignore for payment state |

This table is `OFFICIAL-IMPLEMENTATION`, not the normative retry/status specification. `APN_PAYLOAD = PARTIAL` because `order_no` is consumed but omitted from the same implementation's required-field list, and the public material does not define `pay_amount`、`payment_code` or the relationship between `order_no` and `cust_order_no`.

### Order and account binding

Official WooCommerce stores generated `cust_order_no` as order metadata and later looks up APN `order_no` by that value. V1 similarly treats `order_no`/`cust_order_no` as its platform order ID. This is matching implementation evidence, but the field-name relationship remains undocumented: `APN_ORDER_KEY = PARTIAL`.

The official handler requires `api_id` but does not bind it to configured `username` or `cust_id`. Later V1 introduces separate `Username`、`CustomerID` and `APIID`, compares APN `api_id` to configured APIID, allowlists local merchant/customer IDs, and optionally validates Query `cust_id`/`api_id`. Those semantics have no official public definition: `APN_ACCOUNT_BINDING = UNKNOWN`.

### Checksum / authentication

For the shared payment/CVS callback, official WooCommerce and both V1 variants match exactly:

```text
canonical = api_id + ":" + trans_id + ":" + amount + ":" + status + ":" + nonce
checksum = lowercase hexadecimal MD5(UTF-8 bytes of canonical)
```

This is `OFFICIAL-IMPLEMENTATION + PRIOR-IMPLEMENTATION MATCH`. The separate WooCommerce APP handler's secret-prefixed `$`-joined `chk` belongs to another product and is not an ibon evidence conflict.

The matched ibon checksum contains no merchant-held secret, so it detects corruption but does not authenticate the sender. V1 comments and code make authenticated OAuth Query the trust boundary. V2 may use APN only as an untrusted reconciliation hint and transition state solely after Query verifies order、account (where returned)、transaction (where returned)、amount and official paid `process_code`. Until the Query status and account/transaction fields are normatively defined, `CHECKSUM_AUTH = PARTIAL`; it must not be described as cryptographic origin authentication.

A dummy `LOCAL_TEST_VECTOR` may be created from this canonicalization during implementation, but it must not be called official. No production material may be used.

### ACK, retry, duplicate and replay

Official WooCommerce and V1 return HTTP 200 body exactly `OK` after a valid callback. Later V1 sets `text/plain; charset=utf-8`. Official implementation returns a 400-class response for validation failure. Content type、case sensitivity、Provider retry schedule and required failure response remain undocumented: `ACK = PARTIAL`, `APN_RETRY = UNKNOWN`.

V1 persists a SHA-256 replay key derived from the checksum canonical tuple and uses DB uniqueness plus an atomic state/ledger transaction. Legitimate duplicate paid APNs produce no second state transition、ledger or merchant callback and still receive `200 OK`. This is a sound `PRIOR-IMPLEMENTATION` pattern and aligns with JeePay core idempotency, but official nonce uniqueness、scope、lifetime and retry semantics are absent:

```text
APN_REPLAY_CONTRACT = UNKNOWN
```

V2 must revalidate every duplicate, reuse JeePay's existing state transition / merchant-notify deduplication, and acknowledge only according to a confirmed CCAT rule. Do not add a Redis replay subsystem based solely on V1.

## Create Timeout / Idempotency

Later V1 persists a stable outbound order ID and checkout claim. On a same-request retry it first calls `CvsOrderQuery`; it creates again only when its message-substring parser concludes not found. This is `PRIOR-IMPLEMENTATION`, not a Provider idempotency guarantee. Official SDK states only uniqueness of `cust_order_no` under `cust_id`; it does not document duplicate-Create response or response-lost retry behavior.

```text
CREATE_RETRY_SAFE = UNKNOWN
HIGH_RISK_BLOCKER = duplicate Create / response-lost semantics
```

V2 must preserve the same `PayOrder.payOrderId`, query before any retry, and must not treat an undocumented not-found message as proof that a retry is safe.

## Payment Code / JeePay Response

SDK fields `ibon_shopid`、`ibon_code`、`expire_date` and `bill_amount` are confirmed. Current official WooCommerce concatenates `ibon_shopid + ibon_code` when `short_url` is absent and otherwise exposes `short_url`. V1 later exposes `short_url`/`url`, or renders local instructions containing `ibon_code`.

For V2, use existing `CommonPayDataRS.payUrl` when an official URL exists. The existing generic response has no confirmed structured tuple for all four ibon fields. This is a YELLOW response-representation decision, but it does not justify a RED core change or CCAT-specific DB table.

## V1 vs Official Comparison

| Contract Area | Official | V1 Implementation | Match | Confidence |
| --- | --- | --- | --- | --- |
| Token | POST form password grant; response fields | same; local cache and one 401 retry | surface match | High surface / low lifecycle |
| Create | POST JSON `CvsOrderAppend` fields | same field set | match | High surface |
| ibon constants | official implementation `0` / `2` | `0` / `2` | match | High implementation evidence |
| Amount | decimal major-amount surface; Woo total direct | whole TWD integer ↔ cents | partial | Blocking |
| Query | `cust_id + cust_order_no`; response fields | same plus validation | surface match | High surface / low semantics |
| process_code | field only | two V1 variants disagree on `7`/`8` | `PRIOR_IMPLEMENTATION_DRIFT` | Blocking conflict |
| APN | POST JSON, shared six fields, CVS `order_no` | same plus extra fields/query confirmation | partial | Blocking completeness |
| Checksum | secretless colon-joined MD5 in official implementation | exact match | match | High algorithm / not authentication |
| ACK | observed HTTP 200 `OK` | HTTP 200 text `OK` | match | Medium; normative retry unknown |
| Duplicate/replay | no normative contract | DB replay/state/ledger guards | prior only | Blocking replay semantics |
| Create idempotency | uniqueness only | query-before-retry using message substring | incomplete | High-risk blocking |

## P03 Definition of Ready

| # | Readiness item | Result | Blocking reason / evidence |
| ---: | --- | --- | --- |
| 1 | Token | READY WITH NON-BLOCKING LIFECYCLE UNKNOWN | acquisition confirmed |
| 2 | Create endpoint | READY | official SDK |
| 3 | required fields | NOT READY | full normative required/length rules unavailable |
| 4 | ibon constants | READY | official implementation + V1 match |
| 5 | outbound ID | PARTIAL | stable `payOrderId` design; official length/charset absent |
| 6 | amount | NOT READY | cross-surface unit/scale not normative |
| 7 | payment code | READY | official SDK fields |
| 8 | Create waiting semantics | READY | official implementation + V1 match |
| 9 | Query endpoint | READY | official SDK |
| 10 | Query key | READY | `cust_id + cust_order_no` |
| 11 | paid state | NOT READY | no official `process_code` meaning |
| 12 | necessary process_code mapping | NOT READY | V1 `7`/`8` conflict |
| 13 | APN payload | NOT READY | consumed fields exceed validated/documented fields |
| 14 | APN local-order key | PARTIAL | implementation uses `order_no`; normative mapping absent |
| 15 | APN account binding | NOT READY | `api_id` semantics unknown |
| 16 | checksum/auth | PARTIAL | checksum confirmed; no origin authentication; Query contract incomplete |
| 17 | APN amount verification | NOT READY | amount representation incomplete |
| 18 | ACK | PARTIAL | 200 `OK` implementation match; normative retry/failure rules absent |
| 19 | duplicate behavior | PARTIAL | local idempotency proven; Provider resend/nonce rules unknown |
| 20 | Create timeout/idempotency | NOT READY | `CREATE_RETRY_SAFE = UNKNOWN` |
| 21 | JeePay response representation | PARTIAL | URL works; structured ibon tuple unresolved |
| 22 | no RED core modification | READY | native SPI suffices; response issue is at most YELLOW |

## Resolved Blockers

1. V1 source access recovered on Development VPS and precisely fingerprinted.
2. Token、Create、Query endpoint and request surfaces match official SDK.
3. `payment_type=0` and `payment_acquirerType=2` match official ibon implementation.
4. Create `status=OK` means payment instructions issued / waiting, not paid.
5. APN checksum canonicalization is an official-implementation + V1 match; the APP `chk` path is a different product, not an ibon conflict.
6. Official implementation and V1 both observe successful APN response as HTTP 200 body `OK`.
7. Existing JeePay Provider SPI can implement all three capabilities without RED core changes.

## Remaining Blocking Unknowns

1. Merchant-versioned WEBAPI specification: complete Create requirements、length/charset、expiry and errors.
2. Normative Create/Query/APN amount unit、scale and string/decimal/integer representation.
3. Official complete `process_code` table; specifically resolve V1 conflict for `7` and `8`, plus paid、waiting、failed、expired/cancelled and not-found.
4. APN full payload and normative relationship among `order_no`、`cust_order_no`、`trans_id`、`pay_amount` and `payment_code`.
5. `api_id` account-binding semantics and authoritative Query identity/transaction fields.
6. APN retry、failure ACK、duplicate and nonce/replay rules; successful `200 OK` alone is insufficient.
7. Duplicate Create and response-lost idempotency guarantee; exact Query not-found code/semantic.

## Non-blocking Unknowns

- Token revocation、cross-instance reuse and official retry-on-401 policy if V2 fails closed.
- Optional `short_url` availability and display metadata.
- Refund、transfer、division、channel user、close、COCS、DPH、ATM and other CCAT products, all outside phase 1.

## Runtime Gate

```text
CCAT_RUNTIME_GATE = CLOSED
```

Remaining unknowns directly affect money correctness、order/account identity、callback trust、idempotency and Provider acknowledgement. V1 code narrows the questions but cannot replace the contract, and its own `process_code=7/8` drift proves it is unsafe as normative truth.

## Next Session

```text
JEE-C02 CCAT Merchant WEBAPI Specification Closure
```

Required input: the merchant-versioned `多元支付平台 WEBAPI 規格書` or written CCAT confirmation covering the seven remaining blockers above. Evidence may be stored only if credentials and merchant-specific secrets are redacted. Do not start JEE-P04 until this document's blocking items are closed.
