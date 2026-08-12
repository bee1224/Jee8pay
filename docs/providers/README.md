# Providers

Provider documentation 只記錄特定 Provider 的 official contract、mapping、implementation design、verification 與 Provider-specific operations。本表是 documentation navigation，不是 runtime Provider registry。

## Provider Registry

| Provider | ifCode | Status | Current Scope | Entry |
| --- | --- | --- | --- | --- |
| CCAT | `ccat` | Verification | ibon | [`ccat/README.md`](ccat/README.md) |
| NewebPay | TBD | Deferred | TBD | — |

Taiwan V2 Phase 1 第一個 operational release 是 CCAT-only，支援 `CCAT` / `CCAT_IBON`；NewebPay 已延後且不阻擋 CCAT-only Production Candidate。其他 upstream Provider 可保留為相容性或歷史實作，但不屬於目前 release scope。

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
