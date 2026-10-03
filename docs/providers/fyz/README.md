# FYZ / 黑貓 PAY ibon（統一客樂得上游五：豐盈利）

## Status

```text
Provider: FYZ / 黑貓 PAY（豐盈利）
Status: Verification
ifCode: fyz
wayCode: FYZ_IBON
Capability: ibon CVS Create Payment / Provider Query / APN
Production Deployment: PASS
Production Token Auth: PASS
Production Create: PASS
Production Merchant Query: PASS
Production Provider Query: PASS
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

- **Adapter 一致性**：與 JHD 命名正規化後 19/19 檔逐字元相同（0 diff）。
- **測試**：`mvn -B test` 0 failures；FYZ 9 個測試類別、82 tests / 0 failures（dev VPS `maven:3.9.16-eclipse-temurin-17` 容器）。
- **部署前隔離驗證**：以 production compose/config 在 dev VPS 起隔離 stack（internal network、無對外 port），payment/manager 正常啟動、`patch.sql` 第 5 段可重複執行、`/api/pay/notify/fyz` 進入 `fyzChannelNoticeService` 並 fail-closed 400；新 prod `edge-nginx.conf` `nginx -t` 通過。
- **Production（`jee8pay-v2-production`，release `749262893269-fyz`，2026-10-03 14:24 切換）**
  - 只替換 payment 與 manager image；DB 先備份 `t_pay_way` / `t_pay_interface_define` 再寫入定義；edge / callback-ingress 重建後五家 `notify` 皆 400、`admin-v2` 200。
  - `fyz` 支付參數（`PRODUCTION`）與 `FYZ_IBON` passage 由 operator 於 Manager 設定。
  - Token authentication：PASS（Create 成功即代表 Token 取得成功）
  - `FYZ_IBON` Create：PASS（PayOrder `P2106270937685344257`，TWD 40，ibon code `CCAT627609106873`，`expire_date` 2026-10-03）
  - Merchant Query：PASS（local `PayOrder`，`ifCode=fyz` / `wayCode=FYZ_IBON` / `state=1`）
  - Provider Query：PASS（reissue 對上游查單 `status=OK`、`process_code=3`、`order_amount=40`、`bill_amount=40`）
  - APN route：PASS（`https://ccat-v2.lp33ing.com/api/pay/notify/fyz` 可達並 fail-closed 拒絕無效 payload）
  - Log 未出現 `custId` / `apiPassword` 明文。
- **尚未**完成真實付款 / APN 轉態 / Merchant Notify E2E，因此不宣稱完整 E2E。
- **測試環境**：程式與 callback-ingress 設定已具備，但 dev 尚未部署本 release、DB 無 FYZ 定義，dev edge 公開路由隨 TD-014 待人工核准。

## Sibling Upstreams

| Provider | ifCode | wayCode | 說明 |
| --- | --- | --- | --- |
| RYO | `ryo` | `RYO_IBON` | [`../ryo/README.md`](../ryo/README.md)（上游一） |
| JAY | `jay` | `JAY_IBON` | [`../jay/README.md`](../jay/README.md)（上游二） |
| CHI | `chi` | `CHI_IBON` | [`../chi/README.md`](../chi/README.md)（上游三） |
| JHD | `jhd` | `JHD_IBON` | [`../jhd/README.md`](../jhd/README.md)（上游四；金匯達有限公司） |
| FYZ | `fyz` | `FYZ_IBON` | 本文件（上游五；豐盈利） |
