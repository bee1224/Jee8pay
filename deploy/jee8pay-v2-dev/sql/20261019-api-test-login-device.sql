-- 2026-10-19 測試環境：上帝主選單新增「技術工具 → API 測試」、「系統管理 → 登入裝置」，Telegram 機器人改為獨立一欄。可重複執行。上帝需重新登入才看得到新選單。
-- Telegram 機器人：從系統管理移到獨立的「Telegram」分組
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_TELEGRAM', 'Telegram', 'robot', '', 'RouteView', 'ML', 0, 1, 'ROOT', '190', 'MGR', now(), now());
UPDATE t_sys_entitlement SET pid = 'ENT_TELEGRAM', ent_sort = 10 WHERE ent_id = 'ENT_TELEGRAM_BOT' AND sys_type = 'MGR';
-- API 測試（技術人員）：印出 商戶→平台、平台→商戶、平台→渠道 的完整請求與回應
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_DEV_TOOLS', '技術工具', 'code', '', 'RouteView', 'ML', 0, 1, 'ROOT', '180', 'MGR', now(), now());
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_API_TEST', 'API 測試', 'api', '/apiTest', 'ApiTestPage', 'ML', 0, 1, 'ENT_DEV_TOOLS', '10', 'MGR', now(), now());
-- 登入裝置：自己與所有團長、隊長的 IP、裝置、GPS（只限平台帳號）
INSERT IGNORE INTO t_sys_entitlement VALUES ('ENT_LOGIN_DEVICE', '登入裝置', 'mobile', '/loginDevices', 'LoginDevicePage', 'ML', 0, 1, 'ENT_SYS_CONFIG', '6', 'MGR', now(), now());
-- 回滾：UPDATE t_sys_entitlement SET pid='ENT_SYS_CONFIG', ent_sort=30 WHERE ent_id='ENT_TELEGRAM_BOT' AND sys_type='MGR'; DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_TELEGRAM','ENT_DEV_TOOLS','ENT_API_TEST','ENT_LOGIN_DEVICE') AND sys_type='MGR';
