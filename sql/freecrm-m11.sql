-- M11: 公海回收规则配置 + 站内信/日志表（M14/M15 一并建，减少发版次数）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 系统配置（key-value：公海回收规则 / 短信配置等）
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

INSERT INTO sys_config (config_key, config_value, remark) VALUES
('pool.recycle.days', '30', '掉保回收：客户超 N 天未跟进自动入公海'),
('pool.expire.days', '90', '超期回收：客户创建超 N 天未成交自动入公海')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- 站内信
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人',
  `title` varchar(200) NOT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `type` tinyint DEFAULT 1 COMMENT '1审批待办 2审批结果 3系统',
  `biz_type` tinyint DEFAULT NULL COMMENT '关联业务类型(1合同2回款3请假)',
  `biz_id` bigint DEFAULT NULL,
  `read_flag` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';

-- 操作日志
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `module` varchar(64) DEFAULT NULL COMMENT '业务模块',
  `action` varchar(128) DEFAULT NULL COMMENT '操作描述',
  `method` varchar(200) DEFAULT NULL COMMENT '类.方法',
  `params` text,
  `success` tinyint DEFAULT 1,
  `error_msg` varchar(500) DEFAULT NULL,
  `cost_ms` bigint DEFAULT 0,
  `ip` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- 登录日志
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `success` tinyint DEFAULT 1,
  `error_msg` varchar(200) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- 短信发送记录
CREATE TABLE IF NOT EXISTS `sys_sms_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mobile` varchar(20) NOT NULL,
  `template` varchar(64) DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `channel` varchar(20) DEFAULT 'ALIYUN',
  `success` tinyint DEFAULT 1,
  `error_msg` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录';

-- M12: 合同产品明细
CREATE TABLE IF NOT EXISTS `crm_contract_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` decimal(18,4) NOT NULL DEFAULT 1,
  `price` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '成交单价',
  `amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '小计',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同产品明细';
