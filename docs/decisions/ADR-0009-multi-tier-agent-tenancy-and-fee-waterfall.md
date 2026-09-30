# ADR-0009 — 多層代理租戶與四層手續費瀑布資料模型

Status: Accepted（2026-09-30 修訂：代理採獨立實體，分階段落地）
Date: 2026-09-30

## Context

Jee8pay 的產品方向，是讓平台（Jee8pay 自身組織）把系統出租給高級代理。高級代理再經營一般代理與商戶。

每筆代收訂單的商戶手續費，要拆成四層：平臺費、渠道費、代理費、高代費。每層各有「百分比＋固定金額」。平臺費只能由平台設定，任何租戶層級都不得修改。

現有 JeePay 模型無法表達這些需求：
- `IsvInfo` 只是扁平的組織標籤，沒有上下級關係。
- `MchPayPassage.rate` 是單一扁平費率，也沒有「由誰設定、誰可修改」的資訊。

目前管理端只有佔位畫面，以權限碼 `ENT_ISV_TIER_CONFIG`、`ENT_MCH_PAY_ROUTING_CONFIG` 控制顯示，尚未建立任何資料表或邏輯。

## Decision

2026-09-30 使用者決定「照競品做，代理用獨立實體」。原提案的通用 `t_tenant` 樹改為以下模型：

1. **代理是獨立實體 `t_agent_info`**，不與 `IsvInfo`（服務商）或 `MchInfo` 合併。
   - `agent_level`：1 = 高級代理、2 = 一般代理。一般代理的上級必須是啟用中的高級代理。
   - `agent_path` 物化路徑（例如 `/A1/A2/`）。層級與上級建立後不可變更，避免路徑失真。
   - 仍有下級代理、綁定商戶或推薦關係時不可刪除。
2. **商戶歸屬 `t_agent_mch_rela`**（一商戶一列）：`agent_no` 為直屬代理，`referrer_agent_no` 為推薦人（中人）。推薦關係獨立記錄，不寫入路徑，更換直屬代理不影響推薦。
3. **費率規則 `t_fee_rule`**：`(way_code, target_type, target_id, layer)` 唯一。
   - `target_type`：`DEFAULT`（平台預設）、`AGENT`、`MCH`（商戶覆寫）。
   - `layer`：`PLATFORM`、`CHANNEL`、`SR_AGENT`、`AGENT`。
   - 每層費用 = 金額 × `rate`（比率，最多 6 位小數，四捨五入到分）＋ `fixed_amount`（分）。
   - 解析順序：商戶覆寫優先。平臺費與渠道費退回平台預設；高代費取高級代理的設定；代理費取直屬一般代理的設定。
   - `DEFAULT` 只能設平臺費與渠道費，`AGENT` 只能設代理層，且層級必須與代理等級相符。
4. **平臺費鎖定**：平臺費與渠道費需要權限 `ENT_FEE_RULE_PLATFORM_EDIT`，代理層需要 `ENT_FEE_RULE_EDIT`。後端 API 強制檢查，前端只是呈現。
5. **變更紀錄 `t_fee_rule_log`**：每次儲存或刪除都記錄前後值與操作人。
6. 移除佔位權限 `ENT_ISV_TIER_CONFIG`，改由「代理管理」與「費率瀑布」選單取代。

### 分階段

| 階段 | 範圍 | 狀態 |
|------|------|------|
| 1 | 代理實體、層級、商戶綁定、四層費率設定、試算、變更紀錄、營運平台管理畫面 | 已完成，已部署至測試環境 |
| 2 | 訂單建立時寫入費率快照（觸及 PayOrder，屬 RED 範圍，需另行核准） | 未開始 |
| 3 | 代理登入後台（依 `agent_path` 做資料權限） | 未開始 |
| 4 | 費率範本與批次設定、雙人覆核 | 未開始 |

原提案中的 `min_amt` / `max_amt`、雙人覆核、「四層加總不得超過商戶手續費」寫入檢查，延後到第 2 與第 4 階段。第 1 階段試算時會標示總費用是否超過交易金額。路由規則與錢包提現仍不在本 ADR 範圍。

### 原提案（保留供追溯）

原提案為通用 `t_tenant`（`tenant_type` / `parent_id` / `tenant_path`），並把 `IsvInfo` / `MchInfo` 掛在節點下。已改採上述獨立代理實體，原因如下：
- 競品皆以代理為獨立實體。
- 不需要改動既有服務商與商戶主檔。
- 與 AGENTS.md「最小核心改動」一致。

## Decision Drivers

- 平台與租戶之間的組織邊界，必須落實在資料權限，特別是平臺費的鎖定。
- 分潤與報表都需要可追溯的上下級鏈路。
- 歷史訂單的結算不能被日後的費率調整影響。
- 延續 ADR-0001：沿用 JeePay 核心，不另建平行平台。

## Options Considered

### Option A — 新增 `t_tenant` 物化路徑樹＋`t_fee_rule`（原提案，未採用）

查詢管轄範圍很便宜，層級深度也有彈性，費率可逐層設定與鎖定。代價是需要重新檢視所有「依商戶查歸屬」的查詢與報表。

### Option B — 在 `IsvInfo` 加上 `parent_isv_no`，在 `MchPayPassage` 增加四組費率欄位

改動最小，但只能表達固定層數。查詢整個轄區時需要遞迴。費率鎖定與變更歷史也無處安放。

### Option D — 獨立代理實體 `t_agent_info`（物化路徑）＋`t_agent_mch_rela`＋`t_fee_rule`（採用）

貼近競品做法。既有 `IsvInfo`、`MchInfo`、`PayOrder` 在第 1 階段完全不動。代價是日後若要支援更多層級，需要調整 `agent_level` 與驗證規則。

### Option C — 以鄰接表（只有 `parent_id`）配合遞迴 CTE

結構簡單，但每次查詢轄區都要遞迴，報表成本高。節點搬移時雖然比物化路徑容易，但查詢頻率遠高於搬移頻率。

## Consequences

### Positive

- 平臺費鎖定、分潤鏈、轄區資料權限，都有明確的資料基礎。
- 靠訂單費率快照，歷史結算不會被事後調整影響。

### Negative / Trade-offs

- 屬於資料模型與交易語意的重大變更：影響 PayOrder、MchPayPassage 與報表，屬 RED/YELLOW 範圍，需要完整的回歸測試。
- `tenant_path` 寫錯或節點搬遷，會連動下游費率與資料權限。上線前需要先做只讀校驗與灰度。
- 既有 `rate` 欄位必須有明確的遷移與相容策略。

## Supersedes

None

## Superseded By

None

## Related Documents

- [`ADR-0001-jeepay-as-platform-core.md`](ADR-0001-jeepay-as-platform-core.md)
- [`ADR-0008-remove-legacy-china-providers.md`](ADR-0008-remove-legacy-china-providers.md)
- 競品拆解與工程規格（PRD，外部連結）：https://claude.ai/artifact/WAyCSap3iibt1fuj4D2vz2
