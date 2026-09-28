-- 測試環境（jee8pay-v2-dev）：補上 JHD（金匯達）介面定義與支付方式，與 init.sql 一致。
-- JHD 先前只加入正式環境；本檔只建立定義，不含任何 Provider 憑證（custId/apiPassword 需另行以 Manager 或 secret intake 設定）。
-- 可重複執行（INSERT IGNORE）。已於 2026-09-29 在 jee8pay_v2_dev 執行。正式環境未執行。
INSERT IGNORE INTO t_pay_way (way_code, way_name) VALUES ('JHD_IBON', 'JHD ibon 繳款');
INSERT IGNORE INTO t_pay_interface_define (if_code, if_name, is_mch_mode, is_isv_mode, config_page_type, isv_params, isvsub_mch_params, normal_mch_params, way_codes, icon, bg_color, state, remark)
VALUES ('jhd', 'JHD（黑貓 PAY）', 1, 0, 1,
        NULL,
        NULL,
        '[{"name":"environment","desc":"Provider 環境","type":"radio","verify":"required","values":"TEST,PRODUCTION","titles":"測試環境,正式環境"},{"name":"custId","desc":"契客代號","type":"text","verify":"required"},{"name":"apiPassword","desc":"API 密碼","type":"text","verify":"required","star":"1"}]',
        '[{"wayCode":"JHD_IBON"}]',
        '', '#222222', 1, '黑貓 PAY ibon 通道（上游四）');
SELECT GROUP_CONCAT(if_code ORDER BY if_code) if_codes FROM t_pay_interface_define;
SELECT GROUP_CONCAT(way_code ORDER BY way_code) way_codes FROM t_pay_way;
