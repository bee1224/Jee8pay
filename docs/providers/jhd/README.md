# JHD / 黑貓 PAY ibon（統一客樂得上游四：金匯達有限公司）

## Status

```text
Provider: JHD / 黑貓 PAY
Status: Implementation
ifCode: jhd
wayCode: JHD_IBON
Capability: ibon CVS Create Payment / Provider Query / APN
```

## Overview

JHD（金匯達有限公司）是黑貓 PAY 平台（`www.ccat.com.tw`）上的第四個契約會員上游，與 RYO / JAY / CHI 使用完全相同的平台契約（Token / Collect / Query / APN / checksum）。JeePay 端以獨立 `ifCode=jhd`、`wayCode=JHD_IBON` 提供 passage，使同一商户可依上游分別路由。

- 平台契約證據（共用）：[`../ryo/contract-evidence.md`](../ryo/contract-evidence.md)
- Adapter 設計（CHI 逐字複製，僅換 ifCode/wayCode/params）：[`../ryo/provider-design.md`](../ryo/provider-design.md)
- 上游一（RYO）：[`../ryo/README.md`](../ryo/README.md)

## Implemented JeePay Extension Points

```text
JhdPaymentService
payway/JhdIbon
JhdPayOrderQueryService
JhdChannelNoticeService
model/params/jhd/JhdNormalMchParams
CS.IF_CODE.JHD
CS.PAY_WAY_CODE.JHD_IBON
JHD / JHD_IBON DB definitions（t_pay_interface_define / t_pay_way / t_pay_interface_config / t_mch_pay_passage）
```

## Config Schema

`JhdNormalMchParams` 與 RYO / JAY / CHI 相同（`environment` / `custId` / `apiPassword`）；`t_pay_interface_config.if_params` 為唯一 credential 來源，Provider class 不 hard-code credential。

> **SECURITY**：真實 `custId` / `apiPassword` 只透過 Manager「支付配置 → 支付參數」或 `populate-v2-jhd-secret` 寫入 runtime DB，禁止寫入文件 / code / fixture。`t_pay_interface_config.if_params` 仍標記 `KNOWN SECURITY DEBT`。

## Non-goals

Refund、Transfer、Division、Channel User、Close、COCS 與其他黑貓 PAY products（與 RYO / JAY / CHI 相同，Phase 1 不擴張）。

## Verification

- JHD 提供與 CHI 對等的 provider-specific client、payway、query、APN/security、flow 與 architecture 測試。
- jhd / JHD_IBON 的 DB 定義與 callback route 已納入 seed 與 edge/ingress 設定。
- **尚未**取得 JHD 專屬憑證的 live Token／Create／Provider Query／付款／APN 驗證證據，因此維持 `Status: Implementation`，不宣稱 Verification。
