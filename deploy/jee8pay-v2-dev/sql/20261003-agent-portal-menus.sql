-- 測試環境（jee8pay-v2-dev）：代理後台由單一頁的分頁改為五個選單（錢包與提現、分潤、旗下商戶、下級代理、費率）。
-- 原「我的代理後台」選單改為「錢包與提現」（保留權限 ID 與其下的按鈕權限），另新增四個選單並授權給代理帳號固定角色。
-- 可重複執行。回滾見檔尾註解。

UPDATE t_sys_entitlement SET ent_name = '錢包與提現', menu_icon = 'wallet', menu_uri = '/agentPortal/wallet', updated_at = now()
 WHERE ent_id = 'ENT_AGENT_PORTAL_HOME' AND sys_type = 'MGR';

INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_PROFIT', '分潤', 'pie-chart', '/agentPortal/profit', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '20', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_MCH', '旗下商戶', 'shop', '/agentPortal/merchants', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '30', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_SUB', '下級代理', 'team', '/agentPortal/subAgents', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '40', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_FEE', '費率', 'percentage', '/agentPortal/fee', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '50', 'MGR', now(), now());

INSERT IGNORE INTO t_sys_role_ent_rela VALUES
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_PROFIT'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_MCH'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_SUB'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_FEE');

SELECT ent_id, ent_name, ent_type, menu_uri, pid, ent_sort FROM t_sys_entitlement WHERE sys_type = 'MGR' AND ent_id LIKE 'ENT_AGENT_PORTAL%' ORDER BY ent_type, ent_sort;

-- 回滾：
-- DELETE FROM t_sys_role_ent_rela WHERE ent_id IN ('ENT_AGENT_PORTAL_PROFIT','ENT_AGENT_PORTAL_MCH','ENT_AGENT_PORTAL_SUB','ENT_AGENT_PORTAL_FEE');
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_AGENT_PORTAL_PROFIT','ENT_AGENT_PORTAL_MCH','ENT_AGENT_PORTAL_SUB','ENT_AGENT_PORTAL_FEE');
-- UPDATE t_sys_entitlement SET ent_name='我的代理後台', menu_icon='dashboard', menu_uri='/agentPortal' WHERE ent_id='ENT_AGENT_PORTAL_HOME' AND sys_type='MGR';
