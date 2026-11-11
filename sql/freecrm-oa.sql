-- freecrm M4：OA 审批流
USE `freecrm`;

-- 流程定义（顺序节点 JSON）
DROP TABLE IF EXISTS `oa_flow_def`;
CREATE TABLE `oa_flow_def` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '流程名',
  `biz_type` tinyint NOT NULL COMMENT '业务:1合同审批2回款审批3请假',
  `nodes` text NOT NULL COMMENT '顺序节点JSON [{level,approverUserIds,note}]',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用1启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_type`, `deleted`)
) ENGINE=InnoDB COMMENT='流程定义';

-- 流程实例
DROP TABLE IF EXISTS `oa_flow_instance`;
CREATE TABLE `oa_flow_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `flow_def_id` bigint NOT NULL,
  `biz_type` tinyint NOT NULL,
  `biz_id` bigint NOT NULL,
  `biz_title` varchar(128) DEFAULT NULL COMMENT '展示标题',
  `applicant_user_id` bigint NOT NULL,
  `current_level` int NOT NULL DEFAULT 1 COMMENT '当前审批层级',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0进行中1通过2驳回3撤回',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_type`, `biz_id`, `deleted`)
) ENGINE=InnoDB COMMENT='流程实例';

-- 审批任务（每节点每审批人一条）
DROP TABLE IF EXISTS `oa_flow_task`;
CREATE TABLE `oa_flow_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL,
  `node_level` int NOT NULL,
  `approver_user_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待办1通过2驳回',
  `opinion` varchar(255) DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_approver` (`approver_user_id`, `status`)
) ENGINE=InnoDB COMMENT='审批任务';

-- 请假（示例业务）
DROP TABLE IF EXISTS `oa_leave`;
CREATE TABLE `oa_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '1事假2病假3年假4调休',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `days` decimal(4,1) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0审批中1通过2驳回',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` bigint DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='请假申请';
