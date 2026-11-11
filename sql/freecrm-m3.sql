-- freecrm M3：商机/合同/回款/产品
USE `freecrm`;

-- 商机状态组
DROP TABLE IF EXISTS `crm_stage_group`;
CREATE TABLE `crm_stage_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '组名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='商机状态组';

-- 商机阶段
DROP TABLE IF EXISTS `crm_stage`;
CREATE TABLE `crm_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL,
  `percent` decimal(5,2) NOT NULL DEFAULT 0 COMMENT '赢率%',
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_group` (`group_id`)
) ENGINE=InnoDB COMMENT='商机阶段';

-- 商机
DROP TABLE IF EXISTS `crm_opportunity`;
CREATE TABLE `crm_opportunity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '商机名',
  `customer_id` bigint NOT NULL,
  `stage_group_id` bigint NOT NULL,
  `stage_id` bigint DEFAULT NULL COMMENT '当前阶段',
  `amount` decimal(18,2) DEFAULT 0 COMMENT '金额',
  `expected_deal_date` date DEFAULT NULL COMMENT '预计成交日',
  `end_status` tinyint DEFAULT NULL COMMENT 'NULL进行中 1赢单 2输单',
  `owner_user_id` bigint NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB COMMENT='商机';

-- 产品分类
DROP TABLE IF EXISTS `crm_product_category`;
CREATE TABLE `crm_product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='产品分类';

-- 产品
DROP TABLE IF EXISTS `crm_product`;
CREATE TABLE `crm_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `no` varchar(64) NOT NULL COMMENT '产品编码',
  `category_id` bigint DEFAULT NULL,
  `price` decimal(18,2) NOT NULL DEFAULT 0,
  `unit` tinyint DEFAULT 1 COMMENT '1件2套3年4次',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0下架1上架',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB COMMENT='产品';

-- 合同
DROP TABLE IF EXISTS `crm_contract`;
CREATE TABLE `crm_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_no` varchar(64) NOT NULL COMMENT '合同编号',
  `name` varchar(128) NOT NULL,
  `customer_id` bigint NOT NULL,
  `opportunity_id` bigint DEFAULT NULL,
  `order_date` date NULL DEFAULT NULL COMMENT '下单日期',
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0,
  `received_amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '已回款累计',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0草稿1审批中2通过3驳回',
  `owner_user_id` bigint NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB COMMENT='合同';

-- 回款计划
DROP TABLE IF EXISTS `crm_payment_plan`;
CREATE TABLE `crm_payment_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `period_no` int NOT NULL DEFAULT 1 COMMENT '期数',
  `return_time` date NULL DEFAULT NULL COMMENT '计划回款日',
  `amount` decimal(18,2) NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB COMMENT='回款计划';

-- 回款
DROP TABLE IF EXISTS `crm_payment`;
CREATE TABLE `crm_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `plan_id` bigint DEFAULT NULL COMMENT '关联计划期',
  `return_time` date NULL DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `method` tinyint DEFAULT 1 COMMENT '1对公转账2现金3微信4支付宝5其他',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待审1通过2驳回',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB COMMENT='回款';
