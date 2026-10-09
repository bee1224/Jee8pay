-- 測試環境（jee8pay-v2-dev）：渠道帳號使用範圍（限定只給團長旗下某幾位隊長的商戶）。可重複執行。
CREATE TABLE IF NOT EXISTS `t_channel_account_scope` (
        `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
        `account_id` VARCHAR(64) NOT NULL COMMENT '渠道帳號ID',
        `sr_agent_no` VARCHAR(64) NOT NULL COMMENT '隊長所屬的團長代理號',
        `agent_no` VARCHAR(64) NOT NULL COMMENT '可使用的隊長代理號',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '設定者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '設定時間',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_account_agent` (`account_id`, `agent_no`),
        KEY `idx_agent_no` (`agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道帳號使用範圍表';

-- 回滾：DROP TABLE t_channel_account_scope;
