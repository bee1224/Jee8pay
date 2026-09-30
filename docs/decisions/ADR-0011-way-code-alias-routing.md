# ADR-0011 — 支付方式別名與通道路由

Status: Accepted
Date: 2026-09-30

## Context

RYO、JAY、CHI、JHD 是同一上游（統一客樂得）的四組帳號（ADR-0002）。目前商戶必須在下單時自己指定 `RYO_IBON` 之類的實際代碼：
- 平台想依金額、時段分流，或暫時避開某個帳號時，都得請商戶改串接。
- 競品的 PRD（見 ADR-0009）把「通道代碼轉換＋條件式路由」列為 P0。

限制：
- 不修改 `AbstractPayOrderController` 與 PayOrder 狀態機（RED）。
- 既有以實際代碼下單的商戶行為必須完全不變。

## Decision

1. **別名代碼**
   - 新增 `t_way_route`，把別名（例如 `IBON`）對應到多個實際支付方式。
   - 別名不得與既有 `t_pay_way` 代碼相同，所以不會遮蔽任何實際代碼。
   - 不是別名時，路由服務回傳 null，下單流程完全照舊。
2. **選擇規則**（純函式 `WayRouter`，可單元測試）
   - 商戶專屬規則優先：只要存在任何專屬規則，就不再看通用規則。
   - 硬條件：規則啟用、金額在區間內、在時段內（台北時間，可跨午夜）、商戶應用已開通該實際代碼。
   - 通過硬條件的候選依權重 1–9 加權隨機。
   - 沒有候選時回覆「目前沒有可用的支付通道」。
3. **掛點**：只在 `UnifiedOrderController.buildBizRQ` 把別名換成實際代碼。之後的訂單、快照、費率、結算一律使用實際代碼，現有規則不必為別名另外設定。
4. **決策紀錄**
   - 每次別名下單都寫一筆 `t_way_route_log`，內容是候選、權重與結果。
   - 紀錄寫入失敗不影響下單。
5. 原本支付通道編輯頁的「進階路由規則（規劃中）」佔位與權限 `ENT_MCH_PAY_ROUTING_CONFIG` 移除，改到「支付配置 → 通道路由」統一設定。

## Not in scope

- **失敗自動改走下一個通道**（multi_match 重試）：需要在 `AbstractPayOrderController` 呼叫上游失敗後重新選路，屬 RED，而且有重複建單的風險，暫不做。
- **依餘額門檻路由**：沒有上游帳戶餘額的資料來源。

## Consequences

- 平台可以在不動商戶串接的情況下分流或切換帳號。
- 商戶查單看到的是實際代碼（例如 `RYO_IBON`），不是別名；對接文件需要說明。
- 加權隨機不保證短期內精確符合比例；需要精確配額時要改用加權輪詢。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`ADR-0002-native-provider-extension-contract.md`](ADR-0002-native-provider-extension-contract.md)
- [`ADR-0009-multi-tier-agent-tenancy-and-fee-waterfall.md`](ADR-0009-multi-tier-agent-tenancy-and-fee-waterfall.md)
