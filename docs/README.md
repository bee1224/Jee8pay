# Jee8pay Documentation

本文件是 Taiwan JeePay Workspace documentation 的唯一 canonical index。從這裡選擇與工作相關的分類，不需每次掃描完整 `docs/` tree。

## Documentation Map

| Category | Purpose | Canonical Location |
| --- | --- | --- |
| Architecture | Current system behavior | [`docs/architecture/`](architecture/README.md) |
| Decisions | Durable architectural decisions | [`docs/decisions/`](decisions/README.md) |
| Providers | Provider-specific contract/design | [`docs/providers/`](providers/README.md) |
| Integration | External integration contracts and UAT packages | [`docs/integration/`](integration/README.md) |
| Operations | Deployment/runtime/operations | [`docs/operations/`](operations/README.md) |
| Debt | Known unresolved debt | [`docs/debt/`](debt/README.md) |

## Reading Order

1. Root [`README.md`](../README.md)
2. Root [`AGENTS.md`](../AGENTS.md)
3. This `docs/README.md`
4. Relevant [Architecture](architecture/README.md)
5. Active [ADR index](decisions/README.md)
6. Relevant [Provider documentation](providers/README.md)

## Document Status Markers

文件頂部可能帶狀態標記，判讀規則：

- 無標記：目前事實（current truth）。
- `> **STATUS: STALE**`：內容有部分已被後續證據取代；以標記內指出的文件為準。
- `> **STATUS: SUPERSEDED**`：整份已被取代（保留為歷史）。
- `runtime/` 與 `.agents/tmp/` 下的 JEE-* 報告是 **gitignored 歷史 runtime evidence**，不是 canonical 文件；判斷現況時以 canonical docs + 可重現的 runtime 驗證為準，勿把歷史報告當目前狀態。

## Maintenance Rules

- 新增 Markdown 前確認 canonical category 與 index（本文件）；禁止無索引的 root-level random Markdown。
- Provider 相關內容放 `providers/<ifCode>/`，外部整合契約放 `integration/`，部署/營運放 `operations/`，未解債放 `debt/`，durable 決策放 `decisions/`（ADR）。
- 每份 category README 都是該分類的導航入口，新增文件必須同時更新對應 README 的 index。

## Workspace References

- [`PROJECT_TREE.md`](../PROJECT_TREE.md) is a non-canonical generated snapshot; regenerate it before relying on current tree contents.

## Upstream Documentation Boundary

`jeepay/docs/`, `jeepay/README.md`, and `jeepay-ui/README.md` are upstream project documentation. Taiwan Workspace governance does not modify or duplicate them; Workspace documents may link to them when needed.
