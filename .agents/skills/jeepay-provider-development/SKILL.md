---
name: jeepay-provider-development
description: 新增或修改 JeePay payment Provider、payway、callback、query、refund 或 transfer 時使用。
---

# JeePay Provider Development

使用前先讀 workspace `AGENTS.md`；本 Skill 只定義可重複的執行流程。

## Phase 1 — Scope

確認 Provider、`ifCode`、PayWay、支援 capability 與明確 non-goals。

## Phase 2 — Reference Pattern

至少找一個最接近的小型 Provider 與一個完整 Provider；不可從零設計。

## Phase 3 — Contract Mapping

確認 Payment SPI、Query SPI、Notify SPI、params model、PayWay reflection naming、DB definition 與 config schema。

## Phase 4 — Security Contract

列出 signature/checksum、amount、Provider merchant ID、transaction ID、replay/idempotency、callback ACK，以及 timeout/error handling。

## Phase 5 — Implementation

優先只在 GREEN boundary 新增。YELLOW modification 必須先提出 code evidence；RED modification 預設停止並回報。

## Phase 6 — Tests

依已實作 capability 驗證 success、pending、failure、malformed response、invalid signature、amount mismatch、duplicate callback、Provider timeout/error；不存在的 capability 不要硬做。

## Phase 7 — Handoff

列出 Files changed、Capabilities implemented、Tests、Unknowns、Security debt、Core modifications 與 Provider roadmap impact。

### Documentation Placement

- Provider-specific finding → `docs/providers/<provider>/`。
- Cross-provider architecture decision → handoff 的 `ADR Candidate`。
- Known unresolved issue → `docs/debt/technical-debt-register.md`。
- Runtime architecture changed → relevant architecture document。

不要自動建立 ADR、verification report、decision log 或 new registry；只有工作明確需要且已有內容／證據時才建立。
