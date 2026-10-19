-- 2026-10-19 測試環境：選單改名「旗下商戶／商戶列表」→「我的商戶」、「旗下代理」→「我的代理」。可重複執行。
-- 代理帳號需要重新登入才會看到新名稱（選單快取在登入憑證裡）。
UPDATE t_sys_entitlement SET ent_name = '我的商戶' WHERE ent_id = 'ENT_AGENT_PORTAL_MCH' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '我的代理' WHERE ent_id = 'ENT_AGENT_PORTAL_SUB' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '按鈕：新增代理（限團長）' WHERE ent_id = 'ENT_AGENT_PORTAL_SUB_ADD' AND sys_type = 'MGR';
-- 回滾：
-- UPDATE t_sys_entitlement SET ent_name = '商戶列表' WHERE ent_id = 'ENT_AGENT_PORTAL_MCH' AND sys_type = 'MGR';
-- UPDATE t_sys_entitlement SET ent_name = '旗下代理' WHERE ent_id = 'ENT_AGENT_PORTAL_SUB' AND sys_type = 'MGR';
-- UPDATE t_sys_entitlement SET ent_name = '按鈕：新增旗下代理（限團長）' WHERE ent_id = 'ENT_AGENT_PORTAL_SUB_ADD' AND sys_type = 'MGR';
