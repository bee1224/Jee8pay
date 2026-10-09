-- 測試環境（jee8pay-v2-dev）：移除推薦佣金。手續費回到四層（平臺費、渠道費、高代費、代理費）。
-- 必須在新版 payment、manager、merchant 都上線之後才執行：舊版程式仍會讀寫這些欄位。
-- 執行前確認沒有推薦人綁定、推薦佣金規則與已入帳的推薦佣金；有的話先停下來處理。

SELECT (SELECT COUNT(*) FROM t_agent_mch_rela WHERE referrer_agent_no IS NOT NULL) AS referrer_bound,
       (SELECT COUNT(*) FROM t_fee_rule WHERE layer = 'REFERRER') AS referrer_rules,
       (SELECT COUNT(*) FROM t_fee_template_item WHERE layer = 'REFERRER') AS referrer_template_items,
       (SELECT COUNT(*) FROM t_pay_order_fee WHERE referrer_fee <> 0) AS orders_with_referrer_fee;

DELETE FROM t_fee_rule WHERE layer = 'REFERRER';
DELETE FROM t_fee_template_item WHERE layer = 'REFERRER';
ALTER TABLE t_agent_mch_rela DROP COLUMN IF EXISTS referrer_agent_no;
ALTER TABLE t_pay_order_fee DROP COLUMN IF EXISTS referrer_agent_no, DROP COLUMN IF EXISTS referrer_fee;

-- 回滾（欄位可加回，但已刪除的規則需由備份還原）：
-- ALTER TABLE t_agent_mch_rela ADD COLUMN referrer_agent_no VARCHAR(64) DEFAULT NULL COMMENT '推薦人代理號（選填）' AFTER agent_no;
-- ALTER TABLE t_pay_order_fee ADD COLUMN referrer_agent_no VARCHAR(64) DEFAULT NULL COMMENT '推薦人代理號（下單當下）', ADD COLUMN referrer_fee BIGINT(20) NOT NULL DEFAULT 0 COMMENT '推薦佣金，單位分';
