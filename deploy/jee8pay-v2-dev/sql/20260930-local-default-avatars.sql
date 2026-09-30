-- 測試環境（jee8pay-v2-dev）：預設頭像改用 UI 本地圖片（/imgs/defava_*.png），不再指向上游北京 OSS。
-- 只改仍為上游預設圖的帳號；使用者自行上傳的頭像不受影響。正式環境未執行。
UPDATE t_sys_user SET avatar_url = '/imgs/defava_m.png' WHERE avatar_url LIKE '%jeequan.oss-cn-beijing.aliyuncs.com/jeepay/img/defava_m.png';
UPDATE t_sys_user SET avatar_url = '/imgs/defava_f.png' WHERE avatar_url LIKE '%jeequan.oss-cn-beijing.aliyuncs.com/jeepay/img/defava_f.png';
SELECT avatar_url, COUNT(*) AS users FROM t_sys_user GROUP BY avatar_url;
