# ADR-0005 — Environment Isolation

Status: Accepted
Date: 2026-08-12

## Context

Platform environment and Provider connectivity both affect real-money safety. Current external `application.yml` and DB-backed `paySiteUrl` mechanisms can carry environment values, but physical Development/Production bindings are not supplied.

## Decision

Platform environment and Provider connectivity are security boundaries. Production Provider credentials/endpoints may never be selected through implicit fallback. Missing or mismatched endpoint、credential、database、callback or Provider configuration fails closed.

## Decision Drivers

- Prevent Development activity from reaching Production money systems。
- Prevent Production from falling back to development/test resources。
- Keep secrets outside source control。
- Reuse existing external configuration and Provider params before adding abstractions。

## Options Considered

### Option A — Explicit isolated configuration with fail-closed validation

Bind every physical environment explicitly and reject missing/mismatched configuration。

### Option B — Shared defaults with fallback

允許 missing values 使用另一環境或 Provider endpoint；風險不可接受。

## Consequences

### Positive

- Environment intent is explicit and auditable。
- Physical binding can be completed without embedding secrets in Git。

### Negative / Trade-offs

- Physical inputs and deployment validation remain required before runtime use。
- Existing public development defaults cannot be treated as Production-ready。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../architecture/environment-contract.md`](../architecture/environment-contract.md)
