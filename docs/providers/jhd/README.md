# JHD / 黑貓 PAY ibon（統一客樂得上游四：金匯達有限公司）

## Status

```text
Provider: JHD / 黑貓 PAY（金匯達有限公司）
Status: Verification
ifCode: jhd
wayCode: JHD_IBON
Capability: ibon CVS Create Payment / Provider Query / APN
Production Deployment: PASS
Production Token Auth: PASS
Production Create: PASS
Production Merchant Query: PASS
Production Provider Query: PASS
Live Payment / APN E2E: NOT STARTED
```

## Overview

JHD（金匯達有限公司）是黑貓 PAY 平台（`www.ccat.com.tw`）上的第四個契約會員上游，與 RYO / JAY / CHI 使用完全相同的平台契約（Token / Collect / Query / APN / checksum）。JeePay 端以獨立 `ifCode=jhd`、`wayCode=JHD_IBON` 提供 passage，使同一商户可依上游分別路由。

- 平台契約證據（四家共用）：[`../ryo/contract-evidence.md`](../ryo/contract-evidence.md)
- Adapter 設計（與 CHI 逐字複製，僅換 ifCode/wayCode/params）：[`../ryo/provider-design.md`](../ryo/provider-design.md)
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

- `environment`：`TEST` 或 `PRODUCTION`（必須明確選擇）。
- `custId`：Token username、Collect `cust_id` 與 APN `api_id` 共用的契客代號。
- `apiPassword`：Token password。

> **SECURITY**：真實 `custId` / `apiPassword` 只透過 Manager「商戶管理 → 應用管理 → 支付設定 → 支付參數設定」或 `populate-v2-jhd-secret` 寫入 runtime DB，禁止寫入文件 / code / fixture。`t_pay_interface_config.if_params` 仍標記 `KNOWN SECURITY DEBT`。

## Parity with RYO / JAY / CHI

JHD adapter 以 CHI 為基準逐字複製，未修改任何 shared core。將 provider 命名正規化（`Jhd` / `JHD` / `jhd` 對應 `Chi` / `CHI` / `chi`）後，`channel/*`、`channel/*/payway/*`、`model/params/*` 與對應測試共 19 個檔案與 RYO / JAY / CHI **逐字元相同（0 diff）**。因此下列行為完全一致：

- **API 格式**
  - Token：`POST {base}Token`，`application/x-www-form-urlencoded`，`grant_type=password&username={custId}&password={apiPassword}`；回應取 `access_token` 與 `.expires`（RFC 1123），token 快取至到期前 60 秒。
  - Collect：`POST {base}api/Collect`，`Authorization: Bearer {token}`，JSON body。
  - Create：`cmd=CvsOrderAppend`、`cust_id`、`cust_order_no`（= JeePay `payOrderId`）、`order_amount`（whole TWD）、`expire_date`、`payer_name` / `payer_postcode` / `payer_address` / `payer_mobile` / `payer_email`、`payment_type=0`、`payment_acquirerType=2`、`apn_url`、`order_detail`。
  - Query：`cmd=CvsOrderQuery`、`cust_id`、`cust_order_no`。
  - APN payload：`api_id`、`trans_id`、`order_no`、`amount`、`status`、`nonce`、`checksum`、`payment_code`、`pay_date`、`pay_amount`。
  - Base URL：`TEST=https://test.4128888card.com.tw/app/`、`PRODUCTION=https://cocs.4128888card.com.tw/`。
- **例外處理**：`JhdException` 的五類 `ErrorType`（`CONFIGURATION` / `AUTHENTICATION` / `BUSINESS` / `AMBIGUOUS` / `MALFORMED`）、HTTP 4xx deterministic rejection 分類（排除 408/409/425/429）、transport failure 歸類、以及 Create 結果不明時僅以 Query-first reconciliation 復原（`QUERY_CONFIRMED` / `QUERY_NOT_FOUND` / `QUERY_INCONCLUSIVE` / `QUERY_ERROR`），不自動重送 Append。
- **安全驗證**：`api_id == custId`、MD5 checksum（`api_id:trans_id:amount:status:nonce`）、`payment_code==2`、nonce 為 10 位數字、APN amount 與 authenticated Query 交叉驗證、`trans_id` 綁定 `channelOrderNo`、duplicate / replay、以及 ADR-0007 closed-order paid-APN 例外。
- **金額與狀態映射**：JeePay cents 必須整除 100，映射為 whole TWD；`process_code` `4/7/8` = success、`5/6` = closed、`0/1/3` = waiting、其餘 unknown。

## Non-goals

Refund、Transfer、Division、Channel User、Close、COCS 與其他黑貓 PAY products（與 RYO / JAY / CHI 相同，Phase 1 不擴張）。

## Verification

- **Adapter 一致性**：與 RYO / JAY / CHI 命名正規化後 19/19 檔逐字元相同（0 diff）。
- **測試**：`mvn -B test` → 333 tests / 0 failures；其中 JHD 9 個測試類別、82 tests / 0 failures，與 RYO / JAY / CHI 各 82 對等（architecture 命名契約、kit/checksum、client、params resolver、log sanitizer、query、channel notice、notice flow、payway）。
- **Production live（`jee8pay-v2-production`，release `537d049b1e8b-93c19b0f`）**
  - Token authentication：PASS（`PRODUCTION` base URL，HTTP 200）
  - `JHD_IBON` Create：PASS（PayOrder `P2099898153971400706`，TWD 40，ibon code `CCAT625906937679`，`expire_date` 2026-09-16）
  - Merchant Query：PASS（local `PayOrder`，`ifCode=jhd` / `wayCode=JHD_IBON` / `state=1`）
  - Provider Query：PASS（reissue 對上游查單 `status=OK`、`process_code=3`、`order_amount=40`、`bill_amount=40`）
  - APN route：PASS（`/api/pay/notify/jhd` 可達並 fail-closed 拒絕無效 payload）
- **尚未**完成真實付款 / APN 轉態 / Merchant Notify E2E，因此不宣稱完整 E2E。

## Sibling Upstreams

| Provider | ifCode | wayCode | 說明 |
| --- | --- | --- | --- |
| RYO | `ryo` | `RYO_IBON` | [`../ryo/README.md`](../ryo/README.md)（上游一） |
| JAY | `jay` | `JAY_IBON` | [`../jay/README.md`](../jay/README.md)（上游二） |
| CHI | `chi` | `CHI_IBON` | [`../chi/README.md`](../chi/README.md)（上游三） |
| JHD | `jhd` | `JHD_IBON` | 本文件（上游四；金匯達有限公司） |
