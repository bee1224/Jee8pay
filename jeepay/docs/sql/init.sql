#####  表结构及初始化数据SQL  #####

--   RBAC设计思路：  [用户] 1<->N [角色] 1<->N [权限]

-- 权限表
DROP TABLE IF EXISTS `t_sys_entitlement`;
CREATE TABLE `t_sys_entitlement` (
  `ent_id` VARCHAR(64) NOT NULL COMMENT '权限ID[ENT_功能模块_子模块_操作], eg: ENT_ROLE_LIST_ADD',
  `ent_name` VARCHAR(32) NOT NULL COMMENT '权限名称',
  `menu_icon` VARCHAR(32) COMMENT '菜单图标',
  `menu_uri` VARCHAR(128) COMMENT '菜单uri/路由地址',
  `component_name` VARCHAR(32) COMMENT '组件Name（前后端分离使用）',
  `ent_type` CHAR(2) NOT NULL COMMENT '权限类型 ML-左侧显示菜单, MO-其他菜单, PB-页面/按钮',
  `quick_jump` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '快速开始菜单 0-否, 1-是',
  `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '状态 0-停用, 1-启用',
  `pid` VARCHAR(32) NOT NULL COMMENT '父ID',
  `ent_sort` INT(11) NOT NULL DEFAULT 0 COMMENT '排序字段, 规则：正序',
  `sys_type` VARCHAR(8) NOT NULL COMMENT '所属系统： MGR-运营平台, MCH-商户中心',
  `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ent_id`, `sys_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 角色表
DROP TABLE IF EXISTS `t_sys_role`;
CREATE TABLE `t_sys_role` (
  `role_id` VARCHAR(32) NOT NULL COMMENT '角色ID, ROLE_开头',
  `role_name` VARCHAR(32) NOT NULL COMMENT '角色名称',
  `sys_type` VARCHAR(8) NOT NULL COMMENT '所属系统： MGR-运营平台, MCH-商户中心',
  `belong_info_id` VARCHAR(64) NOT NULL DEFAULT '0' COMMENT '所属商户ID / 0(平台)',
  `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 角色<->权限 关联表
DROP TABLE IF EXISTS `t_sys_role_ent_rela`;
CREATE TABLE `t_sys_role_ent_rela` (
  `role_id` VARCHAR(32) NOT NULL COMMENT '角色ID',
  `ent_id` VARCHAR(64) NOT NULL COMMENT '权限ID' ,
  PRIMARY KEY (`role_id`, `ent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色权限关联表';

-- 系统用户表
DROP TABLE IF EXISTS `t_sys_user`;
CREATE TABLE `t_sys_user` (
	`sys_user_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '系统用户ID',
    `login_username` VARCHAR(32) NOT NULL COMMENT '登录用户名',
	`realname` VARCHAR(32) NOT NULL COMMENT '真实姓名',
	`telphone` VARCHAR(32) NOT NULL COMMENT '手机号',
	`sex` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '性别 0-未知, 1-男, 2-女',
	`avatar_url` VARCHAR(128) COMMENT '头像地址',
    `user_no` VARCHAR(32) COMMENT '员工编号',
    `is_admin` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否超管（超管拥有全部权限） 0-否 1-是',
    `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '状态 0-停用 1-启用',
    `sys_type` VARCHAR(8) NOT NULL COMMENT '所属系统： MGR-运营平台, MCH-商户中心',
    `belong_info_id` VARCHAR(64) NOT NULL DEFAULT '0' COMMENT '所属商户ID / 0(平台)',
	`created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
	PRIMARY KEY (`sys_user_id`),
    UNIQUE KEY(`sys_type`,`login_username`),
    UNIQUE KEY(`sys_type`,`telphone`),
    UNIQUE KEY(`sys_type`, `user_no`)
) ENGINE=InnoDB AUTO_INCREMENT=100001 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 系统用户认证表
DROP TABLE IF EXISTS `t_sys_user_auth`;
CREATE TABLE `t_sys_user_auth` (
	`auth_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
	`user_id` BIGINT(20) NOT NULL COMMENT 'user_id',
	`identity_type` TINYINT(6) NOT NULL DEFAULT '0' COMMENT '登录类型  1-登录账号 2-手机号 3-邮箱  10-微信  11-QQ 12-支付宝 13-微博',
	`identifier` VARCHAR(128) NOT NULL COMMENT '认证标识 ( 用户名 | open_id )',
	`credential` VARCHAR(128) NOT NULL COMMENT '密码凭证',
	`salt` VARCHAR(128) NOT NULL COMMENT 'salt',
    `sys_type` VARCHAR(8) NOT NULL COMMENT '所属系统： MGR-运营平台, MCH-商户中心',
	PRIMARY KEY (`auth_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1001 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户认证表';

-- 操作员<->角色 关联表
DROP TABLE IF EXISTS `t_sys_user_role_rela`;
CREATE TABLE `t_sys_user_role_rela` (
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `role_id`VARCHAR(32) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作员<->角色 关联表';


-- 系统配置表
DROP TABLE IF EXISTS `t_sys_config`;
CREATE TABLE `t_sys_config` (
    `config_key` VARCHAR(50) NOT NULL COMMENT '配置KEY',
    `config_name` VARCHAR(50) NOT NULL COMMENT '配置名称',
    `config_desc` VARCHAR(200) NOT NULL COMMENT '描述信息',
    `group_key` VARCHAR(50) NOT NULL COMMENT '分组key',
    `group_name` VARCHAR(50) NOT NULL COMMENT '分组名称',
    `config_val` TEXT NOT NULL COMMENT '配置内容项',
    `type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '类型: text-输入框, textarea-多行文本, uploadImg-上传图片, switch-开关',
    `sort_num` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 系统操作日志表
DROP TABLE IF EXISTS `t_sys_log`;
CREATE TABLE `t_sys_log` (
  `sys_log_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint(20) DEFAULT NULL COMMENT '系统用户ID',
  `user_name` varchar(32) DEFAULT NULL COMMENT '用户姓名',
  `user_ip` varchar(128) NOT NULL DEFAULT '' COMMENT '用户IP',
  `sys_type` varchar(8) NOT NULL COMMENT '所属系统： MGR-运营平台, MCH-商户中心',
  `method_name` varchar(128) NOT NULL DEFAULT '' COMMENT '方法名',
  `method_remark` varchar(128) NOT NULL DEFAULT '' COMMENT '方法描述',
  `req_url` varchar(256) NOT NULL DEFAULT '' COMMENT '请求地址',
  `opt_req_param` TEXT DEFAULT NULL COMMENT '操作请求参数',
  `opt_res_info` TEXT DEFAULT NULL COMMENT '操作响应结果',
  `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`sys_log_id`)
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COMMENT = '系统操作日志表';

-- 商户信息表
DROP TABLE IF EXISTS t_mch_info;
CREATE TABLE `t_mch_info` (
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
        `mch_name` VARCHAR(64) NOT NULL COMMENT '商户名称',
        `mch_short_name` VARCHAR(32) NOT NULL COMMENT '商户简称',
        `type` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '类型: 1-普通商户, 2-特约商户(服务商模式)',
        `isv_no` VARCHAR(64) COMMENT '服务商号',
        `contact_name` VARCHAR(32) COMMENT '联系人姓名',
        `contact_tel` VARCHAR(32) COMMENT '联系人手机号',
        `contact_email` VARCHAR(32) COMMENT '联系人邮箱',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '商户状态: 0-停用, 1-正常',
        `remark` VARCHAR(128) COMMENT '商户备注',
        `init_user_id` BIGINT(20) DEFAULT NULL COMMENT '初始用户ID（创建商户时，允许商户登录的用户）',
        `created_uid` BIGINT(20) COMMENT '创建者用户ID',
        `created_by` VARCHAR(64) COMMENT '创建者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
        PRIMARY KEY (`mch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户信息表';

-- 商户应用表
DROP TABLE IF EXISTS t_mch_app;
CREATE TABLE `t_mch_app` (
         `app_id` varchar(64) NOT NULL COMMENT '应用ID',
         `app_name` varchar(64) NOT NULL DEFAULT '' COMMENT '应用名称',
         `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
         `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '应用状态: 0-停用, 1-正常',
         `app_secret` VARCHAR(128) NOT NULL COMMENT '应用私钥',
         `remark` varchar(128) DEFAULT NULL COMMENT '备注',
         `created_uid` BIGINT(20) COMMENT '创建者用户ID',
         `created_by` VARCHAR(64) COMMENT '创建者姓名',
         `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
         `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
         PRIMARY KEY (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户应用表';

-- 服务商信息表
DROP TABLE IF EXISTS t_isv_info;
CREATE TABLE `t_isv_info` (
        `isv_no` VARCHAR(64) NOT NULL COMMENT '服务商号',
        `isv_name` VARCHAR(64) NOT NULL COMMENT '服务商名称',
        `isv_short_name` VARCHAR(32) NOT NULL COMMENT '服务商简称',
        `contact_name` VARCHAR(32) COMMENT '联系人姓名',
        `contact_tel` VARCHAR(32) COMMENT '联系人手机号',
        `contact_email` VARCHAR(32) COMMENT '联系人邮箱',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-正常',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '备注',
        `created_uid` BIGINT(20) COMMENT '创建者用户ID',
        `created_by` VARCHAR(64) COMMENT '创建者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
        PRIMARY KEY (`isv_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务商信息表';

-- 代理資訊表（ADR-0009：代理為獨立實體，團長 → 隊長 兩層）
CREATE TABLE `t_agent_info` (
        `agent_no` VARCHAR(64) NOT NULL COMMENT '代理號',
        `agent_name` VARCHAR(64) NOT NULL COMMENT '代理名稱',
        `agent_level` TINYINT(6) NOT NULL COMMENT '代理層級: 1-團長, 2-隊長',
        `parent_agent_no` VARCHAR(64) DEFAULT NULL COMMENT '上級代理號（團長為空）',
        `agent_path` VARCHAR(512) NOT NULL COMMENT '物化路徑，如 /A001/A002/，用於查詢轄區',
        `contact_name` VARCHAR(32) DEFAULT NULL COMMENT '聯絡人姓名',
        `contact_tel` VARCHAR(32) DEFAULT NULL COMMENT '聯絡人手機號',
        `contact_email` VARCHAR(64) DEFAULT NULL COMMENT '聯絡人信箱',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-正常',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `created_uid` BIGINT(20) DEFAULT NULL COMMENT '建立者用戶ID',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '建立者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        `brand_enabled` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '白標是否啟用: 0-否, 1-是（僅團長）',
        `brand_title` VARCHAR(32) DEFAULT NULL COMMENT '白標站台名稱',
        `brand_logo` VARCHAR(255) DEFAULT NULL COMMENT '白標 Logo 圖片位址',
        `is_house` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否平台直屬: 0-否, 1-是（ADR-0012）',
        PRIMARY KEY (`agent_no`),
        KEY `idx_parent_agent_no` (`parent_agent_no`),
        KEY `idx_agent_path` (`agent_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理資訊表';

-- 渠道帳號表（ADR-0012：一組第三方支付金鑰，屬於一位團長；金鑰存 t_pay_interface_config，info_type=4）
CREATE TABLE `t_channel_account` (
        `account_id` VARCHAR(64) NOT NULL COMMENT '渠道帳號ID',
        `if_code` VARCHAR(20) NOT NULL COMMENT '支付接口代碼',
        `account_name` VARCHAR(64) NOT NULL COMMENT '帳號名稱',
        `owner_sr_agent_no` VARCHAR(64) NOT NULL COMMENT '所屬團長代理號',
        `shareable` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否可加派給其他團長: 0-否, 1-是',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-啟用',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `created_uid` BIGINT(20) DEFAULT NULL COMMENT '建立者用戶ID',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '建立者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`account_id`),
        KEY `idx_owner_sr_agent_no` (`owner_sr_agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道帳號表';

-- 渠道帳號派發表（哪些團長可以使用；擁有者固定有一列）
CREATE TABLE `t_channel_account_agent` (
        `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
        `account_id` VARCHAR(64) NOT NULL COMMENT '渠道帳號ID',
        `sr_agent_no` VARCHAR(64) NOT NULL COMMENT '被派發的團長代理號',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '派發者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '派發時間',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_account_agent` (`account_id`, `sr_agent_no`),
        KEY `idx_sr_agent_no` (`sr_agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道帳號派發表';

-- 商戶與代理綁定表（每個商戶一個直屬代理）
CREATE TABLE `t_agent_mch_rela` (
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商戶號',
        `agent_no` VARCHAR(64) NOT NULL COMMENT '直屬代理號',
        `updated_uid` BIGINT(20) DEFAULT NULL COMMENT '最後修改者用戶ID',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`mch_no`),
        KEY `idx_agent_no` (`agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商戶與代理綁定表';

-- 四層手續費規則表（平臺／渠道／團長／代理；每層百分比＋單筆固定金額）
CREATE TABLE `t_fee_rule` (
        `rule_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '規則ID',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `target_type` VARCHAR(16) NOT NULL COMMENT '對象類型: DEFAULT-平台預設, AGENT-代理, MCH-單一商戶覆寫',
        `target_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '對象ID（DEFAULT 為空字串）',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層: PLATFORM-平臺費, CHANNEL-渠道費, SR_AGENT-團長費, AGENT-隊長費',
        `rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '費率（比例，0.006 即 0.6%）',
        `fixed_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '單筆固定金額（分）',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-啟用',
        `updated_uid` BIGINT(20) DEFAULT NULL COMMENT '最後修改者用戶ID',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`rule_id`),
        UNIQUE KEY `uni_rule` (`way_code`, `target_type`, `target_id`, `layer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='四層手續費規則表';

-- 手續費規則變更紀錄（誰、何時、改了什麼；費率屬動錢設定，須可稽核）
CREATE TABLE `t_fee_rule_log` (
        `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '紀錄ID',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `target_type` VARCHAR(16) NOT NULL COMMENT '對象類型',
        `target_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '對象ID',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層',
        `action` VARCHAR(16) NOT NULL COMMENT '動作: SAVE-新增或修改, DELETE-刪除',
        `before_value` VARCHAR(256) DEFAULT NULL COMMENT '變更前（JSON）',
        `after_value` VARCHAR(256) DEFAULT NULL COMMENT '變更後（JSON）',
        `operator_uid` BIGINT(20) DEFAULT NULL COMMENT '操作者用戶ID',
        `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`log_id`),
        KEY `idx_rule_key` (`way_code`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手續費規則變更紀錄';

-- 訂單四層手續費快照（ADR-0009 第二階段；旁表，不修改 t_pay_order；建立後不可變）
CREATE TABLE `t_pay_order_fee` (
        `pay_order_id` VARCHAR(30) NOT NULL COMMENT '支付訂單號',
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商戶號',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `amount` BIGINT(20) NOT NULL COMMENT '訂單金額，單位分',
        `mch_fee_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '下單時商戶手續費（支付通道費率），單位分',
        `agent_no` VARCHAR(64) DEFAULT NULL COMMENT '直屬代理號（下單當下）',
        `sr_agent_no` VARCHAR(64) DEFAULT NULL COMMENT '團長號（下單當下）',
        `platform_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '平臺費，單位分',
        `channel_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '渠道費，單位分',
        `sr_agent_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '團長費，單位分',
        `agent_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '隊長費，單位分',
        `total_fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '各層合計，單位分',
        `exceeds_mch_fee` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '四層合計是否超過商戶手續費: 0-否, 1-是',
        `detail` VARCHAR(1024) DEFAULT NULL COMMENT '各層費率、固定金額與規則來源（JSON）',
        `settle_state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '結算狀態: 0-未結算, 1-已結算, 2-已沖回, 3-不結算',
        `settled_at` DATETIME DEFAULT NULL COMMENT '結算（或沖回）時間',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`pay_order_id`),
        KEY `idx_settle_state` (`settle_state`),
        KEY `idx_sr_agent_no` (`sr_agent_no`),
        KEY `idx_agent_no` (`agent_no`),
        KEY `idx_mch_no` (`mch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='訂單手續費快照';

-- 費率範本（ADR-0009 第四階段；只含團長費／隊長費）
CREATE TABLE `t_fee_template` (
        `template_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '範本ID',
        `template_name` VARCHAR(64) NOT NULL COMMENT '範本名稱',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `updated_uid` BIGINT(20) DEFAULT NULL COMMENT '最後修改者用戶ID',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者姓名',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='費率範本';

CREATE TABLE `t_fee_template_item` (
        `item_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '明細ID',
        `template_id` BIGINT(20) NOT NULL COMMENT '範本ID',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層: SR_AGENT/AGENT',
        `rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '費率（比率）',
        `fixed_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '單筆固定金額，單位分',
        PRIMARY KEY (`item_id`),
        UNIQUE KEY `uni_template_item` (`template_id`, `way_code`, `layer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='費率範本明細';

-- 平臺費／渠道費變更申請（雙人覆核；核准後才寫入 t_fee_rule）
CREATE TABLE `t_fee_rule_change_req` (
        `req_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申請ID',
        `action` VARCHAR(16) NOT NULL COMMENT '動作: SAVE/DELETE',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代碼',
        `target_type` VARCHAR(16) NOT NULL COMMENT '對象類型: DEFAULT/MCH',
        `target_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '對象ID',
        `layer` VARCHAR(16) NOT NULL COMMENT '費率層: PLATFORM/CHANNEL',
        `rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '申請費率（比率）',
        `fixed_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '申請單筆固定金額，單位分',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-待覆核, 1-已核准, 2-已駁回',
        `requester_uid` BIGINT(20) NOT NULL COMMENT '申請人用戶ID',
        `requester_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人姓名',
        `reviewer_uid` BIGINT(20) DEFAULT NULL COMMENT '覆核人用戶ID',
        `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '覆核人姓名',
        `review_remark` VARCHAR(128) DEFAULT NULL COMMENT '覆核意見',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '申請時間',
        `reviewed_at` DATETIME DEFAULT NULL COMMENT '覆核時間',
        PRIMARY KEY (`req_id`),
        KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平臺費變更申請（雙人覆核）';

-- 錢包帳戶（ADR-0010）：可用與凍結分桶；只能經 WalletService 記帳異動
CREATE TABLE `t_wallet_account` (
        `account_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '帳戶ID',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型: MCH/AGENT/PLATFORM/CHANNEL',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID（商戶號／代理號／PLATFORM／ifCode）',
        `balance` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '可用餘額，單位分',
        `frozen` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '凍結金額，單位分',
        `total_in` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '累計入帳，單位分',
        `total_out` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '累計出帳，單位分',
        `payout_bank_name` VARCHAR(64) DEFAULT NULL COMMENT '提現銀行名稱',
        `payout_bank_code` VARCHAR(8) DEFAULT NULL COMMENT '提現銀行代碼',
        `payout_branch` VARCHAR(64) DEFAULT NULL COMMENT '提現分行',
        `payout_account_no` VARCHAR(32) DEFAULT NULL COMMENT '提現帳號',
        `payout_account_name` VARCHAR(64) DEFAULT NULL COMMENT '提現戶名',
        `payout_updated_at` DATETIME DEFAULT NULL COMMENT '收款帳戶最後變更時間',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`account_id`),
        UNIQUE KEY `uni_owner` (`owner_type`, `owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='錢包帳戶';

-- 錢包流水（只增不改；唯一鍵保證結算與提現冪等）
CREATE TABLE `t_wallet_ledger` (
        `ledger_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '流水ID',
        `account_id` BIGINT(20) NOT NULL COMMENT '帳戶ID',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID',
        `biz_type` VARCHAR(32) NOT NULL COMMENT '業務類型: ORDER_SETTLE/ORDER_REVERSE/WITHDRAW_APPLY/WITHDRAW_RELEASE/WITHDRAW_PAID/WITHDRAW_FEE/ADJUST',
        `biz_id` VARCHAR(64) NOT NULL COMMENT '業務單號',
        `amount` BIGINT(20) NOT NULL COMMENT '可用餘額變動（正入負出），單位分',
        `frozen_change` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '凍結金額變動，單位分',
        `balance_before` BIGINT(20) NOT NULL COMMENT '變動前可用餘額',
        `balance_after` BIGINT(20) NOT NULL COMMENT '變動後可用餘額',
        `frozen_after` BIGINT(20) NOT NULL COMMENT '變動後凍結金額',
        `remark` VARCHAR(256) DEFAULT NULL COMMENT '說明',
        `operator_uid` BIGINT(20) DEFAULT NULL COMMENT '操作者用戶ID',
        `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作者',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`ledger_id`),
        UNIQUE KEY `uni_biz` (`account_id`, `biz_type`, `biz_id`),
        KEY `idx_owner` (`owner_type`, `owner_id`, `ledger_id`),
        KEY `idx_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='錢包流水';

-- 提現單（平台人工匯款）
CREATE TABLE `t_withdraw_order` (
        `withdraw_id` VARCHAR(32) NOT NULL COMMENT '提現單號',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型: MCH/AGENT',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID',
        `req_no` VARCHAR(64) NOT NULL COMMENT '申請端冪等鍵',
        `amount` BIGINT(20) NOT NULL COMMENT '申請金額，單位分',
        `fee` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '提現手續費，單位分',
        `actual_amount` BIGINT(20) NOT NULL COMMENT '實際匯款金額，單位分',
        `bank_name` VARCHAR(64) NOT NULL COMMENT '銀行名稱',
        `bank_code` VARCHAR(8) NOT NULL COMMENT '銀行代碼',
        `branch` VARCHAR(64) DEFAULT NULL COMMENT '分行',
        `account_no` VARCHAR(32) NOT NULL COMMENT '帳號',
        `account_name` VARCHAR(64) NOT NULL COMMENT '戶名',
        `risk_flags` VARCHAR(128) DEFAULT NULL COMMENT '風控提示: BLACKLIST/DAILY_LIMIT/RESTRICTED_BANK/NEW_ACCOUNT',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-待審核, 1-已撥款, 2-已駁回, 3-已取消',
        `apply_uid` BIGINT(20) DEFAULT NULL COMMENT '申請人用戶ID',
        `apply_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人',
        `reviewer_uid` BIGINT(20) DEFAULT NULL COMMENT '審核人用戶ID',
        `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '審核人',
        `paid_ref` VARCHAR(64) DEFAULT NULL COMMENT '匯款單號',
        `review_remark` VARCHAR(128) DEFAULT NULL COMMENT '審核說明',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '申請時間',
        `reviewed_at` DATETIME DEFAULT NULL COMMENT '審核時間',
        `agent_approve_by` VARCHAR(64) DEFAULT NULL COMMENT '上級代理同意人（僅註記，撥款仍由平台審核）',
        `agent_approve_at` DATETIME DEFAULT NULL COMMENT '上級代理同意時間',
        PRIMARY KEY (`withdraw_id`),
        UNIQUE KEY `uni_req` (`owner_type`, `owner_id`, `req_no`),
        KEY `idx_state` (`state`, `created_at`),
        KEY `idx_account_no` (`account_no`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提現單';

-- 人工調帳申請（雙人覆核）
CREATE TABLE `t_wallet_adjust_req` (
        `req_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申請ID',
        `account_id` BIGINT(20) NOT NULL COMMENT '帳戶ID',
        `owner_type` VARCHAR(16) NOT NULL COMMENT '擁有者類型',
        `owner_id` VARCHAR(64) NOT NULL COMMENT '擁有者ID',
        `amount` BIGINT(20) NOT NULL COMMENT '調整金額（正加負減），單位分',
        `reason` VARCHAR(128) NOT NULL COMMENT '調帳原因',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-待覆核, 1-已核准, 2-已駁回',
        `requester_uid` BIGINT(20) NOT NULL COMMENT '申請人用戶ID',
        `requester_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人',
        `reviewer_uid` BIGINT(20) DEFAULT NULL COMMENT '覆核人用戶ID',
        `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '覆核人',
        `review_remark` VARCHAR(128) DEFAULT NULL COMMENT '覆核意見',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '申請時間',
        `reviewed_at` DATETIME DEFAULT NULL COMMENT '覆核時間',
        PRIMARY KEY (`req_id`),
        KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工調帳申請';

-- 風控黑名單（GLOBAL 或團長範圍）
CREATE TABLE `t_risk_blacklist` (
        `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
        `list_type` VARCHAR(16) NOT NULL COMMENT '類型: BANK_ACCOUNT/ACCOUNT_NAME/PHONE',
        `list_value` VARCHAR(64) NOT NULL COMMENT '值',
        `scope` VARCHAR(64) NOT NULL DEFAULT 'GLOBAL' COMMENT '範圍: GLOBAL 或團長號',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `created_uid` BIGINT(20) DEFAULT NULL COMMENT '建立者用戶ID',
        `created_by` VARCHAR(64) DEFAULT NULL COMMENT '建立者',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uni_entry` (`list_type`, `list_value`, `scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='風控黑名單';

-- 通道路由規則（ADR-0011）：別名代碼 → 實際支付方式，依金額／時段／權重分流
CREATE TABLE `t_way_route` (
        `route_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '規則ID',
        `alias_way_code` VARCHAR(20) NOT NULL COMMENT '別名代碼（商戶下單用）',
        `target_way_code` VARCHAR(20) NOT NULL COMMENT '實際支付方式代碼',
        `mch_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商戶號，空字串表示全部商戶',
        `min_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '金額下限（含），單位分，0 不限',
        `max_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '金額上限（含），單位分，0 不限',
        `weight` INT(11) NOT NULL DEFAULT 1 COMMENT '權重 1-9',
        `time_start` VARCHAR(5) DEFAULT NULL COMMENT '時段起 HH:mm（台北時間）',
        `time_end` VARCHAR(5) DEFAULT NULL COMMENT '時段迄 HH:mm（不含，可跨午夜）',
        `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '狀態: 0-停用, 1-啟用',
        `remark` VARCHAR(128) DEFAULT NULL COMMENT '備註',
        `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最後修改者',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新時間',
        PRIMARY KEY (`route_id`),
        KEY `idx_alias` (`alias_way_code`, `mch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道路由規則';

-- 路由決策紀錄
CREATE TABLE `t_way_route_log` (
        `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '紀錄ID',
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商戶號',
        `app_id` VARCHAR(64) DEFAULT NULL COMMENT '應用ID',
        `mch_order_no` VARCHAR(64) DEFAULT NULL COMMENT '商戶訂單號',
        `alias_way_code` VARCHAR(20) NOT NULL COMMENT '別名代碼',
        `amount` BIGINT(20) DEFAULT NULL COMMENT '訂單金額，單位分',
        `chosen_way_code` VARCHAR(20) DEFAULT NULL COMMENT '選中的支付方式',
        `candidates` VARCHAR(512) DEFAULT NULL COMMENT '候選與權重（JSON）',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        PRIMARY KEY (`log_id`),
        KEY `idx_mch_order` (`mch_no`, `mch_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路由決策紀錄';

-- 背景匯出工作（下載中心）
CREATE TABLE `t_export_job` (
        `job_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '工作ID',
        `sys_type` VARCHAR(8) NOT NULL COMMENT '所屬系統: MGR/MCH',
        `belong_info_id` VARCHAR(64) NOT NULL DEFAULT '0' COMMENT '所屬（商戶平台為商戶號）',
        `owner_uid` BIGINT(20) NOT NULL COMMENT '申請人用戶ID',
        `owner_name` VARCHAR(64) DEFAULT NULL COMMENT '申請人',
        `job_type` VARCHAR(32) NOT NULL COMMENT '匯出類型',
        `params` VARCHAR(1024) DEFAULT NULL COMMENT '篩選條件（JSON）',
        `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '狀態: 0-排隊中, 1-產生中, 2-完成, 3-失敗',
        `file_name` VARCHAR(128) DEFAULT NULL COMMENT '下載檔名',
        `row_count` BIGINT(20) DEFAULT NULL COMMENT '資料筆數',
        `error_msg` VARCHAR(256) DEFAULT NULL COMMENT '失敗原因',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '建立時間',
        `finished_at` DATETIME DEFAULT NULL COMMENT '完成時間',
        PRIMARY KEY (`job_id`),
        KEY `idx_owner` (`sys_type`, `owner_uid`, `job_id`),
        KEY `idx_state` (`sys_type`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='背景匯出工作';

-- 已關閉訂單補查進度（C5，延伸 ADR-0007；旁表，不修改 t_pay_order）
CREATE TABLE `t_pay_order_audit` (
        `pay_order_id` VARCHAR(30) NOT NULL COMMENT '支付訂單號',
        `audit_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已補查次數',
        `last_result` VARCHAR(32) DEFAULT NULL COMMENT '最後一次查詢結果',
        `reopened` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否轉回支付成功: 0-否, 1-是',
        `last_audit_at` DATETIME DEFAULT NULL COMMENT '最後補查時間',
        PRIMARY KEY (`pay_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='關閉訂單補查紀錄';

-- 支付方式表  pay_way
DROP TABLE IF EXISTS t_pay_way;
CREATE TABLE `t_pay_way` (
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代码  例如： wxpay_jsapi',
        `way_name` VARCHAR(20) NOT NULL COMMENT '支付方式名称',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
        PRIMARY KEY (`way_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付方式表';

-- 支付接口定义表
DROP TABLE IF EXISTS t_pay_interface_define;
CREATE TABLE `t_pay_interface_define` (
          `if_code` VARCHAR(20) NOT NULL COMMENT '接口代码 全小写  wxpay alipay ',
          `if_name` VARCHAR(20) NOT NULL COMMENT '接口名称',
          `is_mch_mode` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '是否支持普通商户模式: 0-不支持, 1-支持',
          `is_isv_mode` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '是否支持服务商子商户模式: 0-不支持, 1-支持',
          `config_page_type` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '支付参数配置页面类型:1-JSON渲染,2-自定义',
          `isv_params` VARCHAR(4096) DEFAULT NULL COMMENT 'ISV接口配置定义描述,json字符串',
          `isvsub_mch_params` VARCHAR(4096) DEFAULT NULL COMMENT '特约商户接口配置定义描述,json字符串',
          `normal_mch_params` VARCHAR(4096) DEFAULT NULL COMMENT '普通商户接口配置定义描述,json字符串',
          `way_codes` JSON NOT NULL COMMENT '支持的支付方式 ["wxpay_jsapi", "wxpay_bar"]',
          `icon` VARCHAR(256) DEFAULT NULL COMMENT '页面展示：卡片-图标',
          `bg_color` VARCHAR(20) DEFAULT NULL COMMENT '页面展示：卡片-背景色',
          `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
          `remark` VARCHAR(128) DEFAULT NULL COMMENT '备注',
          `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
          `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
          PRIMARY KEY (`if_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付接口定义表';

-- 支付接口配置参数表
DROP TABLE IF EXISTS t_pay_interface_config;
CREATE TABLE `t_pay_interface_config` (
          `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
          `info_type` TINYINT(6) NOT NULL COMMENT '账号类型:1-服务商 2-商户 3-商户应用',
          `info_id` VARCHAR(64) NOT NULL COMMENT '服务商号/商户号/应用ID',
          `if_code` VARCHAR(20) NOT NULL COMMENT '支付接口代码',
          `if_params` VARCHAR(4096) NOT NULL COMMENT '接口配置参数,json字符串',
          `if_rate` DECIMAL(20,6) DEFAULT NULL COMMENT '支付接口费率',
          `state` TINYINT(6) NOT NULL default 1 COMMENT '状态: 0-停用, 1-启用',
          `remark` VARCHAR(128) DEFAULT NULL COMMENT '备注',
          `created_uid` BIGINT(20) COMMENT '创建者用户ID',
          `created_by` VARCHAR(64) COMMENT '创建者姓名',
          `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
          `updated_uid` BIGINT(20) COMMENT '更新者用户ID',
          `updated_by` VARCHAR(64) COMMENT '更新者姓名',
          `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
          PRIMARY KEY (`id`),
          UNIQUE KEY `Uni_InfoType_InfoId_IfCode` (`info_type`, `info_id`, `if_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付接口配置参数表';


-- 商户支付通道表 (允许商户  支付方式 对应多个支付接口的配置)
DROP TABLE IF EXISTS t_mch_pay_passage;
CREATE TABLE `t_mch_pay_passage` (
         `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
         `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
         `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
         `if_code` VARCHAR(20) NOT NULL COMMENT '支付接口',
         `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式',
         `rate` DECIMAL(20,6) NOT NULL COMMENT '支付方式费率',
         `risk_config` JSON DEFAULT NULL COMMENT '风控数据',
         `state` TINYINT(6) NOT NULL COMMENT '状态: 0-停用, 1-启用',
         `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
         `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
         PRIMARY KEY (`id`),
         UNIQUE KEY `Uni_AppId_WayCode` (`app_id`,`if_code`, `way_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户支付通道表';


-- 轮询表
-- mch_no, way_code, 轮询策略。


-- 支付订单表
DROP TABLE IF EXISTS t_pay_order;
CREATE TABLE `t_pay_order` (
        `pay_order_id` VARCHAR(30) NOT NULL COMMENT '支付订单号',
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
        `isv_no` VARCHAR(64) DEFAULT NULL COMMENT '服务商号',
        `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
        `mch_name` VARCHAR(30) NOT NULL COMMENT '商户名称',
        `mch_type` TINYINT(6) NOT NULL COMMENT '类型: 1-普通商户, 2-特约商户(服务商模式)',
        `mch_order_no` VARCHAR(64) NOT NULL COMMENT '商户订单号',
        `if_code` VARCHAR(20) COMMENT '支付接口代码',
        `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代码',
        `amount` BIGINT(20) NOT NULL COMMENT '支付金额,单位分',
        `mch_fee_rate` decimal(20,6) NOT NULL COMMENT '商户手续费费率快照',
        `mch_fee_amount` BIGINT(20) NOT NULL COMMENT '商户手续费,单位分',
        `currency` VARCHAR(3) NOT NULL DEFAULT 'twd' COMMENT '三位貨幣代碼，平台預設新臺幣:twd',
        `state` TINYINT(6) NOT NULL DEFAULT '0' COMMENT '支付状态: 0-订单生成, 1-支付中, 2-支付成功, 3-支付失败, 4-已撤销, 5-已退款, 6-订单关闭',
        `notify_state` TINYINT(6) NOT NULL DEFAULT '0' COMMENT '向下游回调状态, 0-未发送,  1-已发送',
        `client_ip` VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
        `subject` VARCHAR(64) NOT NULL COMMENT '商品标题',
        `body` VARCHAR(256) NOT NULL COMMENT '商品描述信息',
        `channel_extra` VARCHAR(512) DEFAULT NULL COMMENT '特定渠道发起额外参数',
        `channel_user` VARCHAR(64) DEFAULT NULL COMMENT '渠道用户标识,如微信openId,支付宝账号',
        `channel_order_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道订单号',
        `refund_state` TINYINT(6) NOT NULL DEFAULT '0' COMMENT '退款状态: 0-未发生实际退款, 1-部分退款, 2-全额退款',
        `refund_times` INT NOT NULL DEFAULT 0 COMMENT '退款次数',
        `refund_amount` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '退款总金额,单位分',
        `division_mode` TINYINT(6) DEFAULT 0 COMMENT '订单分账模式：0-该笔订单不允许分账, 1-支付成功按配置自动完成分账, 2-商户手动分账(解冻商户金额)',
        `division_state` TINYINT(6) DEFAULT 0 COMMENT '订单分账状态：0-未发生分账, 1-等待分账任务处理, 2-分账处理中, 3-分账任务已结束(不体现状态)',
        `division_last_time` DATETIME COMMENT '最新分账时间',
        `err_code` VARCHAR(128) DEFAULT NULL COMMENT '渠道支付错误码',
        `err_msg` VARCHAR(256) DEFAULT NULL COMMENT '渠道支付错误描述',
        `ext_param` VARCHAR(128) DEFAULT NULL COMMENT '商户扩展参数',
        `notify_url` VARCHAR(128) NOT NULL default '' COMMENT '异步通知地址',
        `return_url` VARCHAR(128) DEFAULT '' COMMENT '页面跳转地址',
        `expired_time` DATETIME DEFAULT NULL COMMENT '订单失效时间',
        `success_time` DATETIME DEFAULT NULL COMMENT '订单支付成功时间',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
        PRIMARY KEY (`pay_order_id`),
        UNIQUE KEY `Uni_MchNo_MchOrderNo` (`mch_no`, `mch_order_no`),
        INDEX(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';


-- 商户通知记录表
DROP TABLE IF EXISTS t_mch_notify_record;
CREATE TABLE `t_mch_notify_record` (
        `notify_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '商户通知记录ID',
        `order_id` VARCHAR(64) NOT NULL COMMENT '订单ID',
        `order_type` TINYINT(6) NOT NULL COMMENT '订单类型:1-支付,2-退款',
        `mch_order_no` VARCHAR(64) NOT NULL COMMENT '商户订单号',
        `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
        `isv_no` VARCHAR(64) COMMENT '服务商号',
        `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
        `notify_url` TEXT NOT NULL COMMENT '通知地址',
        `res_result` TEXT DEFAULT NULL COMMENT '通知响应结果',
        `notify_count` INT(11) NOT NULL DEFAULT '0' COMMENT '通知次数',
        `notify_count_limit` INT(11) NOT NULL DEFAULT '6' COMMENT '最大通知次数, 默认6次',
        `state` TINYINT(6) NOT NULL DEFAULT '1' COMMENT '通知状态,1-通知中,2-通知成功,3-通知失败',
        `last_notify_time` DATETIME DEFAULT NULL COMMENT '最后一次通知时间',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
        PRIMARY KEY (`notify_id`),
        UNIQUE KEY `Uni_OrderId_Type` (`order_id`, `order_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1001 DEFAULT CHARSET=utf8mb4 COMMENT='商户通知记录表';


-- 订单接口数据快照（加密存储）
DROP TABLE IF EXISTS `t_order_snapshot`;
CREATE TABLE `t_order_snapshot` (
        `order_id` VARCHAR(64) NOT NULL COMMENT '订单ID',
        `order_type` TINYINT(6) NOT NULL COMMENT '订单类型: 1-支付, 2-退款',
        `mch_req_data` TEXT DEFAULT NULL COMMENT '下游请求数据',
        `mch_req_time` DATETIME DEFAULT NULL COMMENT '下游请求时间',
        `mch_resp_data` TEXT DEFAULT NULL COMMENT '向下游响应数据',
        `mch_resp_time` DATETIME DEFAULT NULL COMMENT '向下游响应时间',
        `channel_req_data` TEXT DEFAULT NULL COMMENT '向上游请求数据',
        `channel_req_time` DATETIME DEFAULT NULL COMMENT '向上游请求时间',
        `channel_resp_data` TEXT DEFAULT NULL COMMENT '上游响应数据',
        `channel_resp_time` DATETIME DEFAULT NULL COMMENT '上游响应时间',
        `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
        PRIMARY KEY (`order_id`, `order_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单接口数据快照';


-- 退款订单表
DROP TABLE IF EXISTS t_refund_order;
CREATE TABLE `t_refund_order` (
          `refund_order_id` VARCHAR(30) NOT NULL COMMENT '退款订单号（支付系统生成订单号）',
          `pay_order_id` VARCHAR(30) NOT NULL COMMENT '支付订单号（与t_pay_order对应）',
          `channel_pay_order_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道支付单号（与t_pay_order channel_order_no对应）',
          `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
          `isv_no` VARCHAR(64) COMMENT '服务商号',
          `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
          `mch_name` VARCHAR(30) NOT NULL COMMENT '商户名称',
          `mch_type` TINYINT(6) NOT NULL COMMENT '类型: 1-普通商户, 2-特约商户(服务商模式)',
          `mch_refund_no` VARCHAR(64) NOT NULL COMMENT '商户退款单号（商户系统的订单号）',
          `way_code` VARCHAR(20) NOT NULL COMMENT '支付方式代码',
          `if_code` VARCHAR(20) NOT NULL COMMENT '支付接口代码',
          `pay_amount` BIGINT(20) NOT NULL COMMENT '支付金额,单位分',
          `refund_amount` BIGINT(20) NOT NULL COMMENT '退款金额,单位分',
          `currency` VARCHAR(3) NOT NULL DEFAULT 'twd' COMMENT '三位貨幣代碼，平台預設新臺幣:twd',
          `state` TINYINT(6) NOT NULL DEFAULT '0' COMMENT '退款状态:0-订单生成,1-退款中,2-退款成功,3-退款失败,4-退款任务关闭',
          `client_ip` VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
          `refund_reason` VARCHAR(256) NOT NULL COMMENT '退款原因',
          `channel_order_no` VARCHAR(32) DEFAULT NULL COMMENT '渠道订单号',
          `err_code` VARCHAR(128) DEFAULT NULL COMMENT '渠道错误码',
          `err_msg` VARCHAR(2048) DEFAULT NULL COMMENT '渠道错误描述',
          `channel_extra` VARCHAR(512) DEFAULT NULL COMMENT '特定渠道发起时额外参数',
          `notify_url` VARCHAR(128) DEFAULT NULL COMMENT '通知地址',
          `ext_param` VARCHAR(64) DEFAULT NULL COMMENT '扩展参数',
          `success_time` DATETIME DEFAULT NULL COMMENT '订单退款成功时间',
          `expired_time` DATETIME DEFAULT NULL COMMENT '退款失效时间（失效后系统更改为退款任务关闭状态）',
          `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
          `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
          PRIMARY KEY (`refund_order_id`),
          UNIQUE KEY `Uni_MchNo_MchRefundNo` (`mch_no`, `mch_refund_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款订单表';


-- 转账订单表
DROP TABLE IF EXISTS t_transfer_order;
CREATE TABLE `t_transfer_order` (
           `transfer_id` VARCHAR(32) NOT NULL COMMENT '转账订单号',
           `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
           `isv_no` VARCHAR(64) COMMENT '服务商号',
           `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
           `mch_name` VARCHAR(30) NOT NULL COMMENT '商户名称',
           `mch_type` TINYINT(6) NOT NULL COMMENT '类型: 1-普通商户, 2-特约商户(服务商模式)',
           `mch_order_no` VARCHAR(64) NOT NULL COMMENT '商户订单号',
           `if_code` VARCHAR(20)  NOT NULL COMMENT '支付接口代码',
           `entry_type` VARCHAR(20) NOT NULL COMMENT '入账方式： WX_CASH-微信零钱; ALIPAY_CASH-支付宝转账; BANK_CARD-银行卡',
           `amount` BIGINT(20) NOT NULL COMMENT '转账金额,单位分',
           `currency` VARCHAR(3) NOT NULL DEFAULT 'twd' COMMENT '三位貨幣代碼，平台預設新臺幣:twd',
           `account_no` VARCHAR(64) NOT NULL COMMENT '收款账号',
           `account_name` VARCHAR(64) COMMENT '收款人姓名',
           `bank_name` VARCHAR(32) COMMENT '收款人开户行名称',
           `transfer_desc` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '转账备注信息',
           `client_ip` VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
           `state` TINYINT(6) NOT NULL DEFAULT '0' COMMENT '支付状态: 0-订单生成, 1-转账中, 2-转账成功, 3-转账失败, 4-订单关闭',
           `channel_extra` VARCHAR(512) DEFAULT NULL COMMENT '特定渠道发起额外参数',
           `channel_order_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道订单号',
           `channel_res_data` TEXT DEFAULT NULL COMMENT '渠道响应数据（如微信确认数据包）',
           `err_code` VARCHAR(128) DEFAULT NULL COMMENT '渠道支付错误码',
           `err_msg` VARCHAR(256) DEFAULT NULL COMMENT '渠道支付错误描述',
           `ext_param` VARCHAR(128) DEFAULT NULL COMMENT '商户扩展参数',
           `notify_url` VARCHAR(128) NOT NULL default '' COMMENT '异步通知地址',
           `success_time` DATETIME DEFAULT NULL COMMENT '转账成功时间',
           `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
           `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
           PRIMARY KEY (`transfer_id`),
           UNIQUE KEY `Uni_MchNo_MchOrderNo` (`mch_no`, `mch_order_no`),
           INDEX(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转账订单表';

-- 商户分账接收者账号组
DROP TABLE IF EXISTS `t_mch_division_receiver_group`;
CREATE TABLE `t_mch_division_receiver_group` (
           `receiver_group_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '组ID',
           `receiver_group_name` VARCHAR(64) NOT NULL COMMENT '组名称',
           `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
           `auto_division_flag` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '自动分账组（当订单分账模式为自动分账，改组将完成分账逻辑） 0-否 1-是',
           `created_uid` BIGINT(20) NOT NULL COMMENT '创建者用户ID',
           `created_by` VARCHAR(64) NOT NULL COMMENT '创建者姓名',
           `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
           `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
           PRIMARY KEY (`receiver_group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100001 DEFAULT CHARSET=utf8mb4 COMMENT='分账账号组';

-- 商户分账接收者账号绑定关系表
DROP TABLE IF EXISTS `t_mch_division_receiver`;
CREATE TABLE `t_mch_division_receiver` (
          `receiver_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '分账接收者ID',
          `receiver_alias` VARCHAR(64) NOT NULL COMMENT '接收者账号别名',
          `receiver_group_id` BIGINT(20) COMMENT '组ID（便于商户接口使用）',
          `receiver_group_name` VARCHAR(64) COMMENT '组名称',
          `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
          `isv_no` VARCHAR(64) COMMENT '服务商号',
          `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
          `if_code` VARCHAR(20) NOT NULL COMMENT '支付接口代码',
          `acc_type` TINYINT(6) NOT NULL COMMENT '分账接收账号类型: 0-个人(对私) 1-商户(对公)',
          `acc_no` VARCHAR(50) NOT NULL COMMENT '分账接收账号',
          `acc_name` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '分账接收账号名称',
          `relation_type` VARCHAR(30) NOT NULL COMMENT '分账关系类型（参考微信）， 如： SERVICE_PROVIDER 服务商等',
          `relation_type_name` VARCHAR(30) NOT NULL COMMENT '当选择自定义时，需要录入该字段。 否则为对应的名称',
          `division_profit` DECIMAL(20,6) COMMENT '分账比例',
          `state` TINYINT(6) NOT NULL COMMENT '分账状态（本系统状态，并不调用上游关联关系）: 1-正常分账, 0-暂停分账',
          `channel_bind_result` TEXT COMMENT '上游绑定返回信息，一般用作查询账号异常时的记录',
          `channel_ext_info` TEXT COMMENT '渠道特殊信息',
          `bind_success_time` DATETIME DEFAULT NULL COMMENT '绑定成功时间',
          `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
          `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
          PRIMARY KEY (`receiver_id`)
) ENGINE=InnoDB AUTO_INCREMENT=800001 DEFAULT CHARSET=utf8mb4 COMMENT='商户分账接收者账号绑定关系表';

-- 分账记录表
DROP TABLE IF EXISTS `t_pay_order_division_record`;
CREATE TABLE `t_pay_order_division_record` (
          `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分账记录ID',
          `mch_no` VARCHAR(64) NOT NULL COMMENT '商户号',
          `isv_no` VARCHAR(64) COMMENT '服务商号',
          `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
          `mch_name` VARCHAR(30) NOT NULL COMMENT '商户名称',
          `mch_type` TINYINT(6) NOT NULL COMMENT '类型: 1-普通商户, 2-特约商户(服务商模式)',
          `if_code` VARCHAR(20)  NOT NULL COMMENT '支付接口代码',
          `pay_order_id` VARCHAR(30) NOT NULL COMMENT '系统支付订单号',
          `pay_order_channel_order_no` VARCHAR(64) COMMENT '支付订单渠道支付订单号',
          `pay_order_amount` BIGINT(20) NOT NULL COMMENT '订单金额,单位分',
          `pay_order_division_amount` BIGINT(20) NOT NULL COMMENT '订单实际分账金额, 单位：分（订单金额 - 商户手续费 - 已退款金额）',
          `batch_order_id` VARCHAR(30) NOT NULL COMMENT '系统分账批次号',
          `channel_batch_order_id` VARCHAR(64) COMMENT '上游分账批次号',
          `state` TINYINT(6) NOT NULL COMMENT '状态: 0-待分账 1-分账成功（明确成功）, 2-分账失败（明确失败）, 3-分账已受理（上游受理）',
          `channel_resp_result` TEXT COMMENT '上游返回数据包',
          `receiver_id` BIGINT(20) NOT NULL COMMENT '账号快照》 分账接收者ID',
          `receiver_group_id` BIGINT(20) COMMENT '账号快照》 组ID（便于商户接口使用）',
          `receiver_alias` VARCHAR(64) COMMENT '接收者账号别名',
          `acc_type` TINYINT(6) NOT NULL COMMENT '账号快照》 分账接收账号类型: 0-个人 1-商户',
          `acc_no` VARCHAR(50) NOT NULL COMMENT '账号快照》 分账接收账号',
          `acc_name` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '账号快照》 分账接收账号名称',
          `relation_type` VARCHAR(30) NOT NULL COMMENT '账号快照》 分账关系类型（参考微信）， 如： SERVICE_PROVIDER 服务商等',
          `relation_type_name` VARCHAR(30) NOT NULL COMMENT '账号快照》 当选择自定义时，需要录入该字段。 否则为对应的名称',
          `division_profit` DECIMAL(20,6) NOT NULL COMMENT '账号快照》 配置的实际分账比例',
          `cal_division_amount` BIGINT(20) NOT NULL COMMENT '计算该接收方的分账金额,单位分',
          `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
          `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
          PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1001 DEFAULT CHARSET=utf8mb4 COMMENT='分账记录表';



#####  ↑↑↑↑↑↑↑↑↑↑  表结构DDL  ↑↑↑↑↑↑↑↑↑↑  #####

#####  ↓↓↓↓↓↓↓↓↓↓  初始化DML  ↓↓↓↓↓↓↓↓↓↓  #####

-- 权限表数据 （ 不包含根目录 ）
insert into t_sys_entitlement values('ENT_COMMONS', '系統通用選單', 'no-icon', '', 'RouteView', 'MO', 0, 1,  'ROOT', '-1', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_C_USERINFO', '個人中心', 'no-icon', '/current/userinfo', 'CurrentUserInfo', 'MO', 0, 1,  'ENT_COMMONS', '-1', 'MGR', now(), now());

insert into t_sys_entitlement values('ENT_C_MAIN', '主頁', 'home', '/main', 'MainPage', 'ML', 0, 1,  'ROOT', '1', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_C_MAIN_PAY_AMOUNT_WEEK', '主頁週支付統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_C_MAIN', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_C_MAIN_NUMBER_COUNT', '主頁數量總統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_C_MAIN', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_C_MAIN_PAY_COUNT', '主頁交易統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_C_MAIN', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_C_MAIN_PAY_TYPE_COUNT', '主頁交易方式統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_C_MAIN', '0', 'MGR', now(), now());

-- 商户管理
insert into t_sys_entitlement values('ENT_MCH', '商戶管理', 'shop', '', 'RouteView', 'MO', 0, 1,  'ROOT', '30', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_INFO', '商戶列表', 'profile', '/mch', 'MchListPage', 'MO', 0, 1,  'ENT_MCH', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_AGENT_BIND', '按鈕：代理綁定', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_LIST', '頁面：商戶列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_INFO_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_INFO_EDIT', '按鈕：編輯', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_INFO_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_INFO_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_CONFIG', '應用設定', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_INFO', '0', 'MGR', now(), now());

    -- 应用管理
    insert into t_sys_entitlement values('ENT_MCH_APP', '應用管理', 'appstore', '/apps', 'MchAppPage', 'MO', 0, 1,  'ENT_MCH', '20', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_LIST', '頁面：應用列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_EDIT', '按鈕：編輯', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_CONFIG_LIST', '應用支付參數設定列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_CONFIG_ADD', '應用支付參數設定', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_CONFIG_LIST', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_CONFIG_VIEW', '應用支付參數設定詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_CONFIG_LIST', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_PASSAGE_LIST', '應用支付通道設定列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_PASSAGE_CONFIG', '應用支付通道設定入口', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_PASSAGE_LIST', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_PASSAGE_ADD', '應用支付通道設定保存', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_PASSAGE_LIST', '0', 'MGR', now(), now());

-- 服务商管理
-- 代理管理（ADR-0009 第一階段：代理、商戶綁定、四層費率設定與試算）
insert into t_sys_entitlement values('ENT_AGENT', '團長管理', 'cluster', '', 'RouteView', 'ML', 0, 1,  'ROOT', '45', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_AGENT_INFO', '團長列表', 'apartment', '/agents', 'AgentListPage', 'ML', 0, 1,  'ENT_AGENT', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_AGENT_LIST', '頁面：代理列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_AGENT_INFO_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_AGENT_INFO_EDIT', '按鈕：編輯', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_AGENT_INFO_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_AGENT_INFO_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_FEE_RULE', '費率設定', 'percentage', '/feeRules', 'FeeRulePage', 'ML', 0, 1,  'ENT_AGENT', '20', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_FEE_RULE_LIST', '頁面：費率列表與試算', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_FEE_RULE_EDIT', '按鈕：修改代理層費率', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_FEE_RULE_PLATFORM_EDIT', '按鈕：修改平臺費與渠道費', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_FEE_RULE_LOG', '頁面：費率變更紀錄', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());

insert into t_sys_entitlement values('ENT_AGENT_ACCOUNT', '按鈕：登入帳號', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PROFIT', '按鈕：分潤報表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_BATCH', '按鈕：批次設定代理層費率', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_RULE_REVIEW', '按鈕：覆核平臺費變更', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_RULE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_TEMPLATE', '費率範本', 'copy', '/feeTemplates', 'FeeTemplatePage', 'ML', 0, 1,  'ENT_AGENT', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_TEMPLATE_EDIT', '按鈕：新增／修改／刪除範本', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_TEMPLATE', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_FEE_TEMPLATE_APPLY', '按鈕：套用範本', 'no-icon', '', '', 'PB', 0, 1,  'ENT_FEE_TEMPLATE', '0', 'MGR', now(), now());
-- ADR-0012 第一階段：上帝從團長點進去看商戶、渠道、隊長；渠道帳號只有上帝能建立與派發
insert into t_sys_entitlement values('ENT_AGENT_DETAIL', '團長詳情', 'no-icon', '/agents/detail', 'AgentDetailPage', 'MO', 0, 1,  'ENT_AGENT', '11', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_CHANNEL_ACCOUNT_LIST', '頁面：渠道帳號列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_CHANNEL_ACCOUNT_EDIT', '按鈕：新增／修改／派發渠道帳號', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_INFO', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_CHANNEL', '渠道列表', 'api', '/agentPortal/channels', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '32', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL', '代理後台', 'team', '', 'RouteView', 'ML', 0, 1,  'ROOT', '46', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_HOME', '錢包與提現', 'wallet', '/agentPortal/wallet', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '10', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_PROFIT', '分潤', 'pie-chart', '/agentPortal/profit', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '20', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_MCH', '商戶列表', 'shop', '/agentPortal/merchants', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_SUB', '旗下代理', 'team', '/agentPortal/subAgents', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '40', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_FEE', '費率', 'percentage', '/agentPortal/fee', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '50', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_ORDER', '訂單', 'account-book', '/agentPortal/orders', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '25', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_OPLOG', '操作紀錄', 'file-text', '/agentPortal/opLogs', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '60', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_REPORT', '統計報表', 'bar-chart', '/agentPortal/report', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '22', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_BRANCH_WALLET', '旗下錢包', 'bank', '/agentPortal/branchWallets', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '42', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_FREEZE', '按鈕：凍結／解凍旗下資金（限團長）', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_BRANCH_WALLET', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_WITHDRAW_AUDIT', '提現審核', 'audit', '/agentPortal/withdrawAudit', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '44', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_ROUTE', '通道路由', 'branches', '/agentPortal/routes', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '52', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_BLACKLIST', '黑名單', 'stop', '/agentPortal/blacklist', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '54', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_BRAND', '品牌設定', 'skin', '/agentPortal/brand', 'AgentPortalPage', 'ML', 0, 1,  'ENT_AGENT_PORTAL', '70', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_MCH_EDIT', '按鈕：商戶歸屬與重設密碼', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_MCH', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_MCH_ADD', '按鈕：新增商戶', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_MCH', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_SUB_ADD', '按鈕：新增旗下代理（限團長）', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_SUB', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_VIEW', '頁面：代理後台資料', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_HOME', '0', 'MGR', now(), now());

insert into t_sys_entitlement values('ENT_WALLET_ACCOUNT', '錢包帳戶', 'wallet', '/wallet/accounts', 'WalletAccountPage', 'ML', 0, 1,  'ENT_WALLET', '5', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_ADJUST', '按鈕：申請人工調帳', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_ACCOUNT', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_ADJUST_REVIEW', '按鈕：覆核人工調帳', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_ACCOUNT', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_SETTLE_RUN', '按鈕：立即結算', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_ACCOUNT', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WALLET_WITHDRAW_REVIEW', '按鈕：審核提現（撥款／駁回）', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WALLET_WITHDRAW', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_RISK_BLACKLIST', '風控黑名單', 'stop', '/risk/blacklist', 'RiskBlacklistPage', 'ML', 0, 1,  'ENT_WALLET', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_RISK_BLACKLIST_EDIT', '按鈕：新增／刪除黑名單', 'no-icon', '', '', 'PB', 0, 1,  'ENT_RISK_BLACKLIST', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_AGENT_PORTAL_FEE_EDIT', '按鈕：設定下級代理費率（限團長）', 'no-icon', '', '', 'PB', 0, 1,  'ENT_AGENT_PORTAL_HOME', '0', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_MCH_WALLET_WITHDRAW', '按鈕：申請／取消提現', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_WALLET', '0', 'MCH', now(), now());
insert into t_sys_entitlement values('ENT_MCH_WALLET_PAYOUT_EDIT', '按鈕：設定收款帳戶', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_WALLET', '0', 'MCH', now(), now());

insert into t_sys_entitlement values('ENT_WAY_ROUTE', '通道路由', 'branches', '/wayRoutes', 'WayRoutePage', 'ML', 0, 1,  'ENT_PC', '30', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_WAY_ROUTE_EDIT', '按鈕：新增／修改／刪除路由規則', 'no-icon', '', '', 'PB', 0, 1,  'ENT_WAY_ROUTE', '0', 'MGR', now(), now());

-- 歷史查詢（代收查詢含匯出；代付查詢尚未實作）
insert into t_sys_entitlement values('ENT_HISTORY', '歷史查詢', 'history', '', 'RouteView', 'ML', 0, 1,  'ROOT', '52', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_HISTORY_PAY', '代收查詢', 'file-search', '/history/pay', 'HistoryPayPage', 'ML', 0, 1,  'ENT_HISTORY', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_EXPORT_CENTER', '按鈕：匯出與下載', 'no-icon', '', '', 'PB', 0, 1,  'ENT_HISTORY_PAY', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_HISTORY_PAYOUT', '代付查詢', 'file-sync', '/history/payout', 'HistoryPayoutPage', 'ML', 0, 1,  'ENT_HISTORY', '20', 'MGR', now(), now());
insert into t_sys_entitlement values('ENT_HISTORY', '歷史查詢', 'history', '', 'RouteView', 'ML', 0, 1,  'ROOT', '25', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_HISTORY_PAY', '代收查詢', 'file-search', '/history/pay', 'HistoryPayPage', 'ML', 0, 1,  'ENT_HISTORY', '10', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_EXPORT_CENTER', '按鈕：匯出與下載', 'no-icon', '', '', 'PB', 0, 1,  'ENT_HISTORY_PAY', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_HISTORY_PAYOUT', '代付查詢', 'file-sync', '/history/payout', 'HistoryPayoutPage', 'ML', 0, 1,  'ENT_HISTORY', '20', 'MCH', now(), now());

insert into t_sys_entitlement values('ENT_ISV', '服務商管理', 'block', '', 'RouteView', 'ML', 0, 0,  'ROOT', '40', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_ISV_INFO', '服務商列表', 'profile', '/isv', 'IsvListPage', 'ML', 0, 0,  'ENT_ISV', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_LIST', '頁面：服務商列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_INFO_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_INFO_EDIT', '按鈕：編輯', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_INFO_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_INFO_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_PAY_CONFIG_LIST', '服務商支付參數設定列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_INFO', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_PAY_CONFIG_ADD', '服務商支付參數設定', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_PAY_CONFIG_LIST', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ISV_PAY_CONFIG_VIEW', '服務商支付參數設定詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_ISV_PAY_CONFIG_LIST', '0', 'MGR', now(), now());

-- 订单管理
-- 錢包與提現（規劃中，P0 佔位；刪除這些列即可整體隱藏選單）
insert into t_sys_entitlement values('ENT_WALLET', '錢包與提現', 'wallet', '', 'RouteView', 'ML', 0, 1,  'ROOT', '55', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_WALLET_LEDGER', '餘額流水', 'account-book', '/wallet/ledger', 'WalletLedgerPage', 'ML', 0, 1,  'ENT_WALLET', '10', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_WALLET_WITHDRAW', '提現審核', 'audit', '/wallet/withdraw', 'WithdrawAuditPage', 'ML', 0, 1,  'ENT_WALLET', '20', 'MGR', now(), now());

insert into t_sys_entitlement values('ENT_ORDER', '訂單中心', 'transaction', '', 'RouteView', 'ML', 0, 1,  'ROOT', '50', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_PAY_ORDER', '訂單管理', 'account-book', '/pay', 'PayOrderListPage', 'ML', 0, 1,  'ENT_ORDER', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_ORDER_LIST', '頁面：訂單列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_REFUND', '按鈕：訂單退款', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_MANUAL_NOTIFY', '按鈕：人工回調', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_SEARCH_PAY_WAY', '篩選項：支付方式', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_REFUND_ORDER', '退款記錄', 'exception', '/refund', 'RefundOrderListPage', 'ML', 0, 1,  'ENT_ORDER', '20', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_REFUND_LIST', '頁面：退款訂單列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_REFUND_ORDER', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_REFUND_ORDER_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_REFUND_ORDER', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_TRANSFER_ORDER', '轉帳訂單', 'property-safety', '/transfer', 'TransferOrderListPage', 'ML', 0, 1,  'ENT_ORDER', '25', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_TRANSFER_ORDER_LIST', '頁面：轉帳訂單列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_TRANSFER_ORDER', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_TRANSFER_ORDER_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_TRANSFER_ORDER', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_NOTIFY', '商戶通知', 'notification', '/notify', 'MchNotifyListPage', 'ML', 0, 1,  'ENT_ORDER', '30', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_NOTIFY_LIST', '頁面：商戶通知列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_NOTIFY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_NOTIFY_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_NOTIFY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_NOTIFY_RESEND', '按鈕：重發通知', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_NOTIFY', '0', 'MGR', now(), now());

-- 支付配置菜单
insert into t_sys_entitlement values('ENT_PC', '支付設定', 'file-done', '', 'RouteView', 'ML', 0, 1,  'ROOT', '60', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_PC_IF_DEFINE', '支付介面', 'interaction', '/ifdefines', 'IfDefinePage', 'ML', 0, 1,  'ENT_PC', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_IF_DEFINE_LIST', '頁面：支付接口定義列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_IF_DEFINE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_IF_DEFINE_SEARCH', '頁面：搜尋', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_IF_DEFINE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_IF_DEFINE_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_IF_DEFINE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_IF_DEFINE_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_IF_DEFINE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_IF_DEFINE_EDIT', '按鈕：修改', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_IF_DEFINE', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_IF_DEFINE_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_IF_DEFINE', '0', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_PC_WAY', '支付方式', 'appstore', '/payways', 'PayWayPage', 'ML', 0, 1,  'ENT_PC', '20', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_WAY_LIST', '頁面：支付方式列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_WAY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_WAY_SEARCH', '頁面：搜尋', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_WAY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_WAY_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_WAY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_WAY_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_WAY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_WAY_EDIT', '按鈕：修改', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_WAY', '0', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_PC_WAY_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PC_WAY', '0', 'MGR', now(), now());

-- 系统管理
insert into t_sys_entitlement values('ENT_SYS_CONFIG', '系統管理', 'setting', '', 'RouteView', 'ML', 0, 1,  'ROOT', '200', 'MGR', now(), now());
    insert into t_sys_entitlement values('ENT_UR', '用戶角色管理', 'team', '', 'RouteView', 'ML', 0, 1,  'ENT_SYS_CONFIG', '10', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_UR_USER', '操作員管理', 'contacts', '/users', 'SysUserPage', 'ML', 0, 1,  'ENT_UR', '10', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_LIST', '頁面：操作員列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_SEARCH', '按鈕：搜尋', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_ADD', '按鈕：新增操作員', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_VIEW', '按鈕： 詳情', '', 'no-icon', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_EDIT', '按鈕： 修改基本資訊', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_DELETE', '按鈕： 刪除操作員', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_UPD_ROLE', '按鈕： 角色分配', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MGR', now(), now());

        insert into t_sys_entitlement values('ENT_UR_ROLE', '角色管理', 'user', '/roles', 'RolePage', 'ML', 0, 1,  'ENT_UR', '20', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_LIST', '頁面：角色列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_SEARCH', '頁面：搜尋', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_ADD', '按鈕：新增角色', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_DIST', '按鈕： 分配權限', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_EDIT', '按鈕： 修改基本信息', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_DEL', '按鈕： 刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MGR', now(), now());

        insert into t_sys_entitlement values('ENT_UR_ROLE_ENT', '權限管理', 'apartment', '/ents', 'EntPage', 'ML', 0, 1,  'ENT_UR', '30', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_ENT_LIST', '頁面： 權限列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE_ENT', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_ENT_EDIT', '按鈕： 權限變更', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE_ENT', '0', 'MGR', now(), now());

    insert into t_sys_entitlement values('ENT_SYS_CONFIG_INFO', '系統設定', 'setting', '/config', 'SysConfigPage', 'ML', 0, 1,  'ENT_SYS_CONFIG', '15', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_SYS_CONFIG_EDIT', '按鈕： 修改', 'no-icon', '', '', 'PB', 0, 1,  'ENT_SYS_CONFIG_INFO', '0', 'MGR', now(), now());

    insert into t_sys_entitlement values('ENT_SYS_LOG', '系統日誌', 'file-text', '/log', 'SysLogPage', 'ML', 0, 1,  'ENT_SYS_CONFIG', '20', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_LOG_LIST', '頁面：系統日誌列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_SYS_LOG', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_SYS_LOG_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_SYS_LOG', '0', 'MGR', now(), now());
            insert into t_sys_entitlement values('ENT_SYS_LOG_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_SYS_LOG', '0', 'MGR', now(), now());


-- 【商户系统】 主页
insert into t_sys_entitlement values('ENT_COMMONS', '系統通用選單', 'no-icon', '', 'RouteView', 'MO', 0, 1,  'ROOT', '-1', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_C_USERINFO', '個人中心', 'no-icon', '/current/userinfo', 'CurrentUserInfo', 'MO', 0, 1,  'ENT_COMMONS', '-1', 'MCH', now(), now());

insert into t_sys_entitlement values('ENT_MCH_MAIN', '主頁', 'home', '/main', 'MainPage', 'ML', 0, 1,  'ROOT', '1', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_MAIN_PAY_AMOUNT_WEEK', '主頁週支付統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_MAIN', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_MAIN_NUMBER_COUNT', '主頁數量總統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_MAIN', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_MAIN_PAY_COUNT', '主頁交易統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_MAIN', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_MAIN_PAY_TYPE_COUNT', '主頁交易方式統計', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_MAIN', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_MAIN_USER_INFO', '主頁用戶資訊', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_MAIN', '0', 'MCH', now(), now());

-- 【商户系统】 商户中心
insert into t_sys_entitlement values('ENT_MCH_CENTER', '商戶中心', 'team', '', 'RouteView', 'ML', 0, 1, 'ROOT', '10', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_MCH_APP', '應用管理', 'appstore', '/apps', 'MchAppPage', 'ML', 0, 1,  'ENT_MCH_CENTER', '10', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_LIST', '頁面：應用列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_ADD', '按鈕：新增', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_EDIT', '按鈕：編輯', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_APP_DEL', '按鈕：刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_CONFIG_LIST', '應用支付參數設定列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_CONFIG_ADD', '應用支付參數設定', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_CONFIG_LIST', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_CONFIG_VIEW', '應用支付參數設定詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_CONFIG_LIST', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_PASSAGE_LIST', '應用支付通道設定列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_APP', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_PASSAGE_CONFIG', '應用支付通道設定入口', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_PASSAGE_LIST', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_MCH_PAY_PASSAGE_ADD', '應用支付通道設定保存', 'no-icon', '', '', 'PB', 0, 1,  'ENT_MCH_PAY_PASSAGE_LIST', '0', 'MCH', now(), now());


    -- 我的錢包（規劃中，P0 佔位）
    insert into t_sys_entitlement values('ENT_MCH_WALLET', '我的錢包', 'wallet', '/wallet', 'MchWalletPage', 'ML', 0, 1,  'ENT_MCH_CENTER', '40', 'MCH', now(), now());

-- 【商户系统】 订单管理
insert into t_sys_entitlement values('ENT_ORDER', '訂單中心', 'transaction', '', 'RouteView', 'ML', 0, 1,  'ROOT', '20', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_PAY_ORDER', '訂單管理', 'account-book', '/pay', 'PayOrderListPage', 'ML', 0, 1,  'ENT_ORDER', '10', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_ORDER_LIST', '頁面：訂單列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_SEARCH_PAY_WAY', '篩選項：支付方式', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_PAY_ORDER_REFUND', '按鈕：訂單退款', 'no-icon', '', '', 'PB', 0, 1,  'ENT_PAY_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_REFUND_ORDER', '退款記錄', 'exception', '/refund', 'RefundOrderListPage', 'ML', 0, 1,  'ENT_ORDER', '20', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_REFUND_LIST', '頁面：退款訂單列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_REFUND_ORDER', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_REFUND_ORDER_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_REFUND_ORDER', '0', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_TRANSFER_ORDER', '轉帳訂單', 'property-safety', '/transfer', 'TransferOrderListPage', 'ML', 0, 1,  'ENT_ORDER', '30', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_TRANSFER_ORDER_LIST', '頁面：轉帳訂單列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_TRANSFER_ORDER', '0', 'MCH', now(), now());
        insert into t_sys_entitlement values('ENT_TRANSFER_ORDER_VIEW', '按鈕：詳情', 'no-icon', '', '', 'PB', 0, 1,  'ENT_TRANSFER_ORDER', '0', 'MCH', now(), now());

-- 【商户系统】 分账管理


-- 【商户系统】 系统管理
insert into t_sys_entitlement values('ENT_SYS_CONFIG', '系統管理', 'setting', '', 'RouteView', 'ML', 0, 1,  'ROOT', '200', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_UR', '用戶角色管理', 'team', '', 'RouteView', 'ML', 0, 1,  'ENT_SYS_CONFIG', '10', 'MCH', now(), now());
    insert into t_sys_entitlement values('ENT_UAT_EDGE_ALLOWLIST', 'UAT Edge 白名單', 'safety', '/uatedge/allowlist', 'UatEdgeAllowlistPage', 'ML', 0, 1,  'ENT_SYS_CONFIG', '25', 'MGR', now(), now());
        insert into t_sys_entitlement values('ENT_UR_USER', '操作員管理', 'contacts', '/users', 'SysUserPage', 'ML', 0, 1,  'ENT_UR', '10', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_LIST', '頁面：操作員列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_SEARCH', '按鈕：搜尋', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_ADD', '按鈕：新增操作員', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_VIEW', '按鈕： 詳情', '', 'no-icon', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_EDIT', '按鈕： 修改基本資訊', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_DELETE', '按鈕： 刪除操作員', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_USER_UPD_ROLE', '按鈕： 角色分配', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_USER', '0', 'MCH', now(), now());

        insert into t_sys_entitlement values('ENT_UR_ROLE', '角色管理', 'user', '/roles', 'RolePage', 'ML', 0, 1,  'ENT_UR', '20', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_LIST', '頁面：角色列表', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_SEARCH', '頁面：搜尋', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_ADD', '按鈕：新增角色', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_DIST', '按鈕： 分配權限', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_EDIT', '按鈕： 修改名稱', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MCH', now(), now());
            insert into t_sys_entitlement values('ENT_UR_ROLE_DEL', '按鈕： 刪除', 'no-icon', '', '', 'PB', 0, 1,  'ENT_UR_ROLE', '0', 'MCH', now(), now());

-- 默认角色
insert into t_sys_role values ('ROLE_ADMIN', '系統管理員', 'MGR', '0', '2021-05-01');
insert into t_sys_role values ('ROLE_OP', '普通操作員', 'MGR', '0', '2021-05-01');
-- 代理帳號固定角色：只含代理後台與個人中心，不含任何平台權限
insert into t_sys_role values ('ROLE_AGENT_PORTAL', '代理帳號（系統角色）', 'MGR', '0', now());
insert into t_sys_role_ent_rela values ('ROLE_AGENT_PORTAL', 'ENT_COMMONS'), ('ROLE_AGENT_PORTAL', 'ENT_C_USERINFO'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_HOME'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_VIEW'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_FEE_EDIT'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_PROFIT'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_MCH'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_SUB'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_FEE'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_ORDER'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_OPLOG'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_MCH_ADD'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_SUB_ADD'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_REPORT'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BRANCH_WALLET'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_FREEZE'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_WITHDRAW_AUDIT'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_ROUTE'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BLACKLIST'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_BRAND'), ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_MCH_EDIT'),
    ('ROLE_AGENT_PORTAL', 'ENT_AGENT_PORTAL_CHANNEL');

-- 平台直屬團長（ADR-0012）：上帝自己經營的一支，既有商戶與渠道帳號歸在它底下
insert into t_agent_info (agent_no, agent_name, agent_level, parent_agent_no, agent_path, state, remark, created_by, is_house)
    values ('A_HOUSE', '平台直屬', 1, NULL, '/A_HOUSE/', 1, '上帝自己經營的一支', 'system', 1);
-- 隊長（第三代）帳號固定角色：沒有下級代理、操作紀錄與設定下級費率
insert into t_sys_role values ('ROLE_AGENT_PORTAL_L2', '隊長帳號（系統角色）', 'MGR', '0', now());
insert into t_sys_role_ent_rela values ('ROLE_AGENT_PORTAL_L2', 'ENT_COMMONS'), ('ROLE_AGENT_PORTAL_L2', 'ENT_C_USERINFO'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_HOME'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_VIEW'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_PROFIT'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_MCH'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_MCH_ADD'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_ORDER'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_FEE'),
    ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_REPORT'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_BRANCH_WALLET'), ('ROLE_AGENT_PORTAL_L2', 'ENT_AGENT_PORTAL_MCH_EDIT');
-- 角色权限关联， [超管]用户 拥有所有权限
-- insert into t_sys_role_ent_rela select '801', ent_id from t_sys_entitlement;

-- 超管用户： jeepay / jeepay123
insert into t_sys_user values (801, 'jeepay', '上帝', '13000000001', '1', '/imgs/defava_m.png', 'D0001', 1, 1, 'MGR', '0', '2020-06-13', '2020-06-13');
insert into t_sys_user_auth values (801, '801', '1', 'jeepay', '$2a$10$eFKb3B384Qq5.NGM6i6W8OxViX.6TXJpUYm9tMdJCzqdRw2JZ21Bi', 'testkey', 'MGR');

-- insert into t_sys_user_role_rela values (801, 801);

INSERT INTO `t_sys_config` VALUES ('mgrSiteUrl', '營運平台網址(不包含結尾/)', '營運平台網址(不包含結尾/)', 'applicationConfig', '系統應用設定', 'http://127.0.0.1:9217', 'text', 0, '2021-5-18 14:46:10');
INSERT INTO `t_sys_config` VALUES ('mchSiteUrl', '商戶平台網址(不包含結尾/)', '商戶平台網址(不包含結尾/)', 'applicationConfig', '系統應用設定', 'http://127.0.0.1:9218', 'text', 0, '2021-5-18 14:46:10');
INSERT INTO `t_sys_config` VALUES ('paySiteUrl', '支付網關地址(不包含結尾/)', '支付網關地址(不包含結尾/)', 'applicationConfig', '系統應用設定', 'http://127.0.0.1:9216', 'text', 0, '2021-5-18 14:46:10');
INSERT INTO `t_sys_config` VALUES ('ossPublicSiteUrl', '公共oss存取地址(不包含結尾/)', '公共oss存取地址(不包含結尾/)', 'applicationConfig', '系統應用設定', 'http://127.0.0.1:9217/api/anon/localOssFiles', 'text', 0, '2021-5-18 14:46:10');
INSERT INTO `t_sys_config` VALUES ('walletSettleDelayDays', '結算延遲天數', '0 = 成功即結算（T+0）；1 = 隔日結算（T+1）', 'walletConfig', '錢包與提現', '1', 'text', 10, now());
INSERT INTO `t_sys_config` VALUES ('withdrawMinAmount', '單筆最低提現金額（元）', '低於此金額不可申請', 'walletConfig', '錢包與提現', '100', 'text', 20, now());
INSERT INTO `t_sys_config` VALUES ('withdrawMaxAmount', '單筆最高提現金額（元）', '高於此金額不可申請', 'walletConfig', '錢包與提現', '500000', 'text', 30, now());
INSERT INTO `t_sys_config` VALUES ('withdrawFeeAmount', '每筆提現手續費（元）', '撥款時自申請金額扣除並記入平台帳戶', 'walletConfig', '錢包與提現', '0', 'text', 40, now());
INSERT INTO `t_sys_config` VALUES ('withdrawDailyLimitPerAccount', '同一收款帳號每日提現次數提示門檻', '超過會在審核畫面標記 DAILY_LIMIT', 'walletConfig', '錢包與提現', '3', 'text', 50, now());
INSERT INTO `t_sys_config` VALUES ('withdrawRestrictedBankCodes', '需特別注意的銀行代碼（逗號分隔）', '命中會在審核畫面標記 RESTRICTED_BANK', 'walletConfig', '錢包與提現', '', 'text', 60, now());


-- 初始化支付方式：僅黑貓 PAY 平台上的四個 ibon 上游
INSERT INTO t_pay_way (way_code, way_name) VALUES ('RYO_IBON', 'RYO ibon 繳款');
INSERT INTO t_pay_way (way_code, way_name) VALUES ('JAY_IBON', 'JAY ibon 繳款');
INSERT INTO t_pay_way (way_code, way_name) VALUES ('CHI_IBON', 'CHI ibon 繳款');
INSERT INTO t_pay_way (way_code, way_name) VALUES ('JHD_IBON', 'JHD ibon 繳款');


-- 初始化支付介面定義
INSERT INTO t_pay_interface_define (if_code, if_name, is_mch_mode, is_isv_mode, config_page_type, isv_params, isvsub_mch_params, normal_mch_params, way_codes, icon, bg_color, state, remark)
VALUES ('ryo', 'RYO（黑貓 PAY）', 1, 0, 1,
        NULL,
        NULL,
        '[{"name":"environment","desc":"Provider 環境","type":"radio","verify":"required","values":"TEST,PRODUCTION","titles":"測試環境,正式環境"},{"name":"custId","desc":"契客代號","type":"text","verify":"required"},{"name":"apiPassword","desc":"API 密碼","type":"text","verify":"required","star":"1"}]',
        '[{"wayCode":"RYO_IBON"}]',
        '', '#222222', 1, '黑貓 PAY ibon 通道（上游一）');

INSERT INTO t_pay_interface_define (if_code, if_name, is_mch_mode, is_isv_mode, config_page_type, isv_params, isvsub_mch_params, normal_mch_params, way_codes, icon, bg_color, state, remark)
VALUES ('jay', 'JAY（黑貓 PAY）', 1, 0, 1,
        NULL,
        NULL,
        '[{"name":"environment","desc":"Provider 環境","type":"radio","verify":"required","values":"TEST,PRODUCTION","titles":"測試環境,正式環境"},{"name":"custId","desc":"契客代號","type":"text","verify":"required"},{"name":"apiPassword","desc":"API 密碼","type":"text","verify":"required","star":"1"}]',
        '[{"wayCode":"JAY_IBON"}]',
        '', '#222222', 1, '黑貓 PAY ibon 通道（上游二）');

INSERT INTO t_pay_interface_define (if_code, if_name, is_mch_mode, is_isv_mode, config_page_type, isv_params, isvsub_mch_params, normal_mch_params, way_codes, icon, bg_color, state, remark)
VALUES ('chi', 'CHI（黑貓 PAY）', 1, 0, 1,
        NULL,
        NULL,
        '[{"name":"environment","desc":"Provider 環境","type":"radio","verify":"required","values":"TEST,PRODUCTION","titles":"測試環境,正式環境"},{"name":"custId","desc":"契客代號","type":"text","verify":"required"},{"name":"apiPassword","desc":"API 密碼","type":"text","verify":"required","star":"1"}]',
        '[{"wayCode":"CHI_IBON"}]',
        '', '#222222', 1, '黑貓 PAY ibon 通道（上游三）');

INSERT INTO t_pay_interface_define (if_code, if_name, is_mch_mode, is_isv_mode, config_page_type, isv_params, isvsub_mch_params, normal_mch_params, way_codes, icon, bg_color, state, remark)
VALUES ('jhd', 'JHD（黑貓 PAY）', 1, 0, 1,
        NULL,
        NULL,
        '[{"name":"environment","desc":"Provider 環境","type":"radio","verify":"required","values":"TEST,PRODUCTION","titles":"測試環境,正式環境"},{"name":"custId","desc":"契客代號","type":"text","verify":"required"},{"name":"apiPassword","desc":"API 密碼","type":"text","verify":"required","star":"1"}]',
        '[{"wayCode":"JHD_IBON"}]',
        '', '#222222', 1, '黑貓 PAY ibon 通道（上游四）');
