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

## Workspace References

- [`PROJECT_TREE.md`](../PROJECT_TREE.md) is a non-canonical generated snapshot; regenerate it before relying on current tree contents.

## Upstream Documentation Boundary

`jeepay/docs/`, `jeepay/README.md`, and `jeepay-ui/README.md` are upstream project documentation. Taiwan Workspace governance does not modify or duplicate them; Workspace documents may link to them when needed.
