-- F2: 拜访签到（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 客户坐标（签到距离校验依据，选填）
SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'address_lng') = 0,
  'ALTER TABLE `crm_customer` ADD COLUMN `address_lng` DECIMAL(10,6) NULL DEFAULT NULL COMMENT ''客户地址经度'' AFTER `address`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'address_lat') = 0,
  'ALTER TABLE `crm_customer` ADD COLUMN `address_lat` DECIMAL(10,6) NULL DEFAULT NULL COMMENT ''客户地址纬度'' AFTER `address_lng`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 签到记录
CREATE TABLE IF NOT EXISTS `crm_checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `customer_id` BIGINT NOT NULL COMMENT '客户 id',
  `user_id` BIGINT NOT NULL COMMENT '签到人',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '签到点经度',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '签到点纬度',
  `distance` INT DEFAULT NULL COMMENT '距客户坐标米数（客户无坐标为 NULL）',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` BIGINT DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拜访签到';

-- 菜单：拜访签到
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(142, 80, '拜访签到', 2, 'checkin', 'crm/checkin/index', 'crm:checkin:list', 'LocationInformation', 10, 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

-- 超管授权 + 已有客户列表权限的角色（销售均可见自己的签到）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 142 FROM sys_role_menu rm
  JOIN sys_menu m ON m.id = rm.menu_id AND m.permission = 'crm:customer:list' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 142);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 142 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 142);
