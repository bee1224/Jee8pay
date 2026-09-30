-- 測試環境（jee8pay-v2-dev）：ADR-0009 第二～四階段。
-- 1) 新增 t_pay_order_fee（訂單手續費快照）、t_fee_template／t_fee_template_item（費率範本）、
--    t_fee_rule_change_req（平臺費雙人覆核）；皆為純新增，不修改既有資料表
-- 2) 新增代理後台、範本、覆核、批次、分潤、登入帳號權限，以及代理帳號固定角色 ROLE_AGENT_PORTAL
-- 正式環境未執行。
-- 訂單四層手續費快照（ADR-0009 第二階段；旁表，不修改 t_pay_order；建立後不可變）
CREATE TABLE IF NOT EXISTS `t_pay_order_fee` (
        `pay_order_id` VARCHAR(30) NOT NULL COMMENT '支付訂單號',
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商戶號',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `amount` BIGINT(20) NOT NULL COMMENT '訂單金額，單位分',
        `mch_fee_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '下單時商戶手續費（支付通道費率），單位分',
        `agent_no` VARCHAR(64) DEFAULT NULL COMMENT '直屬代理號（下單當下）',
        `sr_agent_no` VARCHAR(64) DEFAULT NULL COMMENT '高級代理號（下單當下）',
        `referrer_agent_no` VARCHAR(64) DEFAULT NULL COMMENT '推薦人代理號（下單當下）',
        `platform_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '平臺費，單位分',
        `channel_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '渠道費，單位分',
        `sr_agent_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '高代費，單位分',
        `agent_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '代理費，單位分',
        `total_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '四層合計，單位分',
        `exceeds_mch_fee` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '四層合計是否超過商戶手續費: 0-否, 1-是',
        `detail` VARCHAR(1024) DEFAULT NULL COMMENT '各層費率、固定金額與規則來源（JSON）',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`pay_order_id`),
        KEY `idx_sr_agent_no` (`sr_agent_no`),
        KEY `idx_agent_no` (`agent_no`),
        KEY `idx_mch_no` (`mch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='訂單手續費快照';

-- 費率範本（ADR-0009 第四階段；只含高代費／代理費）
CREATE TABLE IF NOT EXISTS `t_fee_template` (
        `template_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '範本ID',
        `template_name` VARCHAR(64) NOT NULL COMMENT '範本名稱',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `updated_uid` BIGINT(20) DEFAULT NULL COMMENT '最後修改者用戶ID',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='費率範本';

CREATE TABLE IF NOT EXISTS `t_fee_template_item` (
        `item_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '明細ID',
        `template_id` BIGINT(20) NOT NULL COMMENT '範本ID',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層: SR_AGENT/AGENT',
        `rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '費率（比率）',
        `fixed_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '單筆固定金額，單位分',
        PRIMARY KEY (`item_id`),
        UNIQUE KEY `uni_template_item` (`template_id`, `way_code`, `layer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='費率範本明細';

-- 平臺費／渠道費變更申請（雙人覆核；核准後才寫入 t_fee_rule）
CREATE TABLE IF NOT EXISTS `t_fee_rule_change_req` (
        `req_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申請ID',
        `action` VARCHAR(16) NOT NULL COMMENT '動作: SAVE/DELETE',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `target_type` VARCHAR(16) NOT NULL COMMENT '對象類型: DEFAULT/MCH',
        `target_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '對象ID',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層: PLATFORM/CHANNEL',
        `rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '申請費率（比率）',
        `fixed_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '申請單筆固定金額，單位分',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-待覆核, 1-已核准, 2-已駁回',
        `requester_uid` BIGINT(20) NOT NULL COMMENT '申請人用戶ID',
        `requester_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人姓名',
        `reviewer_uid` BIGINT(20) DEFAULT NULL COMMENT '覆核人用戶ID',
        `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '覆核人姓名',
        `review_remark` VARCHAR(128) DEFAULT NULL COMMENT '覆核意見',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '申請時間',
        `reviewed_at` DATETIME DEFAULT NULL COMMENT '覆核時間',
        PRIMARY KEY (`req_id`),
        KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平臺費變更申請（雙人覆核）';

insert into t_sys_entitlement values('ENT_AGENT_ACCOUNT', '按鈕：登入帳號', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PROFIT', '按鈕：分潤報表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_BATCH', '按鈕：批次設定代理層費率', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_REVIEW', '按鈕：覆核平臺費變更', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_TEMPLATE', '費率範本', 'copy', '/feeTemplates', 'FeeTemplatePage', 'ML', 0, 1,  'ENT_AGENT', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_TEMPLATE_EDIT', '按鈕：新增／修改／刪除範本', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_TEMPLATE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_TEMPLATE_APPLY', '按鈕：套用範本', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_TEMPLATE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL', '代理後台', 'team', '', 'RouteView', 'ML', 0, 1,  'ROOT', '46', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_HOME', '我的代理後台', 'dashboard', '/agentPortal', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '10', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_VIEW', '頁面：代理後台資料', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_HOME', '0', 'MGR', now(), now());
-- 代理帳號固定角色：只含代理後台與個人中心，不含任何平台權限
insert into t_sys_role values ('ROLE_AGENT_PORTAL', '代理帳號（系統角色）', 'MGR', '0', now());
insert into t_sys_role_ent_rela values ('ROLE_AGENT_PORTAL', 'ENT_COMMONS'), ('ROLE_AGENT_PORTAL', 'ENT_C_USERINFO'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_HOME'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_VIEW');

SELECT COUNT(*) AS new_ents FROM t_sys_entitlement WHERE ent_id IN ('ENT_AGENT_ACCOUNT','ENT_AGENT_PROFIT','ENT_FEE_RULE_BATCH','ENT_FEE_RULE_REVIEW','ENT_FEE_TEMPLATE','ENT_FEE_TEMPLATE_EDIT','ENT_FEE_TEMPLATE_APPLY','ENT_AGENT_PORTAL','ENT_AGENT_PORTAL_HOME','ENT_AGENT_PORTAL_VIEW');
