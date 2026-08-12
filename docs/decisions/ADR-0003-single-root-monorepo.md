# ADR-0003 — Single Root Monorepo

Status: Accepted
Date: 2026-08-12

## Context

Backend、frontend、Workspace docs、Agent configuration 與 delivery governance 必須形成可原子驗收與交付的一個工作單位。Nested repositories 會造成多個 Git truth、跨 Session 整合歧義與不完整 delivery。

## Decision

Jee8pay uses one root Git monorepo containing `README.md`、`AGENTS.md`、`.agents/`、`.codex/`（需要時）、`docs/`、`jeepay/` 與 `jeepay-ui/`。不得重新建立 `jeepay/.git` 或 `jeepay-ui/.git`。

## Decision Drivers

- Atomic backend/frontend/docs delivery。
- AI multi-session collaboration。
- One canonical Git state。
- Easier integration acceptance。
- Simpler delivery。

## Options Considered

### Option A — One root monorepo

由 root repository 管理所有 Workspace content。

### Option B — Nested repositories or automatic submodules

保留多個 Git state；需要額外同步與版本協調，且不符合本 Workspace 已採納 topology。

## Consequences

### Positive

- 一次 diff、驗收、commit 與 push 可涵蓋完整變更。
- Session 間只有一個 root Git truth。

### Negative / Trade-offs

- Nested upstream histories未直接成為 root history。
- Upstream provenance必須保存。
- Upstream sync需要明確策略。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../architecture/source-provenance.md`](../architecture/source-provenance.md)
