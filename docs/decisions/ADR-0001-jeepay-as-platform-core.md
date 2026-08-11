# ADR-0001 — JeePay as Platform Core

Status: Accepted
Date: 2026-08-12

## Context

本 Workspace 的目標是整合台灣支付公司，不是重新建立另一套 payment platform。JeePay 已有 Merchant、Application、PayOrder、Refund、Transfer、Provider routing、Callback、Merchant notification、RBAC 與 Operations UI 等 platform concerns；重建平行平台會複製這些責任。

## Decision

JeePay remains the platform core. Taiwan payment companies are integrated as Providers. Provider-specific behavior belongs in a Provider Adapter，並沿用 JeePay core 的共通 transaction、routing、callback、notification 與 operational capabilities。

## Decision Drivers

- Reuse existing platform capabilities。
- 避免重複 transaction infrastructure 與 operational surface。
- 為台灣 Providers 維持共同 integration model。
- 控制每個 Provider 的 implementation scope。

## Options Considered

### Option A — Retain JeePay as platform core

以 Provider Adapter 整合台灣支付公司，沿用既有 platform responsibilities。

### Option B — Build a separate Taiwan payment platform

另建 transaction model、routing、callback、notification、access control 與 operations surface，再與 JeePay 並存或同步。

## Consequences

### Positive

- Reuse mature platform capabilities。
- Smaller implementation scope。
- Less duplicated infrastructure。
- Common model for Taiwan Providers。

### Negative / Trade-offs

- Taiwan Provider integration must respect JeePay conventions。
- Some upstream Chinese-market assumptions may remain。
- Upstream compatibility becomes an architectural consideration。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../architecture/provider-extension-model.md`](../architecture/provider-extension-model.md)
- [`../providers/README.md`](../providers/README.md)
