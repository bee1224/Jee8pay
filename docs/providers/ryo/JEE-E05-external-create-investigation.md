# JEE-E05 — External Merchant First Create / Empty CCAT payData Investigation

Investigation date: `2026-08-13`

Scope: nnviopp V2 Development/UAT, read-only forensic investigation

Incident PayOrder: `P2087769567971483650`

Control PayOrder: `P2087602494821605377`

Second external evidence PayOrder: `P2087814046925430785`

## Verdict

```text
JEE-E05_INVESTIGATION = INCOMPLETE
EXTERNAL_CREATE_ACCEPTED = YES
PAY_ORDER_CREATED = YES
PAY_ORDER_ID = P2087769567971483650
MERCHANT_AMOUNT_MINOR = 1000
EXPECTED_TWD_AMOUNT = 10
PROVIDER_REQUEST_SENT = YES
PROVIDER_ORDER_CREATED = NO
PROVIDER_AMOUNT_TWD = 10
CCAT_RESPONSE = Create response/HTTP status UNPROVEN; later authenticated Query returned non-OK with msg="找不到此筆代繳資訊" 110 times
PAYDATA_EXPECTED = YES
PAYDATA_ACTUAL = {}
FIRST_DIVERGENCE = CcatIbon.pay: executeCreate did not reach successful populateWaitingResult; a CcatException catch returned an unpopulated CcatIbonOrderRS
ROOT_CAUSE = PROVEN symptom cause: CCAT Create error path was converted to native ING/success envelope and serialized an unpopulated response as {}; underlying CCAT Create failure trigger is UNPROVEN
AMOUNT_STRING_CAUSAL = NO
TWD10_MINIMUM_CAUSAL = UNPROVEN
HTTP_NOTIFY_URL_CAUSAL = NO
LOCALHOST_CLIENT_IP_CAUSAL = NO
REAL_PROVIDER_SIDE_EFFECT_EXISTS = NO
SAFE_TO_RETRY = NO
RECOMMENDED_NEXT_ACTION = First authorize a code-only patch that preserves a sanitized CCAT Create failure and prevents code=0/ING/{}; verify Provider expiry policy and add an explicit safe expiredTime before one separately authorized new-order retry
```

`INCOMPLETE` means the empty-payData code path and absence of a Provider order are proven, but the original CCAT Create HTTP status/body was neither logged nor persisted. It cannot be reconstructed without inventing evidence.

The second external order is material counterevidence to a minimum-amount root cause. It used `4000` minor units (TWD 40), the same amount as the historical successful control, and the same External Merchant Provider binding. Existing runtime Query five seconds after Create returned `status=OK`, `process_code=3`, `order_amount=40`, `bill_amount=40`, proving that CCAT created this Provider order even though Manager later showed no `channelOrderNo` and the user no longer had the Create response's ibon instruction. Therefore incidents B and C are different outcomes: B has no Provider order; C has a real waiting Provider order.

```text
SECOND_EXTERNAL_PAY_ORDER = P2087814046925430785
SECOND_EXTERNAL_AMOUNT_MINOR = 4000
SECOND_EXTERNAL_PROVIDER_AMOUNT_TWD = 40
SECOND_EXTERNAL_PROVIDER_ORDER_CREATED = YES
SECOND_EXTERNAL_PROCESS_CODE = 3
SAME_DAY_EXPIRE_STATEMENT = RUNTIME_ACCEPTED
NORMATIVE_SAME_DAY_EXPIRE_SUPPORT = NOT_SPECIFIED
```

`channelOrderNo` being null for C does not refute Provider-order existence. The Create waiting path does not set a Provider transaction ID; CCAT `trans_id` is bound later from a fully validated APN. Manager also does not persist or reconstruct the one-time UnifiedOrder `payData`. The 394-byte successful Create response and immediate authenticated Query distinguish C from B's 205-byte `{}` response and repeated not-found Queries.

`PROVIDER_REQUEST_SENT=YES` means the application reached `HttpClient.send` for Collect. CCAT receipt of the first TCP request, the exact number of Append attempts, and its original response are not logged. The adapter can make a bounded same-key second Append after an ambiguous result, so the exact Append count is also `UNPROVEN`.

## Runtime identity and incident timing

- Runtime host: `server1.nnviopp.com`.
- Compose project: `jee8pay-v2-dev`.
- Payment container: `jee8pay-v2-dev-payment-1`, healthy during inspection.
- Payment image: `jee8pay-v2-dev-payment:1f313e776d03-td011-134c78229b7a`.
- Runtime base source marker: `1f313e776d03c2383adff5aa96b9aac9b78efedc`; TD-011 Provider-local resolver is present in the payment artifact described by `docs/operations/ccat-v2-development.md:64-70`.
- Edge completion log: `2026-08-13T05:13:48.829877188Z`, `POST /api/pay/unifiedOrder`, HTTP `200`, response length `205`, request duration `0.294s`.
- Equivalent Taipei time: `2026-08-13 13:13:48 +08:00`.
- The exact outbound Collect subsecond timestamp is not logged. It occurred inside the approximately 294 ms UnifiedOrder window.

The DB also contains seven orders for `M_D01_EXTERNAL_UAT` between 13:09 and 13:18. The specified order is the fourth by `created_at`, not the first DB order for that Merchant. This is incident-premise drift; no order was created by this investigation.

## Actual DB state

Read-only projection from `jee8pay_v2_dev.t_pay_order`:

| Field | Abnormal order | Successful control |
| --- | --- | --- |
| `pay_order_id` | `P2087769567971483650` | `P2087602494821605377` |
| `state` at investigation | `6` / CLOSED | `2` / SUCCESS |
| `state` in Create response | `1` / ING | `1` / ING |
| `amount` | `1000` | `4000` |
| `currency` | `TWD` | `TWD` |
| `if_code` | `ccat` | `ccat` |
| `way_code` | `CCAT_IBON` | `CCAT_IBON` |
| `channel_order_no` | `NULL` | `2026081300245913` |
| `err_code` / `err_msg` | `NULL` / `NULL` | `NULL` / `NULL` |
| `client_ip` | `127.0.0.1` | `127.0.0.1` |
| Merchant `notify_url` scheme | `http` | `http` |
| `expired_time` | `2026-08-13 15:13:48` | `2026-08-20 02:09:55` |
| Provider `expire_date` derived by source | `2026-08-13` | `2026-08-20` |
| `success_time` | `NULL` | `2026-08-13 02:27:04` |
| `created_at` | `2026-08-13 13:13:48.000` | `2026-08-13 02:09:55.000` |
| `updated_at` | `2026-08-13 15:14:00.006` | `2026-08-13 02:27:22.880` |

The abnormal order was automatically closed after its native two-hour expiry. The close is post-incident lifecycle behavior, not the Create root cause: `PayOrderService.updateOrderExpired` closes INIT/ING orders whose expiry has passed (`jeepay/jeepay-service/src/main/java/com/jeequan/jeepay/service/impl/PayOrderService.java:190-200`).

`err_code` and `err_msg` being null do not prove there was no Create error. For an ING target, `updateInit2Ing` persists state/interface/channel fields but not error fields, and `updateIng2SuccessOrFail` returns without an update when the target is still ING (`PayOrderService.java:58-72`, `PayOrderService.java:124-134`). This is why the original adapter error is absent from current DB evidence.

No abnormal-order Merchant Notify record exists and the payment log contains zero APN callback lines for it. The control has one Merchant Notify record and its real Provider reference.

## Provider binding and endpoint

The abnormal Merchant and control each have one enabled `ccat / CCAT_IBON` passage. Their `t_pay_interface_config` projections are valid JSON, enabled, `PRODUCTION`, and have all required credential fields. A DB-side equality comparison, without printing values, showed the two applications use the same Provider account and API password.

Consequently the endpoint selected by source was:

```text
POST https://cocs.4128888card.com.tw/api/Collect
cmd = CvsOrderAppend
```

Endpoint selection is `CcatClient.resolveBaseUrl` and `collect` (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatClient.java:33-35`, `CcatClient.java:66-87`). Credentials and Authorization were never printed.

## Outbound non-secret Create fields

The abnormal order passed CCAT pre-check before DB insertion. The adapter therefore had TWD, an amount divisible by 100, all five required payer fields, and a non-null expiry (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/payway/CcatIbon.java:42-56`; insertion happens later at `AbstractPayOrderController.java:187-205`). Raw payer values are not persisted and were not logged, so their exact values are `UNPROVEN`.

Source and DB prove this outbound shape:

```text
cmd = CvsOrderAppend
cust_id = <REDACTED; present>
cust_order_no = P2087769567971483650
order_amount = 10
expire_date = 2026-08-13
payer_name/postcode/address/mobile/email = present; exact values UNPROVEN
payment_type = 0
payment_acquirerType = 2
apn_url = https://ccat-v2-dev.nnviopp.com/api/pay/notify/ccat
order_detail = 訂單標題 訂單描述
```

Construction is at `CcatIbon.java:75-103`; the exact minor-to-whole-TWD conversion is division by 100 after divisibility validation at `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatKit.java:22-30`.

The Merchant's own `notifyUrl` is not sent to CCAT. `CcatIbon` uses the platform callback returned by `AbstractPaymentService.getNotifyUrl` (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/AbstractPaymentService.java:55-57`). Runtime `paySiteUrl` was `https://ccat-v2-dev.nnviopp.com`.

`clientIp` is not read anywhere in CCAT Append construction. It is only stored on `PayOrder` by `AbstractPayOrderController.java:257`.

## Was CCAT invoked?

### `ccatPaymentService`

`YES`. Passage lookup selected `if_code=ccat`, bean resolution is `${ifCode}PaymentService`, and payment dispatch calls `paymentService.pay` (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/AbstractPayOrderController.java:166-205`). The response's provider-specific `payDataType=ccatIbon` can only come from `CcatIbonOrderRS.buildPayDataType` (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatIbonOrderRS.java:17-20`).

### `CcatIbon`

`YES`. `CcatPaymentService` resolves `CCAT_IBON` through `PaywayUtil` and delegates to the Provider payway (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatPaymentService.java:27-40`; `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/util/PaywayUtil.java:33-48`). The provider-specific response subtype proves this dispatch completed.

### Outbound Create

`YES` at application invocation level. The observed ING state plus empty provider-specific response excludes configuration/authentication/validation paths that remain INIT. In `CcatIbon.pay`, only a post-dispatch `CcatException` of `BUSINESS`, `AMBIGUOUS`, or `MALFORMED` can return an empty `CcatIbonOrderRS` and be mapped to ING (`CcatIbon.java:59-72`, `CcatIbon.java:200-212`). Those errors originate after `client.append`; Collect uses `HttpClient.send` (`CcatClient.java:76-101`, `CcatClient.java:238-253`).

The raw Create HTTP status, response `status`, `msg`, and fields were not logged. They are `UNPROVEN`. The 294 ms end-to-end duration rules out the configured 45-second timeout, but does not distinguish a fast Provider business rejection, a fast transport error, or a malformed response.

## Provider-order existence

The native scheduled reissue flow queried this ING order once per minute after ten minutes (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/task/PayOrderReissueTask.java:48-69`). Actual sanitized log evidence:

```text
query_count = 110
first = 2026-08-13 13:24:00.777 +08:00
last  = 2026-08-13 15:13:00.571 +08:00
result = channelState=UNKNOWN, channelOrderId=null,
         channelErrMsg=找不到此筆代繳資訊
```

The Query adapter can produce that Provider message only after an authenticated `CvsOrderQuery` returned non-OK (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/ccat/CcatPayOrderQueryService.java:32-48`). There was no APN, no `channel_order_no`, and no Merchant Notify. Therefore:

```text
PROVIDER_ORDER_CREATED = NO
REAL_PROVIDER_SIDE_EFFECT_EXISTS = NO
```

This means no CCAT payment order/reference was found. It does not mean no network request occurred: the Create attempt and subsequent Provider Queries did occur.

## Why `payData` became `{}`

The exact response path is:

1. `CcatIbon.pay` allocates an empty `CcatIbonOrderRS` (`CcatIbon.java:60-61`).
2. A `CcatException` prevents successful `populateWaitingResult`; the catch stores only `ChannelRetMsg`, leaving ibon fields null (`CcatIbon.java:63-72`).
3. `errorResult` maps `BUSINESS` to `API_RET_ERROR` and ambiguous/malformed errors to `UNKNOWN` (`CcatIbon.java:200-212`).
4. Core maps both `API_RET_ERROR` and `UNKNOWN` to `PayOrder.STATE_ING` (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/AbstractPayOrderController.java:321-365`).
5. UnifiedOrder treats ING as eligible for payment data and invokes the provider subtype's builders (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/UnifiedOrderController.java:66-78`).
6. `CcatIbonOrderRS.buildPayData` puts only null values into a Fastjson `JSONObject`; default serialization omits them, producing `{}` (`CcatIbonOrderRS.java:22-31`).
7. The API wraps and signs that result as `code=0`, even though no Provider instruction exists (`UnifiedOrderController.java:69-78`).

Classification of the alternatives in the task:

| Candidate | Finding |
| --- | --- |
| A. Provider returned no payment data | Underlying Create result `UNPROVEN`; no Provider order was created |
| B. Provider error converted into WAITING | `YES` semantically: error became Core ING; exact error type is unpersisted |
| C. Adapter returned empty map/object | `YES`: unpopulated `CcatIbonOrderRS` |
| D. Mapping lost valid Provider fields | `NO` evidence; successful mapping works in control |
| E. Serialization produced `{}` | `YES`, from all-null response fields |
| F. Exception/error caught and suppressed | `YES`, converted to `ChannelRetMsg` and success envelope |
| G. Other cause | Original Provider/transport trigger remains `UNPROVEN` |

## Amount-string parsing

Jackson was not involved. The request body is parsed by Fastjson at `jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/beans/RequestKitBean.java:75-92`. `AbstractCtrl.getObject` calls `JSONObject.toJavaObject` (`jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/ctrls/AbstractCtrl.java:205-220`), which coerced the JSON string `"1000"` into the DTO's `Long amount` (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/rqrs/payorder/UnifiedOrderRQ.java:51-54`). `genPayOrder` copied that Long unchanged (`AbstractPayOrderController.java:233-257`), and DB contains numeric `1000`.

Signature validation occurred after coercion: `ApiController.getRQByWithMchSign` validates Merchant/App and signs the typed RQ converted back to JSON (`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/ApiController.java:58-103`). The request is contract-nonconforming because amount should be an integer, but the runtime coercion and outbound amount prove it did not cause `{}`.

```text
AMOUNT_STRING_CAUSAL = NO
```

## Three named request deviations

### HTTP Merchant notify URL

`NO`. Core explicitly accepts both `http://` and `https://` at `AbstractPayOrderController.java:121-126`. The Merchant URL is stored for downstream notify but is not used in CCAT Create. The successful control also has an HTTP Merchant notify URL in DB.

### `clientIp=127.0.0.1`

`NO`. CCAT Append does not read client IP. The successful control has the same DB client IP.

### TWD 10 / minimum

`UNPROVEN`. Local authenticated contract evidence states that CCAT publishes a global cap but no universal minimum; account-specific bounds are Provider validation (`docs/providers/ryo/contract-evidence.md:69-95`, `docs/providers/ryo/contract-evidence.md:330-337`). The original Create response is absent, so this incident cannot prove an amount-minimum rejection.

```text
OFFICIAL_MINIMUM = NOT_SPECIFIED
TWD10_MINIMUM_CAUSAL = UNPROVEN
```

The second external order under the same binding used the proven TWD 40 amount and CCAT Query found the real waiting order. This disproves TWD 10 as a general explanation for both external observations, but does not reconstruct B's missing Create response; B's TWD 10 causality remains `UNPROVEN`.

## Newly identified expiry divergence

The abnormal request omitted `expiredTime`, so native Core defaulted it to two hours (`AbstractPayOrderController.java:269-276`). CCAT receives only a `yyyy-MM-dd` date, yielding same-day `expire_date=2026-08-13`; the successful control explicitly used seven days and sent `2026-08-20` (`CcatIbon.java:83-103`). Local Provider design says `expire_date` must be within the CCAT-configured range (`docs/providers/ryo/provider-design.md:149-159`).

The Merchant UAT README calls `expiredTime` optional with a native two-hour default, while its successful vector explicitly uses `604800` (`docs/integration/merchant-uat/README.md:68-76`; `docs/integration/merchant-uat/examples/create-vector.json:16`). The second external order also derived `expire_date=2026-08-13`, and its immediate authenticated CCAT Query returned the order as waiting. Runtime evidence therefore proves same-day expiry was accepted for this configured account on 2026-08-13. It does not create a universal normative CCAT expiry range beyond the authenticated contract's account-configured policy.

## Control comparison and first divergence

| Stage | Abnormal | Successful control | Result |
| --- | --- | --- | --- |
| Merchant UnifiedOrder | accepted, signed | accepted, signed | same logical path |
| PayOrder creation | native row | native row | same |
| passage lookup | enabled `ccat/CCAT_IBON` | enabled `ccat/CCAT_IBON` | equivalent |
| params load | valid PRODUCTION config | valid PRODUCTION config | same Provider account/credential by equality check |
| `ccatPaymentService` | invoked | invoked | same |
| `CcatIbon` | invoked | invoked | same |
| request amount | TWD 10 | TWD 40 | data divergence |
| request expiry | same day | seven days | data divergence |
| CCAT Create outcome | error path; raw result missing | validated OK | first proven behavioral divergence |
| payData construction | fields remain null | ibon code/expiry/bill/short URL populated | divergent consequence |
| UnifiedOrder response | `code=0`, ING, `{}` | `code=0`, ING, populated data | divergent consequence |
| later Query | not found 110 times | Provider order found and progressed | confirms no abnormal Provider order |
| APN / notify | none | status A/B, SUCCESS, one Merchant Notify | confirms divergence |

The first proven behavioral divergence is inside `CcatIbon.pay`, between `executeCreate` and `populateWaitingResult` (`CcatIbon.java:63-70`). The exact upstream field that triggered it cannot be chosen between amount, expiry, another Provider validation, transport, or malformed-response handling.

For the three-order comparison, A and C both reach a validated Provider order while B does not. C's absence of Manager-visible instructions is a Merchant response-retention/consumption problem, not the same Provider Create failure as B. The first meaningful A-versus-B divergence remains CCAT Create response handling; the first meaningful A-versus-C divergence occurs after the successful UnifiedOrder response, where A's payment instruction was consumed and C's was not retained by the caller.

## Defect and classification

- **Adapter/core integration defect — proven:** a CCAT Create error can return a signed `code=0`, `orderState=1`, provider-specific `payDataType`, and `{}` instead of a failure or an actionable sanitized error.
- **Observability defect — proven:** original Create HTTP/status/msg is not logged or persisted; ING error fields are discarded by native state update.
- **Merchant request defects — present but mostly non-causal:** quoted amount violates the documented type, HTTP notify violates the UAT HTTPS requirement, localhost client IP should be omitted. Source/runtime prove none caused the empty response.
- **Provider rejection — unproven as the precise trigger:** no original response survived.
- **Configuration defect — no evidence:** abnormal and control use enabled equivalent passages and the same valid Provider binding; authenticated Query worked.
- **Documentation mismatch — present:** `expiredTime` is documented optional despite the CCAT date-range requirement and the successful vector's explicit seven-day value.

A fix is required before asking the external integrator to retry. At minimum, a deterministic Provider business rejection must not become a normal ING response, ambiguous handling must surface a sanitized actionable result, and a regression must assert that an empty ibon instruction cannot be returned as `code=0`. The original Provider response cannot be recovered by restarting, and no restart is justified.

## Operational actions not taken

- No UnifiedOrder or Provider Create was called by this investigation.
- No Token request or manual Provider Query was called by this investigation.
- No payment, callback, retry, state update, DB write, container restart, DNS/Cloudflare/firewall/allowlist/credential change, V1 change, or Production action occurred.
- All runtime inspection was read-only and all credential-bearing fields were excluded or redacted.
