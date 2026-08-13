# JEE-EC01R1 — External Consumer Nonblocking Finding Closure

Execution date: `2026-08-13`

```text
EC01R1 = PASS
NC_01 = CLOSED
NC_02 = CLOSED
NC_03 = CLOSED
AMOUNT_SCALE_EXPLICIT = YES
OFFICIAL_MINIMUM = NOT_SPECIFIED
FAILURE_INVARIANT_EXPLICIT = YES
EMPTY_PAYDATA_DOCUMENTED_INVALID = YES
DOUBLE_JSON_PARSE_FIXTURE = PASS
PAYDATA_TYPE = JSON_STRING
SHORTURL_OPTIONAL_TEST = PASS
CREATE_VECTOR = PASS
NOTIFY_VECTOR = PASS
SECRET_EXPOSURE = 0
PRODUCTION_SOURCE_CHANGES = 0
RUNTIME_CALLS = 0
REAL_PROVIDER_REQUESTS = 0
REAL_ORDERS_CREATED = 0
GIT_COMMIT = NOT_PERFORMED
GIT_PUSH = NOT_PERFORMED
RECOMMENDED_NEXT_STEP = Freeze worktree and start JEE-I07R2-P05 fresh independent acceptance.
```

## 1. Changed files

- `docs/integration/merchant-uat/README.md`
- `docs/integration/merchant-uat/examples/unified-order-success.json`
- `docs/integration/merchant-uat/examples/verify_vectors.py`
- `docs/integration/merchant-uat/JEE-EC01R1-external-consumer-closure.md`

本 worker 未修改 production source。工作開始前已存在的 CCAT production Java 與 Provider worktree changes 未被本任務觸碰，也不計入上述 `PRODUCTION_SOURCE_CHANGES`。

## 2. NC-01 before/after

- Before：文件有 `amount=4000` 與可整除 100 的規則，但 TWD 與 JeePay amount units 的 scale 關係不夠直接。
- After：明文定義 `1 TWD = 100 JeePay amount units`、`1000 = TWD10`、`4000 = TWD40`、integer type、exact integer conversion 與 `amount % 100 == 0`；並保留 `OFFICIAL_MINIMUM = NOT_SPECIFIED`，明確排除將 TWD40 解讀為官方 minimum。

## 3. NC-02 before/after

- Before：文件說明 WAITING 不是已付款，但未把正常 WAITING 必須具有可用 payment instruction 寫成 explicit invariant。
- After：明文規定 `code=0`、`orderState=1` 必須搭配 populated JSON-string `payData` 與可用 payment instruction；`payData={}` 或無可用指示屬 invalid response。Merchant 不得視為正常可付款／支付成功、不得上分或 blind retry，並須保留原 `mchOrderNo`，依 Query／error contract 處理。

## 4. NC-03 fixture/test

- `unified-order-success.json` 是完整 synthetic UnifiedOrder success outer response；`data.payData` 保持 JSON string。
- `verify_vectors.py` 先 parse outer response，再 assert `payData` type 為 string，第二次 parse 後驗證 `ibonCode`、`paymentCode`、`expireDate`、`billAmount` 與 composed payment code。
- Fixture 刻意省略 `shortUrl`；validator 仍成功完成第二次 parse 並確認付款指示可用，未要求 `shortUrl` 存在。
- Fresh `python3 docs/integration/merchant-uat/examples/verify_vectors.py`：Create、Notify、UnifiedOrder、double parse、shortUrl optional 全數 PASS。
- Fresh Maven 3.9.16／JDK 21 targeted run：`MerchantUatContractTest` 4 tests，0 failures，0 errors，`BUILD SUCCESS`。

## 5. Remaining External Consumer findings

```text
NONBLOCKING_CLARITY_FINDINGS = 0
```

未新增 unrelated finding。工作至此停止，交由 JEE-I07R2-P05 fresh independent acceptance。
