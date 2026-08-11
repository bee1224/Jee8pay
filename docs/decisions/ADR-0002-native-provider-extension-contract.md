# ADR-0002 — Native Provider Extension Contract

Status: Accepted
Date: 2026-08-12

## Context

JeePay runtime 已透過 `ifCode`、Spring capability beans 與 `wayCode` reflection 提供 Provider extension points。台灣 Providers 需要一致的接入方式，同時避免建立與現有 runtime resolution 平行的 registry 或 plugin architecture。

## Decision

Taiwan Providers use JeePay's native Provider Extension Contract：

```text
MchPayPassage.ifCode
→ ${ifCode}PaymentService
→ wayCode
→ PaywayUtil
→ Provider payway implementation
```

Capability-specific services 沿用 `${ifCode}PayOrderQueryService`、`${ifCode}ChannelNoticeService`、`${ifCode}RefundService`、`${ifCode}ChannelRefundNoticeService`、`${ifCode}TransferService`、`${ifCode}TransferNoticeService`、`${ifCode}ChannelUserService` 與 `${ifCode}DivisionService`。

不得預設建立另一套 Provider registry 或 plugin architecture。只有 code evidence 證明既有 extension point 不足時，才可考慮 shared core modification，且必須提出 alternative 並維持最小 diff。

P01 確認的 modification boundary：

- `GREEN`：Provider-specific channel、params、ifCode、seed/config data 與 tests。
- `YELLOW`：既有 extension point 明確不足時，最小修改 shared request/response、generic callback contract、generic config rendering 或 Provider resolution；修改前必須提出 code evidence 與 alternative。
- `RED`：新增 Provider 原則上不得修改 PayOrder domain/state machine、order/notice controllers、`PayOrderProcessService`、merchant notification MQ/retry、authentication 或 RBAC；除非任務明確授權重設 core architecture。

## Decision Drivers

- Runtime code 已提供 native extension points。
- Cross-provider consistency 與可預測的 bean resolution。
- Preserve upstream core and minimize compatibility cost。
- Provider-specific concerns 與 core responsibilities 保持邊界。

## Options Considered

### Option A — Use the native Provider Extension Contract

依現有 `ifCode`、capability bean naming 與 `PaywayUtil` 擴充 Provider。

### Option B — Add a separate Provider registry/plugin architecture

建立平行 discovery、registration 與 dispatch mechanism；會重複既有 resolution，且目前沒有 extension-point insufficiency evidence。

## Consequences

### Positive

- 新 Provider 遵循相同 runtime path 與 capability naming。
- Provider-specific implementation 可集中在 adapter boundary。
- Shared core 與 upstream diff 維持最小。

### Negative / Trade-offs

- Provider implementation 必須符合 JeePay naming 與 dispatch conventions。
- 真正不足的 extension point 必須先取得 code evidence，不能直接繞過。
- 少數特殊 Provider 可能需要受控的 YELLOW change。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`../architecture/provider-extension-model.md`](../architecture/provider-extension-model.md)
