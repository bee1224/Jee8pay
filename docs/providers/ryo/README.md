# RYO / 黑貓 PAY ibon（統一客樂得上游一）

> **RENAME NOTE（2026-08-20）**：原 `CCAT` / `CCAT_IBON` 改名為 `RYO` / `RYO_IBON`。黑貓 PAY 平台（`www.ccat.com.tw`）不變；JeePay 的 Provider 身份改以上游公司命名。同一平台另有兩個上游 `JAY` / `CHI`（參見 [`../jay/README.md`](../jay/README.md)、[`../chi/README.md`](../chi/README.md)），三者的 Token / Create / Query / APN contract 相同，共用本目錄的 [`contract-evidence.md`](contract-evidence.md) 平台契約證據。歷史 JEE-* 報告保留原名 `CCAT` 作為當時記錄。

## Status

```text
Provider: RYO / 黑貓 PAY
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
- 其他黑貓 PAY payment products

除非現有需求明確證明需要，否則不納入本階段。

## Implemented JeePay Extension Points

沿用 JeePay native Provider contract 實作下列 extension points：

```text
RyoPaymentService
payway/RyoIbon
RyoPayOrderQueryService
RyoChannelNoticeService
model/params/ryo/RyoNormalMchParams
CS.IF_CODE.RYO
CS.PAY_WAY_CODE.RYO_IBON
RYO / RYO_IBON DB definitions（t_pay_interface_define / t_pay_way / t_pay_interface_config / t_mch_pay_passage）
```

## Contract Closure

以契約會員取得的 `多元支付平台-WEBAPI介面規格(V1.28.1)` 完成 Token、Create、Query 與 APN contract confirmation：

- B1 amount：JeePay cents 必須整除 100，映射為黑貓 PAY whole-TWD `order_amount`; APN amount 以 authenticated Query 的 order/bill/paid amounts 交叉驗證。
- B2 state：`4/7/8` 是 payment success；`5/6` 是 closed；`0/1/3` waiting。Sample-only code `2` 保守不轉態。
- B3 identity：`username=cust_id`；APN `api_id/order_no/trans_id` 分別綁定 configured account、`PayOrder.payOrderId`、`channelOrderNo`，狀態由 account-scoped Query 授權。
- B4 APN：官方確認即時一次、每 15 分鐘、每狀態最多三次，純文字 `OK` 停止；duplicate/replay 沿用 Query + JeePay native idempotency。
- B5 Create：同 account 的 `cust_order_no` 唯一且 duplicate Append 明文拒絕；Create response ambiguity 僅使用 Query-first reconciliation，不再由 Create path 自動重送 Append。

## Security

禁止放入任何真實 credential；`t_pay_interface_config.if_params` 仍標記 `KNOWN SECURITY DEBT`（未證明 DB field-level encryption）。

## Documentation

- [`provider-design.md`](provider-design.md)：evidence-backed Provider design（原 CCAT design，改名 RYO）。
- [`contract-evidence.md`](contract-evidence.md)：黑貓 PAY 平台契約證據（RYO / JAY / CHI 共用）、31-item Definition of Ready 與 V1 drift。
- [`JEE-E05-external-create-investigation.md`](JEE-E05-external-create-investigation.md)：外部 Merchant Create 回傳空 `payData` 的唯讀 runtime forensic investigation（歷史記錄，當時名為 CCAT）。
- [`JEE-P05-create-failure-semantics.md`](JEE-P05-create-failure-semantics.md)：Create failure semantics、empty-payData invariant、安全 observability 與驗證結果。
- [`JEE-P05R1-i07-blocker-closure.md`](JEE-P05R1-i07-blocker-closure.md)：I07 sanitizer 與 E06 WAITING regression blocker closure。

歷史 runtime evidence：JEE-E02 部署 isolated V2 Development runtime（V2-only PRODUCTION credential、一次成功 Token authentication、exactly one TWD 40 ibon E2E）；JEE-E04 部署 isolated Production Candidate（fresh V2 DB/Redis/MQ，V1 6/6 healthy），Production credential intake 與 public callback activation 仍為 Human Gate。Development operations 見 [`../../operations/ccat-v2-development.md`](../../operations/ccat-v2-development.md)，Production Candidate 見 [`../../operations/ccat-v2-production-candidate.md`](../../operations/ccat-v2-production-candidate.md)（文件名保留歷史 `ccat-v2`）。

## Sibling Upstreams

| Provider | ifCode | wayCode | 說明 |
| --- | --- | --- | --- |
| RYO | `ryo` | `RYO_IBON` | 本文件（上游一） |
| JAY | `jay` | `JAY_IBON` | [`../jay/README.md`](../jay/README.md) |
| CHI | `chi` | `CHI_IBON` | [`../chi/README.md`](../chi/README.md) |

NewebPay 維持 deferred。
