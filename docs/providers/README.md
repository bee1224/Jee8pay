# Providers

Provider documentation 只記錄特定 Provider 的 official contract、mapping、implementation design、verification 與 Provider-specific operations。本表是 documentation navigation，不是 runtime Provider registry。

## Provider Registry

| Provider | ifCode | Status | Current Scope | Entry |
| --- | --- | --- | --- | --- |
| CCAT | `ccat` | Design Blocked | ibon | [`ccat/README.md`](ccat/README.md) |
| NewebPay | TBD | Planned | TBD | — |
| ECPay | TBD | Planned | TBD | — |
| PAYUNi | TBD | Planned | TBD | — |
| LINE Pay | TBD | Planned | TBD | — |
| TapPay | TBD | Planned | TBD | — |

Planned Providers 尚未完成設計；表格只表達 roadmap 與文件入口。

## Provider Documentation Lifecycle

Provider README 的 status 只使用：

```text
Planned
Design
Implementation
Verification
Production
Deprecated
```

Status 直接標在 Provider README。只有真的取得驗證證據時才建立 `verification.md`，不為 lifecycle 建立空白 status 文件。
