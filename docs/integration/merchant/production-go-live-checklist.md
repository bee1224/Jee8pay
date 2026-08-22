# 正式環境啟用檢查清單（Go-live Checklist）

> **STATUS（2026-08-23）**：本清單記錄 V2 Production（`jee8pay-v2-production`）正式啟用所需的人工動作。
> 平台技術面（V1 退役、edge 接管、Create/Query/Provider Query/APN 入口）已完成並驗證；
> 以下項目為 operator / 業務層面的 Human Gate，完成後即可正式對外。

## 0. 平台現況（已完成，僅供對照）

| 項目 | 狀態 |
| --- | --- |
| V1（`payment-service`） | 已退役：0 容器、networks 移除、DB 封存、DNS NXDOMAIN |
| edge | `jee8pay-v2-production-edge`（V2 compose 管理） |
| 對外 hostnames | `admin-v2` / `api-v2` / `ccat-v2.lp33ing.com` |
| Provider credentials | ryo / jay / chi 已綁定（`t_pay_interface_config`，PRODUCTION） |
| pilot | `P2091285666526339074`（RYO TWD40）Create 出單成功 |

## 1. 正式 Merchant 串接前

- [ ] **提供 merchant `notifyUrl`**：正式 Create 必須在 request 提供**獨立**的 merchant 異步通知 URL（不能與 Provider APN URL 共用）。
  - Provider APN URL（我方）：`https://ccat-v2.lp33ing.com/api/pay/notify/{ryo|jay|chi}`（由 `paySiteUrl` 自動建構，Merchant 不需提供）
  - Merchant Notify URL（Merchant 提供）：例如 `https://merchant.example.com/callback/jeepay`
  - ⚠️ 若未提供獨立 notifyUrl，JeePay 會把 `payOrder.notifyUrl` 存為 Provider APN URL，付款後 Merchant Notify 會打到 APN 路徑被拒
- [ ] **確認 api-v2 白名單**：系統商建單/查單的 source IP 需加入 allowlist
  - 現有：`34.92.245.74`、`34.92.52.162`（Talend 保留）、`2001:b011:8000:3ca1:d5d1:7535:2d23:2ff2`（專案負責人）、`18.166.134.117`（系統商）
  - 新增方式：營運平台 → 系統管理 → UAT Edge 白名單（dev 環境；prod 白名單自助管理為另立任務，見 `docs/operations/platform-access.md`）
  - 或直接編輯 `/opt/jee8pay-v2-production/edge-allowlist/allowlist.json`（會 1 分鐘內自動套用）

## 2. 正式 Create 驗證（每個 wayCode 至少一次）

```bash
# 從白名單 IP 執行（或內部端點測試）：
# POST https://api-v2.lp33ing.com/api/pay/unifiedOrder
# 簽名規則與欄位見 docs/integration/merchant/README.md
# 預期：code=0 SUCCESS，回傳 payData（ibonShopId / ibonCode / paymentCode / expireDate / billAmount）
```

- [ ] RYO_IBON Create → 出單
- [ ] JAY_IBON Create → 出單
- [ ] CHI_IBON Create → 出單

## 3. 真實付款 + APN 驗證

- [ ] 到 ibon 完成繳款（pilot 參考單：`CCAT624203770661`，TWD40，expire 2026-08-30）
- [ ] 驗證 APN 到達：`ccat-v2.lp33ing.com/api/pay/notify/{wayCode}` → 對應 Provider ChannelNoticeService
- [ ] 驗證訂單狀態：`t_pay_order.state` 2（SUCCESS）、`channel_order_no` 有值
- [ ] 驗證 Merchant Notify：`t_mch_notify_record` 建立、MQ 投遞成功、Merchant 回 `SUCCESS`

## 4. 營運驗證（debt D1-D4）

- [ ] host reboot 後 V2 12 容器自動復原（`restart=unless-stopped`）與 edge 路由恢復
- [ ] RocketMQ memory 上限觀察（compose hard limit 已設）
- [ ] 每日 reconciliation：APN 遺失/Query 失敗的滯留訂單稽核（C5 待辦）

## 5. 業務決策（deferred）

- [ ] NewebPay：是否在 V2 實作（V1 曾 Sandbox verified；V2 目前 deferred）
- [ ] 代付（payout）/ 結算模型：V2 目前 fail-closed（`無此轉帳通道介面`），需業務需求才實作

## 6. 驗證工具

- 黑箱測試：`docs/integration/merchant-uat/examples/run-d01-blackbox.py`（設定 `UAT_*` env；正式環境用 `UAT_INTERNAL_BASE_URL=http://127.0.0.1:29216`）
- 人工 Query：`/api/pay/query`（見 `docs/integration/merchant/README.md` 簽名規則）
- 正式 pilot 腳本：`var/prod-ryo-pilot-create.py`（讀 db secret 不輸出；僅供運維參考，不 commit）
