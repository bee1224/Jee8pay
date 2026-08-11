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

## Unknowns

- CCAT request mapping
- query mapping
- APN checksum/signature contract
- amount/status mapping
- acknowledgement
- expired/closed mapping
- SDK vs direct HTTP

repository 內目前沒有可用的 CCAT official specification；以上維持 `UNKNOWN`。

## Security

禁止放入任何真實 credential。

## Documentation

- [`provider-design.md`](provider-design.md)：目前的 evidence-backed design 與 contract unknowns。
