-- =====================================================================
-- EAM 资产管理（对齐参考系统固定资产管理）
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 资产分类（树）
CREATE TABLE IF NOT EXISTS `eam_asset_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `name` varchar(64) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产分类';

-- 资产台账
CREATE TABLE IF NOT EXISTS `eam_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_no` varchar(64) NOT NULL COMMENT '资产编号',
  `name` varchar(128) NOT NULL COMMENT '资产名称',
  `category_id` bigint DEFAULT NULL,
  `model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `purchase_date` date DEFAULT NULL,
  `purchase_amount` decimal(18,2) DEFAULT 0,
  `use_years` int DEFAULT 5 COMMENT '折旧年限(年)',
  `dept_id` bigint DEFAULT NULL COMMENT '使用部门',
  `keeper_user_id` bigint DEFAULT NULL COMMENT '保管人',
  `location` varchar(128) DEFAULT NULL COMMENT '存放位置',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0闲置 1在用 2维修 3报废',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_no` (`asset_no`),
  KEY `idx_status` (`status`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产台账';

-- 资产变更记录（领用/退回/借出/归还/维修完成/报废）
CREATE TABLE IF NOT EXISTS `eam_asset_change` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint NOT NULL,
  `type` tinyint NOT NULL COMMENT '1领用 2退回 3借出 4归还 5维修 6报废',
  `from_user_id` bigint DEFAULT NULL,
  `to_user_id` bigint DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `change_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产变更记录';

-- 盘点单
CREATE TABLE IF NOT EXISTS `eam_check` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_no` varchar(64) NOT NULL,
  `scope_type` tinyint NOT NULL DEFAULT 1 COMMENT '1全部 2按部门',
  `dept_id` bigint DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0进行中 1已完成',
  `total_count` int DEFAULT 0,
  `diff_count` int DEFAULT 0,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单';

-- 盘点明细
CREATE TABLE IF NOT EXISTS `eam_check_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL,
  `asset_id` bigint NOT NULL,
  `expect_status` tinyint DEFAULT NULL COMMENT '账面状态',
  `actual_status` tinyint DEFAULT NULL COMMENT '实盘状态(NULL=未盘)',
  `actual_keeper_id` bigint DEFAULT NULL,
  `diff` tinyint DEFAULT 0 COMMENT '0一致 1差异',
  `checked_by` bigint DEFAULT NULL,
  `checked_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_check` (`check_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细';

-- 折旧记录（直线法，每月任务生成）
CREATE TABLE IF NOT EXISTS `eam_depreciation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint NOT NULL,
  `dep_month` varchar(7) NOT NULL COMMENT 'yyyy-MM',
  `amount` decimal(18,2) NOT NULL COMMENT '本月折旧额',
  `net_value` decimal(18,2) NOT NULL COMMENT '期末净值',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_month` (`asset_id`, `dep_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产折旧记录';
