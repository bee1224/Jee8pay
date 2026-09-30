-- 測試環境（jee8pay-v2-dev）：ADR-0009 第一階段——代理與四層費率。
-- 1) 建立 t_agent_info／t_agent_mch_rela／t_fee_rule／t_fee_rule_log（純新增，不修改既有資料表）
-- 2) 新增「代理管理」選單與權限；新增商戶列表的「代理綁定」按鈕權限
-- 3) 移除已被取代的服務商頁佔位權限 ENT_ISV_TIER_CONFIG（代理改為獨立實體）
-- 正式環境未執行。
-- 代理資訊表（ADR-0009：代理為獨立實體，高級代理 → 一般代理 兩層）
CREATE TABLE IF NOT EXISTS `t_agent_info` (
        `agent_no` VARCHAR(64) NOT NULL COMMENT '代理號',
        `agent_name` VARCHAR(64) NOT NULL COMMENT '代理名稱',
        `agent_level` TINYINT(6) NOT NULL COMMENT '代理層級: 1-高級代理, 2-一般代理',
        `parent_agent_no` VARCHAR(64) DEFAULT NULL COMMENT '上級代理號（高級代理為空）',
        `agent_path` VARCHAR(512) NOT NULL COMMENT '物化路徑，如 /A001/A002/，用於查詢轄區',
        `contact_name` VARCHAR(32) DEFAULT NULL COMMENT '聯絡人姓名',
        `contact_tel` VARCHAR(32) DEFAULT NULL COMMENT '聯絡人手機號',
        `contact_email` VARCHAR(64) DEFAULT NULL COMMENT '聯絡人信箱',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-正常',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `created_uid` BIGINT(20) DEFAULT NULL COMMENT '建立者用戶ID',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '建立者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`agent_no`),
        KEY `idx_parent_agent_no` (`parent_agent_no`),
        KEY `idx_agent_path` (`agent_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理資訊表';

-- 商戶與代理綁定表（直屬代理與推薦人分開，更換直屬代理不影響推薦人）
CREATE TABLE IF NOT EXISTS `t_agent_mch_rela` (
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商戶號',
        `agent_no` VARCHAR(64) NOT NULL COMMENT '直屬代理號',
        `referrer_agent_no` VARCHAR(64) DEFAULT NULL COMMENT '推薦人代理號（選填）',
        `updated_uid` BIGINT(20) DEFAULT NULL COMMENT '最後修改者用戶ID',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`mch_no`),
        KEY `idx_agent_no` (`agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商戶與代理綁定表';

-- 四層手續費規則表（平臺／渠道／高代／代理；每層百分比＋單筆固定金額）
CREATE TABLE IF NOT EXISTS `t_fee_rule` (
        `rule_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '規則ID',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `target_type` VARCHAR(16) NOT NULL COMMENT '對象類型: DEFAULT-平台預設, AGENT-代理, MCH-單一商戶覆寫',
        `target_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '對象ID（DEFAULT 為空字串）',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層: PLATFORM-平臺費, CHANNEL-渠道費, SR_AGENT-高代費, AGENT-代理費',
        `rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '費率（比例，0.006 即 0.6%）',
        `fixed_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '單筆固定金額（分）',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-啟用',
        `updated_uid` BIGINT(20) DEFAULT NULL COMMENT '最後修改者用戶ID',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`rule_id`),
        UNIQUE KEY `uni_rule` (`way_code`, `target_type`, `target_id`, `layer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='四層手續費規則表';

-- 手續費規則變更紀錄（誰、何時、改了什麼；費率屬動錢設定，須可稽核）
CREATE TABLE IF NOT EXISTS `t_fee_rule_log` (
        `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '紀錄ID',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `target_type` VARCHAR(16) NOT NULL COMMENT '對象類型',
        `target_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '對象ID',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層',
        `action` VARCHAR(16) NOT NULL COMMENT '動作: SAVE-新增或修改, DELETE-刪除',
        `before_value` VARCHAR(256) DEFAULT NULL COMMENT '變更前（JSON）',
        `after_value` VARCHAR(256) DEFAULT NULL COMMENT '變更後（JSON）',
        `operator_uid` BIGINT(20) DEFAULT NULL COMMENT '操作者用戶ID',
        `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`log_id`),
        KEY `idx_rule_key` (`way_code`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手續費規則變更紀錄';

insert into t_sys_entitlement values('ENT_AGENT', '代理管理', 'cluster', '', 'RouteView', 'ML', 0, 1,  'ROOT', '45', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_INFO', '代理列表', 'apartment', '/agents', 'AgentListPage', 'ML', 0, 1,  'ENT_AGENT', '10', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_LIST', '頁面：代理列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_INFO_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_INFO_EDIT', '按鈕：編輯', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_INFO_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_INFO_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE', '費率設定', 'percentage', '/feeRules', 'FeeRulePage', 'ML', 0, 1,  'ENT_AGENT', '20', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_LIST', '頁面：費率列表與試算', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_EDIT', '按鈕：修改代理層費率', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_PLATFORM_EDIT', '按鈕：修改平臺費與渠道費', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_LOG', '頁面：費率變更紀錄', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_MCH_AGENT_BIND', '按鈕：代理綁定', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
DELETE FROM t_sys_role_ent_rela WHERE ent_id = 'ENT_ISV_TIER_CONFIG';
DELETE FROM t_sys_entitlement WHERE ent_id = 'ENT_ISV_TIER_CONFIG';
SELECT COUNT(*) AS agent_ents FROM t_sys_entitlement WHERE ent_id LIKE 'ENT_AGENT%' OR ent_id LIKE 'ENT_FEE_RULE%' OR ent_id = 'ENT_MCH_AGENT_BIND';
