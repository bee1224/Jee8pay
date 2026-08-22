# Jee8pay — Taiwan JeePay Workspace

> Jee8pay extends JeePay with Taiwan payment providers.
>
> This repository is developed primarily through AI-assisted engineering.
> Project continuity must therefore rely on explicit repository evidence,
> canonical documentation, and runtime evidence rather than undocumented
> human or chat memory.
>
> This README is the stable workspace entry point.
> It defines project identity, architecture boundaries, current-state semantics,
> AI collaboration responsibilities, and where authoritative information belongs.
>
> It is **not** a live runtime status report, deployment status page, task handoff,
> or audit report.

---

## 1. Project Identity

JeePay is the platform core.

Jee8pay integrates Taiwan payment providers through JeePay Provider / Channel
extensions while preserving the JeePay transaction architecture.

```text
Merchant
   ↓
JeePay Core
   ↓
Provider / Channel Adapter
   ↓
Taiwan Payment Provider
```

The project must not build or maintain a second payment platform beside JeePay.

Provider implementations translate between JeePay contracts and upstream
provider protocols.

They must not create parallel platform-level sources of truth for:

* PayOrder lifecycle
* merchant state
* payment state
* Merchant Notify
* transaction ledger
* Provider registration
* platform-level retry / reconciliation state

JeePay remains the authoritative platform core.

---

## 2. Product Scope

### RYO / JAY / CHI（黑貓 PAY ibon）

Phase 1 active Providers：黑貓 PAY 平台上的三個統一客樂得上游（原 `CCAT` 已改名 `RYO`）：

```text
RYO_IBON  (ifCode=ryo)
JAY_IBON  (ifCode=jay)
CHI_IBON  (ifCode=chi)
```

Current scope（每個上游相同）：

* ibon
* Create Payment
* Provider Query
* APN / Payment Notify

Current non-goals:

* Refund
* Transfer
* Division
* Channel User
* Close Order
* 其他黑貓 PAY products outside the approved Phase 1 scope

### NewebPay / 藍新

Deferred.

NewebPay does not block a 黑貓 PAY-only Production Candidate.

Inherited, generic, experimental, or reference Provider implementations do not
become part of the active Taiwan roadmap merely because code exists.

Scope changes require an explicit project decision.

---

## 3. Architecture Boundary

Reuse existing JeePay capabilities before creating Taiwan-specific equivalents.

### JeePay Core owns

Platform-level responsibilities include:

* Merchant / App / authentication
* PayOrder lifecycle
* payment state transitions
* Merchant Query
* Merchant Notify
* MQ / retry infrastructure
* reconciliation orchestration
* RBAC
* Manager / Merchant / Cashier capabilities
* Provider registration and routing infrastructure

### Taiwan Provider Adapter owns

Provider-specific responsibilities may include:

* upstream protocol translation
* Provider Create / Query / Notify integration
* amount conversion
* Provider-specific status interpretation
* payment instructions
* request / response mapping
* Provider-specific diagnostics and observability

### Provider Adapter must not recreate

* a second PayOrder state machine
* a second Merchant Notify pipeline
* a second Provider registry
* a second merchant source of truth
* a second transaction ledger
* a parallel payment lifecycle

Changes to shared JeePay Core require explicit architectural justification.

---

## 4. Workspace and Documentation Map

Start here:

```text
README.md
   ↓
applicable AGENTS.md
   ↓
docs/README.md
   ↓
active task / handoff
   ↓
relevant repository / runtime / T0 evidence
```

Primary entry points:

* `README.md` — Project identity, durable invariants, and AI entry contract.
* `AGENTS.md` — Repository-wide Codex execution rules.
* Nested `AGENTS.md` — More specific rules for their applicable subtree.
* [`docs/README.md`](docs/README.md) — Canonical documentation index and reading map.
* [`jeepay/README.md`](jeepay/README.md) — Java backend documentation.
* [`jeepay-ui/README.md`](jeepay-ui/README.md) — Frontend documentation.
* `.agents/` — Reusable Codex workflows and task-support material.
* `.codex/` — Project-scoped Codex configuration when explicitly required.

Detailed architecture, ADRs, Provider contracts, operations, runbooks,
security rules, technical debt, and engineering knowledge belong under the
canonical documentation structure.

Live blockers, deployment hashes, audit findings, temporary evidence, and
task-specific state must not be duplicated into this README.

---

## 5. Current-State Truth Model

Jee8pay does not have one universal source of truth.

Different evidence answers different questions.

```text
Documented State
Implemented State
Observed State
```

Operationally, current state may require independent snapshots:

```text
Repository Snapshot
Development Snapshot
Production Candidate Snapshot
```

These snapshots must not be assumed identical.

### Repository

Answers:

> What is currently implemented in the tracked source state?

### Documentation

Answers:

> What architecture, contract, decision, operation, or intended behavior has
> been documented?

Documentation does not prove implementation or deployment.

### Runtime Evidence

Answers:

> What was actually observable in a deployed environment at a specific time?

Runtime behavior does not silently redefine architecture policy or Provider
contract.

### T0 / Audit Evidence

Reconciles repository, documentation, configuration, deployment, runtime
observations, and known limitations for a defined observation scope.

When evidence disagrees, preserve the disagreement.

Typical current-state classifications include:

```text
ALIGNED
DRIFT
ISSUE
UNKNOWN
BLOCKED
```

`UNKNOWN` is not automatically an `ISSUE`.

`BLOCKED` is not automatically a `FAIL`.

Historical reports describe historical observations.

They must not automatically be treated as current state when newer evidence
exists.

---

## 6. Evidence Semantics

A conclusion must never claim more than its evidence proves.

Examples:

```text
Plan PASS
≠
Execution PASS
```

```text
Repository Tests PASS
≠
Deployment PASS
```

```text
Component PASS
≠
Wire-to-Wire E2E PASS
```

```text
Development Acceptance PASS
≠
Production Readiness
```

A trusted baseline means that the observed state is sufficiently reproducible,
traceable, and evidence-backed.

It does not mean:

```text
Baseline Accepted
      ≠
No Bugs
      ≠
No Drift
      ≠
Production Ready
```

Detailed verdict vocabulary and acceptance rules belong in the canonical
governance documentation.

The governing principle is:

> **Evidence defines the boundary of the conclusion.**

---

## 7. AI Collaboration Model

Jee8pay uses three responsibility layers.

```text
ChatGPT
Master Brain / Control Plane
        ↓
Codex Plan Mode
Technical Brain / Repo Brain
        ↓
Codex Normal Mode
Execution Plane
```

### ChatGPT — Master Brain / Control Plane

Responsible for:

* Why / What
* project direction
* architecture decisions
* scope and priorities
* task decomposition
* Acceptance Criteria
* cross-session coordination
* final integration of evidence
* final Gate decisions

### Codex Plan Mode — Technical / Repo Brain

Responsible for:

* repository investigation
* dependency and impact analysis
* implementation planning
* file-level change planning
* test and regression strategy
* identifying unknowns and technical constraints

### Codex Normal Mode — Execution Plane

Responsible for:

* authorized implementation
* modification of authorized files
* tests and builds
* runtime or deployment operations when authorized
* execution evidence

The governing workflow is:

```text
Master decides WHAT
        ↓
Plan Mode investigates HOW
        ↓
Master approves HOW
        ↓
Codex executes
        ↓
Evidence decides DONE
```

Parallel investigation is allowed.

Conflicting Writers in the same architectural or runtime scope are not.

Detailed write-authority and execution rules are defined by the applicable
`AGENTS.md`.

---

## 8. New Session Entry Contract

A new AI / Codex Session should normally begin from the `Jee8pay` workspace root.

Before mutation:

```text
1. Read README.md
        ↓
2. Read applicable AGENTS.md
        ↓
3. Read docs/README.md
        ↓
4. Load the active task and relevant evidence
        ↓
5. Inspect before changing
```

The default engineering sequence is:

```text
Observe
   ↓
Understand
   ↓
Plan
   ↓
Approve
   ↓
Change
   ↓
Verify
```

Do not:

* reconstruct current state from chat memory alone when stronger evidence exists;
* treat historical reports as automatically current;
* convert missing evidence into assumptions;
* redesign established architecture merely because a fresh Session lacks context;
* silently turn a READ / PLAN task into a WRITE task;
* invent build, deployment, or runtime commands from convention or memory.

Use the actual repository configuration, applicable `AGENTS.md`, scripts,
runbooks, and environment-specific documentation.

---

## 9. Safety and Completion Invariants

### Payment side effects

A failed or unconfirmed Provider operation must not be represented as success.

Potentially side-effecting operations must not be blindly retried when the
previous outcome may be ambiguous.

Use non-transactional checks for connectivity and infrastructure health whenever
possible.

### External acceptance

Individually successful components do not establish a Wire-to-Wire E2E PASS.

An E2E claim must correlate the same transaction across the required path.

### Secrets

Live secrets are runtime inputs.

They must not be intentionally stored in Git or durable project documentation.

Detailed secret-handling and evidence-safety rules belong in the canonical
security documentation.

### Definition of Done

A task must distinguish:

```text
PLANNED
EXECUTED
VERIFIED
```

Code written is not equivalent to task complete.

Verification requires evidence appropriate to the active Acceptance Criteria.

The final rule is:

> **Evidence decides DONE.**

---

## 10. Permanent Operating Principles

### Architecture

> **JeePay is the platform core. Taiwan payment companies are Provider / Channel extensions.**

### AI-First Continuity

> **Project continuity must not depend on undocumented human or chat memory.**

### Current State

> **Repository, documentation, and runtime observations represent different truth domains.**

### Evidence

> **A conclusion must never claim more than its evidence proves.**

### Historical State

> **Historical evidence does not automatically represent current truth.**

### Parallel Work

> **Parallel reading is allowed; conflicting Writers are not.**

### Change Discipline

> **Observe → Understand → Plan → Approve → Change → Verify.**

### Completion

> **Evidence decides DONE.**
