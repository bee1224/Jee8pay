-- 測試環境（jee8pay-v2-dev）專用：移除中國/海外舊通道的設定資料。
-- 僅刪除設定類資料；不刪除任何訂單、退款、轉帳、分帳或通知紀錄（保留稽核軌跡）。
-- 正式環境（jee8pay-v2-production）不得執行本檔。
-- 執行方式：以 DB root 在 jee8pay_v2_dev 資料庫執行；先看「BEFORE」筆數，確認後 COMMIT 已包含在檔案內。

SET @legacy_if_codes := 'alipay,wxpay,ysfpay,xxpay,plspay,pppay';

SELECT 'BEFORE' AS phase,
  (SELECT COUNT(*) FROM t_pay_way WHERE way_code NOT IN ('RYO_IBON','JAY_IBON','CHI_IBON','JHD_IBON')) AS legacy_pay_way,
  (SELECT COUNT(*) FROM t_pay_interface_define WHERE FIND_IN_SET(if_code, @legacy_if_codes)) AS legacy_if_define,
  (SELECT COUNT(*) FROM t_pay_interface_config WHERE FIND_IN_SET(if_code, @legacy_if_codes)) AS legacy_if_config,
  (SELECT COUNT(*) FROM t_mch_pay_passage WHERE FIND_IN_SET(if_code, @legacy_if_codes)) AS legacy_passage,
  (SELECT COUNT(*) FROM t_sys_entitlement WHERE ent_id = 'ENT_MCH_TRANSFER_CHANNEL_USER') AS legacy_entitlement;

START TRANSACTION;

DELETE FROM t_mch_pay_passage WHERE FIND_IN_SET(if_code, @legacy_if_codes);
DELETE FROM t_pay_interface_config WHERE FIND_IN_SET(if_code, @legacy_if_codes);
DELETE FROM t_pay_interface_define WHERE FIND_IN_SET(if_code, @legacy_if_codes);
DELETE FROM t_pay_way WHERE way_code NOT IN ('RYO_IBON','JAY_IBON','CHI_IBON','JHD_IBON');
DELETE FROM t_sys_role_ent_rela WHERE ent_id = 'ENT_MCH_TRANSFER_CHANNEL_USER';
DELETE FROM t_sys_entitlement WHERE ent_id = 'ENT_MCH_TRANSFER_CHANNEL_USER';

-- 台灣四個上游的顯示名稱改為繁體（與 init.sql 一致）
UPDATE t_pay_way SET way_name = CONCAT(SUBSTRING_INDEX(way_code, '_', 1), ' ibon 繳款')
  WHERE way_code IN ('RYO_IBON','JAY_IBON','CHI_IBON','JHD_IBON');
UPDATE t_pay_interface_define SET if_name = CONCAT(UPPER(if_code), '（黑貓 PAY）')
  WHERE if_code IN ('ryo','jay','chi','jhd');

COMMIT;

SELECT 'AFTER' AS phase,
  (SELECT COUNT(*) FROM t_pay_way) AS pay_way_total,
  (SELECT GROUP_CONCAT(way_code ORDER BY way_code) FROM t_pay_way) AS pay_way_codes,
  (SELECT GROUP_CONCAT(if_code ORDER BY if_code) FROM t_pay_interface_define) AS if_codes,
  (SELECT COUNT(*) FROM t_pay_interface_config WHERE FIND_IN_SET(if_code, @legacy_if_codes)) AS legacy_if_config_left,
  (SELECT COUNT(*) FROM t_mch_pay_passage WHERE FIND_IN_SET(if_code, @legacy_if_codes)) AS legacy_passage_left;
