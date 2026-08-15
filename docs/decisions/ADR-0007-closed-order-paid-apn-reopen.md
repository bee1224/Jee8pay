# ADR-0007 — CLOSED 訂單允許經完整驗證的 paid-APN 轉回 SUCCESS

Status: Accepted
Date: 2026-08-16

## Context

`PayOrderExpiredTask`（每分鐘，`jeepay-payment/.../task/PayOrderExpiredTask.java`）呼叫
`updateOrderExpired()`（`jeepay-service/.../PayOrderService.java:191-200`）把已到期且仍為
INIT/ING 的訂單**純本地**轉為 `STATE_CLOSED(6)`：不打 CCAT、不發通知。已有一致 live 證據
（08-13 的 18 筆 2 小時到期訂單全部 CLOSED，`notify_state=0`）。

`PayOrderReissueTask` 只查 `STATE_ING` 訂單（`PayOrderReissueTask.java:55`），所以 CLOSED 訂單
**不會再被補單查詢**。但 CCAT 的 ibon 帳單以 `expire_date`（日期）為有效邊界，與本地
`expiredTime`（datetime）可能相差數小時：付款人可能在「本地已關閉、CCAT 仍接受付款」的窗口內
完成付款（CCAT `process_code=4` + APN status=B）。

此時 `CcatChannelNoticeService.doNotice` 的狀態守衛（`CcatChannelNoticeService.java:85-89`，
僅接受 ING/SUCCESS/FAIL）會**拒絕**該 APN：本地不轉 SUCCESS、不發 Merchant Notify、商戶不入帳，
但付款人已真實付款 → **滯留款項**，只能人工對帳（目前無任何自動化 recovery）。

## Decision

允許「經完整驗證的 paid-APN」把本地 `STATE_CLOSED` 訂單轉回 `STATE_SUCCESS`：

1. **Adapter（GREEN）**：`CcatChannelNoticeService.doNotice` 的狀態守衛加入 CLOSED；且
   `ensureTerminalStateConsistency` 對 CLOSED 只接受 `CONFIRM_SUCCESS`（paid Query）→ 允許 reopen；
   CLOSED + WAITING/FAIL Query 一律維持 CLOSED（不轉態）。
2. **Controller（RED，經本 ADR 授權）**：`ChannelNoticeController.doNotify` 增加分支
   `state == CLOSED && CONFIRM_SUCCESS → payOrderService.updateClosed2Success(...)`（CAS 守衛：
   `WHERE pay_order_id=? AND state=6`）。
3. **Service（RED，經本 ADR 授權）**：新增 `PayOrderService.updateClosed2Success`。
4. 轉態後沿用既有 `confirmSuccess`（發 Merchant Notify、自動分賬），不新增通知管線。

安全前提不變：paid-APN 必須通過 account/order/checksum/amount/transaction 驗證 **且** authenticated
CCAT Query 顯示 paid（`process_code=4/7/8`）才可能 reopen — 與正常成功路徑同一信任邊界；攻擊者無法
讓 CCAT Query 顯示已付款而不真實付款。

## Decision Drivers

- 商戶資金完整性：已真實付款的帳單必須入帳；靜默滯留比受控 reopen 更糟。
- 本地 CLOSED 只是「未諮詢 Provider 的本地臆測」；Provider 真實狀態（paid）應勝出。
- 安全模型（authenticated Query 為授權邊界）不需放寬。
- 變更範圍小且可測試。

## Options Considered

### Option A — 維持現況（fail-closed + 人工對帳）

零 core 變更、零誤入帳風險；但滯留款項會靜默發生，需人工 CCAT 查詢→手動轉態→手動通知（可借
B 功能），營運負擔與遺漏風險高。

### Option B — 允許驗證過之 paid-APN reopen CLOSED → SUCCESS（採納）

上述 Decision。資金正確結算、商戶正常入帳；需 1 個 controller 分支 + 1 個 service CAS 方法 +
adapter 守衛放寬 + 測試（RED 變更，經本 ADR 授權）。

### Option C — Provider-aware 關閉（到期前先查 CCAT）

`PayOrderExpiredTask` 關閉前先查 CCAT 真實狀態。可縮小競態窗口，但無法消除（關閉後、Provider
cutoff 前仍可能付款）；且讓每分鐘 task 對每筆到期訂單打 Provider（rate/latency/失敗處理成本）。
不單獨採納；可作為 B 之後的增強。

### Option D — B + C 合併

最穩健但變更最大；現階段不需要，留待後續 reconciliation 任務評估。

## Consequences

### Positive

- 已付款訂單正確入帳並通知商戶，消除主要滯留路徑。
- 信任邊界不變（authenticated Query 仍為授權來源）。
- reopen 只針對「本地關閉但 Provider 已付款」的真實案例。

### Negative / Trade-offs

- 修改 PayOrder state machine 相關路徑（RED），需完整回歸測試（APN 全狀態矩陣、並發 duplicate、
  terminal consistency、notify 恰好一次）。
- CLOSED 訂單的 reopen 會產生 SUCCESS notify — 商戶端需依既有冪等契約處理（已支援）。
- 若 APN 遺失（edge down）或 Query 暫時失敗，滯留仍可能發生 → 長期仍建議 reconciliation 任務
  （定期以 CCAT Query 稽核 CLOSED 訂單），本 ADR 不涵蓋。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`provider-design.md`](../providers/ccat/provider-design.md)（APN 驗證與狀態映射）
- [`UAT-START-NOTICE.md`](../integration/merchant-uat/UAT-START-NOTICE.md) §4（競態風險描述）
- `PayOrderExpiredTask`、`PayOrderReissueTask`、`ChannelNoticeController.doNotify`、
  `PayOrderProcessService.confirmSuccess`（code evidence）
