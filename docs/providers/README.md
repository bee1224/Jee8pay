# Providers

Provider documentation 只記錄特定 Provider 的 official contract、mapping、implementation design、verification 與 Provider-specific operations。本表是 documentation navigation，不是 runtime Provider registry。

## Provider Registry

| Provider | ifCode | Status | Current Scope | Entry |
| --- | --- | --- | --- | --- |
| RYO | `ryo` | Verification | ibon | [`ryo/README.md`](ryo/README.md) |
| JAY | `jay` | Implementation | ibon | [`jay/README.md`](jay/README.md) |
| CHI | `chi` | Implementation | ibon | [`chi/README.md`](chi/README.md) |
| NewebPay | TBD | Deferred | TBD | — |

Taiwan V2 目前 active 的是黑貓 PAY 平台上的三個統一客樂得上游：`RYO`（由原 `CCAT` 改名）/ `RYO_IBON`、`JAY` / `JAY_IBON`、`CHI` / `CHI_IBON`；三者共用同一平台契約（見 [`ryo/contract-evidence.md`](ryo/contract-evidence.md)）。NewebPay 已延後且不阻擋黑貓 PAY-only 使用。

## Provider Documentation Lifecycle

Provider README 的 status 只使用：

```text
Planned
Deferred
Design
Implementation
Verification
Production
Deprecated
```

Status 直接標在 Provider README。只有真的取得驗證證據時才建立 `verification.md`，不為 lifecycle 建立空白 status 文件。
