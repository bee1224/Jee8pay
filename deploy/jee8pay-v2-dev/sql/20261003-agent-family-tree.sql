-- 測試環境（jee8pay-v2-dev）：家族樹權限第一階段。
-- 爺爺＝平台超管、爸爸＝高級代理、兒子＝一般代理。代理只看得到、操作得到自己這一支的後代。
-- 1) 代理後台新增選單：訂單、操作紀錄；新增按鈕權限：新增商戶、新增下級代理
-- 2) 一般代理改用獨立角色 ROLE_AGENT_PORTAL_L2（沒有下級代理、操作紀錄、設定下級費率）
-- 3) 既有一般代理的登入帳號改綁新角色
-- 可重複執行。回滾見檔尾註解。

INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_ORDER', '訂單', 'account-book', '/agentPortal/orders', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '25', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_OPLOG', '操作紀錄', 'file-text', '/agentPortal/opLogs', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '60', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_MCH_ADD', '按鈕：新增商戶', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_PORTAL_MCH', '0', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_SUB_ADD', '按鈕：新增下級代理（限高級代理）', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_PORTAL_SUB', '0', 'MGR', now(), now());

INSERT IGNORE INTO t_sys_role_ent_rela VALUES
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_ORDER'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_OPLOG'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_MCH_ADD'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_SUB_ADD');

INSERT IGNORE INTO t_sys_role VALUES ('ROLE_AGENT_PORTAL_L2', '一般代理帳號（系統角色）', 'MGR', '0', now());
INSERT IGNORE INTO t_sys_role_ent_rela VALUES
    ('ROLE_AGENT_PORTAL_L2', 'ENT_COMMONS'), ('ROLE_AGENT_PORTAL_L2', 'ENT_C_USERINFO'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_HOME'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_VIEW'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_PROFIT'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_MCH'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_MCH_ADD'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_ORDER'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_FEE');

UPDATE t_sys_user_role_rela r
  JOIN t_sys_user u ON u.sys_user_id = r.user_id AND u.sys_type = 'MGR'
  JOIN t_agent_info a ON a.agent_no = u.belong_info_id AND a.agent_level = 2
   SET r.role_id = 'ROLE_AGENT_PORTAL_L2'
 WHERE r.role_id = 'ROLE_AGENT_PORTAL';

SELECT role_id, COUNT(*) AS ents FROM t_sys_role_ent_rela WHERE role_id LIKE 'ROLE_AGENT_PORTAL%' GROUP BY role_id;

-- 回滾：
-- UPDATE t_sys_user_role_rela SET role_id='ROLE_AGENT_PORTAL' WHERE role_id='ROLE_AGENT_PORTAL_L2';
-- DELETE FROM t_sys_role_ent_rela WHERE role_id='ROLE_AGENT_PORTAL_L2' OR ent_id IN ('ENT_AGENT_PORTAL_ORDER','ENT_AGENT_PORTAL_OPLOG','ENT_AGENT_PORTAL_MCH_ADD','ENT_AGENT_PORTAL_SUB_ADD');
-- DELETE FROM t_sys_role WHERE role_id='ROLE_AGENT_PORTAL_L2';
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_AGENT_PORTAL_ORDER','ENT_AGENT_PORTAL_OPLOG','ENT_AGENT_PORTAL_MCH_ADD','ENT_AGENT_PORTAL_SUB_ADD');
