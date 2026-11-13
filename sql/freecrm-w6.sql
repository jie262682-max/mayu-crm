-- W6: 合同到期提醒 + 公海规则增强 + 看板增强
-- 可重复导入（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 合同到期日期（MySQL 5.7 无 ADD COLUMN IF NOT EXISTS，用 information_schema 守卫）
SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract' AND COLUMN_NAME = 'end_date') = 0,
  'ALTER TABLE `crm_contract` ADD COLUMN `end_date` DATE NULL DEFAULT NULL COMMENT ''到期日期'' AFTER `order_date`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 客户领取时间（公海领取保护期依据）
SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'owner_time') = 0,
  'ALTER TABLE `crm_customer` ADD COLUMN `owner_time` DATETIME NULL DEFAULT NULL COMMENT ''领取/转移时间'' AFTER `owner_user_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 配置种子
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('pool.recycle.enabled', '1', '公海自动回收开关：1启用 0停用'),
('pool.protect.days', '7', '领取保护期：客户被领取/转移后 N 天内不被自动回收'),
('contract.remind.days', '30', '合同到期提醒：到期前 N 天开始提醒负责人')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
