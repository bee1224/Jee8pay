# ADR-0006 — Asia/Taipei Platform Timezone

Status: Accepted
Date: 2026-08-12

## Context

The three service Dockerfiles use `Asia/Shanghai`. Both zones are UTC+8, but the name encodes a China platform default and is not the correct Taiwan runtime identity. Provider protocol timezone remains a separate adapter concern.

## Decision

Taiwan platform runtime containers default to `Asia/Taipei`. DB、JVM、UI and Provider timestamps must still be interpreted through their explicit contracts; this decision does not rewrite stored timestamps or Provider protocol requirements.

## Decision Drivers

- Taiwan runtime identity and operator expectations。
- Avoid ambiguous `CST` naming。
- Preserve Provider-specific timestamp rules。

## Options Considered

### Option A — Asia/Taipei platform default

Use the canonical IANA Taiwan zone for platform containers。

### Option B — Retain Asia/Shanghai

Same current offset but wrong regional semantic and future governance signal。

## Consequences

### Positive

- Service containers use the Taiwan platform timezone name。

### Negative / Trade-offs

- Existing DB/JVM host timezone and historical data still require physical-environment verification。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../architecture/taiwan-platform-baseline.md`](../architecture/taiwan-platform-baseline.md)
