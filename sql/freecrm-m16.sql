-- M16: 客户资料（附件）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;

CREATE TABLE IF NOT EXISTS `crm_customer_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '存储相对路径',
  `file_size` bigint DEFAULT 0 COMMENT '字节',
  `content_type` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL COMMENT '上传人',
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户资料附件';

-- 跟进附件关联（可空：客户资料区上传的为 NULL，跟进时上传的指向跟进记录）
ALTER TABLE `crm_customer_file` ADD COLUMN `follow_up_id` bigint DEFAULT NULL COMMENT '关联跟进记录' AFTER `remark`;
