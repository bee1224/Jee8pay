-- Runs only after the canonical fresh-schema init.sql in the V2-only database.
-- Production operator provisioning is a separate human-controlled action.
DELETE FROM t_sys_user_role_rela WHERE user_id = 801;
DELETE FROM t_sys_user_auth WHERE user_id = 801;
DELETE FROM t_sys_user WHERE sys_user_id = 801;

UPDATE t_sys_config
SET config_val = 'https://ccat-v2.lp33ing.com'
WHERE config_key = 'paySiteUrl';

