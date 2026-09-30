-- 補齊 seed 漂移：以下兩個權限碼於 2026-08 直接寫入測試環境 DB，從未有對應的遷移腳本，init.sql 也缺漏。
-- 權限檢查（TD-015）生效後，缺少權限碼等於連超管都無法使用對應功能，因此以 INSERT IGNORE 冪等補上。
-- 測試環境已存在，執行不會有變動。
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_UAT_EDGE_ALLOWLIST', 'UAT Edge 白名單', 'safety', '/uatedge/allowlist', 'UatEdgeAllowlistPage', 'ML', 0, 1, 'ENT_SYS_CONFIG', '25', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_PAY_ORDER_MANUAL_NOTIFY', '按鈕：人工回調', 'no-icon', '', '', 'PB', 0, 1, 'ENT_PAY_ORDER', '0', 'MGR', now(), now());
