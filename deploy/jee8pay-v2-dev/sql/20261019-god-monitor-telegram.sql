-- 2026-10-19 測試環境：上帝的流量監控（容量上限設定）與 Telegram 機器人整理。可重複執行。上帝需重新登入才看得到新選單。
-- Telegram 機器人整理（上帝）：token 明文存放屬已知資安債 TD-001，API 只回遮罩值
CREATE TABLE IF NOT EXISTS `t_telegram_bot` (
  `bot_id` BIGINT NOT NULL AUTO_INCREMENT,
  `bot_name` VARCHAR(64) NOT NULL COMMENT '名稱',
  `bot_username` VARCHAR(64) DEFAULT NULL COMMENT 'Telegram @username（測試連線時帶入）',
  `api_token` VARCHAR(128) NOT NULL COMMENT 'Bot API token',
  `chat_id` VARCHAR(64) DEFAULT NULL COMMENT '通知對象 chat_id',
  `purpose` VARCHAR(256) NOT NULL COMMENT '用途',
  `state` TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用, 1-啟用',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '備註',
  `last_check_at` TIMESTAMP(3) NULL DEFAULT NULL COMMENT '最近測試時間',
  `last_check_result` VARCHAR(256) DEFAULT NULL COMMENT '最近測試結果',
  `created_by` VARCHAR(64) DEFAULT NULL,
  `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`bot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telegram 機器人';

-- 流量監控的容量上限（0 = 不設上限；只用於提醒，不擋單）
INSERT IGNORE INTO t_sys_config (config_key, config_name, config_desc, group_key, group_name, config_val, type, sort_num) VALUES
 ('monitorMaxOrdersPerMinute', '每分鐘建單上限', '流量監控：每分鐘建單數上限，0 為不設', 'monitorCapacity', '容量上限', '0', 'text', 1),
 ('monitorMaxDailyOrders', '每日建單上限', '流量監控：每日建單數上限，0 為不設', 'monitorCapacity', '容量上限', '0', 'text', 2),
 ('monitorMaxDailyAmount', '每日成交金額上限（分）', '流量監控：每日成功金額上限（分），0 為不設', 'monitorCapacity', '容量上限', '0', 'text', 3),
 ('monitorWarnPercent', '警示門檻（%）', '流量監控：使用率超過此百分比顯示警示', 'monitorCapacity', '容量上限', '80', 'text', 4);

-- 選單：系統管理 → 流量監控、Telegram 機器人（上帝帳號自動擁有全部權限）
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_SYS_MONITOR', '流量監控', 'dashboard', '/monitor', 'MonitorPage', 'ML', 0, 1, 'ENT_SYS_CONFIG', '5', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_SYS_MONITOR_EDIT', '按鈕：修改容量上限', 'no-icon', '', '', 'PB', 0, 1, 'ENT_SYS_MONITOR', '0', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_TELEGRAM_BOT', 'Telegram 機器人', 'robot', '/telegramBots', 'TelegramBotPage', 'ML', 0, 1, 'ENT_SYS_CONFIG', '30', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_TELEGRAM_BOT_EDIT', '按鈕：新增／修改／測試', 'no-icon', '', '', 'PB', 0, 1, 'ENT_TELEGRAM_BOT', '0', 'MGR', now(), now());
-- 回滾：DROP TABLE t_telegram_bot; DELETE FROM t_sys_config WHERE group_key='monitorCapacity'; DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_SYS_MONITOR','ENT_SYS_MONITOR_EDIT','ENT_TELEGRAM_BOT','ENT_TELEGRAM_BOT_EDIT');
