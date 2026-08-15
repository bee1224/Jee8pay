# JeePay V2 Merchant UAT 串接文件

## A. Environment

| 項目 | 值 |
| --- | --- |
| Platform environment | `DEVELOPMENT`（nnviopp JeePay V2） |
| Upstream payment environment | `PRODUCTION` |
| Base URL | `https://api-v2-dev.nnviopp.com` |
| Merchant ID (`mchNo`) | `M_D01_EXTERNAL_UAT` |
| App ID (`appId`) | `APP_D01_EXTERNAL_UAT` |
| Channel code (`wayCode`) | `CCAT_IBON` |
| Currency | `TWD` |

這是外部 Merchant UAT 平台，但 `CCAT_IBON` 連接真實 Production payment provider。成功 Create 可能產生真實 ibon 訂單；請只提交已獲測試授權的金額與筆數。外部系統只串接本文件的 JeePay V2 API，不串接 CCAT，也不需要任何 CCAT credential。

UAT ingress 只允許 `34.92.245.74`、`34.92.52.162`。`35.220.239.87` 只記錄為未來 Production allowlist，本 UAT 未啟用。

## B. Credential delivery

Merchant API credential 是 App Secret。真實值不在 Git，請由我方透過 secure handoff 交付；收到後應放在 secret manager 或受限環境變數，不可寫入 source、log、ticket 或群組訊息。

```text
Merchant ID = M_D01_EXTERNAL_UAT
App ID      = APP_D01_EXTERNAL_UAT
App Secret  = 【敏感：請從 secure handoff 取得】
```

Merchant ID／App ID／App Secret 是「外部 Merchant → JeePay」的 downstream credential。CCAT custId、API password、Token 等 upstream credential 不會也不應提供給外部系統商。

## C. Authentication and signing

所有 Create／Query request 都以同一個 App Secret 驗簽：

1. request 必須包含 `version=1.0`、`signType=MD5`、`reqTime`、`mchNo`、`appId` 與 `sign`。
2. 移除 `sign`；排除值為 `null` 或空字串 `""` 的欄位，數字 `0` 仍參與。
3. 依欄位名稱做 case-insensitive 升冪排序。
4. 每個欄位串為 `key=value&`，最後直接附加 `key=<AppSecret>`。
5. 對完整 UTF-8 bytes 計算 MD5，輸出 32 字元 uppercase hexadecimal。

`channelExtra` 是 JSON **字串**，其字元、空白與欄位順序會直接影響簽名；建議先產生 compact JSON 字串，再組 request 與簽名。簽名值可用大小寫任一形式送出，但範例固定 uppercase。

`reqTime` 是必填且參與簽名的字串，建議使用目前 Unix epoch milliseconds。現行 runtime **沒有 timestamp freshness／expiry 驗證**；不存在 nonce 欄位。MD5 shared-secret signature 是既有 native contract，不是新設計；所有呼叫必須使用 HTTPS，且不可記錄含 App Secret 的 canonical string。

Synthetic Create vector：[`examples/create-vector.json`](examples/create-vector.json)。執行：

```bash
python3 docs/integration/merchant-uat/examples/verify_vectors.py
```

## D. Create order

```text
METHOD       = POST
URL          = https://api-v2-dev.nnviopp.com/api/pay/unifiedOrder
CONTENT-TYPE = application/json; charset=UTF-8
```

| 欄位 | 型別 | UAT 規則 |
| --- | --- | --- |
| `version` | string | 必填，`1.0` |
| `signType` | string | 必填，`MD5` |
| `reqTime` | string | 必填，建議 current epoch milliseconds |
| `mchNo` | string | 必填，secure handoff 的 Merchant ID |
| `appId` | string | 必填，secure handoff 的 App ID |
| `mchOrderNo` | string | 必填；同一 Merchant 必須唯一，重複會回「商戶訂單已存在」 |
| `wayCode` | string | 必填，固定 `CCAT_IBON` |
| `amount` | integer | 必填，JeePay amount units；`1 TWD = 100 JeePay amount units`；CCAT_IBON 另要求可整除 100 |
| `currency` | string | 必填，固定 uppercase `TWD` |
| `subject` | string | 必填，商品／訂單標題 |
| `body` | string | 必填，商品／訂單描述 |
| `notifyUrl` | string | UAT flow 必填；外部可接收的 HTTPS callback URL |
| `returnUrl` | string | 選填；CCAT_IBON direct API flow 不依賴此欄位 |
| `clientIp` | string | 選填；省略時使用 ingress 傳入的 caller IP |
| `expiredTime` | integer | 選填，從建立時間起算的秒數；省略時 native default 為 2 小時 |
| `channelExtra` | JSON string | `CCAT_IBON` 必填，內容見下表 |
| `extParam` | string | 選填；原樣帶入 Query／Notify |
| `divisionMode` | integer | 本產品不使用，請省略 |
| `sign` | string | 必填，依上節計算 |

`amount` 的 JSON type 必須是 integer，金額換算是 exact integer conversion：`1000 = TWD10`、`4000 = TWD40`。因此 `amount=4000` 不是 TWD4000，而是 TWD40。`CCAT_IBON` 另要求 `amount % 100 == 0`。`OFFICIAL_MINIMUM = NOT_SPECIFIED`；TWD40 只是已成功實測金額，不是官方 minimum。

`channelExtra` 必須是字串化 JSON，且包含：

| 欄位 | 規則 |
| --- | --- |
| `payerName` | 必填，繳款人姓名資料；不代表已做實名/KYC 驗證 |
| `payerPostcode` | 必填 |
| `payerAddress` | 必填 |
| `payerMobile` | 必填 |
| `payerEmail` | 必填 |

完整 synthetic request 已在 Create vector；換入 secure handoff 的三個 downstream 值、目前 `reqTime`、唯一 `mchOrderNo`、真實 callback URL 後重新簽名即可送出。

### Create response

成功的 native envelope：

```json
{
  "code": 0,
  "msg": "SUCCESS",
  "data": {
    "payOrderId": "P...",
    "mchOrderNo": "UAT-...",
    "orderState": 1,
    "payDataType": "ccatIbon",
    "payData": "{\"ibonShopId\":\"...\",\"ibonCode\":\"...\",\"paymentCode\":\"...\",\"expireDate\":\"YYYY-MM-DD\",\"billAmount\":40,\"shortUrl\":\"https://...\"}"
  },
  "sign": "..."
}
```

`payData` 是 JSON 字串，需再 parse 一次；不得把它當作 nested JSON object。第一次 parse 取得 UnifiedOrder response，第二次 parse `data.payData` 才取得付款指示。欄位均由 actual runtime response model 產生：`ibonShopId`、`ibonCode`、組合後的 `paymentCode`、`expireDate`、whole-TWD `billAmount`、可選 `shortUrl`。真實 TWD 40 acceptance response 同步回傳付款資訊與非空 `shortUrl`，所以問卷分類為 `3 = 兩者皆同步返回`；程式仍應容許 provider 未回 `shortUrl` 時只使用付款碼。

可 machine-read 的 synthetic response 見 [`examples/unified-order-success.json`](examples/unified-order-success.json)。該 fixture 刻意省略可選的 `shortUrl`，但仍包含可實際使用的付款碼、到期日與金額，並由 `verify_vectors.py` 驗證 outer parse、`payData` string type、第二次 JSON parse 與付款指示欄位。

response envelope 的 `sign` 是只針對 `data` object、用同一 App Secret 與同一 canonicalization 計算，不包含 `code`／`msg`。

### WAITING is not paid

`code=0` 與 `orderState=1` 代表 Create 成功、付款資訊已建立，**不代表付款成功**。Native state：`0=INIT`、`1=ING/WAITING`、`2=SUCCESS`、`3=FAIL`、`4=REVOKED`、`5=REFUND`、`6=CLOSED`。只能在 Merchant Notify 或 Query 確認 `state=2` 後上分。

`CCAT_IBON` 正常成功的 WAITING response 有一項 invariant：`code=0`、`orderState=1` 時，`payData` 必須是 populated JSON string，且第二次 parse 後必須包含可解析、可實際使用的 payment instruction。Deterministic Provider Create failure 不得回傳 `code=0`、`orderState=1`、`payData={}`（wire value 可能表現為空 JSON string `"{}"`）。

若收到違反此 invariant 的 response，尤其 `payData={}` 或沒有可使用的 payment instruction，Merchant 不得將它視為正常可付款訂單或支付成功、不得上分，也不得 blind retry 同一業務交易。請保留原 `mchOrderNo`，依 Query／error contract 處理；必要時聯絡我方查核。

## E. Query order

```text
METHOD       = POST
URL          = https://api-v2-dev.nnviopp.com/api/pay/query
CONTENT-TYPE = application/json; charset=UTF-8
```

Auth 欄位與 Create 相同。另傳 `payOrderId` 或 `mchOrderNo` 至少一個；建議只傳一個，若兩者同時存在，native implementation 優先使用 `payOrderId`。Query 只讀 JeePay local PayOrder，不會為每次 Merchant Query 同步呼叫 CCAT。

```json
{
  "version": "1.0",
  "signType": "MD5",
  "reqTime": "<current epoch milliseconds>",
  "mchNo": "M_D01_EXTERNAL_UAT",
  "appId": "APP_D01_EXTERNAL_UAT",
  "mchOrderNo": "UAT-20260813-0001",
  "sign": "<依完整 request 重算>"
}
```

WAITING response 的 `data.state=1`；SUCCESS response 的 `data.state=2`。`data` 的 actual 欄位為 `payOrderId`、`mchNo`、`appId`、`mchOrderNo`、`ifCode`、`wayCode`、`amount`、`currency`、`state`、`clientIp`、`subject`、`body`、`channelOrderNo`、`errCode`、`errMsg`、`extParam`、`successTime`、`createdAt`；值為 null 的欄位可能不出現。Envelope `sign` 同樣只簽 `data`。

WAITING synthetic example：

```json
{
  "code": 0,
  "msg": "SUCCESS",
  "data": {
    "payOrderId": "P_SYNTHETIC_0001",
    "mchNo": "M_SYNTHETIC_UAT",
    "appId": "APP_SYNTHETIC_UAT",
    "mchOrderNo": "UAT-SYNTH-0001",
    "ifCode": "ccat",
    "wayCode": "CCAT_IBON",
    "amount": 4000,
    "currency": "TWD",
    "state": 1
  },
  "sign": "<依 data 與 App Secret 計算>"
}
```

同一訂單付款完成後的 SUCCESS synthetic example：

```json
{
  "code": 0,
  "msg": "SUCCESS",
  "data": {
    "payOrderId": "P_SYNTHETIC_0001",
    "mchNo": "M_SYNTHETIC_UAT",
    "appId": "APP_SYNTHETIC_UAT",
    "mchOrderNo": "UAT-SYNTH-0001",
    "ifCode": "ccat",
    "wayCode": "CCAT_IBON",
    "amount": 4000,
    "currency": "TWD",
    "state": 2,
    "channelOrderNo": "SYNTHETIC_PROVIDER_REFERENCE",
    "successTime": 1786581600000
  },
  "sign": "<依 data 與 App Secret 計算>"
}
```

## F. Merchant Notify

外部只需實作 `JeePay → Merchant` callback；`CCAT → JeePay` APN 是我方內部責任，請勿設定或處理。

```text
METHOD       = POST
CONTENT-TYPE = application/x-www-form-urlencoded
SOURCE IP    = 159.198.40.128
ACK BODY     = SUCCESS
```

Create 的 `notifyUrl` 可使用 HTTP 或 HTTPS 語法；D01 對外 UAT 要求使用 HTTPS。JeePay 將 Query response 的非 null 訂單欄位，加上 `reqTime` 與 `sign`，以 form fields POST 到該 URL。驗簽時移除 `sign`，再套用與 request 完全相同的 App Secret canonicalization。Synthetic Notify vector 與 exact ACK 在 [`examples/notify-vector.json`](examples/notify-vector.json)。

只有 terminal state 會建立通知。對 CCAT_IBON 上分只接受 `state=2`；入帳金額欄位是 `amount`，單位仍為 minor units。`billAmount` 是付款指示中的 whole-TWD payer amount，不是 Merchant 上分欄位。

Merchant 必須以 `payOrderId`（並可交叉比對 `mchOrderNo`）做 idempotency。JeePay 對同一 order 只建立一筆 logical Notify record；若 callback response body 不是 case-insensitive exact `SUCCESS` 或連線失敗，最多送 6 次，首次立即，後續以 30、60、90、120、150 秒 delay 重新排送。HTTP 2xx 但 body 不是 `SUCCESS` 仍視為失敗。

Native sender 的成功判斷只比對 response body，沒有另外檢查 response HTTP status 或 Content-Type。為確保 UAT 行為明確，Merchant 應回 `HTTP 200`、`Content-Type: text/plain`、body `SUCCESS`（大小寫不敏感，但不可含額外空白或其他內容）。

UAT Merchant Notify outbound IP 已從 actual `jee8pay-v2-dev-payment` container 的 network namespace，以兩個獨立外部 IP reflectors 實測一致為 `159.198.40.128`。

## G. Error handling

- HTTP route：非 allowlist source 回 `403`；非公開 path 回 `404`；非 POST method 不允許。
- API envelope：`code=0` 才是 API success；常見 business failure 為 `code=9999` 並在 `msg` 帶原因。
- Invalid／modified signature、wrong Merchant/App、malformed payload 都不得重送為新 `mchOrderNo` 來碰運氣，先修正 request。
- Duplicate `mchOrderNo`：改用 Query 查既有 order；不要換新編號重送同一業務交易。
- Create timeout／connection ambiguity：先 Query 原 `mchOrderNo`，不要直接換 key 重送。
- Create success 後持續 WAITING：讓使用者付款，並以 Notify／Query 判定；不可先上分。

## H. UAT flow

1. 由 secure handoff 取得 Merchant ID、App ID、App Secret。
2. 提供 UAT HTTPS Merchant Notify callback URL。
3. 依 canonicalization 簽 Create request。
4. 呼叫 public UAT Base URL；JeePay 建立 native PayOrder。
5. JeePay 以 `CCAT_IBON` 路由並同步回傳 ibon 付款資訊。
6. 測試者依已授權金額完成真實付款。
7. CCAT 通知 JeePay；JeePay 將 native PayOrder 轉為 SUCCESS。
8. JeePay 對 Merchant callback URL 發送 Merchant Notify。
9. Merchant 驗簽、確認 `state=2`／`amount`／order identity、冪等上分，回純文字 `SUCCESS`。
10. Merchant 呼叫 Query，確認同一 order 為 `state=2`。

外部系統商負責保存 Merchant credential、Create、Query、callback URL、Notify 驗簽／ACK、冪等與配合付款。我方負責 JeePay V2、Provider credential/APN、reconciliation、Merchant config/credential、Notify/retry、DNS/TLS、infrastructure 與 logs。

## I. Product answers

| 問題 | 回答 |
| --- | --- |
| 代收 | 支援，`CCAT_IBON` |
| 代付 | 不支援／deferred；餘額查詢與固定銀行編碼不適用 |
| 代收實名 | 不要求實名/KYC；Create 仍必填 payer name/contact/address data |
| 真實金額上分 | 是；只在 SUCCESS Notify／Query 後使用 `amount`（minor units） |
| 同步返回 | `3`；actual acceptance response 同時有付款資訊與 `shortUrl` |
| Official minimum | `NOT_SPECIFIED`；provider/account validation 為準 |
| Successfully tested | TWD 40（`amount=4000`） |

## J. Security boundary

本 package 與 examples 只有 synthetic secret。它們不包含 upstream Provider credential、真實 UAT App Secret、DB/VPS/Cloudflare credential。外部串接不需要 upstream secret。

## K. External Consumer clarity closure

JEE-EC01 的 NC-01／NC-02／NC-03 closure 與 fresh execution evidence 見 [`JEE-EC01R1-external-consumer-closure.md`](JEE-EC01R1-external-consumer-closure.md)。

## L. Related documents

| 文件 | 用途 |
| --- | --- |
| [`UAT-START-NOTICE.md`](UAT-START-NOTICE.md) | **外部 UAT 啟動前必讀**：精確錯誤訊息表、notifyUrl 陷阱、真人付款安排、freeze 規則、到期行為（CLOSED(6)）與競態風險 |
| [`JEE-EC01R1-external-consumer-closure.md`](JEE-EC01R1-external-consumer-closure.md) | External consumer NC-01/02/03 closure 與執行證據 |
| [`examples/`](examples/) | synthetic 簽名向量（`create-vector.json`、`notify-vector.json`、`unified-order-success.json`、`verify_vectors.py`、`run-d01-blackbox.py`） |
| [`../../operations/merchant-uat-frontend-operator-map.md`](../../operations/merchant-uat-frontend-operator-map.md) | Manager/Merchant/Cashier 頁面對應（operator 操作指引） |
| [`../../providers/ccat/README.md`](../../providers/ccat/README.md) | CCAT Provider 狀態與契約文件入口 |
| `deploy/jee8pay-v2-dev/scripts/monitor-uat.sh` | UAT 期間唯讀監控快照（在 nnviopp-sandbox 上以 sudo 執行） |

> 本文件所有請求範例的 `notifyUrl` 均為 placeholder；外部系統商必須使用自己的接收端 URL（見 [`UAT-START-NOTICE.md`](UAT-START-NOTICE.md) §3）。
