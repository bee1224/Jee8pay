# CCAT / 黑貓 PAY

## Status

```text
Provider: CCAT / 黑貓 PAY
Status: Design
Gate: BLOCKED-BY-CONTRACT-UNKNOWNS
```

## Phase 1 Scope

- ibon CVS
- Create Payment
- Provider Query
- APN / Payment Notify

## Non-goals

- Refund
- Transfer
- Division
- Channel User
- Close
- COCS
- 其他 CCAT payment products

除非現有需求明確證明需要，否則不納入本階段。

## Expected JeePay Extension Points

下列為目前已驗證的候選，全部為 `NOT IMPLEMENTED`：

```text
CcatPaymentService
CcatIbon
CcatPayOrderQueryService
CcatChannelNoticeService
CcatNormalMchParams
CS.IF_CODE.CCAT
CCAT / CCAT_IBON DB definitions
```

## Evidence and Unknowns

- CCAT request mapping
- query mapping
- APN checksum/signature contract
- amount/status mapping
- acknowledgement
- expired/closed mapping
- SDK vs direct HTTP

JEE-C01 已追回 Development VPS 的兩個 V1 source traces，並重新核對 CCAT SDK／WooCommerce implementation。Token/Create/Query surface、ibon constants、waiting semantic、checksum canonicalization與 observed `200 OK` ACK 已收斂；但 V1 對 `process_code=7/8` 互相衝突，amount cross-surface mapping、APN account/retry 與 Create idempotency仍缺 merchant-versioned normative evidence。Runtime Gate 維持 `CLOSED`。

## Security

禁止放入任何真實 credential。

## Documentation

- [`provider-design.md`](provider-design.md)：目前的 evidence-backed design 與 contract unknowns。
- [`contract-evidence.md`](contract-evidence.md)：JEE-C01 V1/official evidence inventory、confidence、Definition of Ready 與 blocking unknowns。
