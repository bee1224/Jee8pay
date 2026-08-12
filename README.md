# Taiwan JeePay Workspace

## Project Identity

JeePay 是 platform core。本 Workspace 將台灣支付公司整合為 Provider，保留 JeePay 核心架構，不另造一套 payment platform。

## Workspace Structure

- `jeepay/`：Java backend；其既有說明見 [`jeepay/README.md`](jeepay/README.md)。
- `jeepay-ui/`：frontend；其既有說明見 [`jeepay-ui/README.md`](jeepay-ui/README.md)。
- `.agents/`：可重複使用的 Codex workflows。
- [`docs/`](docs/README.md)：Taiwan Workspace documentation 的 canonical index。
- `.codex/`：project-scoped Codex configuration；僅在有實際需要時使用。

## Architecture Principle

```text
Merchant
   ↓
JeePay
   ↓
Provider Adapter
   ↓
Taiwan Payment Provider
```

不得建立繞過 JeePay core 的第二套 transaction system。

## Provider Roadmap

- CCAT / 黑貓 PAY — current；ibon
- NewebPay / 藍新 — planned

其他 upstream Provider 可作相容性或歷史參考，但不屬於目前 Taiwan Provider roadmap。

## Current CCAT Scope

- ibon
- Create Payment
- Provider Query
- APN / Payment Notify

Non-goals：Refund、Transfer、Division、Channel User、Close Order，以及其他 CCAT products。

## Development Entry Point

AI / Codex 工作建議由此 `Jee8pay` Workspace root 開始。build 或 run 指令請只依 nested README 與既有 `docker-compose` 檔案的實際內容執行；本 README 不臆測指令。
