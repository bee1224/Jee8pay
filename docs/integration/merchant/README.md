# JeePay V2 Merchant 串接文件（正式環境）

> **STATUS（2026-08-21）**：本文件是**正式環境的 Merchant 串接契約**，API 契約／簽名／欄位與
> [UAT 串接文件](../merchant-uat/README.md) 完全一致，差別只在環境網址與商戶憑證。
> 正式環境（`jee8pay-v2-production` Production Candidate）的 **Production Validation 尚未完成**：
> credential binding、正式網址 DNS/TLS、public callback 啟用與 pilot 交易皆為 Human Gate。
> **Base URL 與 Merchant 憑證以正式啟用後正式公布的值為準**；在啟用前請以 UAT 環境先行驗證串接。

## A. Environment

| 項目 | 值 |
| --- | --- |
| Platform environment | `PRODUCTION` |
| Upstream payment environment | `PRODUCTION`（黑貓 PAY，ibon CVS） |
| Base URL | 【正式網址，啟用後公布】（UAT 為 `https://api-v2-dev.nnviopp.com`） |
| Merchant ID (`mchNo`) | 【正式商戶號，secure handoff 交付】 |
| App ID (`appId`) | 【正式應用 ID，secure handoff 交付】 |
| Channel code (`wayCode`) | `RYO_IBON` / `JAY_IBON` / `CHI_IBON` 三選一（黑貓 PAY ibon 上游，契約相同） |
| Currency | `TWD` |

## B. Credential delivery

- Merchant API credential 是 **App Secret**（MD5 簽名用）。真實值不經 Git／Email 明文交付，以 secure handoff 提供；收到後放入 secret manager 或受限環境變數，不可寫入 source、log、ticket 或群組訊息。
- App Secret **大小寫敏感**（簽名以原值參與）。
- 黑貓 PAY 的 `custId`、API password、Token 等 upstream credential 屬我方，不會也不應提供給外部系統商。

## C. Authentication and signing

所有 Create／Query request 都以同一個 App Secret 驗簽：

1. request 必須包含 `version=1.0`、`signType=MD5`、`reqTime`、`mchNo`、`appId` 與 `sign`。
2. 移除 `sign`；排除值為 `null` 或空字串 `""` 的欄位，數字 `0` 仍參與。
3. 依欄位名稱做 case-insensitive 升冪排序。
4. 每個欄位串為 `key=value&`，最後直接附加 `key=<AppSecret>`。
5. 對完整 UTF-8 bytes 計算 MD5，輸出 32 字元 uppercase hexadecimal。

`channelExtra` 是 JSON **字串**，其字元、空白與欄位順序會直接影響簽名；建議先產生 compact JSON 字串，再組 request 與簽名。簽名值可用大小寫任一形式送出，但範例固定 uppercase。

`reqTime` 是必填且參與簽名的字串，值為目前 Unix epoch milliseconds。Runtime 對 reqTime 有 **5 分鐘 freshness 窗口**：與系統時間偏差超過 5 分鐘會回「請求時間戳已過期」；非數字 reqTime 不檢查 freshness，但仍參與簽名。不存在 nonce 欄位。所有呼叫必須使用 HTTPS，且不可記錄含 App Secret 的 canonical string。

## D. Create order

```text
METHOD       = POST
URL          = <正式 Base URL>/api/pay/unifiedOrder
CONTENT-TYPE = application/json; charset=UTF-8
```

| 欄位 | 型別 | 規則 |
| --- | --- | --- |
| `version` | string | 必填，`1.0` |
| `signType` | string | 必填，`MD5` |
| `reqTime` | string | 必填，目前 Unix epoch milliseconds |
| `mchNo` | string | 必填，secure handoff 的 Merchant ID |
| `appId` | string | 必填，secure handoff 的 App ID |
| `mchOrderNo` | string | 必填；同一 Merchant 必須唯一，重複會回「商戶訂單已存在」 |
| `wayCode` | string | 必填，黑貓 PAY ibon 上游擇一：`RYO_IBON` / `JAY_IBON` / `CHI_IBON` |
| `amount` | integer | 必填，JeePay amount units；`1 TWD = 100 JeePay amount units`；三個 `*_IBON` 通道皆要求可整除 100 |
| `currency` | string | 必填，固定 uppercase `TWD` |
| `subject` | string | 必填，商品／訂單標題 |
| `body` | string | 必填，商品／訂單描述 |
| `notifyUrl` | string | 必填；外部可接收的 HTTPS callback URL |
| `returnUrl` | string | 選填；direct API flow 不依賴此欄位 |
| `clientIp` | string | 選填；省略時使用 ingress 傳入的 caller IP |
| `expiredTime` | integer | 選填，從建立時間起算的秒數；省略時 native default 為 2 小時 |
| `channelExtra` | JSON string | 必填，內容見下表 |
| `extParam` | string | 選填；原樣帶入 Query／Notify |
| `divisionMode` | integer | 本產品不使用，請省略 |
| `sign` | string | 必填，依上節計算 |

`amount` 的 JSON type 必須是 integer，金額換算是 exact integer conversion：`1000 = TWD10`、`4000 = TWD40`。因此 `amount=4000` 不是 TWD4000，而是 TWD40。

`channelExtra` 必須是字串化 JSON，且包含：

| 欄位 | 規則 |
| --- | --- |
| `payerName` | 必填，繳款人姓名資料；不代表已做實名/KYC 驗證 |
| `payerPostcode` | 必填 |
| `payerAddress` | 必填 |
| `payerMobile` | 必填 |
| `payerEmail` | 必填 |

### Create response

成功的 native envelope：

```json
{
  "code": 0,
  "msg": "SUCCESS",
  "data": {
    "payOrderId": "P...",
    "mchOrderNo": "...",
    "orderState": 1,
    "payDataType": "ryoIbon",
    "payData": "{\"ibonShopId\":\"...\",\"ibonCode\":\"...\",\"paymentCode\":\"...\",\"expireDate\":\"YYYY-MM-DD\",\"billAmount\":40,\"shortUrl\":\"https://...\"}"
  },
  "sign": "..."
}
```

`payData` 是 JSON 字串，需再 parse 一次；不得把它當作 nested JSON object。第一次 parse 取得 UnifiedOrder response，第二次 parse `data.payData` 才取得付款指示。欄位：`ibonShopId`、`ibonCode`、組合後的 `paymentCode`、`expireDate`、whole-TWD `billAmount`、可選 `shortUrl`。程式應容許 provider 未回 `shortUrl` 時只使用付款碼。`payDataType` 依通道為 `ryoIbon` / `jayIbon` / `chiIbon`。

response envelope 的 `sign` 是只針對 `data` object、用同一 App Secret 與同一 canonicalization 計算，不包含 `code`／`msg`。

### WAITING is not paid

`code=0` 與 `orderState=1` 代表 Create 成功、付款資訊已建立，**不代表付款成功**。Native state：`0=INIT`、`1=ING/WAITING`、`2=SUCCESS`、`3=FAIL`、`4=REVOKED`、`5=REFUND`、`6=CLOSED`。只能在 Merchant Notify 或 Query 確認 `state=2` 後上分。

成功 WAITING response 有一項 invariant：`code=0`、`orderState=1` 時，`payData` 必須是 populated JSON string，且第二次 parse 後必須包含可解析、可實際使用的 payment instruction。若收到違反此 invariant 的 response（尤其 `payData={}` 或沒有可使用的 payment instruction），Merchant 不得將它視為正常可付款訂單或支付成功、不得上分，也不得 blind retry 同一業務交易。請保留原 `mchOrderNo`，依 Query／error contract 處理。

## E. Query order

```text
METHOD       = POST
URL          = <正式 Base URL>/api/pay/query
CONTENT-TYPE = application/json; charset=UTF-8
```

Auth 欄位與 Create 相同。另傳 `payOrderId` 或 `mchOrderNo` 至少一個；建議只傳一個，若兩者同時存在，native implementation 優先使用 `payOrderId`。Query 只讀 JeePay local PayOrder，不會為每次 Merchant Query 同步呼叫黑貓 PAY。

WAITING response 的 `data.state=1`；SUCCESS response 的 `data.state=2`。`data` 的 actual 欄位為 `payOrderId`、`mchNo`、`appId`、`mchOrderNo`、`ifCode`、`wayCode`、`amount`、`currency`、`state`、`clientIp`、`subject`、`body`、`channelOrderNo`、`errCode`、`errMsg`、`extParam`、`successTime`、`createdAt`；值為 null 的欄位可能不出現。Envelope `sign` 同樣只簽 `data`。

## F. Merchant Notify

外部只需實作 `JeePay → Merchant` callback；`黑貓 PAY → JeePay` APN 是我方內部責任，請勿設定或處理。

```text
METHOD       = POST
CONTENT-TYPE = application/x-www-form-urlencoded
SOURCE IP    = 【正式 outbound IP，啟用後公布】
ACK BODY     = SUCCESS
```

Create 的 `notifyUrl` 必須是 HTTPS。JeePay 將 Query response 的非 null 訂單欄位，加上 `reqTime` 與 `sign`，以 form fields POST 到該 URL。驗簽時移除 `sign`，再套用與 request 完全相同的 App Secret canonicalization。

只有 terminal state 會建立通知。上分只接受 `state=2`；入帳金額欄位是 `amount`，單位仍為 minor units。`billAmount` 是付款指示中的 whole-TWD payer amount，不是 Merchant 上分欄位。

Merchant 必須以 `payOrderId`（並可交叉比對 `mchOrderNo`）做 idempotency。JeePay 對同一 order 只建立一筆 logical Notify record；若 callback response body 不是 case-insensitive exact `SUCCESS` 或連線失敗，最多送 6 次，首次立即，後續以 30、60、90、120、150 秒 delay 重新排送。HTTP 2xx 但 body 不是 `SUCCESS` 仍視為失敗。

Merchant 應回 `HTTP 200`、`Content-Type: text/plain`、body `SUCCESS`（大小寫不敏感，但不可含額外空白或其他內容）。

## G. Error handling

- API envelope：`code=0` 才是 API success；常見 business failure 為 `code=9999` 並在 `msg` 帶原因。
- Invalid／modified signature、wrong Merchant/App、malformed payload 都不得重送為新 `mchOrderNo` 來碰運氣，先修正 request。
- Duplicate `mchOrderNo`：改用 Query 查既有 order；不要換新編號重送同一業務交易。
- Create timeout／connection ambiguity：先 Query 原 `mchOrderNo`，不要直接換 key 重送。
- Create success 後持續 WAITING：讓使用者付款，並以 Notify／Query 判定；不可先上分。

## H. 串接流程

1. 由 secure handoff 取得 Merchant ID、App ID、App Secret（正式網址啟用後）。
2. 提供正式 HTTPS Merchant Notify callback URL。
3. 依 canonicalization 簽 Create request。
4. 呼叫正式 Base URL；JeePay 建立 native PayOrder。
5. JeePay 以 `RYO_IBON`／`JAY_IBON`／`CHI_IBON` 路由並同步回傳 ibon 付款資訊。
6. 付款人依訂單金額完成真實付款。
7. 黑貓 PAY 通知 JeePay；JeePay 將 native PayOrder 轉為 SUCCESS。
8. JeePay 對 Merchant callback URL 發送 Merchant Notify。
9. Merchant 驗簽、確認 `state=2`／`amount`／order identity、冪等上分，回純文字 `SUCCESS`。
10. Merchant 呼叫 Query，確認同一 order 為 `state=2`。

外部系統商負責保存 Merchant credential、Create、Query、callback URL、Notify 驗簽／ACK、冪等與配合付款。我方負責 JeePay V2、Provider credential/APN、reconciliation、Merchant config/credential、Notify/retry、DNS/TLS、infrastructure 與 logs。

## I. Product answers

| 問題 | 回答 |
| --- | --- |
| 代收 | 支援，`RYO_IBON` / `JAY_IBON` / `CHI_IBON` |
| 退款 | 不支援（Provider Phase 1 non-goal） |
| 通道 | ibon CVS 繳費碼（`paymentCode` = `ibonShopId` + `ibonCode`）；可選 `shortUrl` |
| 查詢 | 只讀 JeePay local PayOrder（`/api/pay/query`），不會同步打黑貓 PAY |
| Notify | 純文字 `SUCCESS` ACK；最多 6 次重試；以 `payOrderId` 冪等 |

## 工具

- UAT 的 Talend 產生器可換成正式環境變數重複使用：
  `docs/integration/merchant-uat/examples/talend-request-gen.py`（`--way-code RYO_IBON|JAY_IBON|CHI_IBON`）。
- 簽名向量與驗證：`docs/integration/merchant-uat/examples/verify_vectors.py`（演算法驗證用；正式值請以正式憑證重算）。
