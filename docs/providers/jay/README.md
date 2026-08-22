# JAY / 黑貓 PAY ibon（統一客樂得上游二）

## Status

```text
Provider: JAY / 黑貓 PAY
Status: Implementation
ifCode: jay
wayCode: JAY_IBON
Capability: ibon CVS Create Payment / Provider Query / APN
```

## Overview

JAY 是黑貓 PAY 平台（`www.ccat.com.tw`）上的第二個契約會員上游，與 RYO 使用完全相同的平台契約（Token / Collect / Query / APN / checksum）。JeePay 端以獨立 `ifCode=jay`、`wayCode=JAY_IBON` 提供 passage，使同一商户可依上游分別路由。

- 平台契約證據（共用）：[`../ryo/contract-evidence.md`](../ryo/contract-evidence.md)
- Adapter 設計（RYO 逐字複製，僅換 ifCode/wayCode/params）：[`../ryo/provider-design.md`](../ryo/provider-design.md)
- 上游一（RYO）：[`../ryo/README.md`](../ryo/README.md)

## Implemented JeePay Extension Points

```text
JayPaymentService
payway/JayIbon
JayPayOrderQueryService
JayChannelNoticeService
model/params/jay/JayNormalMchParams
CS.IF_CODE.JAY
CS.PAY_WAY_CODE.JAY_IBON
JAY / JAY_IBON DB definitions（t_pay_interface_define / t_pay_way / t_pay_interface_config / t_mch_pay_passage）
```

## Config Schema

`JayNormalMchParams` 與 RYO 相同（`environment` / `custId` / `apiPassword`）；`t_pay_interface_config.if_params` 為唯一 credential 來源，Provider class 不 hard-code credential。

> **SECURITY**：真實 `custId` / `apiPassword` 只透過 Manager「支付配置 → 支付參數」或 `populate-v2-jay-secret` 寫入 runtime DB，禁止寫入文件 / code / fixture。`t_pay_interface_config.if_params` 仍標記 `KNOWN SECURITY DEBT`。

## Non-goals

Refund、Transfer、Division、Channel User、Close、COCS 與其他黑貓 PAY products（與 RYO 相同，Phase 1 不擴張）。

## Verification

- JAY 提供與 RYO 對等的 provider-specific client、payway、query、APN/security、flow 與 architecture 測試。
- Development live TWD 40 Token／Create／Provider Query／真實付款／APN 已於 2026-08-21 驗證 PASS（PayOrder `P2090732500417515522`）。該訂單未設定 `notifyUrl`，因此不宣稱 JAY Merchant Notify E2E；外部 Merchant 完整驗收仍是後續 gate。
