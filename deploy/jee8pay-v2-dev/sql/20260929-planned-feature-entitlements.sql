-- 測試環境（jee8pay-v2-dev）：規劃中功能的顯示開關權限碼。
-- 已於 2026-09-29 在 jee8pay_v2_dev 執行。超管（is_admin=1）會自動取得 DB 內所有權限碼，無需設定角色關聯。
-- 前端以 $access('ENT_...') 包住對應區塊；刪除這兩列即可整體隱藏。正式環境未執行。
insert into t_sys_entitlement values('ENT_ISV_TIER_CONFIG', '服務商代理層級與費率瀑布（規劃中）', 'no-icon', '', '', 'PB', 0, 1, 'ENT_ISV_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_MCH_PAY_ROUTING_CONFIG', '支付通道進階路由規則（規劃中）', 'no-icon', '', '', 'PB', 0, 1, 'ENT_MCH_PAY_PASSAGE_LIST', '0', 'MGR', now(), now());
