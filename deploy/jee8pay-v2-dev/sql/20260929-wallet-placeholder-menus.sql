-- 測試環境（jee8pay-v2-dev）：錢包與提現（P0 規劃中功能）的選單佔位。
-- 選單本身即為開關：刪除這四列即可整體隱藏；頁面為純前端佔位，不呼叫任何 API。
-- 超管（is_admin=1）自動取得全部權限碼。已於 2026-09-29 在 jee8pay_v2_dev 執行。正式環境未執行。
insert into t_sys_entitlement values('ENT_WALLET', '錢包與提現（規劃中）', 'wallet', '', 'RouteView', 'ML', 0, 1,  'ROOT', '55', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_LEDGER', '餘額流水', 'account-book', '/wallet/ledger', 'WalletLedgerPage', 'ML', 0, 1,  'ENT_WALLET', '10', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_WITHDRAW', '提現審核', 'audit', '/wallet/withdraw', 'WithdrawAuditPage', 'ML', 0, 1,  'ENT_WALLET', '20', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_MCH_WALLET', '我的錢包（規劃中）', 'wallet', '/wallet', 'MchWalletPage', 'ML', 0, 1,  'ENT_MCH_CENTER', '40', 'MCH', now(), now());
