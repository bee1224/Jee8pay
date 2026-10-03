-- 測試環境（jee8pay-v2-dev）：家族樹權限第二階段（旗下管理）。
-- 1) 欄位：代理白標、提現單的「上級代理同意」註記
-- 2) 代理後台新增選單：統計報表、旗下錢包、提現審核、通道路由、黑名單、品牌設定；按鈕權限：凍結解凍、商戶歸屬與重設密碼
-- 3) 「下級代理」改名為「旗下代理」
-- 可重複執行（MariaDB）。回滾見檔尾註解。

ALTER TABLE t_agent_info
  ADD COLUMN IF NOT EXISTS `brand_enabled` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '白標是否啟用: 0-否, 1-是（僅高級代理）',
  ADD COLUMN IF NOT EXISTS `brand_title` VARCHAR(32) DEFAULT NULL COMMENT '白標站台名稱',
  ADD COLUMN IF NOT EXISTS `brand_logo` VARCHAR(255) DEFAULT NULL COMMENT '白標 Logo 圖片位址';
ALTER TABLE t_withdraw_order
  ADD COLUMN IF NOT EXISTS `agent_approve_by` VARCHAR(64) DEFAULT NULL COMMENT '上級代理同意人（僅註記，撥款仍由平台審核）',
  ADD COLUMN IF NOT EXISTS `agent_approve_at` DATETIME DEFAULT NULL COMMENT '上級代理同意時間';

UPDATE t_sys_entitlement SET ent_name = '旗下代理', updated_at = now() WHERE ent_id = 'ENT_AGENT_PORTAL_SUB' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '按鈕：新增旗下代理（限高級代理）', updated_at = now() WHERE ent_id = 'ENT_AGENT_PORTAL_SUB_ADD' AND sys_type = 'MGR';

INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_REPORT', '統計報表', 'bar-chart', '/agentPortal/report', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '22', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_BRANCH_WALLET', '旗下錢包', 'bank', '/agentPortal/branchWallets', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '42', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_FREEZE', '按鈕：凍結／解凍旗下資金（限高級代理）', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_PORTAL_BRANCH_WALLET', '0', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_WITHDRAW_AUDIT', '提現審核', 'audit', '/agentPortal/withdrawAudit', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '44', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_ROUTE', '通道路由', 'branches', '/agentPortal/routes', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '52', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_BLACKLIST', '黑名單', 'stop', '/agentPortal/blacklist', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '54', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_BRAND', '品牌設定', 'skin', '/agentPortal/brand', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '70', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_AGENT_PORTAL_MCH_EDIT', '按鈕：商戶歸屬與重設密碼', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_PORTAL_MCH', '0', 'MGR', now(), now());

INSERT IGNORE INTO t_sys_role_ent_rela VALUES
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_REPORT'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BRANCH_WALLET'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_FREEZE'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_WITHDRAW_AUDIT'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_ROUTE'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BLACKLIST'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BRAND'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_MCH_EDIT'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_REPORT'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_BRANCH_WALLET'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_MCH_EDIT');

SELECT role_id, COUNT(*) AS ents FROM t_sys_role_ent_rela WHERE role_id LIKE 'ROLE_AGENT_PORTAL%' GROUP BY role_id;

-- 回滾：
-- DELETE FROM t_sys_role_ent_rela WHERE ent_id IN ('ENT_AGENT_PORTAL_REPORT','ENT_AGENT_PORTAL_BRANCH_WALLET','ENT_AGENT_PORTAL_FREEZE','ENT_AGENT_PORTAL_WITHDRAW_AUDIT','ENT_AGENT_PORTAL_ROUTE','ENT_AGENT_PORTAL_BLACKLIST','ENT_AGENT_PORTAL_BRAND','ENT_AGENT_PORTAL_MCH_EDIT');
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_AGENT_PORTAL_REPORT','ENT_AGENT_PORTAL_BRANCH_WALLET','ENT_AGENT_PORTAL_FREEZE','ENT_AGENT_PORTAL_WITHDRAW_AUDIT','ENT_AGENT_PORTAL_ROUTE','ENT_AGENT_PORTAL_BLACKLIST','ENT_AGENT_PORTAL_BRAND','ENT_AGENT_PORTAL_MCH_EDIT');
-- UPDATE t_sys_entitlement SET ent_name='下級代理' WHERE ent_id='ENT_AGENT_PORTAL_SUB' AND sys_type='MGR';
-- 欄位為新增且有預設值，舊版程式不受影響，不需移除。
