-- 測試環境（jee8pay-v2-dev）：移除商戶平台的空殼選單（支付測試、轉帳、分帳管理）。
-- 這三組頁面只支援已移除的微信／支付寶通道；前端頁面與對應商戶端 controller 已一併刪除。
-- 只刪權限與角色關聯；分帳/轉帳資料表與訂單紀錄不動。正式環境未執行。

SELECT 'BEFORE' AS phase, sys_type, COUNT(*) AS ents FROM t_sys_entitlement
 WHERE ent_id LIKE 'ENT_MCH_PAY_TEST%' OR ent_id LIKE 'ENT_MCH_TRANSFER%' OR ent_id LIKE 'ENT_DIVISION%'
 GROUP BY sys_type;

START TRANSACTION;
DELETE r FROM t_sys_role_ent_rela r
  JOIN t_sys_role ro ON ro.role_id = r.role_id AND ro.sys_type = 'MCH'
 WHERE r.ent_id LIKE 'ENT_MCH_PAY_TEST%' OR r.ent_id LIKE 'ENT_MCH_TRANSFER%' OR r.ent_id LIKE 'ENT_DIVISION%';
DELETE FROM t_sys_entitlement
 WHERE sys_type = 'MCH' AND (ent_id LIKE 'ENT_MCH_PAY_TEST%' OR ent_id LIKE 'ENT_MCH_TRANSFER%' OR ent_id LIKE 'ENT_DIVISION%');
COMMIT;

SELECT 'AFTER' AS phase, COUNT(*) AS ents_left FROM t_sys_entitlement
 WHERE sys_type = 'MCH' AND (ent_id LIKE 'ENT_MCH_PAY_TEST%' OR ent_id LIKE 'ENT_MCH_TRANSFER%' OR ent_id LIKE 'ENT_DIVISION%');
