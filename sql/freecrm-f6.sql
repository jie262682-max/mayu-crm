-- F6: 报价单（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

CREATE TABLE IF NOT EXISTS `crm_quotation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_no` VARCHAR(40) NOT NULL COMMENT '报价编号 BJ 自动',
  `customer_id` BIGINT NOT NULL,
  `opportunity_id` BIGINT DEFAULT NULL COMMENT '关联商机（可空）',
  `owner_user_id` BIGINT DEFAULT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '明细合计-折让',
  `discount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '整单折让',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已确认 2已转合同 3已作废',
  `contract_id` BIGINT DEFAULT NULL COMMENT '转化出的合同 id',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` BIGINT DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_opportunity` (`opportunity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价单';

CREATE TABLE IF NOT EXISTS `crm_quotation_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quotation_id` BIGINT NOT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(120) DEFAULT NULL COMMENT '产品名快照（打印用）',
  `price` DECIMAL(14,2) DEFAULT 0,
  `quantity` DECIMAL(14,2) DEFAULT 1,
  `amount` DECIMAL(14,2) DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` BIGINT DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_quotation` (`quotation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价明细';

-- 菜单：报价管理
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(145, 80, '报价管理', 2, 'quotation', 'crm/quotation/index', 'crm:quote:list', 'Document', 12, 1, NOW(), NOW(), 1, 0),
(146, 145, '报价保存', 3, NULL, NULL, 'crm:quote:save', NULL, 1, 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id FROM sys_role_menu rm, sys_menu m
  WHERE m.id IN (145, 146) AND m.deleted = 0
  AND rm.menu_id IN (SELECT id FROM sys_menu WHERE permission = 'crm:contract:list' AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.id IN (145, 146) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = 1 AND x.menu_id = m.id);
