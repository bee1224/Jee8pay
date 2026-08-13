# JEE-P05 — CCAT Create Failure Semantics & Observability Fix

Implementation date: `2026-08-13`

Scope: CCAT GREEN adapter, tests, and Provider documentation only. No real Provider request was made.

## Verdict

```text
JEE_P05 = PASS
ROOT_DEFECT_FIXED = YES
FALSE_SUCCESS_FIXED = YES
EMPTY_PAYDATA_SUCCESS_IMPOSSIBLE = YES
PROVIDER_REJECTION_OBSERVABLE = YES
AMBIGUOUS_CREATE_SAFE = YES
SECRET_EXPOSURE = 0
SAME_DAY_EXPIRE_STATEMENT = RUNTIME_ACCEPTED
NORMATIVE_SAME_DAY_EXPIRE_SUPPORT = NOT_SPECIFIED
CCAT_TESTS = 56/56 PASS
BACKEND_TESTS = 64/64 PASS
RED_CORE_CHANGED = NO
SHARED_YELLOW_CHANGED = NO
REAL_PROVIDER_REQUESTS = 0
REAL_ORDERS_CREATED = 0
RECOMMENDED_NEXT_STEP = Independent fresh-session review of this diff; only after acceptance and explicit human authorization, deploy to Development and perform exactly one new External Merchant UAT Create
```

## Modified files

Runtime and tests:

- `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatClient.java`
- `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatIbonOrderRS.java`
- `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/payway/CcatIbon.java`
- `jeepay/jeepay-payment/src/test/java/com/jeequan/jeepay/pay/channel/ccat/CcatClientTest.java`
- `jeepay/jeepay-payment/src/test/java/com/jeequan/jeepay/pay/channel/ccat/CcatKitTest.java`
- `jeepay/jeepay-payment/src/test/java/com/jeequan/jeepay/pay/channel/ccat/payway/CcatIbonTest.java`

Provider documentation:

- `docs/providers/ccat/README.md`
- `docs/providers/ccat/provider-design.md`
- `docs/providers/ccat/contract-evidence.md`
- `docs/providers/ccat/JEE-E05-external-create-investigation.md`
- `docs/providers/ccat/JEE-P05-create-failure-semantics.md`

## Root defect

Before JEE-P05, `CcatIbon.pay` caught a deterministic Provider business rejection and returned `ChannelState.API_RET_ERROR` on an otherwise empty `CcatIbonOrderRS`. Native Core maps `API_RET_ERROR` to `PayOrder.STATE_ING`; `UnifiedOrderController` then asks the Provider response to build payment data for the ING order. Fastjson omitted every null field and serialized `{}`, so the Merchant received a signed success envelope with an unusable waiting order.

The same recovery path also sent a second Append after an ambiguous Create followed by Query not-found. That retry was bounded and used the same stable key, but it did not satisfy JEE-P05's stricter no-blind-retry rule.

## Implemented behavior

### Deterministic rejection

Provider `status != OK` and deterministic HTTP 4xx responses now map to native `ChannelRetMsg.confirmFail`. Potentially ambiguous `408/409/425/429` remain on the reconciliation path. Core remains the sole owner of the PayOrder transition and writes `STATE_FAIL` plus the adapter error code/message. UnifiedOrder does not call payment-data builders for failed orders.

The Merchant-facing error is deliberately generic (`CCAT_BUSINESS` / `CCAT Provider rejected request`). Provider detail remains in sanitized structured logs so an echoed credential cannot escape through the response.

### Ambiguous Create

Timeout, connection reset, HTTP 5xx, or malformed/insufficient Create response remains `UNKNOWN` with `needQuery=true`. The adapter performs at most one immediate Query using the same stable order key:

```text
Append once
→ Query confirms order: validate instructions and return WAITING
→ Query not-found / inconclusive / error: stop; no second Append
→ native PayOrderReissue continues Query-only reconciliation
```

This preserves native Core state/MQ ownership and removes automatic Create replay.

### Empty-payData invariant

`CcatIbonOrderRS.buildPayData` now requires either the complete ibon shop/code pair or a nonblank Provider `shortUrl`. An empty response fails closed. Consequently `code=0 + orderState=ING + payData={}` cannot be produced by CCAT_IBON.

For an ambiguous order with no instruction, Core may already have persisted native ING before the outer UnifiedOrder response is built. The Provider-local invariant then reaches the existing global exception resolver, which emits a non-success system-error envelope. This is the narrowest safe behavior available without changing shared/RED Core; scheduled Query reconciliation remains active.

## Structured observability

One `event=CCAT_CREATE` record now contains:

- `operation`, `outcome`, `requestTimestamp`, `latencyMs`
- JeePay `payOrderId`, Merchant `mchOrderNo`, exact whole-TWD amount
- HTTP status when available
- allowlisted Provider `status`, `process_code/result_code/code`, message, and transaction reference
- reconciliation result (`NONE`, `QUERY_CONFIRMED`, `QUERY_NOT_FOUND`, `QUERY_INCONCLUSIVE`, or `QUERY_ERROR`)

Outcomes are `SUCCESS`, `REJECTED`, `AMBIGUOUS`, or `TRANSPORT_ERROR`. Provider bodies are never dumped. Extraction uses an explicit allowlist; message values are credential-pattern redacted, control-character stripped, and bounded to 256 characters. Authorization, Token, `custId`, `apiPassword`, App Secret, and canonical credential strings are excluded.

## Expiry finding

Source still derives `expire_date` by converting `PayOrder.expiredTime` to an Asia/Taipei `yyyy-MM-dd`. No expiry code was changed.

- Historical control `P2087602494821605377`: `expire_date=2026-08-20`.
- External B `P2087769567971483650`: `expire_date=2026-08-13`; no Provider order.
- External C `P2087814046925430785`: `expire_date=2026-08-13`; immediate authenticated Query returned `status=OK`, waiting `process_code=3`, amount TWD 40.

Thus same-day expiry was accepted by the current configured account/runtime. The authenticated contract still defines the allowed range as account-configured and does not establish a universal range.

```text
SAME_DAY_EXPIRE_STATEMENT = RUNTIME_ACCEPTED
NORMATIVE_SAME_DAY_EXPIRE_SUPPORT = NOT_SPECIFIED
```

## Verification matrix

| Case | Assertion |
| --- | --- |
| Create success | WAITING with populated code and payment data |
| deterministic rejection | native CONFIRM_FAIL; no Query/retry; empty data cannot serialize |
| pre-success exception | no fake payment instruction |
| ambiguous transport | UNKNOWN + Query reconciliation; exactly one Append |
| Query recovery | Provider-confirmed instructions returned without second Append |
| sanitized logging | correlation/status/code/reference retained; fixture secrets absent |
| amount conversion | 4000→40 and 1000→10, exact integer division |
| historical regression | existing CCAT success/APN/Query test suite remains green |

Execution evidence:

```text
Maven: 3.9.16
JDK: 21.0.11; source release 17

CCAT focused:
mvn -pl jeepay-payment -am \
  -Dtest=com.jeequan.jeepay.pay.channel.ccat.**,com.jeequan.jeepay.pay.channel.ccat.payway.** \
  -Dsurefire.failIfNoSpecifiedTests=false test
Result: 56/56 PASS; BUILD SUCCESS

Payment relevant:
mvn -pl jeepay-payment -am test
Result: jeepay-payment 60/60 PASS; dependency reactor 4/4 PASS; BUILD SUCCESS

Backend package:
mvn package
Result: 10/10 reactor modules SUCCESS; 64/64 tests PASS; BUILD SUCCESS
```

The tests use mocked `CcatClient` or an in-memory fake transport. They do not resolve or call a real CCAT endpoint.

## Boundaries and debt

- No RED Core or Shared YELLOW file changed.
- No second state machine, Provider registry, notify, callback, or query architecture was added.
- Original E05-B CCAT rejection reason remains unrecoverable because the historical raw Create response was never stored.
- Official universal CCAT minimum remains `NOT_SPECIFIED`.
- Ambiguous UnifiedOrder cannot express “ING without payment instruction” as a successful Merchant response. The Provider-local invariant intentionally returns a non-success API while native Query reconciliation proceeds; a richer ambiguous-create API contract would require separately authorized shared design.
