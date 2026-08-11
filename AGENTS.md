# JeePay Taiwan Workspace Instructions

## PROJECT IDENTITY

```text
JeePay is the platform core.
Taiwan payment companies are Providers.
```

## CURRENT PHASE

```text
Current provider: CCAT
Current product: ibon CVS payment
Current capabilities:
- Create Payment
- Provider Query
- Payment Notify / APN
```

其他 CCAT capability 不得自行擴張。

## UPSTREAM PRESERVATION RULE

加入 Provider 時，先找既有 JeePay extension point，優先新增 Provider-specific adapter；不可只因「設計更漂亮」重構 core。只有證明既有 contract 無法完成需求時才可修改 shared core，且須保持 upstream diff 最小。

## PROVIDER RUNTIME CONTRACT

```text
MchPayPassage.ifCode
→ ${ifCode}PaymentService
→ wayCode
→ PaywayUtil
→ payway/<WayCode>
```

Capability bean naming：`${ifCode}PaymentService`、`${ifCode}PayOrderQueryService`、`${ifCode}ChannelNoticeService`、`${ifCode}RefundService`、`${ifCode}ChannelRefundNoticeService`、`${ifCode}TransferService`、`${ifCode}TransferNoticeService`、`${ifCode}ChannelUserService`、`${ifCode}DivisionService`。不得另建 Provider registry。

## CONFIG CONTRACT

Provider config 優先依下列 contract：

```text
NormalMchParams
→ model/params/<ifCode>/<IfCode>NormalMchParams
```

CCAT 預期為 `model/params/ccat/CcatNormalMchParams`。credentials 使用既有 `PayInterfaceConfig` / `if_params` JSON，Provider class 不得 hard-code credential。

## PAYMENT QUERY RULE

Merchant Query API 只查 JeePay local `PayOrder`。Provider upstream query 應遵循：

```text
PayOrderReissue
→ ChannelOrderReissueService
→ ${ifCode}PayOrderQueryService
```

除非未來有明確需求與架構決策，不得將 `QueryOrderController` 改為每次同步打 Provider。

## CALLBACK OWNERSHIP

JeePay Core 負責 callback routing、local PayOrder lookup、state transition、merchant notify 與 MQ / retry。Provider Adapter 負責 Provider signature/checksum、status、merchant/account identity、amount、provider-specific replay/security validation 與 Provider ACK；不得繞過 JeePay merchant notify infrastructure。

## IDEMPOTENCY RULE

沿用 JeePay existing state transition / `MchNotifyRecord` 防重。APN 仍必須處理 duplicate callback、replay、invalid checksum、invalid amount、mismatched provider transaction、mismatched merchant/provider identity，不能只因 Core 有 state guard 而略過 Provider security。

## CORE MODIFICATION BOUNDARY

### GREEN

正常 extension：`channel/<provider>/**`、`model/params/<provider>/**`、Provider ifCode constant、PayInterface / PayWay seed/config data、generic Provider config schema、必要 Provider-specific tests。

### YELLOW

只有既有 extension point 明確不足時才可修改 shared payment request/response、generic callback contract、generic config rendering 或 generic Provider resolution。修改前須指出 extension point、以 code evidence 說明不足、提出 alternative，並保持 diff 最小。

### RED

新增 Provider 原則上不得修改 PayOrder domain/state machine、`AbstractPayOrderController`、`ChannelNoticeController`、`PayOrderProcessService`、merchant notification MQ/retry、authentication 或 RBAC；除非任務明確要求重新設計核心架構。

## SECURITY

不得 commit Provider credentials、API passwords、tokens、private keys 或 production secrets，也不得將真實 secret 放入 README、AGENTS、Skill 或 test fixture。測試資料只用 placeholder、environment variable、test-only dummy value 或 safe fixture。

`t_pay_interface_config.if_params` 尚未證明有 DB field-level encryption，標記為 `KNOWN SECURITY DEBT`。不得在 Provider migration 任務中順手重構 Secret Management；須另立任務。

## CHANGE DISCIPLINE

每次 Provider 任務先讀相似 Provider，明確能力與 non-goals，做最小 diff 與 targeted tests；不順手清理無關 code、不擴 scope，也不自行 git commit / push，除非使用者明確要求。

## EVIDENCE RULE

架構判斷優先 code evidence；docs 與 runtime code 衝突時標記 drift，不自行猜測。無證據結論標示 `UNKNOWN`。

## DOCUMENTATION GOVERNANCE

### Canonical Entry

所有 project documentation 從 `docs/README.md` 導航。

### Documentation Responsibilities

- Architecture：描述 current system behavior。
- ADR：描述 durable decision and why。
- Provider Docs：描述 provider-specific contract、design 與 verification。
- Operations：描述 runtime、deployment 與 troubleshooting。
- Debt：描述 known unresolved debt。

### ADR Creation Rule

普通 implementation work 不建立 ADR。只有 architectural、security、data 或 cross-provider durable decision 才建立 ADR。

### ADR Candidate Rule

工作中若出現可能值得 ADR 的新決策，先在 handoff 列為 `ADR Candidate`，包括 decision、alternatives、reason 與 impact。工作本身已明確授權建立 ADR 時才可建立；否則不要自動產生。

### No Orphan Docs

新增 Markdown 前：

1. 確認 canonical category。
2. 確認 index。
3. 確認是否已有同義文件。

禁止新增無索引的 root-level random Markdown。
