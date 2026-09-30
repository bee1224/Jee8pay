# ADR-0009 — 多層代理租戶與四層手續費瀑布資料模型

Status: Proposed
Date: 2026-09-30

## Context

Jee8pay 的產品方向，是讓平台（Jee8pay 自身組織）把系統出租給高級代理。高級代理再經營一般代理與商戶。

每筆代收訂單的商戶手續費，要拆成四層：平臺費、渠道費、代理費、高代費。每層各有「百分比＋固定金額」。平臺費只能由平台設定，任何租戶層級都不得修改。

現有 JeePay 模型無法表達這些需求：
- `IsvInfo` 只是扁平的組織標籤，沒有上下級關係。
- `MchPayPassage.rate` 是單一扁平費率，也沒有「由誰設定、誰可修改」的資訊。

目前管理端只有佔位畫面，以權限碼 `ENT_ISV_TIER_CONFIG`、`ENT_MCH_PAY_ROUTING_CONFIG` 控制顯示，尚未建立任何資料表或邏輯。

## Decision（提案）

1. **新增 `t_tenant`**，欄位包含 `tenant_type`（`PLATFORM` / `SR_AGENT` / `AGENT` / `MCH`）、`parent_id`，以及物化路徑 `tenant_path`（例如 `/1/5/12/`）。「查詢我管轄範圍」以前綴比對完成，不需要遞迴。
2. 既有 `IsvInfo` / `MchInfo` 改為掛在 `t_tenant` 節點下，不另建第二套商戶主檔。
3. 推薦人（中人）關係獨立存於 `t_merchant_referrer`，不寫入 `tenant_path`。這樣更換管轄時不影響推薦佣金。
4. **新增 `t_fee_rule`**，欄位包含 `tenant_id`、`channel`、`layer`、`rate_pct`、`rate_flat`、`min_amt`、`max_amt`、`locked`。`PLATFORM` 層必須 `locked`，並在 API 與 UI 兩端同時阻擋修改，不能只靠隱藏欄位。
5. 訂單建立時，把當下解析出的四層費率快照寫進訂單，例如 `PayOrder` 的 `fee_snapshot`。之後調整費率不得影響已建立的訂單。
6. 費率變更要保留變更歷史，並經雙人覆核。寫入時拒絕負值，也拒絕四層加總超過商戶手續費的設定。

路由規則（金額區間、權重、時段、餘額門檻）以旁表 `t_routing_rule` 掛在通道上。錢包與提現（複式記帳、提現狀態機）另立 ADR，不在本提案範圍。

## Decision Drivers

- 平台與租戶之間的組織邊界，必須落實在資料權限，特別是平臺費的鎖定。
- 分潤與報表都需要可追溯的上下級鏈路。
- 歷史訂單的結算不能被日後的費率調整影響。
- 延續 ADR-0001：沿用 JeePay 核心，不另建平行平台。

## Options Considered

### Option A — 新增 `t_tenant` 物化路徑樹＋`t_fee_rule`（提案）

查詢管轄範圍很便宜，層級深度也有彈性，費率可逐層設定與鎖定。代價是需要重新檢視所有「依商戶查歸屬」的查詢與報表。

### Option B — 在 `IsvInfo` 加上 `parent_isv_no`，在 `MchPayPassage` 增加四組費率欄位

改動最小，但只能表達固定層數。查詢整個轄區時需要遞迴。費率鎖定與變更歷史也無處安放。

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
