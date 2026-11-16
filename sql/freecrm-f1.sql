-- F1: 业绩目标与完成率（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 月度销售目标（物理删：save 时按月重建，避免逻辑删与唯一键冲突）
CREATE TABLE IF NOT EXISTS `crm_sales_target` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 id',
  `month` CHAR(7) NOT NULL COMMENT '月份 yyyy-MM',
  `amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '目标金额（签约口径）',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` BIGINT DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_month` (`user_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售业绩目标';

-- 菜单：目标管理（CRM 目录 80 下）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(140, 80, '目标管理', 2, 'target', 'crm/target/index', 'crm:target:list', 'Trophy', 9, 1, NOW(), NOW(), 1, 0),
(141, 140, '目标保存', 3, NULL, NULL, 'crm:target:save', NULL, 1, 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

-- 超管授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.id IN (140, 141) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
