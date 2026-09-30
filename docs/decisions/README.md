# Architecture Decision Records

本文件同時是 ADR policy 與 canonical ADR index。ADR 記錄長期有效的 decision、drivers、alternatives 與 consequences；不記錄 task log、changelog、implementation/test report、meeting note 或 protocol fact。

## ADR Index

| ADR | Decision | Status | Scope |
| --- | --- | --- | --- |
| [ADR-0001](ADR-0001-jeepay-as-platform-core.md) | JeePay remains the platform core; Taiwan payment companies are Providers | Accepted | Workspace architecture boundary |
| [ADR-0002](ADR-0002-native-provider-extension-contract.md) | Taiwan Providers use JeePay native Provider Extension Contract | Accepted | Cross-provider runtime extension contract |
| [ADR-0003](ADR-0003-single-root-monorepo.md) | Jee8pay uses one root Git monorepo | Accepted | Source and delivery topology |
| [ADR-0004](ADR-0004-twd-platform-default.md) | TWD is the Taiwan platform default; currency remains explicit | Accepted | Cross-provider currency contract |
| [ADR-0005](ADR-0005-environment-isolation.md) | Platform environment and Provider connectivity fail closed | Accepted | Security and deployment boundary |
| [ADR-0006](ADR-0006-taipei-platform-timezone.md) | Asia/Taipei is the Taiwan platform runtime default timezone | Accepted | Cross-service runtime locale |
| [ADR-0007](ADR-0007-closed-order-paid-apn-reopen.md) | Validated paid-APN may reopen a locally CLOSED PayOrder to SUCCESS | Accepted | Payment state machine / settlement integrity |
| [ADR-0008](ADR-0008-remove-legacy-china-providers.md) | 移除中國／海外支付通道與其衍生功能，只保留黑貓 PAY 四個 ibon 上游 | Accepted | Cross-provider scope / security boundary |
| [ADR-0009](ADR-0009-multi-tier-agent-tenancy-and-fee-waterfall.md) | 多層代理（獨立實體 t_agent_info）與四層手續費瀑布（t_fee_rule；訂單快照於第 2 階段） | Accepted | Data model / transaction semantics |
| [ADR-0010](ADR-0010-wallet-ledger-settlement-withdrawal.md) | 錢包記帳、T+N 分潤結算、人工提現、風控黑名單、人工調帳雙人覆核；推薦佣金層與下級費率 | Accepted | Money movement / data |
| [ADR-0011](ADR-0011-way-code-alias-routing.md) | 支付方式別名（例如 IBON）依金額／時段／權重路由到實際通道，並記錄每次決策 | Accepted | Payment routing |

## Qualification Rule

只有至少符合一項下列條件的 durable decision 才能建立 ADR：

- 改變 system architecture boundary。
- 影響多個 Provider。
- 影響 transaction semantics。
- 影響 data model。
- 影響 security boundary。
- 影響 deployment/runtime topology。
- 有兩個以上合理方案，且選擇具有長期維護成本。
- 未來工程師很可能問「為什麼當初選這個？」

### MUST ADR

已採納且會長期影響 architecture/security/data boundary、transaction semantics、deployment topology 或 cross-provider contract 的決策。

### MAY ADR

有兩個以上合理方案、選擇帶來長期維護成本，或很可能需要保存「為什麼」的 durable decision。仍須至少符合一項 qualification rule。

### NO ADR

普通 class、Provider method、bug fix、test repair、dependency bump、文件整理、單純遵循既有 extension contract、protocol fact、Provider API field mapping、task status 與 implementation progress 通常不得建立 ADR。

工作中若出現未獲授權的新 ADR，handoff 只列 `ADR Candidate`（decision、alternatives、reason、impact），不得自動建立。

## Lifecycle

ADR status 只允許：

- `Proposed`
- `Accepted`
- `Superseded`
- `Deprecated`

`Accepted` 表示目前採用；`Proposed` 尚未採用。被取代的 ADR 保留並改為 `Superseded`，同時連到取代它的新 ADR。ADR number 依 `ADR-0001`、`ADR-0002`、`ADR-0003` 遞增，永不重用或刪除歷史編號。

## Template

```markdown
# ADR-XXXX — <Decision Title>

Status: Accepted
Date: YYYY-MM-DD

## Context

## Decision

## Decision Drivers

## Options Considered

### Option A

### Option B

## Consequences

### Positive

### Negative / Trade-offs

## Supersedes

None

## Superseded By

None

## Related Documents
```

保持 ADR 短、可讀、decision-centric；index 只導航，不複製 ADR 內容。
