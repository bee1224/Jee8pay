# ADR-0004 — TWD as Platform Default Currency

Status: Accepted
Date: 2026-08-12

## Context

Upstream fresh schema、internal tools、cashier defaults 與 generic UI use CNY/renminbi assumptions. Taiwan deployment needs a TWD default without making generic JeePay core TWD-only or breaking Provider-specific currency contracts.

## Decision

Taiwan JeePay uses TWD as the platform default currency. Currency remains explicit at the generic Merchant API boundary. Provider-specific currency requirements remain inside Provider adapters. Internal amount-unit semantics remain the smallest currency unit and are unchanged.

## Decision Drivers

- Taiwan-facing defaults must not silently create CNY orders。
- Generic Merchant API remains multi-currency capable。
- China Provider CNY protocol requirements must remain isolated and compatible。
- Money-domain changes are unnecessary for this baseline。

## Options Considered

### Option A — TWD default with explicit API currency

Change generic defaults and display while preserving explicit currency fields and Provider validation。

### Option B — Globally enforce TWD-only

Reject every non-TWD order in core；會破壞 generic contract 與既有 Provider-specific requirements。

## Consequences

### Positive

- Fresh Taiwan installations and internal tools default to TWD。
- Provider adapters retain ownership of supported-currency validation。

### Negative / Trade-offs

- Existing databases require a separate migration。
- Some upstream Provider code and historical material correctly retain CNY。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../architecture/taiwan-platform-baseline.md`](../architecture/taiwan-platform-baseline.md)
