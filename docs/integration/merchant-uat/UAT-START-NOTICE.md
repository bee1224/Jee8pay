# JeePay V2 External UAT — 啟動前注意事項（UAT-START-NOTICE）

日期：2026-08-16
狀態：外部 Merchant UAT 啟動前 checklist；外部系統商開始測試前，我方與外部都應逐項確認。

---

## 1. 環境與識別

| 項目 | 值 |
| --- | --- |
| UAT Base URL | `https://api-v2-dev.nnviopp.com` |
| Merchant ID | `M_D01_EXTERNAL_UAT` |
| App ID | `APP_D01_EXTERNAL_UAT` |
| wayCode | `CCAT_IBON` |
| 平台環境 / Provider 環境 | DEVELOPMENT / **PRODUCTION（真實 ibon 帳單）** |
| Allowlist 來源 IP | `34.92.245.74`、`34.92.52.162`（**只能從這兩個 IP 發送**，其他 IP 回 403）；edge 為 Cloudflare proxied 模式（CF-Connecting-IP 信任） |
| 金額單位 | minor units：`4000` = TWD 40；必須被 100 整除 |

## 2. 精確錯誤訊息表（黑箱實測，測試預期以此為準）

| 場景 | code | 精確 msg |
| --- | --- | --- |
| 竄改簽名 | 9999 | `簽章驗證失敗` |
| Query 缺 payOrderId/mchOrderNo | 9999 | `mchOrderNo 和 payOrderId 不能同時為空` |
| 重複建單（同 mchNo+mchOrderNo） | 9999 | `商戶訂單[<mchOrderNo>]已存在` |
| 金額不可被 100 整除 | 9999 | `金額必須為整數 TWD 元` |
| 非 TWD 幣別 | 9999 | `CCAT ibon 僅支援 TWD` |
| channelExtra 非 JSON | 9999 | `CCAT channelExtra 格式錯誤` |
| channelExtra 缺 payer 欄位 | 9999 | `CCAT channelExtra 缺少繳款人資料` |
| 不支援的 wayCode | 9999 | `商戶應用不支援該支付方式` |
| 缺 subject | 9999 | `商品標題不能為空` |
| 代付（未開放） | 9999 | `無此轉帳通道介面` |
| 退款-訂單不存在 | 9999 | `退款訂單不存在` |
| 退款-訂單未支付 | 9999 | `訂單狀態不正確，無法完成退款` |

## 3. 必須遵守的行為（避免測試卡住）

1. **notifyUrl 必須用外部系統商自己的接收端**；`notifyUrl` 為每筆訂單可指定。若複製範例中的 `https://merchant.example.test/...` 假網域，通知會送不出去並在重試後標記 FAIL。我方測試用 receiver（`http://merchant:9218/api/anon/paytestNotify/...`）是內部測試端，**不是**外部應使用的 URL。
2. **保存 Create response**：ibon 繳費碼（`paymentCode` = `ibonShopId+ibonCode`）只在 Create 回傳中提供，**Query 不會再回傳 payData**。弄丟只能靠 CCAT 端依 `cust_order_no` 找回。
3. **Create 逾時/不明回應 → 用「同一個 mchOrderNo」Query-first**，不要用新 mchOrderNo 重試（會產生第二張真實帳單）。我方 adapter 對 ambiguous Create 只做一次 Query 恢復、絕不自動二次 Append。
4. **真人付款是 UAT 完成付款迴圈的必經步驟**：訂單成功後需在 `expireDate` 前到 7-11 ibon 付款，付款後 CCAT 會發 APN、我方驗證後轉 SUCCESS 並發 Merchant Notify。請事先安排「誰付款、何時付款、付幾筆」。
5. **Merchant Notify 驗簽**：與 Create/Query 相同演算法（App Secret MD5）；收到後回純文字 `SUCCESS`（大小寫不拘）；我方最多 6 次、間隔 0/30/60/90/120/150 秒。
6. **不要對 `state=1`（WAITING）訂單上分**；只在 Notify 或 Query 確認 `state=2` 後入帳。
7. **receiver 需能處理 `state=3`（FAIL）通知**：訂單到期會轉 FAIL（見第 4 節），屆時可能收到失敗通知。
8. **reqTime**：必填且參與簽名；**系統有 freshness 檢查（5 分鐘視窗）**，超出窗口回 `9999 請求時間戳已過期`。請確保系統時鐘已 NTP 同步。

## 4. 已知行為（本輪實測新增的確定事實）

- **訂單到期 → 本地狀態轉為 `CLOSED(6)`**，由 `PayOrderExpiredTask`（每分鐘，`jeepay-payment/.../task/PayOrderExpiredTask.java`）執行 `updateOrderExpired()`（`PayOrderService.java:191-200`）純本地 DB 更新：**不打 CCAT、不發 Merchant Notify**。已有 18 筆 live 實例（08-13，2 小時到期）證明：`state=6`、`err_code=NULL`、`notify_state=0`。
- **reissue（補單）只查 `STATE_ING` 訂單**（`PayOrderReissueTask.java:55`）→ 訂單被關閉（CLOSED）後**不會再向 CCAT 補查**。
- **競態風險（NEW FINDING）**：若付款人在本地關閉後才到 7-11 付款 → CCAT 顯示已付款（process_code=4）+ APN status=B → `CcatChannelNoticeService.doNotice` 因本地狀態為 CLOSED（不在 ING/SUCCESS/FAIL）而**拒絕** → 本地不轉 SUCCESS、不發通知、商戶不入帳，但付款人已付錢 → **滯留款項需人工對帳**。此為 fail-closed 安全行為，但存在真實營運風險；若要允許「經完整驗證的 paid-APN 將 CLOSED 轉回 SUCCESS」，屬 PayOrder state machine 變更（RED 邊界），需另立任務與 ADR 決策，**不在本文件範圍內自動修復**。
- **到期/關閉不會通知商戶**（上述 18 筆 notify_state=0）：外部商戶需自行 Query 才能發現訂單到期。
- **代付/退款端點在無台灣 adapter 時 fail-closed**：`code=9999` 乾淨 JSON，不會 500（transfer→`無此轉帳通道介面`、refund→`退款訂單不存在`/`訂單狀態不正確，無法完成退款`）。
- **UAT 期間 freeze**：不要重啟 payment/merchant、不要套用 Cloudflare proxy-mode 變更（未 commit 的 `manage-sandbox-*-edge` / proxy 工作）。

## 5. 我方監控計畫（見 `deploy/jee8pay-v2-dev/scripts/monitor-uat.sh`）

- 定期（建議每 30-60 分鐘）快照：D01 商戶訂單數與狀態、notify record 狀態、payment log 中 CCAT 事件。
- 專注觀測物件：
  - `P2088706121058914305`（P05 live smoke，TWD 40，已授權付款）
  - `P2088706123088957441`（到期驗證，expire_date=2026-08-17，**不要付款**）
  - `P2088297924112322562`（D01 既有訂單，到期日 ~08-22）
- 任一 FAIL notify 或 CCAT 異常事件 → 回報。

## 6. 本次啟動前已執行之驗證（2026-08-16）

- [x] 黑箱 suite 9 負向場景精確 msg 全 PASS（0 provider call）
- [x] 代付/退款端點 fail-closed 實測（C1/C2/C3）
- [x] P05 live smoke 訂單已建立（等待真人付款 → APN → notify 驗證）
- [x] 到期驗證訂單已建立（等待 08-17 後觀測轉態與通知行為）
- [x] B 手動通知重發已部署並端到端驗證（commit 02b82b0）
