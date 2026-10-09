-- 測試環境（jee8pay-v2-dev）：角色改名。超級管理員 → 上帝、高級代理 → 團長、一般代理 → 隊長。
-- 只改顯示名稱；ent_id、role_id、layer 代碼（SR_AGENT／AGENT）與 agent_level 數值都不變。

UPDATE t_sys_entitlement SET ent_name = REPLACE(ent_name, '高級代理', '團長') WHERE ent_name LIKE '%高級代理%';
UPDATE t_sys_role SET role_name = '隊長帳號（系統角色）' WHERE role_id = 'ROLE_AGENT_PORTAL_L2';
UPDATE t_sys_user SET realname = '上帝' WHERE sys_user_id = 801 AND realname = '超管';

-- 回滾：
-- UPDATE t_sys_entitlement SET ent_name = REPLACE(ent_name, '限團長', '限高級代理') WHERE ent_name LIKE '%限團長%';
-- UPDATE t_sys_role SET role_name = '一般代理帳號（系統角色）' WHERE role_id = 'ROLE_AGENT_PORTAL_L2';
-- UPDATE t_sys_user SET realname = '超管' WHERE sys_user_id = 801 AND realname = '上帝';
