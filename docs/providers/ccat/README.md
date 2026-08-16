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
Public Callback: PASS (V2-only exact path; restart-persistent edge)
Pre-Live: PASS
Live E2E: PASS (one TWD 40 order / one human payment / APN / native notify)
Production Candidate: DEPLOYED / ACCEPTED
Production Credential Binding: HUMAN GATE
Production Token Auth: DEFERRED
Production Public Callback: PLAN READY / HUMAN GATE
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
- B5 Create：同 account 的 `cust_order_no` 唯一且 duplicate Append 明文拒絕；JEE-P05 起 response ambiguity 僅使用 Query-first reconciliation，不再由 Create path 自動重送 Append。

31-item DoR：`28 READY`、`3 NONBLOCKING_PARTIAL`、`0 BLOCKED`、`0 runtime CONFLICT`。P04 已完成 offline runtime implementation 與 46 個 CCAT-specific tests（後續 P05/P05R1 擴充至 86 個，ADR-0007 另增 5 個）；未執行 live Provider call、真實付款或 production validation。

## Security

禁止放入任何真實 credential。

## Documentation

- [`provider-design.md`](provider-design.md)：P04 已實作的 evidence-backed Provider design。
- [`contract-evidence.md`](contract-evidence.md)：JEE-C03 authenticated-spec closure report、page provenance、31-item Definition of Ready 與 V1 drift。
- [`JEE-E05-external-create-investigation.md`](JEE-E05-external-create-investigation.md)：外部 Merchant Create 回傳空 CCAT `payData` 的唯讀 runtime forensic investigation。
- [`JEE-P05-create-failure-semantics.md`](JEE-P05-create-failure-semantics.md)：Create failure semantics、empty-payData invariant、安全 observability 與驗證結果。
- [`JEE-P05R1-i07-blocker-closure.md`](JEE-P05R1-i07-blocker-closure.md)：I07 sanitizer 與 E06 WAITING regression blocker closure。

JEE-E02 已部署 isolated V2 Development runtime，完成 V2-only PRODUCTION credential row、一次成功 Token authentication，以及只接受 CCAT exact APN path 的 V2-only public HTTPS callback。TD-011 已以 Provider-local native config resolver 修復並通過 cache enabled/disabled regression、完整 backend verification 與 V2 deployment。舊 INIT order 留作不處理的 test artifact；TD-012 為 nonblocking。2026-08-13 經明確授權透過 native unified-order flow 建立 exactly one 新 TWD 40 CCAT ibon order，並由真人完成 exactly one payment。CCAT status `A` / `B` APN 均通過 fail-closed 驗證；paid APN 經 authenticated Query reconciliation 後觸發 native `ING` → `SUCCESS`、Provider `OK` ACK 與 exactly one native Merchant Notify，receiver 首送回覆 `SUCCESS`。JEE-N01 已將 public edge 改為 restart-persistent Compose network/config attachment 並關閉 TD-010；External Merchant signed Query preflight 仍是 real Create retry gate。Development operations 見 [`../../operations/ccat-v2-development.md`](../../operations/ccat-v2-development.md) 與 [`../../operations/sandbox-edge-recovery.md`](../../operations/sandbox-edge-recovery.md)。

JEE-E04 已將 I04 exact artifact 部署為 `server1.lp33ing.com` 上完全隔離的 `jee8pay-v2-production` Production Candidate；fresh V2 DB、Redis、MQ、三個 backend、三個 UI 與 internal callback ingress 均 healthy，V1 維持 6/6 healthy。JEE-I05 已獨立驗收 source provenance、runtime health、V1/V2 isolation、rollback 與 V1 non-interference。Production CCAT DNS/TCP/TLS/time readiness 通過，未執行 Token 或任何 transaction。Production credential intake、native Merchant/Application config 與 public callback control-plane activation 保留為精確 Human Gate；operations 與 rollback 見 [`../../operations/ccat-v2-production-candidate.md`](../../operations/ccat-v2-production-candidate.md)。

後續可由使用者選擇 `JEE-D01 Downstream Merchant Integration Readiness & UAT Package` 或稍後的 `JEE-M01 V1 → V2 Cutover Rehearsal`；兩者都不自動開始。NewebPay 維持 deferred。
