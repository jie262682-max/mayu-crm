-- W2: 商机产品明细 + 跟进提醒无需表（复用客户字段）
CREATE DATABASE IF NOT EXISTS `freecrm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `freecrm`;
CREATE TABLE IF NOT EXISTS `crm_opp_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `opportunity_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` decimal(18,4) NOT NULL DEFAULT 1,
  `price` decimal(18,2) NOT NULL DEFAULT 0,
  `amount` decimal(18,2) NOT NULL DEFAULT 0,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_opp` (`opportunity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机产品明细';
