-- 2026-10-19 測試環境：登入裝置紀錄（手機版「我的」顯示上次登入；平台在團長詳情看裝置識別碼、型號、GPS）。可重複執行。
CREATE TABLE IF NOT EXISTS `t_sys_login_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sys_user_id` BIGINT NOT NULL COMMENT '登入的用戶ID',
  `sys_type` VARCHAR(8) NOT NULL COMMENT 'MGR／MCH',
  `belong_info_id` VARCHAR(64) DEFAULT NULL COMMENT '所屬代理號（平台帳號為 0）',
  `user_name` VARCHAR(64) DEFAULT NULL COMMENT '用戶姓名',
  `user_ip` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '登入 IP',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '裝置識別碼（瀏覽器端隨機產生）',
  `device_model` VARCHAR(128) DEFAULT NULL COMMENT '裝置型號',
  `device_os` VARCHAR(64) DEFAULT NULL COMMENT '作業系統',
  `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '緯度',
  `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '經度',
  `accuracy` INT DEFAULT NULL COMMENT '定位精確度（公尺）',
  `geo_state` VARCHAR(16) NOT NULL DEFAULT 'UNSUPPORTED' COMMENT '定位狀態',
  `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登入時間',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`sys_user_id`, `created_at`),
  KEY `idx_belong` (`belong_info_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登入裝置紀錄';
-- 回滾：DROP TABLE t_sys_login_device;
