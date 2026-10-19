# ADR-0013 — 商戶 API 簽名改用 ML-DSA-65，移除 MD5

Status: Accepted
Date: 2026-10-19

> 使用者 2026-10-19 決定：MD5 一律移除；只用 ML-DSA-65（不並存 RSA）；後端升到 JDK 25 + Spring Boot 3.5，使用 JDK 內建的 ML-DSA，不另加 Bouncy Castle。範圍先限測試環境（`test-env-overhaul`），不合併 main、不動正式環境。

## Context

商戶 API（下單、查單、關單，以及沿用同一套驗簽的退款、轉帳、分帳端點）原本沿用 JeePay 的 MD5 共用密鑰簽名：

- `ApiController.getRQByWithMchSign` 以 `JeepayKit.getSign(params, appSecret)` 驗簽，`signType` 欄位只是標記，不參與選擇演算法。
- 平台回給商戶的同步回應（`ApiRes.okWithSign`）與非同步通知（`PayMchNotifyService.createNotifyUrl`、管理端的人工補發）也用同一把 `t_mch_app.app_secret` 簽。
- 平台自己呼叫支付服務時（後台退款、團長的渠道測試）透過 `jeepay-sdk-java` 的 `JeepayClient`，同樣拿商戶的 `app_secret` 簽。

這個做法的問題：MD5 已不具抗碰撞性；共用密鑰代表平台與商戶持有同一把鑰匙，平台資料外洩即可冒充任一商戶，商戶也無法證明請求不是平台偽造；`reqTime` 非數字時不檢查時效。使用者另要求這次直接採用抗量子簽名。

## Decision

1. **演算法**：商戶 API 的請求、回應與通知一律使用 ML-DSA-65（FIPS 204，pure 模式，空 context）。`signType` 固定為 `ML-DSA-65`，其他值（含 `MD5`）一律拒絕。MD5 簽名程式碼與 `t_mch_app.app_secret` 欄位移除。
2. **待簽字串**：除 `sign` 外、值不為 null 或空字串的所有參數，依參數名稱的 ASCII 由小到大排序，組成 `k1=v1&k2=v2`（無結尾 `&`），以 UTF-8 編碼後簽名。`sign` 為標準 Base64。
3. **金鑰**：
   - 商戶自行產生 ML-DSA-65 金鑰對，只把公鑰（X.509 SubjectPublicKeyInfo，PEM 或 Base64）交給平台，存於 `t_mch_app.mch_public_key`；儲存時驗證必須是 ML-DSA-65 公鑰。
   - **平台簽名金鑰**：平台回應與通知商戶用。私鑰只放在伺服器 secrets 目錄並掛進需要簽名的容器，不進 Git、不進資料庫；公鑰放在系統配置，供後台顯示給商戶。
   - **平台內部金鑰**：後台（manager、merchant 服務）代商戶呼叫支付服務時用，`signType` 為 `PLATFORM-ML-DSA-65`，支付服務以內部公鑰驗證。與平台簽名金鑰分開，避免商戶拿到的已簽通知被當成內部請求重放。
4. **時效**：`reqTime` 改為必填的毫秒時間戳，與系統時間相差超過 5 分鐘即拒絕。
5. **上游不受影響**：黑貓 PAY（RYO、JAY、CHI、JHD）的 MD5 檢查碼是上游規定的協定，維持原樣；本決策只涵蓋商戶面向的 API。
6. **執行環境**：後端升到 JDK 25（Spring Boot 3.5），使用 JDK 內建的 `ML-DSA` 簽名與金鑰實作。

## Decision Drivers

- 使用者要求移除 MD5 並直接採用抗量子簽名。
- 非對稱簽名讓平台與商戶各自持有私鑰，任一方資料外洩不會讓對方被冒充。
- JDK 24 起內建 ML-DSA（JEP 497），JDK 25 是長期支援版本，不需要第三方加密套件。

## Options Considered

### Option A：RSA-3072（SHA256withRSA）

相容性最好，商戶各種語言都能直接使用；但不抗量子，日後仍要再換一次。未採用。

### Option B：RSA 與 ML-DSA 並存，商戶自選

過渡最平順；但要長期維護兩套演算法與兩種金鑰，且 RSA 一側仍不抗量子。使用者選擇不並存。

### Option C：ML-DSA-65，JDK 17 加 Bouncy Castle

不需升級 JDK；但多一個加密套件依賴。使用者選擇升級 JDK 使用內建實作。

### Option D（採用）：只用 ML-DSA-65，JDK 25 內建實作

## Consequences

### Positive

- 商戶 API 簽名抗量子；平台不再保存可冒充商戶的密鑰。
- 拒絕未知 `signType` 與過期 `reqTime`，降低重放風險。

### Negative / Trade-offs

- **不相容的變更**：所有已串接的商戶都必須改程式、產生金鑰並上傳公鑰。
- 商戶環境需要支援 ML-DSA，例如 OpenSSL 3.5 以上、JDK 24 以上，或其他實作 FIPS 204 的套件；舊環境無法串接。
- 簽名約 3.3 KB（Base64 約 4.4 KB），公鑰約 1.9 KB；請求與通知變大。通知原本就以 POST 表單送出，`t_mch_notify_record.notify_url` 為 TEXT，可容納。
- `jeepay-sdk-java`（MD5）不能再用來呼叫支付服務，後台改用平台內部金鑰的內建用戶端。
- JDK 25 與 Spring Boot 3.5 升級影響全部後端模組與容器基礎映像，需要完整回歸。
- 平台私鑰以檔案保存在伺服器 secrets 目錄，與既有 secrets 同等級保護；`if_params` 加密債（TD-001）不在本決策範圍。

## Supersedes

None

## Superseded By

None

## Related Documents

- `docs/integration/merchant-uat/README.md`（商戶簽名規則）
- ADR-0005（環境隔離、secrets 不進 Git）
