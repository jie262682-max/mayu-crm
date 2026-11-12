-- W3: 通用审批申请单（自定义表单 + 申请 + 审批回调）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 表单定义：fields JSON 数组 [{"key":"amount","label":"报销金额","type":"number","required":true,"options":["a","b"]}]
CREATE TABLE IF NOT EXISTS oa_form_def (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '表单名（如 差旅报销）',
  `fields` text NOT NULL COMMENT '字段定义 JSON',
  `icon` varchar(64) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表单定义';

-- 通用申请单（bizType=100 固定，form 区分业务）
CREATE TABLE IF NOT EXISTS oa_form_apply (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `form_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `form_data` text COMMENT '填写的字段值 JSON',
  `user_id` bigint NOT NULL COMMENT '申请人',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0审批中 1通过 2驳回',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_form` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用审批申请单';

-- 内置演示：差旅报销表单
INSERT INTO oa_form_def (id, name, fields, status, creator) VALUES
(1, '差旅报销', '[{"key":"trip_date","label":"出差日期","type":"date","required":true},{"key":"from_city","label":"出发城市","type":"text","required":true},{"key":"to_city","label":"到达城市","type":"text","required":true},{"key":"days","label":"天数","type":"number","required":true},{"key":"amount","label":"报销金额(元)","type":"number","required":true},{"key":"reason","label":"事由","type":"textarea","required":false}]', 1, 1)
ON DUPLICATE KEY UPDATE deleted = 0;
