-- 測試環境（jee8pay-v2-dev）：代理後台比照競品補三個流程：統計看板、我的子帳號、渠道測試。
-- 可重複執行（MariaDB）。回滾見檔尾註解。

INSERT IGNORE INTO t_sys_entitlement VALUES
    ('ENT_AGENT_PORTAL_DASH', '統計看板', 'dashboard', '/agentPortal/dashboard', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '5', 'MGR', now(), now()),
    ('ENT_AGENT_PORTAL_STAFF', '我的子帳號', 'solution', '/agentPortal/staff', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '41', 'MGR', now(), now()),
    ('ENT_AGENT_PORTAL_CHANNEL_TEST', '按鈕：渠道測試', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_PORTAL_MCH', '0', 'MGR', now(), now());

-- 子帳號管理只給團長；隊長有看板與渠道測試
INSERT IGNORE INTO t_sys_role_ent_rela VALUES
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_DASH'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_STAFF'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_CHANNEL_TEST'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_DASH'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_CHANNEL_TEST');

-- 回滾：
-- DELETE FROM t_sys_role_ent_rela WHERE ent_id IN ('ENT_AGENT_PORTAL_DASH', 'ENT_AGENT_PORTAL_STAFF', 'ENT_AGENT_PORTAL_CHANNEL_TEST');
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_AGENT_PORTAL_DASH', 'ENT_AGENT_PORTAL_STAFF', 'ENT_AGENT_PORTAL_CHANNEL_TEST') AND sys_type = 'MGR';
-- 子帳號（角色 ROLE_AGSUB_*）需另行清除：t_sys_user_role_rela、t_sys_role_ent_rela、t_sys_role、t_sys_user_auth、t_sys_user。
