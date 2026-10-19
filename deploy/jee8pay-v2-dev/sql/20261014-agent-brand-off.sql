-- 2026-10-14 測試環境：停用團長「品牌設定」（白標），左側選單不再顯示團長自訂的站名與標誌。
-- 可重複執行。只改選單權限與品牌開關，不刪資料。
-- 另外清掉指向已刪除商戶的團長綁定（舊版刪商戶時沒有一併清除，會讓團長無法刪除）。

UPDATE t_sys_entitlement SET state = 0, updated_at = now() WHERE ent_id = 'ENT_AGENT_PORTAL_BRAND';
DELETE FROM t_sys_role_ent_rela WHERE ent_id = 'ENT_AGENT_PORTAL_BRAND';
UPDATE t_agent_info SET brand_enabled = 0 WHERE brand_enabled = 1;

DELETE r FROM t_agent_mch_rela r LEFT JOIN t_mch_info m ON m.mch_no = r.mch_no WHERE m.mch_no IS NULL;

-- 回滾（恢復品牌設定）：
-- UPDATE t_sys_entitlement SET state = 1 WHERE ent_id = 'ENT_AGENT_PORTAL_BRAND';
-- INSERT IGNORE INTO t_sys_role_ent_rela VALUES ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BRAND');
