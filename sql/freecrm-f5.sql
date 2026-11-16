-- F5: 线索池（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

-- 线索领取时间（保护期依据）
SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_lead' AND COLUMN_NAME = 'owner_time') = 0,
  'ALTER TABLE `crm_lead` ADD COLUMN `owner_time` DATETIME NULL DEFAULT NULL COMMENT ''领取/转移时间'' AFTER `owner_user_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 配置种子（保护期复用 pool.protect.days）
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('pool.lead.enabled', '1', '线索池自动回收开关：1启用 0停用'),
('pool.lead.recycle.days', '30', '线索超过 N 天未跟进自动回收至线索池')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
