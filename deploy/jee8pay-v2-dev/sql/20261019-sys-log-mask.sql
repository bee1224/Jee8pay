-- 2026-10-19 測試環境：遮罩操作紀錄裡既有的密碼與金鑰。
-- 原因：MethodLogAop 原本把請求參數原樣寫入 t_sys_log.opt_req_param，登入（ip = Base64 密碼）、修改密碼、渠道金鑰（ifParams）都被保存。
-- 程式已改為寫入前遮罩；這支 SQL 處理既有資料。可重複執行，不可回復（遮罩後原值即消失，這正是目的）。
UPDATE t_sys_log
SET opt_req_param = REGEXP_REPLACE(opt_req_param,
    '"(ip|vc|password|pwd|loginPwd|originalPwd|newPwd|confirmPwd|ifParams|apiToken|botToken|token|iToken|secret|appSecret|privateKey|apiPassword)":"([^"\\\\]|\\\\.)*"',
    '"\\1":"******"')
WHERE opt_req_param REGEXP '"(ip|vc|password|pwd|loginPwd|originalPwd|newPwd|confirmPwd|ifParams|apiToken|botToken|token|iToken|secret|appSecret|privateKey|apiPassword)":"';
