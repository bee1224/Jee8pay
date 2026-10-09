-- 測試環境（jee8pay-v2-dev）：ADR-0012 第一階段。渠道帳號、平台直屬團長、選單改為「從團長點進去」。
-- 不改 payment：下單仍依商戶應用取金鑰；本檔把既有金鑰「複製」成渠道帳號，第二階段切換後才移除商戶應用上的那一份。
-- 可重複執行。

ALTER TABLE t_agent_info ADD COLUMN IF NOT EXISTS `is_house` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否平台直屬: 0-否, 1-是（ADR-0012）';

CREATE TABLE IF NOT EXISTS `t_channel_account` (
        `account_id` VARCHAR(64) NOT NULL COMMENT '渠道帳號ID',
        `if_code` VARCHAR(20) NOT NULL COMMENT '支付接口代碼',
        `account_name` VARCHAR(64) NOT NULL COMMENT '帳號名稱',
        `owner_sr_agent_no` VARCHAR(64) NOT NULL COMMENT '所屬團長代理號',
        `shareable` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否可加派給其他團長: 0-否, 1-是',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-啟用',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `created_uid` BIGINT(20) DEFAULT NULL COMMENT '建立者用戶ID',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '建立者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`account_id`),
        KEY `idx_owner_sr_agent_no` (`owner_sr_agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道帳號表';

CREATE TABLE IF NOT EXISTS `t_channel_account_agent` (
        `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
        `account_id` VARCHAR(64) NOT NULL COMMENT '渠道帳號ID',
        `sr_agent_no` VARCHAR(64) NOT NULL COMMENT '被派發的團長代理號',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '派發者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '派發時間',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_account_agent` (`account_id`, `sr_agent_no`),
        KEY `idx_sr_agent_no` (`sr_agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道帳號派發表';

-- 平台直屬團長，並把還沒有歸屬的商戶歸到它底下
INSERT IGNORE INTO t_agent_info (agent_no, agent_name, agent_level, parent_agent_no, agent_path, state, remark, created_by, is_house)
    VALUES ('A_HOUSE', '平台直屬', 1, NULL, '/A_HOUSE/', 1, '上帝自己經營的一支', 'system', 1);
INSERT INTO t_agent_mch_rela (mch_no, agent_no, updated_by)
    SELECT m.mch_no, 'A_HOUSE', 'migration' FROM t_mch_info m
    WHERE NOT EXISTS (SELECT 1 FROM t_agent_mch_rela r WHERE r.mch_no = m.mch_no);

-- 既有商戶應用上的金鑰：相同接口且內容相同的算同一個帳號，各複製成一個渠道帳號並派發給平台直屬
CREATE TEMPORARY TABLE tmp_house_account AS
    SELECT CONCAT('CAHOUSE', UPPER(if_code), LPAD(ROW_NUMBER() OVER (PARTITION BY if_code ORDER BY MIN(id)), 2, '0')) AS account_id,
           if_code, if_params,
           CONCAT(UPPER(if_code), ' 平台直屬 ', ROW_NUMBER() OVER (PARTITION BY if_code ORDER BY MIN(id))) AS account_name
    FROM t_pay_interface_config
    WHERE info_type = 3 AND NOT EXISTS (SELECT 1 FROM t_channel_account)
    GROUP BY if_code, if_params;
INSERT INTO t_channel_account (account_id, if_code, account_name, owner_sr_agent_no, shareable, state, remark, created_by)
    SELECT account_id, if_code, account_name, 'A_HOUSE', 0, 1, '由既有商戶應用金鑰轉入', 'migration' FROM tmp_house_account;
INSERT INTO t_pay_interface_config (info_type, info_id, if_code, if_params, state, remark, created_by, updated_by)
    SELECT 4, account_id, if_code, if_params, 1, '由既有商戶應用金鑰轉入', 'migration', 'migration' FROM tmp_house_account;
INSERT INTO t_channel_account_agent (account_id, sr_agent_no, created_by)
    SELECT account_id, 'A_HOUSE', 'migration' FROM tmp_house_account;
DROP TEMPORARY TABLE tmp_house_account;

-- 選單：上帝從團長點進去；全站商戶列表與應用列表改為隱藏路由；原廠服務商選單停用
UPDATE t_sys_entitlement SET ent_name = '團長管理' WHERE ent_id = 'ENT_AGENT' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '團長列表' WHERE ent_id = 'ENT_AGENT_INFO' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '商戶列表' WHERE ent_id = 'ENT_AGENT_PORTAL_MCH' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_type = 'MO' WHERE ent_id IN ('ENT_MCH', 'ENT_MCH_INFO', 'ENT_MCH_APP') AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET state = 0 WHERE ent_id IN ('ENT_ISV', 'ENT_ISV_INFO') AND sys_type = 'MGR';
INSERT IGNORE INTO t_sys_entitlement VALUES
    ('ENT_AGENT_DETAIL', '團長詳情', 'no-icon', '/agents/detail', 'AgentDetailPage', 'MO', 0, 1, 'ENT_AGENT', '11', 'MGR', now(), now()),
    ('ENT_CHANNEL_ACCOUNT_LIST', '頁面：渠道帳號列表', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_INFO', '0', 'MGR', now(), now()),
    ('ENT_CHANNEL_ACCOUNT_EDIT', '按鈕：新增／修改／派發渠道帳號', 'no-icon', '', '', 'PB', 0, 1, 'ENT_AGENT_INFO', '0', 'MGR', now(), now()),
    ('ENT_AGENT_PORTAL_CHANNEL', '渠道列表', 'api', '/agentPortal/channels', 'AgentPortalPage', 'ML', 0, 1, 'ENT_AGENT_PORTAL', '32', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_role_ent_rela VALUES ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_CHANNEL');

-- 回滾（渠道帳號與金鑰副本一併刪除；商戶應用上的金鑰不受影響）：
-- DELETE FROM t_pay_interface_config WHERE info_type = 4;
-- DROP TABLE t_channel_account_agent; DROP TABLE t_channel_account;
-- DELETE FROM t_agent_mch_rela WHERE agent_no = 'A_HOUSE'; DELETE FROM t_agent_info WHERE agent_no = 'A_HOUSE';
-- ALTER TABLE t_agent_info DROP COLUMN is_house;
-- UPDATE t_sys_entitlement SET ent_type = 'ML' WHERE ent_id IN ('ENT_MCH', 'ENT_MCH_INFO', 'ENT_MCH_APP') AND sys_type = 'MGR';
-- UPDATE t_sys_entitlement SET state = 1 WHERE ent_id IN ('ENT_ISV', 'ENT_ISV_INFO') AND sys_type = 'MGR';
-- DELETE FROM t_sys_role_ent_rela WHERE ent_id = 'ENT_AGENT_PORTAL_CHANNEL';
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_AGENT_DETAIL', 'ENT_CHANNEL_ACCOUNT_LIST', 'ENT_CHANNEL_ACCOUNT_EDIT', 'ENT_AGENT_PORTAL_CHANNEL');
