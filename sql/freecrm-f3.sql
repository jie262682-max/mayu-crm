-- F3: 发票管理（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

CREATE TABLE IF NOT EXISTS `crm_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT NOT NULL COMMENT '合同 id',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '1普票 2专票',
  `amount` DECIMAL(14,2) NOT NULL COMMENT '开票金额',
  `title` VARCHAR(120) NOT NULL COMMENT '发票票头',
  `tax_no` VARCHAR(32) DEFAULT NULL COMMENT '纳税人识别号（专票必填）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1已开 2驳回',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` BIGINT DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票';

-- 开票审批流（bizType=5，财务 → 总经理，参照回款流程）
INSERT INTO `oa_flow_def` (`biz_type`, `name`, `nodes`, `status`, `create_time`, `update_time`, `creator`, `deleted`)
SELECT 5, 'invoice-std', '[{"level":1,"approverUserIds":[7]},{"level":2,"approverUserIds":[1]}]', 1, NOW(), NOW(), 1, 0
WHERE NOT EXISTS (SELECT 1 FROM `oa_flow_def` WHERE `biz_type` = 5 AND `deleted` = 0);

-- 菜单：发票管理
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(143, 80, '发票管理', 2, 'invoice', 'crm/invoice/index', 'crm:invoice:list', 'Tickets', 11, 1, NOW(), NOW(), 1, 0),
(144, 143, '开票申请', 3, NULL, NULL, 'crm:invoice:save', NULL, 1, 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

-- 超管授权 + 有合同权限的角色（销售可申请开票）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id FROM sys_role_menu rm, sys_menu m
  WHERE m.id IN (143, 144) AND m.deleted = 0
  AND rm.menu_id IN (SELECT id FROM sys_menu WHERE permission = 'crm:contract:list' AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.id IN (143, 144) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = 1 AND x.menu_id = m.id);
