-- F7: 联系人生日关怀提醒（幂等）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

SET @s = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contact' AND COLUMN_NAME = 'birthday') = 0,
  'ALTER TABLE `crm_contact` ADD COLUMN `birthday` DATE NULL DEFAULT NULL COMMENT ''生日（关怀提醒）'' AFTER `post`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
