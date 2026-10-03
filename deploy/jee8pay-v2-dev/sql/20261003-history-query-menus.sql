-- 測試環境（jee8pay-v2-dev）：以「歷史查詢」取代「下載中心」。
-- 1) 兩個平台新增「歷史查詢」選單：代收查詢（完整篩選＋匯出）、代付查詢（尚未實作的佔位頁）
-- 2) 「下載中心」不再是選單；原權限碼保留為代收查詢底下的按鈕權限，既有角色授權與 API 權限檢查不變
-- 3) 已有「訂單管理」選單的角色自動取得新選單
-- 可重複執行。回滾見檔尾註解。

INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_HISTORY', '歷史查詢', 'history', '', 'RouteView', 'ML', 0, 1, 'ROOT', '52', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_HISTORY_PAY', '代收查詢', 'file-search', '/history/pay', 'HistoryPayPage', 'ML', 0, 1, 'ENT_HISTORY', '10', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_HISTORY_PAYOUT', '代付查詢', 'file-sync', '/history/payout', 'HistoryPayoutPage', 'ML', 0, 1, 'ENT_HISTORY', '20', 'MGR', now(), now());

INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_HISTORY', '歷史查詢', 'history', '', 'RouteView', 'ML', 0, 1, 'ROOT', '25', 'MCH', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_HISTORY_PAY', '代收查詢', 'file-search', '/history/pay', 'HistoryPayPage', 'ML', 0, 1, 'ENT_HISTORY', '10', 'MCH', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_HISTORY_PAYOUT', '代付查詢', 'file-sync', '/history/payout', 'HistoryPayoutPage', 'ML', 0, 1, 'ENT_HISTORY', '20', 'MCH', now(), now());

UPDATE t_sys_entitlement SET ent_name = '按鈕：匯出與下載', menu_icon = 'no-icon', menu_uri = '', component_name = '', ent_type = 'PB', pid = 'ENT_HISTORY_PAY', ent_sort = '0', updated_at = now()
 WHERE (ent_id = 'ENT_EXPORT_CENTER' AND sys_type = 'MGR') OR (ent_id = 'ENT_MCH_EXPORT_CENTER' AND sys_type = 'MCH');

INSERT IGNORE INTO t_sys_role_ent_rela (role_id, ent_id)
SELECT r.role_id, n.ent_id FROM t_sys_role_ent_rela r
  JOIN (SELECT 'ENT_HISTORY' AS ent_id UNION ALL SELECT 'ENT_HISTORY_PAY' UNION ALL SELECT 'ENT_HISTORY_PAYOUT') n
 WHERE r.ent_id = 'ENT_PAY_ORDER';

SELECT sys_type, ent_id, ent_name, ent_type, pid FROM t_sys_entitlement WHERE ent_id LIKE 'ENT_HISTORY%' OR ent_id LIKE '%EXPORT_CENTER' ORDER BY sys_type, ent_id;

-- 回滾：
-- DELETE FROM t_sys_role_ent_rela WHERE ent_id IN ('ENT_HISTORY','ENT_HISTORY_PAY','ENT_HISTORY_PAYOUT');
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_HISTORY','ENT_HISTORY_PAY','ENT_HISTORY_PAYOUT');
-- UPDATE t_sys_entitlement SET ent_name='下載中心', menu_icon='download', menu_uri='/exports', component_name='ExportCenterPage', ent_type='ML', pid='ROOT', ent_sort='190'
--  WHERE ent_id IN ('ENT_EXPORT_CENTER','ENT_MCH_EXPORT_CENTER');
