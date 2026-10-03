# FYZ / 黑貓 PAY ibon（統一客樂得上游五：豐盈利）

## Status

```text
Provider: FYZ / 黑貓 PAY（豐盈利）
Status: Implementation
ifCode: fyz
wayCode: FYZ_IBON
Capability: ibon CVS Create Payment / Provider Query / APN
Production Deployment: NOT STARTED
Production Token Auth: NOT STARTED
Production Create: NOT STARTED
Production Merchant Query: NOT STARTED
Production Provider Query: NOT STARTED
Live Payment / APN E2E: NOT STARTED
```

## Overview

FYZ（豐盈利）是黑貓 PAY 平台（`www.ccat.com.tw`）上的第五個契約會員上游，與 RYO / JAY / CHI / JHD 使用完全相同的平台契約（Token / Collect / Query / APN / checksum）。JeePay 端以獨立 `ifCode=fyz`、`wayCode=FYZ_IBON` 提供 passage，使同一商户可依上游分別路由。

- 平台契約證據（五家共用）：[`../ryo/contract-evidence.md`](../ryo/contract-evidence.md)
- Adapter 設計（與 JHD 逐字複製，僅換 ifCode/wayCode/params）：[`../ryo/provider-design.md`](../ryo/provider-design.md)
- 上游一（RYO）：[`../ryo/README.md`](../ryo/README.md)

## Implemented JeePay Extension Points

```text
FyzPaymentService
payway/FyzIbon
FyzPayOrderQueryService
FyzChannelNoticeService
model/params/fyz/FyzNormalMchParams
CS.IF_CODE.FYZ
CS.PAY_WAY_CODE.FYZ_IBON
FYZ / FYZ_IBON DB definitions（t_pay_interface_define / t_pay_way；t_pay_interface_config / t_mch_pay_passage 由 Manager 設定）
```

另外同步加入的 cross-provider 清單：`PayOrderMapper.selectDailyProviderAmount` 與 `TelegramDailySummaryTask`（每日收款彙總多一列 `FYZ`）、callback route（dev/prod callback-ingress、prod edge、dev edge renderer）。

## Config Schema

`FyzNormalMchParams` 與 RYO / JAY / CHI / JHD 相同（`environment` / `custId` / `apiPassword`）；`t_pay_interface_config.if_params` 為唯一 credential 來源，Provider class 不 hard-code credential。

- `environment`：`TEST` 或 `PRODUCTION`（必須明確選擇）。
- `custId`：Token username、Collect `cust_id` 與 APN `api_id` 共用的契客代號。
- `apiPassword`：Token password。

> **SECURITY**：真實 `custId` / `apiPassword` 只透過 Manager「商戶管理 → 應用管理 → 支付設定 → 支付參數設定」或 `populate-v2-fyz-secret` 寫入 runtime DB，禁止寫入文件 / code / fixture。`t_pay_interface_config.if_params` 仍標記 `KNOWN SECURITY DEBT`。

## Parity with RYO / JAY / CHI / JHD

FYZ adapter 以 JHD 為基準複製，未修改任何 shared core。將 provider 命名正規化（`Fyz` / `FYZ` / `fyz` 對應 `Jhd` / `JHD` / `jhd`）後，`channel/*`、`channel/*/payway/*`、`model/params/*` 與對應測試共 19 個檔案逐字元相同；API 格式、例外處理、安全驗證與金額/狀態映射見 [`../jhd/README.md`](../jhd/README.md) 的 Parity 章節。

## Non-goals

Refund、Transfer、Division、Channel User、Close、COCS 與其他黑貓 PAY products（與其他四家相同，Phase 1 不擴張）。

## Rollout Plan（依 JHD 2026-09-16 實際上線流程）

1. 新 release 目錄；**只重建並替換 payment image**（manager 另需重建以帶入 Telegram 彙總的 FYZ 列）。
2. 正式 DB 執行 `patch.sql` 第 5 段（`FYZ_IBON` / `fyz` 定義）。
3. Manager 建立 `FYZ_IBON` passage 並填入 `fyz` 支付參數（`PRODUCTION`）。
4. 套用 prod edge 與 callback-ingress 的 `/api/pay/notify/fyz` 路由，`nginx -t` 後 reload。
5. Pilot：Token → Create → Merchant Query → Provider Query；APN route 以無效 payload 驗證 fail-closed（HTTP 400）。Pilot 須帶**真實** merchant `notifyUrl`，否則 Merchant Notify 必然失敗。

## Verification

- 尚無 runtime 證據。

## Sibling Upstreams

| Provider | ifCode | wayCode | 說明 |
| --- | --- | --- | --- |
| RYO | `ryo` | `RYO_IBON` | [`../ryo/README.md`](../ryo/README.md)（上游一） |
| JAY | `jay` | `JAY_IBON` | [`../jay/README.md`](../jay/README.md)（上游二） |
| CHI | `chi` | `CHI_IBON` | [`../chi/README.md`](../chi/README.md)（上游三） |
| JHD | `jhd` | `JHD_IBON` | [`../jhd/README.md`](../jhd/README.md)（上游四；金匯達有限公司） |
| FYZ | `fyz` | `FYZ_IBON` | 本文件（上游五；豐盈利） |
