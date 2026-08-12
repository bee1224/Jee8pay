# CCAT / 黑貓 PAY

## Status

```text
Provider: CCAT / 黑貓 PAY
Status: Verification
Contract Verdict: PASS-WITH-DEBT
Contract Phase: COMPLETE
Runtime Gate: OPEN
Runtime Implementation: COMPLETE
Offline Acceptance: PASS
Development Deployment: PASS
Credential Binding: PASS (V2-only, PRODUCTION)
Live Token Auth: PASS (1/1 request, token not exposed)
Public Callback: PASS (V2-only exact path; edge restart guard)
Pre-Live: PASS
Live E2E: PASS (one TWD 40 order / one human payment / APN / native notify)
Production Candidate: NOT STARTED
Production Validation: NOT STARTED
V1 Cutover: NOT STARTED
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

## Implemented JeePay Extension Points

P04 已沿用 JeePay native Provider contract 實作下列 extension points：

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

31-item DoR：`28 READY`、`3 NONBLOCKING_PARTIAL`、`0 BLOCKED`、`0 runtime CONFLICT`。P04 已完成 offline runtime implementation 與 46 個 CCAT-specific tests；未執行 live Provider call、真實付款或 production validation。

## Security

禁止放入任何真實 credential。

## Documentation

- [`provider-design.md`](provider-design.md)：P04 已實作的 evidence-backed Provider design。
- [`contract-evidence.md`](contract-evidence.md)：JEE-C03 authenticated-spec closure report、page provenance、31-item Definition of Ready 與 V1 drift。

JEE-E02 已部署 isolated V2 Development runtime，完成 V2-only PRODUCTION credential row、一次成功 Token authentication，以及只接受 CCAT exact APN path 的 V2-only public HTTPS callback。TD-011 已以 Provider-local native config resolver 修復並通過 cache enabled/disabled regression、完整 backend verification 與 V2 deployment。舊 INIT order 留作不處理的 test artifact；TD-012 為 nonblocking。2026-08-13 經明確授權透過 native unified-order flow 建立 exactly one 新 TWD 40 CCAT ibon order，並由真人完成 exactly one payment。CCAT status `A` / `B` APN 均通過 fail-closed 驗證；paid APN 經 authenticated Query reconciliation 後觸發 native `ING` → `SUCCESS`、Provider `OK` ACK 與 exactly one native Merchant Notify，receiver 首送回覆 `SUCCESS`。Public route 使用符合本次 zero-stop 限制的 runtime mount，任何 edge restart 後必須先重新驗證（TD-010）。Operations 見 [`../../operations/ccat-v2-development.md`](../../operations/ccat-v2-development.md)。

下一階段為 `JEE-E04 CCAT-only Production Candidate Deployment`；不得自動開始。NewebPay 維持 deferred。
