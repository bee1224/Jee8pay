# External Integration

本分類只放外部系統商可使用的 JeePay integration contract；不放 Provider credential、VPS／DB credential 或內部 APN 細節。

## Merchant（正式環境，downstream）

- [`merchant/README.md`](merchant/README.md)：**正式環境 Merchant 串接文件**（Create／Query／Notify 契約，`RYO_IBON`／`JAY_IBON`／`CHI_IBON`；Base URL 與憑證待正式啟用後公布）。

## Merchant UAT（downstream）

- [`merchant-uat/README.md`](merchant-uat/README.md)：nnviopp JeePay V2 Development 的 downstream Merchant UAT package（Create／Query／Notify 契約與範例）。
- [`merchant-uat/UAT-START-NOTICE.md`](merchant-uat/UAT-START-NOTICE.md)：**外部 UAT 啟動前注意事項**（精確錯誤訊息表、行為紅線、真人付款安排、監控計畫）。
- [`merchant-uat/JEE-EC01R1-external-consumer-closure.md`](merchant-uat/JEE-EC01R1-external-consumer-closure.md)：External consumer findings closure。
- [`merchant-uat/examples/`](merchant-uat/examples/)：synthetic 簽名向量與驗證工具（含精確 msg 黑箱 suite）。

> 外部 UAT 期間如需手動重發已付款訂單的 Merchant Notify，使用 Manager 端點 `POST /api/mchNotify/send/{payOrderId}`（權限 `ENT_MCH_NOTIFY_RESEND`；僅終態訂單）。
