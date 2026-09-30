-- 測試環境（jee8pay-v2-dev）：ADR-0010 錢包、結算、提現、風控黑名單；ADR-0009 推薦佣金層。
-- 1) t_pay_order_fee 加上推薦佣金與結算狀態欄位（ADR-0009 旁表，非 t_pay_order）
-- 2) 新增錢包帳戶／流水／提現單／調帳申請／黑名單資料表（純新增）
-- 3) 錢包與提現選單去掉「規劃中」，新增錢包帳戶、黑名單、審核與調帳權限；代理後台加下級費率權限；商戶錢包加提現與收款帳戶權限
-- 4) 系統配置新增 walletConfig 群組
-- 正式環境未執行。
ALTER TABLE t_pay_order_fee
  ADD COLUMN referrer_fee BIGINT(20) NOT NULL DEFAULT 0 COMMENT '推薦佣金，單位分' AFTER agent_fee,
  ADD COLUMN settle_state TINYINT(6) NOT NULL DEFAULT 0 COMMENT '結算狀態: 0-未結算, 1-已結算, 2-已沖回, 3-不結算' AFTER detail,
  ADD COLUMN settled_at DATETIME DEFAULT NULL COMMENT '結算（或沖回）時間' AFTER settle_state,
  ADD KEY idx_settle_state (settle_state);
-- 錢包帳戶（ADR-0010）：可用與凍結分桶；只能經 WalletService 記帳異動
CREATE TABLE IF NOT EXISTS `t_wallet_account` (
        `account_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '帳戶ID',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型: MCH/AGENT/PLATFORM/CHANNEL',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID（商戶號／代理號／PLATFORM／ifCode）',
        `balance` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '可用餘額，單位分',
        `frozen` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '凍結金額，單位分',
        `total_in` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '累計入帳，單位分',
        `total_out` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '累計出帳，單位分',
        `payout_bank_name` VARCHAR(64) DEFAULT NULL COMMENT '提現銀行名稱',
        `payout_bank_code` VARCHAR(8) DEFAULT NULL COMMENT '提現銀行代碼',
        `payout_branch` VARCHAR(64) DEFAULT NULL COMMENT '提現分行',
        `payout_account_no` VARCHAR(32) DEFAULT NULL COMMENT '提現帳號',
        `payout_account_name` VARCHAR(64) DEFAULT NULL COMMENT '提現戶名',
        `payout_updated_at` DATETIME DEFAULT NULL COMMENT '收款帳戶最後變更時間',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`account_id`),
        UNIQUE KEY `uni_owner` (`owner_type`, `owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='錢包帳戶';

-- 錢包流水（只增不改；唯一鍵保證結算與提現冪等）
CREATE TABLE IF NOT EXISTS `t_wallet_ledger` (
        `ledger_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '流水ID',
        `account_id` BIGINT(20) NOT NULL COMMENT '帳戶ID',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID',
        `biz_type` VARCHAR(32) NOT NULL COMMENT '業務類型: ORDER_SETTLE/ORDER_REVERSE/WITHDRAW_APPLY/WITHDRAW_RELEASE/WITHDRAW_PAID/WITHDRAW_FEE/ADJUST',
        `biz_id` VARCHAR(64) NOT NULL COMMENT '業務單號',
        `amount` BIGINT(20) NOT NULL COMMENT '可用餘額變動（正入負出），單位分',
        `frozen_change` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '凍結金額變動，單位分',
        `balance_before` BIGINT(20) NOT NULL COMMENT '變動前可用餘額',
        `balance_after` BIGINT(20) NOT NULL COMMENT '變動後可用餘額',
        `frozen_after` BIGINT(20) NOT NULL COMMENT '變動後凍結金額',
        `remark` VARCHAR(256) DEFAULT NULL COMMENT '說明',
        `operator_uid` BIGINT(20) DEFAULT NULL COMMENT '操作者用戶ID',
        `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作者',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`ledger_id`),
        UNIQUE KEY `uni_biz` (`account_id`, `biz_type`, `biz_id`),
        KEY `idx_owner` (`owner_type`, `owner_id`, `ledger_id`),
        KEY `idx_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='錢包流水';

-- 提現單（平台人工匯款）
CREATE TABLE IF NOT EXISTS `t_withdraw_order` (
        `withdraw_id` VARCHAR(32) NOT NULL COMMENT '提現單號',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型: MCH/AGENT',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID',
        `req_no` VARCHAR(64) NOT NULL COMMENT '申請端冪等鍵',
        `amount` BIGINT(20) NOT NULL COMMENT '申請金額，單位分',
        `fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '提現手續費，單位分',
        `actual_amount` BIGINT(20) NOT NULL COMMENT '實際匯款金額，單位分',
        `bank_name` VARCHAR(64) NOT NULL COMMENT '銀行名稱',
        `bank_code` VARCHAR(8) NOT NULL COMMENT '銀行代碼',
        `branch` VARCHAR(64) DEFAULT NULL COMMENT '分行',
        `account_no` VARCHAR(32) NOT NULL COMMENT '帳號',
        `account_name` VARCHAR(64) NOT NULL COMMENT '戶名',
        `risk_flags` VARCHAR(128) DEFAULT NULL COMMENT '風控提示: BLACKLIST/DAILY_LIMIT/RESTRICTED_BANK/NEW_ACCOUNT',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-待審核, 1-已撥款, 2-已駁回, 3-已取消',
        `apply_uid` BIGINT(20) DEFAULT NULL COMMENT '申請人用戶ID',
        `apply_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人',
        `reviewer_uid` BIGINT(20) DEFAULT NULL COMMENT '審核人用戶ID',
        `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '審核人',
        `paid_ref` VARCHAR(64) DEFAULT NULL COMMENT '匯款單號',
        `review_remark` VARCHAR(128) DEFAULT NULL COMMENT '審核說明',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '申請時間',
        `reviewed_at` DATETIME DEFAULT NULL COMMENT '審核時間',
        PRIMARY KEY (`withdraw_id`),
        UNIQUE KEY `uni_req` (`owner_type`, `owner_id`, `req_no`),
        KEY `idx_state` (`state`, `created_at`),
        KEY `idx_account_no` (`account_no`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提現單';

-- 人工調帳申請（雙人覆核）
CREATE TABLE IF NOT EXISTS `t_wallet_adjust_req` (
        `req_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申請ID',
        `account_id` BIGINT(20) NOT NULL COMMENT '帳戶ID',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID',
        `amount` BIGINT(20) NOT NULL COMMENT '調整金額（正加負減），單位分',
        `reason` VARCHAR(128) NOT NULL COMMENT '調帳原因',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-待覆核, 1-已核准, 2-已駁回',
        `requester_uid` BIGINT(20) NOT NULL COMMENT '申請人用戶ID',
        `requester_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人',
        `reviewer_uid` BIGINT(20) DEFAULT NULL COMMENT '覆核人用戶ID',
        `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '覆核人',
        `review_remark` VARCHAR(128) DEFAULT NULL COMMENT '覆核意見',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '申請時間',
        `reviewed_at` DATETIME DEFAULT NULL COMMENT '覆核時間',
        PRIMARY KEY (`req_id`),
        KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工調帳申請';

-- 風控黑名單（GLOBAL 或高級代理範圍）
CREATE TABLE IF NOT EXISTS `t_risk_blacklist` (
        `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
        `list_type` VARCHAR(16) NOT NULL COMMENT '類型: BANK_ACCOUNT/ACCOUNT_NAME/PHONE',
        `list_value` VARCHAR(64) NOT NULL COMMENT '值',
        `scope` VARCHAR(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '範圍: GLOBAL 或高級代理號',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `created_uid` BIGINT(20) DEFAULT NULL COMMENT '建立者用戶ID',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '建立者',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uni_entry` (`list_type`, `list_value`, `scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='風控黑名單';

UPDATE t_sys_entitlement SET ent_name = '錢包與提現' WHERE ent_id = 'ENT_WALLET';
UPDATE t_sys_entitlement SET ent_name = '我的錢包' WHERE ent_id = 'ENT_MCH_WALLET';
insert into t_sys_entitlement values('ENT_WALLET_ACCOUNT', '錢包帳戶', 'wallet', '/wallet/accounts', 'WalletAccountPage', 'ML', 0, 1,  'ENT_WALLET', '5', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_ADJUST', '按鈕：申請人工調帳', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_ACCOUNT', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_ADJUST_REVIEW', '按鈕：覆核人工調帳', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_ACCOUNT', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_SETTLE_RUN', '按鈕：立即結算', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_ACCOUNT', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_WITHDRAW_REVIEW', '按鈕：審核提現（撥款／駁回）', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_WITHDRAW', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_RISK_BLACKLIST', '風控黑名單', 'stop', '/risk/blacklist', 'RiskBlacklistPage', 'ML', 0, 1,  'ENT_WALLET', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_RISK_BLACKLIST_EDIT', '按鈕：新增／刪除黑名單', 'no-icon', '', '', 'PB', 0, 1,  'ENT_RISK_BLACKLIST', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_FEE_EDIT', '按鈕：設定下級代理費率（限高級代理）', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_HOME', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_MCH_WALLET_WITHDRAW', '按鈕：申請／取消提現', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_WALLET', '0', 'MCH', now(), now());
insert into t_sys_entitlement values('ENT_MCH_WALLET_PAYOUT_EDIT', '按鈕：設定收款帳戶', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_WALLET', '0', 'MCH', now(), now());
INSERT INTO t_sys_role_ent_rela VALUES ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_FEE_EDIT');
INSERT INTO `t_sys_config` VALUES ('walletSettleDelayDays', '結算延遲天數', '0 = 成功即結算（T+0）；1 = 隔日結算（T+1）', 'walletConfig', '錢包與提現', '1', 'text', 10, now());
INSERT INTO `t_sys_config` VALUES ('withdrawMinAmount', '單筆最低提現金額（元）', '低於此金額不可申請', 'walletConfig', '錢包與提現', '100', 'text', 20, now());
INSERT INTO `t_sys_config` VALUES ('withdrawMaxAmount', '單筆最高提現金額（元）', '高於此金額不可申請', 'walletConfig', '錢包與提現', '500000', 'text', 30, now());
INSERT INTO `t_sys_config` VALUES ('withdrawFeeAmount', '每筆提現手續費（元）', '撥款時自申請金額扣除並記入平台帳戶', 'walletConfig', '錢包與提現', '0', 'text', 40, now());
INSERT INTO `t_sys_config` VALUES ('withdrawDailyLimitPerAccount', '同一收款帳號每日提現次數提示門檻', '超過會在審核畫面標記 DAILY_LIMIT', 'walletConfig', '錢包與提現', '3', 'text', 50, now());
INSERT INTO `t_sys_config` VALUES ('withdrawRestrictedBankCodes', '需特別注意的銀行代碼（逗號分隔）', '命中會在審核畫面標記 RESTRICTED_BANK', 'walletConfig', '錢包與提現', '', 'text', 60, now());

SELECT COUNT(*) AS wallet_tables FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('t_wallet_account','t_wallet_ledger','t_withdraw_order','t_wallet_adjust_req','t_risk_blacklist');
