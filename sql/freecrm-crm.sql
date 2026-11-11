-- freecrm CRM 模块建表脚本
USE `freecrm`;

-- 线索
DROP TABLE IF EXISTS `crm_lead`;
CREATE TABLE `crm_lead` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '线索名称',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `mobile` varchar(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `source` tinyint DEFAULT NULL COMMENT '来源:1广告2搜索引擎3转介绍4电话陌拜5其他',
  `industry` tinyint DEFAULT NULL COMMENT '行业字典',
  `remark` varchar(500) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人,NULL=公海',
  `transform_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未转化1已转化',
  `customer_id` bigint DEFAULT NULL COMMENT '转化后的客户id',
  `next_contact_time` datetime DEFAULT NULL COMMENT '下次联系时间',
  `last_contact_time` datetime DEFAULT NULL COMMENT '最后联系时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB COMMENT='线索';

-- 客户
DROP TABLE IF EXISTS `crm_customer`;
CREATE TABLE `crm_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '客户名称',
  `mobile` varchar(20) DEFAULT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `source` tinyint DEFAULT NULL,
  `industry` tinyint DEFAULT NULL,
  `level` tinyint DEFAULT NULL COMMENT '等级:A1B2C3D4',
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人,NULL=公海',
  `lock_status` tinyint NOT NULL DEFAULT 0 COMMENT '0正常1锁定',
  `deal_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未成交1已成交',
  `next_contact_time` datetime DEFAULT NULL,
  `last_contact_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB COMMENT='客户';

-- 联系人
DROP TABLE IF EXISTS `crm_contact`;
CREATE TABLE `crm_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `customer_id` bigint NOT NULL COMMENT '所属客户',
  `mobile` varchar(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `post` varchar(64) DEFAULT NULL COMMENT '职务',
  `decision_role` tinyint DEFAULT NULL COMMENT '角色:1决策者2影响者3使用者4教练',
  `remark` varchar(500) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB COMMENT='联系人';

-- 跟进记录
DROP TABLE IF EXISTS `crm_follow_up`;
CREATE TABLE `crm_follow_up` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` tinyint NOT NULL COMMENT '1线索2客户',
  `biz_id` bigint NOT NULL,
  `type` tinyint DEFAULT NULL COMMENT '方式:1电话2微信3上门4邮件5其他',
  `content` varchar(1000) NOT NULL,
  `next_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB COMMENT='跟进记录';

-- 团队成员（数据权限）
DROP TABLE IF EXISTS `crm_team_member`;
CREATE TABLE `crm_team_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` tinyint NOT NULL COMMENT '1线索2客户',
  `biz_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `level` tinyint NOT NULL COMMENT '1负责人2读写3只读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_user` (`biz_type`, `biz_id`, `user_id`)
) ENGINE=InnoDB COMMENT='团队成员';
