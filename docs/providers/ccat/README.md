# CCAT / 黑貓 PAY

## Status

```text
Provider: CCAT / 黑貓 PAY
Status: Design
Contract Verdict: PASS-WITH-DEBT
Runtime Gate: OPEN
Runtime Implementation: NOT STARTED
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

## Contract Closure

JEE-C03 已以契約會員取得的 `多元支付平台-WEBAPI介面規格(V1.28.1)` 完成 Token、Create、Query 與 APN contract confirmation：

- B1 amount：JeePay cents 必須整除 100，映射為 CCAT whole-TWD `order_amount`; APN amount 以 authenticated Query 的 order/bill/paid amounts 交叉驗證。
- B2 state：`4/7/8` 是 payment success；`5/6` 是 closed；`0/1/3` waiting。Sample-only code `2` 保守不轉態。
- B3 identity：`username=cust_id`；APN `api_id/order_no/trans_id` 分別綁定 configured account、`PayOrder.payOrderId`、`channelOrderNo`，狀態由 account-scoped Query 授權。
- B4 APN：官方確認即時一次、每 15 分鐘、每狀態最多三次，純文字 `OK` 停止；duplicate/replay 沿用 Query + JeePay native idempotency。
- B5 Create：同 account 的 `cust_order_no` 唯一且 duplicate Append 明文拒絕；response ambiguity 使用 Query-first、bounded same-key retry。

31-item DoR：`28 READY`、`3 NONBLOCKING_PARTIAL`、`0 BLOCKED`、`0 runtime CONFLICT`。`CCAT_RUNTIME_GATE = OPEN`；下一階段可開始 JEE-P04，但本文件不表示 runtime 已實作。

## Security

禁止放入任何真實 credential。

## Documentation

- [`provider-design.md`](provider-design.md)：JEE-P04 可直接採用的 evidence-backed Provider design。
- [`contract-evidence.md`](contract-evidence.md)：JEE-C03 authenticated-spec closure report、page provenance、31-item Definition of Ready 與 V1 drift。
