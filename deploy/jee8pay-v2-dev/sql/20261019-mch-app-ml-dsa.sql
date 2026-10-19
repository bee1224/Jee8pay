-- 2026-10-19 測試環境：商戶 API 簽名改用 ML-DSA-65，移除 MD5（ADR-0013）。
-- 新增商戶公鑰欄位、刪除 MD5 共用密鑰欄位。可重複執行。
-- 執行後所有商戶應用都沒有公鑰，商戶必須產生 ML-DSA-65 金鑰並在後台上傳公鑰後才能呼叫 API；
-- 後台代商戶呼叫（退款、渠道測試）改用平台內部金鑰，不受影響。

ALTER TABLE t_mch_app ADD COLUMN IF NOT EXISTS `mch_public_key` TEXT DEFAULT NULL
    COMMENT '商戶 ML-DSA-65 公鑰（PEM），驗證商戶請求簽名（ADR-0013）' AFTER `state`;
ALTER TABLE t_mch_app DROP COLUMN IF EXISTS `app_secret`;

-- 回滾（舊版程式需要 app_secret；原本的密鑰值無法還原，請從部署前備份還原，或重新產生後交給商戶）：
-- ALTER TABLE t_mch_app ADD COLUMN `app_secret` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '应用私钥' AFTER `state`;
-- ALTER TABLE t_mch_app DROP COLUMN `mch_public_key`;
