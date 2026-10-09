-- 測試環境（jee8pay-v2-dev）：上帝的「渠道管理 → 渠道列表」選單（全部團長的渠道帳號）。可重複執行。
INSERT IGNORE INTO t_sys_entitlement VALUES
    ('ENT_CHANNEL', '渠道管理', 'api', '', 'RouteView', 'ML', 0, 1, 'ROOT', '47', 'MGR', now(), now()),
    ('ENT_CHANNEL_ACCOUNT', '渠道列表', 'unordered-list', '/channels', 'ChannelAccountPage', 'ML', 0, 1, 'ENT_CHANNEL', '10', 'MGR', now(), now());

-- 原「支付設定 → 支付介面」搬到「渠道管理」底下並改名為大渠道（一家第三方支付的串接）
UPDATE t_sys_entitlement SET ent_name = '大渠道', pid = 'ENT_CHANNEL', ent_sort = 5 WHERE ent_id = 'ENT_PC_IF_DEFINE' AND sys_type = 'MGR';

-- 回滾：
-- DELETE FROM t_sys_entitlement WHERE ent_id IN ('ENT_CHANNEL', 'ENT_CHANNEL_ACCOUNT') AND sys_type = 'MGR';
-- UPDATE t_sys_entitlement SET ent_name = '支付介面', pid = 'ENT_PC', ent_sort = 10 WHERE ent_id = 'ENT_PC_IF_DEFINE' AND sys_type = 'MGR';
