-- 測試環境（jee8pay-v2-dev）：顯示名稱裡殘留的簡體字改為繁體。只改名稱，不改代碼。
UPDATE t_sys_entitlement SET ent_name = '頁面：支付介面定義列表' WHERE ent_id = 'ENT_PC_IF_DEFINE_LIST' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '按鈕： 修改名稱' WHERE ent_id = 'ENT_UR_ROLE_EDIT' AND sys_type = 'MCH';
UPDATE t_sys_entitlement SET ent_name = '按鈕： 修改名稱' WHERE ent_id = 'ENT_UR_ROLE_EDIT' AND sys_type = 'MGR';
UPDATE t_sys_entitlement SET ent_name = '按鈕： 權限變更' WHERE ent_id = 'ENT_UR_ROLE_ENT_EDIT' AND sys_type = 'MGR';
UPDATE t_sys_role SET role_name = '系統管理員' WHERE role_id = 'ROLE_ADMIN';
