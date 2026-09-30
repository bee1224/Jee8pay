-- 測試環境（jee8pay-v2-dev）：ADR-0011 通道路由（別名代碼分流）與下載中心（背景匯出）。
-- 1) 新增 t_way_route、t_way_route_log（純新增）
-- 2) 支付配置下新增「通道路由」選單；移除被取代的佔位權限 ENT_MCH_PAY_ROUTING_CONFIG
-- 3) 新增 t_export_job 與兩個平台的「下載中心」選單
-- 4) 新增 t_pay_order_audit（已關閉訂單補查進度，C5）
-- 正式環境未執行。
-- 通道路由規則（ADR-0011）：別名代碼 → 實際支付方式，依金額／時段／權重分流
CREATE TABLE IF NOT EXISTS `t_way_route` (
        `route_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '規則ID',
        `alias_way_code` VARCHAR(20) NOT NULL COMMENT '別名代碼（商戶下單用）',
        `target_way_code` VARCHAR(20) NOT NULL COMMENT '實際支付方式代碼',
        `mch_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商戶號，空字串表示全部商戶',
        `min_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '金額下限（含），單位分，0 不限',
        `max_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '金額上限（含），單位分，0 不限',
        `weight` INT(11) NOT NULL DEFAULT 1 COMMENT '權重 1-9',
        `time_start` VARCHAR(5) DEFAULT NULL COMMENT '時段起 HH:mm（台北時間）',
        `time_end` VARCHAR(5) DEFAULT NULL COMMENT '時段迄 HH:mm（不含，可跨午夜）',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-啟用',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`route_id`),
        KEY `idx_alias` (`alias_way_code`, `mch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道路由規則';

-- 路由決策紀錄
CREATE TABLE IF NOT EXISTS `t_way_route_log` (
        `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '紀錄ID',
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商戶號',
        `app_id` VARCHAR(64) DEFAULT NULL COMMENT '應用ID',
        `mch_order_no` VARCHAR(64) DEFAULT NULL COMMENT '商戶訂單號',
        `alias_way_code` VARCHAR(20) NOT NULL COMMENT '別名代碼',
        `amount` BIGINT(20) DEFAULT NULL COMMENT '訂單金額，單位分',
        `chosen_way_code` VARCHAR(20) DEFAULT NULL COMMENT '選中的支付方式',
        `candidates` VARCHAR(512) DEFAULT NULL COMMENT '候選與權重（JSON）',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`log_id`),
        KEY `idx_mch_order` (`mch_no`, `mch_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路由決策紀錄';

insert into t_sys_entitlement values('ENT_WAY_ROUTE', '通道路由', 'branches', '/wayRoutes', 'WayRoutePage', 'ML', 0, 1,  'ENT_PC', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WAY_ROUTE_EDIT', '按鈕：新增／修改／刪除路由規則', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WAY_ROUTE', '0', 'MGR', now(), now());
DELETE FROM t_sys_role_ent_rela WHERE ent_id = 'ENT_MCH_PAY_ROUTING_CONFIG';
DELETE FROM t_sys_entitlement WHERE ent_id = 'ENT_MCH_PAY_ROUTING_CONFIG';
-- 背景匯出工作（下載中心）
CREATE TABLE IF NOT EXISTS `t_export_job` (
        `job_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '工作ID',
        `sys_type` VARCHAR(8) NOT NULL COMMENT '所屬系統: MGR/MCH',
        `belong_info_id` VARCHAR(64) NOT NULL DEFAULT '0' COMMENT '所屬（商戶平台為商戶號）',
        `owner_uid` BIGINT(20) NOT NULL COMMENT '申請人用戶ID',
        `owner_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人',
        `job_type` VARCHAR(32) NOT NULL COMMENT '匯出類型',
        `params` VARCHAR(1024) DEFAULT NULL COMMENT '篩選條件（JSON）',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-排隊中, 1-產生中, 2-完成, 3-失敗',
        `file_name` VARCHAR(128) DEFAULT NULL COMMENT '下載檔名',
        `row_count` BIGINT(20) DEFAULT NULL COMMENT '資料筆數',
        `error_msg` VARCHAR(256) DEFAULT NULL COMMENT '失敗原因',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `finished_at` DATETIME DEFAULT NULL COMMENT '完成時間',
        PRIMARY KEY (`job_id`),
        KEY `idx_owner` (`sys_type`, `owner_uid`, `job_id`),
        KEY `idx_state` (`sys_type`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='背景匯出工作';

insert into t_sys_entitlement values('ENT_EXPORT_CENTER', '下載中心', 'download', '/exports', 'ExportCenterPage', 'ML', 0, 1,  'ROOT', '190', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_MCH_EXPORT_CENTER', '下載中心', 'download', '/exports', 'ExportCenterPage', 'ML', 0, 1,  'ROOT', '190', 'MCH', now(), now());
-- 已關閉訂單補查進度（C5，延伸 ADR-0007；旁表，不修改 t_pay_order）
CREATE TABLE IF NOT EXISTS `t_pay_order_audit` (
        `pay_order_id` VARCHAR(30) NOT NULL COMMENT '支付訂單號',
        `audit_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已補查次數',
        `last_result` VARCHAR(32) DEFAULT NULL COMMENT '最後一次查詢結果',
        `reopened` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否轉回支付成功: 0-否, 1-是',
        `last_audit_at` DATETIME DEFAULT NULL COMMENT '最後補查時間',
        PRIMARY KEY (`pay_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='關閉訂單補查紀錄';

SELECT COUNT(*) AS route_tables FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('t_way_route','t_way_route_log','t_export_job','t_pay_order_audit');
