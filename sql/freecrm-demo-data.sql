-- =====================================================================
-- FreeCRM 演示数据（可重复导入：先清业务表，保留 admin/菜单/流程定义结构）
-- 所有演示账号密码统一：admin123
-- 导入：mysql -uroot -p --default-character-set=utf8mb4 freecrm < freecrm-demo-data.sql
-- =====================================================================

-- ---------- 0. 清理业务数据（保留菜单结构 / 流程定义，可重复导入） ----------
TRUNCATE TABLE crm_lead;
TRUNCATE TABLE crm_customer;
TRUNCATE TABLE crm_contact;
TRUNCATE TABLE crm_follow_up;
TRUNCATE TABLE crm_team_member;
TRUNCATE TABLE crm_opportunity;
TRUNCATE TABLE crm_stage_group;
TRUNCATE TABLE crm_stage;
TRUNCATE TABLE crm_contract;
TRUNCATE TABLE crm_payment_plan;
TRUNCATE TABLE crm_payment;
TRUNCATE TABLE crm_product_category;
TRUNCATE TABLE crm_product;
TRUNCATE TABLE oa_flow_instance;
TRUNCATE TABLE oa_flow_task;
TRUNCATE TABLE oa_leave;

-- 演示用的部门/用户/角色/授权也先清（admin 与菜单 79-98 保留）
DELETE FROM sys_user_role WHERE user_id > 1;
DELETE FROM sys_role_menu WHERE role_id > 1;
DELETE FROM sys_role WHERE id > 1;
DELETE FROM sys_user WHERE id > 1;
DELETE FROM sys_dept WHERE id > 1;
-- 菜单不清删：id>=100 含 EAM(128-137)/系统设置等后期菜单，误删导致侧栏消失（2026-11-12 修复）


SET @PWD = '$2a$10$yzjb.gwLKZbB3jiRCjAHH.t6Zrhc6Wy1W/OPMlAfsjvINvBWQF1Rm';
-- ---------- 0.1 根账号种子（幂等）：admin/总部/super_admin 及菜单授权 ----------
INSERT INTO sys_dept (id, parent_id, name, leader_user_id, sort, status, create_time, update_time, creator, deleted)
SELECT 1, 0, '总部', NULL, 0, 1, NOW(), NOW(), 1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 1);
INSERT INTO sys_role (id, name, code, sort, status, remark, create_time, update_time, creator, deleted)
SELECT 1, '超级管理员', 'super_admin', 0, 1, '全部权限', NOW(), NOW(), 1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 1);
INSERT INTO sys_user (id, username, password, nickname, mobile, email, dept_id, status, create_time, update_time, creator, deleted)
SELECT 1, 'admin', @PWD, '管理员', '13800000000', 'admin@maoyu.cn', 1, 1, NOW(), NOW(), 1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 1);
INSERT INTO sys_user_role (user_id, role_id) SELECT 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.deleted = 0 AND m.type <= 2
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);

-- ---------- 1. 部门 ----------
INSERT INTO sys_dept (id, parent_id, name, leader_user_id, sort, status, create_time, update_time, creator, deleted) VALUES
(2, 1, '销售部', 2, 1, 1, NOW(), NOW(), 1, 0),
(3, 2, '华东销售组', 4, 1, 1, NOW(), NOW(), 1, 0),
(4, 2, '华南销售组', 5, 2, 1, NOW(), NOW(), 1, 0),
(5, 1, '研发部', 6, 2, 1, NOW(), NOW(), 1, 0),
(6, 1, '人事行政部', 7, 3, 1, NOW(), NOW(), 1, 0),
(7, 1, '财务部', 8, 4, 1, NOW(), NOW(), 1, 0);

-- ---------- 2. 用户（密码同 admin123） ----------
INSERT INTO sys_user (id, username, password, nickname, mobile, email, dept_id, status, create_time, update_time, creator, deleted) VALUES
(2, 'zhang3',  @PWD, '张三（销售总监）', '13800000002', 'zhang3@freecrm.cn',  2, 1, NOW(), NOW(), 1, 0),
(4, 'wang5',   @PWD, '王五（华东销售）', '13800000004', 'wang5@freecrm.cn',   3, 1, NOW(), NOW(), 1, 0),
(5, 'zhao6',   @PWD, '赵六（华南销售）', '13800000005', 'zhao6@freecrm.cn',   4, 1, NOW(), NOW(), 1, 0),
(6, 'li7',     @PWD, '李七（研发经理）', '13800000006', 'li7@freecrm.cn',     5, 1, NOW(), NOW(), 1, 0),
(7, 'hrzhou',  @PWD, '周蕙（人事主管）', '13800000007', 'hrzhou@freecrm.cn',  6, 1, NOW(), NOW(), 1, 0),
(8, 'finwu',   @PWD, '吴敏（财务）',     '13800000008', 'finwu@freecrm.cn',   7, 1, NOW(), NOW(), 1, 0),
(9, 'guest',   @PWD, '顾客（只读演示）', '13800000009', 'guest@freecrm.cn',   1, 1, NOW(), NOW(), 1, 0);

-- ---------- 3. 角色 ----------
INSERT INTO sys_role (id, name, code, sort, status, remark, create_time, update_time, creator, deleted) VALUES
(2, '销售经理', 'sales_manager', 2, 1, 'CRM 全部 + 合同审核 + 流程定义', NOW(), NOW(), 1, 0),
(3, '销售员',   'sales',         3, 1, 'CRM 业务操作，无审核/删除权',   NOW(), NOW(), 1, 0),
(4, '财务',     'finance',       4, 1, '合同/回款审核与录入',           NOW(), NOW(), 1, 0),
(5, '人事',     'hr',            5, 1, '用户管理 + OA 审批',            NOW(), NOW(), 1, 0),
(6, '只读演示', 'viewer',        6, 1, '全部列表只读，无写操作',         NOW(), NOW(), 1, 0);

-- ---------- 4. 按钮级权限菜单（type=3 不进侧栏，只授权限码） ----------
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(100, 86, '线索新增/编辑', 3, '', NULL, 'crm:lead:save',      NULL, 1, 0, NOW(), NOW(), 1, 0),
(101, 86, '线索转化',      3, '', NULL, 'crm:lead:transform', NULL, 2, 0, NOW(), NOW(), 1, 0),
(102, 86, '线索删除',      3, '', NULL, 'crm:lead:delete',    NULL, 3, 0, NOW(), NOW(), 1, 0),
(103, 87, '客户新增/编辑', 3, '', NULL, 'crm:customer:save',   NULL, 1, 0, NOW(), NOW(), 1, 0),
(104, 87, '公海领取',      3, '', NULL, 'crm:customer:receive',NULL, 2, 0, NOW(), NOW(), 1, 0),
(105, 87, '客户删除',      3, '', NULL, 'crm:customer:delete', NULL, 3, 0, NOW(), NOW(), 1, 0),
(106, 89, '联系人新增/编辑',3,'', NULL, 'crm:contact:save',   NULL, 1, 0, NOW(), NOW(), 1, 0),
(107, 89, '联系人删除',    3, '', NULL, 'crm:contact:delete',  NULL, 2, 0, NOW(), NOW(), 1, 0),
(108, 90, '商机新增/编辑', 3, '', NULL, 'crm:opportunity:save',NULL, 1, 0, NOW(), NOW(), 1, 0),
(109, 90, '商机删除',      3, '', NULL, 'crm:opportunity:delete',NULL,2,0, NOW(), NOW(), 1, 0),
(110, 91, '合同新增/提交', 3, '', NULL, 'crm:contract:save',  NULL, 1, 0, NOW(), NOW(), 1, 0),
(111, 91, '合同审核',      3, '', NULL, 'crm:contract:audit', NULL, 2, 0, NOW(), NOW(), 1, 0),
(112, 91, '合同删除',      3, '', NULL, 'crm:contract:delete',NULL, 3, 0, NOW(), NOW(), 1, 0),
(113, 92, '回款录入',      3, '', NULL, 'crm:contract:payment',NULL,1, 0, NOW(), NOW(), 1, 0),
(114, 93, '产品新增/编辑', 3, '', NULL, 'crm:product:save',   NULL, 1, 0, NOW(), NOW(), 1, 0),
(115, 93, '产品删除',      3, '', NULL, 'crm:product:delete', NULL, 2, 0, NOW(), NOW(), 1, 0),
(116, 82, '用户新增/编辑', 3, '', NULL, 'sys:user:save',      NULL, 1, 0, NOW(), NOW(), 1, 0),
(117, 82, '用户删除',      3, '', NULL, 'sys:user:delete',    NULL, 2, 0, NOW(), NOW(), 1, 0),
(118, 83, '角色新增/编辑', 3, '', NULL, 'sys:role:save',      NULL, 1, 0, NOW(), NOW(), 1, 0),
(119, 83, '角色删除',      3, '', NULL, 'sys:role:delete',    NULL, 2, 0, NOW(), NOW(), 1, 0),
(120, 84, '菜单新增/编辑', 3, '', NULL, 'sys:menu:save',      NULL, 1, 0, NOW(), NOW(), 1, 0),
(121, 84, '菜单删除',      3, '', NULL, 'sys:menu:delete',    NULL, 2, 0, NOW(), NOW(), 1, 0),
(122, 85, '部门新增/编辑', 3, '', NULL, 'sys:dept:save',      NULL, 1, 0, NOW(), NOW(), 1, 0),
(123, 85, '部门删除',      3, '', NULL, 'sys:dept:delete',    NULL, 2, 0, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

-- ---------- 5. 角色-菜单授权 ----------
-- 销售经理：CRM 全部 + OA 全部 + 全部 CRM 写权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.id FROM sys_menu m
WHERE m.id IN (80,81,86,87,88,89,90,91,92,93,94,95,96,97,98,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 2 AND rm.menu_id = m.id);
-- 销售员：CRM 全部页面 + 基础写权限（无删除/无审核）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, m.id FROM sys_menu m
WHERE m.id IN (80,86,87,88,89,90,91,92,93,94,100,101,103,104,106,108,110,113) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 3 AND rm.menu_id = m.id);
-- 财务：客户/合同/回款查看 + 合同审核 + 回款录入 + OA 待办
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 4, m.id FROM sys_menu m
WHERE m.id IN (80,87,91,92,81,95,96,110,111,113) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 4 AND rm.menu_id = m.id);
-- 人事：系统-用户管理 + OA 全部 + 用户编辑
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 5, m.id FROM sys_menu m
WHERE m.id IN (79,82,81,95,96,97,98,116) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 5 AND rm.menu_id = m.id);
-- 只读演示：CRM 全部列表 + OA 待办/申请
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 6, m.id FROM sys_menu m
WHERE m.id IN (80,86,87,88,89,90,91,92,93,94,81,95,96) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 6 AND rm.menu_id = m.id);

-- ---------- 6. 用户-角色 ----------
INSERT INTO sys_user_role (user_id, role_id) VALUES
(2, 2), (4, 3), (5, 3), (6, 6), (7, 5), (8, 4), (9, 6);

-- ---------- 7. 流程定义（两级审批；INSERT 保证空库也有定义，UPDATE 保证旧库升级） ----------
INSERT INTO oa_flow_def (id, name, biz_type, nodes, status, create_time, update_time, creator, deleted) VALUES
(1, 'contract-std', 1, '[{"level":1,"approverUserIds":[2]},{"level":2,"approverUserIds":[1]}]', 1, NOW(), NOW(), 1, 0),
(2, 'payment-std',  2, '[{"level":1,"approverUserIds":[8]},{"level":2,"approverUserIds":[1]}]', 1, NOW(), NOW(), 1, 0),
(3, 'leave-std',    3, '[{"level":1,"approverUserIds":[7]},{"level":2,"approverUserIds":[1]}]', 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE nodes = VALUES(nodes), status = 1, deleted = 0;

-- ---------- 8. 产品 ----------
INSERT INTO crm_product_category (id, name, parent_id, sort, create_time, update_time, creator, deleted) VALUES
(1, '软件产品', 0, 1, NOW(), NOW(), 1, 0),
(2, '服务产品', 0, 2, NOW(), NOW(), 1, 0),
(3, '硬件产品', 0, 3, NOW(), NOW(), 1, 0);

INSERT INTO crm_product (id, name, no, category_id, price, unit, status, remark, create_time, update_time, creator, deleted) VALUES
(1, 'FreeCRM 标准版（年订阅）', 'SW-001', 1, 8800.00,  3,   1, '10 用户以内，含移动端',       NOW(), NOW(), 1, 0),
(2, 'FreeCRM 企业版（年订阅）', 'SW-002', 1, 39800.00, 3,   1, '不限用户，含审批流与统计',     NOW(), NOW(), 1, 0),
(3, '客户数据大屏模块',         'SW-003', 1, 12000.00, 2,   1, '可视化数据大屏，按年授权',     NOW(), NOW(), 1, 0),
(4, 'CRM 实施部署服务',         'SV-001', 2, 5000.00,  4, 1, '含需求调研/部署/数据迁移',     NOW(), NOW(), 1, 0),
(5, '团队培训服务',             'SV-002', 2, 3000.00,  4,   1, '现场或线上培训',               NOW(), NOW(), 1, 0),
(6, '营销短信包',               'SV-003', 2, 680.00,   1, 1, '跟进提醒/生日祝福群发',        NOW(), NOW(), 1, 0),
(7, '定制开发服务',             'SV-004', 2, 2000.00,  4, 1, '按需定制模块',                 NOW(), NOW(), 1, 0),
(8, 'POS 收银一体机',           'HW-001', 3, 3200.00,  1,   0, '已停产，仅供历史合同查看',     NOW(), NOW(), 1, 0);

-- ---------- 9. 商机阶段 ----------
INSERT INTO crm_stage_group (id, name, create_time, update_time, creator, deleted) VALUES
(1, '标准销售流程', NOW(), NOW(), 1, 0),
(2, '大客户流程',   NOW(), NOW(), 1, 0);

INSERT INTO crm_stage (id, group_id, name, percent, sort, create_time, update_time, creator, deleted) VALUES
(1, 1, '初步接触', 10, 1, NOW(), NOW(), 1, 0),
(2, 1, '需求确认', 30, 2, NOW(), NOW(), 1, 0),
(3, 1, '方案报价', 60, 3, NOW(), NOW(), 1, 0),
(4, 1, '商务谈判', 80, 4, NOW(), NOW(), 1, 0),
(5, 1, '待成交',   95, 5, NOW(), NOW(), 1, 0),
(6, 2, '初步接触', 10, 1, NOW(), NOW(), 1, 0),
(7, 2, '技术验证', 50, 2, NOW(), NOW(), 1, 0),
(8, 2, '框架谈判', 90, 3, NOW(), NOW(), 1, 0);

-- ---------- 10. 线索 ----------
INSERT INTO crm_lead (id, name, contact_name, mobile, email, source, industry, remark, owner_user_id, transform_status, customer_id, next_contact_time, last_contact_time, create_time, update_time, creator, deleted) VALUES
(1,  '阿里云大会-钱多多',   '钱多多', '13911110001', 'qian@aly.cn',     3, 2, '大会展台留资，对标准版感兴趣', 4, 1, 1, NULL,                          '2026-10-12 15:00:00', '2026-10-11 10:20:00', NOW(), 4, 0),
(2,  '官网咨询-SaaS版',     '孙浩',   '13911110002', 'sun@test.cn',     2, 2, '官网表单，要报价单',           2, 0, NULL, '2026-11-12 10:00:00', '2026-11-06 14:30:00', '2026-11-06 14:00:00', NOW(), 2, 0),
(3,  '展会名片-李老板',     '李国富', '13911110003', NULL,              3, 3, '机械行业展会换名片',           5, 0, NULL, '2026-11-11 09:30:00', '2026-11-04 16:00:00', '2026-11-04 15:40:00', NOW(), 5, 0),
(4,  '朋友推荐-孙总',       '孙立',   '13911110004', 'sunli@xd.cn',     4, 1, '老客户转介绍，意向明确',       4, 1, 2, NULL,                          '2026-10-20 11:00:00', '2026-10-18 09:00:00', NOW(), 4, 0),
(5,  '百度广告-周女士',     '周颖',   '13911110005', NULL,              5, 4, '搜索"CRM系统"进落地页留资',    2, 0, NULL, '2026-11-13 14:00:00', '2026-11-07 10:10:00', '2026-11-07 10:00:00', NOW(), 2, 0),
(6,  '老客户转介绍-吴总',   '吴天华', '13911110006', 'wuth@ljzn.cn',    4, 2, '蓝鲸智能老板介绍',             5, 1, 3, NULL,                          '2026-10-25 17:20:00', '2026-10-23 17:00:00', NOW(), 5, 0),
(7,  '400来电-郑先生',      '郑凯',   '13911110007', NULL,              1, 3, '咨询硬件+软件整体方案',         4, 0, NULL, '2026-11-10 15:00:00', '2026-11-08 11:30:00', '2026-11-08 11:20:00', NOW(), 4, 0),
(8,  '行业峰会-王总',       '王志远', '13911110008', 'wangzy@zkzl.cn',  3, 2, '中科智联峰会遇到，聊过需求',   2, 0, NULL, '2026-11-15 10:00:00', '2026-10-30 09:40:00', '2026-10-30 09:30:00', NOW(), 2, 0),
(9,  '微信咨询-冯经理',     '冯娟',   '13911110009', NULL,              6, 4, '公众号菜单进来的咨询',         5, 0, NULL, '2026-11-14 09:00:00', '2026-11-09 10:00:00', '2026-11-09 09:50:00', NOW(), 5, 0),
(10, '淘宝店铺咨询-陈店长', '陈晨',   '13911110010', NULL,              6, 1, '想给连锁店上 CRM',             4, 0, NULL, '2026-11-12 16:00:00', '2026-11-05 13:00:00', '2026-11-05 12:40:00', NOW(), 4, 0),
(11, '电话回访-何主任',     '何建',   '13911110011', NULL,              1, 5, '医院信息科，二期预算咨询',     1, 0, NULL, '2026-11-20 10:00:00', '2026-11-06 15:00:00', '2026-11-06 14:50:00', NOW(), 1, 0),
(12, '展会返场-郭经理',     '郭涛',   '13911110012', NULL,              3, 1, '返场沟通后决定试用',           4, 1, 4, NULL,                          '2026-10-29 10:00:00', '2026-10-27 10:00:00', NOW(), 4, 0);

-- ---------- 11. 客户（2 个公海 owner 为空） ----------
INSERT INTO crm_customer (id, name, mobile, telephone, email, website, address, remark, source, industry, level, owner_user_id, lock_status, deal_status, next_contact_time, last_contact_time, create_time, update_time, creator, deleted) VALUES
(1,  '杭州云启科技有限公司',   '13700000001', '0571-88880001', 'contact@yunqi.cn',  'www.yunqi.cn',  '杭州市余杭区梦想小镇', 'SaaS 标杆客户',            6, 2, 1, 4,    0, 1, '2026-11-15 10:00:00', '2026-11-08 16:00:00', '2026-10-12 10:00:00', NOW(), 4, 0),
(2,  '上海鑫达贸易有限公司',   '13700000002', '021-66660002',  'sales@xinda.cn',   NULL,           '上海市闵行区',         '由线索转化',               4, 1, 2, 4,    0, 0, '2026-11-12 14:00:00', '2026-11-05 10:00:00', '2026-10-19 09:30:00', NOW(), 4, 0),
(3,  '深圳蓝鲸智能科技',       '13700000003', '0755-77770003', 'bd@lanjing.cn',    'www.lanjing.cn','深圳市南山区科技园',   'ERP 整体方案成交客户',     5, 2, 1, 5,    0, 1, '2026-11-20 10:00:00', '2026-11-07 11:00:00', '2026-10-24 14:00:00', NOW(), 5, 0),
(4,  '广州优选百货连锁',       '13700000004', '020-55550004',  NULL,               NULL,           '广州市天河区',         '20 家连锁店，短信包意向',   1, 1, 3, 4,    0, 0, '2026-11-11 15:00:00', '2026-11-05 15:20:00', '2026-10-28 11:00:00', NOW(), 4, 0),
(5,  '北京中科智联科技',       '13700000005', '010-33330005',  'pk@zkzl.cn',       'www.zkzl.cn',  '北京市海淀区中关村',   '大客户流程，需技术验证',   2, 2, 2, 2,    0, 0, '2026-11-14 10:30:00', '2026-11-06 09:40:00', '2026-10-31 09:00:00', NOW(), 2, 0),
(6,  '苏州恒泰机械设备',       '13700000006', '0512-22220006', NULL,               NULL,           '苏州市工业园区',       '轻量版试点意向',           3, 3, 3, 2,    0, 0, '2026-11-13 09:00:00', '2026-11-04 14:00:00', '2026-11-02 10:00:00', NOW(), 2, 0),
(7,  '杭州明州医院',           '13700000007', '0571-99990007', 'info@mzhosp.cn',   NULL,           '杭州市上城区',         '二期项目已签约待启动',     6, 5, 1, 1,    0, 1, NULL,                 '2026-11-09 10:00:00', '2026-10-15 10:00:00', NOW(), 1, 0),
(8,  '成都锦程物流有限公司',   '13700000008', '028-11110008',  NULL,               NULL,           '成都市高新区',         '关注司机管理场景',         2, 4, 4, 5,    0, 0, '2026-11-18 11:00:00', '2026-11-03 16:00:00', '2026-10-21 15:00:00', NOW(), 5, 0),
(9,  '南京紫峰商贸',           '13700000009', '025-44440009',  NULL,               NULL,           '南京市鼓楼区',         '公海客户，欢迎领取',       2, 1, 3, NULL, 0, 0, NULL, NULL,                 '2026-10-26 10:00:00', NOW(), 4, 0),
(10, '武汉光谷软件园入驻企业', '13700000010', '027-22220010',  NULL,               NULL,           '武汉市东湖高新区',     '公海客户，软件园渠道推荐', 5, 2, 2, NULL, 0, 0, NULL, NULL,                 '2026-10-29 10:00:00', NOW(), 5, 0),
(11, '东莞锐创电子厂',         '13700000011', '0769-88880011', NULL,               NULL,           '东莞市长安镇',         '已锁定（合同争议处理中）', 3, 3, 2, 5,    1, 1, NULL,                 '2026-11-01 10:00:00', '2026-10-16 10:00:00', NOW(), 5, 0),
(12, '西安秦风文旅集团',       '13700000012', '029-66660012',  NULL,               NULL,           '西安市曲江新区',       '文旅综合体，预算待明确',   2, 4, 4, 2,    0, 0, '2026-11-25 14:00:00', '2026-11-02 11:00:00', '2026-11-01 16:00:00', NOW(), 2, 0);

-- ---------- 12. 团队成员（1=负责人 2=可写 3=只读；公海客户无记录） ----------
INSERT INTO crm_team_member (id, biz_type, biz_id, user_id, level, create_time, update_time, creator, deleted) VALUES
(1,  2, 1,  4, 1, NOW(), NOW(), 4, 0),
(2,  2, 1,  5, 3, NOW(), NOW(), 4, 0),   -- 赵六对云启只读
(3,  2, 1,  2, 2, NOW(), NOW(), 4, 0),   -- 张三对云启可写
(4,  2, 2,  4, 1, NOW(), NOW(), 4, 0),
(5,  2, 3,  5, 1, NOW(), NOW(), 5, 0),
(6,  2, 4,  4, 1, NOW(), NOW(), 4, 0),
(7,  2, 5,  2, 1, NOW(), NOW(), 2, 0),
(8,  2, 5,  4, 3, NOW(), NOW(), 2, 0),   -- 王五对中科智联只读
(9,  2, 6,  2, 1, NOW(), NOW(), 2, 0),
(10, 2, 7,  1, 1, NOW(), NOW(), 1, 0),
(11, 2, 7,  2, 2, NOW(), NOW(), 1, 0),   -- 张三对明州可写
(12, 2, 8,  5, 1, NOW(), NOW(), 5, 0),
(13, 2, 11, 5, 1, NOW(), NOW(), 5, 0),
(14, 2, 11, 1, 3, NOW(), NOW(), 5, 0),   -- admin 对锐创只读
(15, 2, 12, 2, 1, NOW(), NOW(), 2, 0);

-- ---------- 13. 联系人 ----------
INSERT INTO crm_contact (id, name, customer_id, mobile, email, post, decision_role, remark, owner_user_id, create_time, update_time, creator, deleted) VALUES
(1,  '钱程',   1,  '13611110001', 'qiancheng@yunqi.cn', 'CTO',        1, '技术决策人',       4, '2026-10-13 10:00:00', NOW(), 4, 0),
(2,  '罗丽',   1,  '13611110002', 'luoli@yunqi.cn',     '行政采购',   2, '续约对接人',       4, '2026-10-20 10:00:00', NOW(), 4, 0),
(3,  '孙立',   2,  '13611110003', 'sunli@xinda.cn',     '总经理',     1, '一把手拍板',       4, '2026-10-19 10:00:00', NOW(), 4, 0),
(4,  '李静',   2,  '13611110004', NULL,                 '财务主管',   2, NULL,               4, '2026-10-22 10:00:00', NOW(), 4, 0),
(5,  '吴天华', 3,  '13611110005', 'wuth@lanjing.cn',    '董事长',     1, '转介绍来源',       5, '2026-10-24 10:00:00', NOW(), 5, 0),
(6,  '黄敏',   3,  '13611110006', 'huangmin@lanjing.cn','信息部经理', 3, 'ERP 对接负责人',   5, '2026-10-26 10:00:00', NOW(), 5, 0),
(7,  '陈晨',   4,  '13611110007', NULL,                 '运营店长',   3, '连锁店运营',       4, '2026-10-28 10:00:00', NOW(), 4, 0),
(8,  '王志远', 5,  '13611110008', 'wangzy@zkzl.cn',     '销售副总',   1, '峰会认识',         2, '2026-10-31 10:00:00', NOW(), 2, 0),
(9,  '刘芳',   5,  '13611110009', 'liufang@zkzl.cn',    'IT 主管',    2, '技术验证对接',     2, '2026-11-03 10:00:00', NOW(), 2, 0),
(10, '周强',   6,  '13611110010', NULL,                 '设备科长',   3, NULL,               2, '2026-11-02 10:00:00', NOW(), 2, 0),
(11, '何建',   7,  '13611110011', 'hejian@mzhosp.cn',   '信息科主任', 1, '二期项目负责人',   1, '2026-10-15 10:00:00', NOW(), 1, 0),
(12, '马丽',   7,  '13611110012', NULL,                 '采购办',     2, NULL,               1, '2026-10-18 10:00:00', NOW(), 1, 0),
(13, '杨帆',   8,  '13611110013', NULL,                 '调度主管',   3, '司机管理需求方',   5, '2026-10-22 10:00:00', NOW(), 5, 0),
(14, '郭涛',   11, '13611110014', NULL,                 '厂长',       1, '合同争议中',       5, '2026-10-17 10:00:00', NOW(), 5, 0),
(15, '袁媛',   12, '13611110015', NULL,                 '办公室主任', 2, '文旅项目对接',     2, '2026-11-02 10:00:00', NOW(), 2, 0);

-- ---------- 14. 跟进记录（时间分散，喂饱统计图） ----------
INSERT INTO crm_follow_up (id, biz_type, biz_id, type, content, next_time, create_time, update_time, creator, deleted) VALUES
(1,  1, 1,  1, '电话初步沟通，发送产品介绍 PDF',                '2026-10-12 10:00:00', '2026-10-11 10:20:00', NOW(), 4, 0),
(2,  1, 1,  2, '上门演示标准版，CTO 满意',                      '2026-10-12 15:00:00', '2026-10-12 15:00:00', NOW(), 4, 0),
(3,  1, 4,  3, '微信发送报价，约定电话会议',                    '2026-10-18 10:00:00', '2026-10-18 09:00:00', NOW(), 4, 0),
(4,  1, 4,  1, '电话会议确认采购意向，转客户',                  NULL,                  '2026-10-20 11:00:00', NOW(), 4, 0),
(5,  1, 6,  2, '拜访蓝鲸介绍来的吴总，需求明确',                '2026-10-24 14:00:00', '2026-10-23 17:00:00', NOW(), 5, 0),
(6,  1, 12, 1, '返场电话确认试用开通，转客户',                  NULL,                  '2026-10-27 10:00:00', NOW(), 4, 0),
(7,  1, 2,  1, '电话沟通，对方要等预算批复',                    '2026-11-12 10:00:00', '2026-11-06 14:30:00', NOW(), 2, 0),
(8,  1, 8,  2, '中科智联峰会二次拜访，聊方案框架',              '2026-11-15 10:00:00', '2026-10-30 09:40:00', NOW(), 2, 0),
(9,  2, 1,  1, '试用期第 1 周回访，使用正常',                   NULL,                  '2026-10-19 10:00:00', NOW(), 4, 0),
(10, 2, 1,  2, '上门签标准版年单',                              NULL,                  '2026-10-20 11:00:00', NOW(), 4, 0),
(11, 2, 1,  3, '微信推数据大屏介绍，客户有兴趣',                '2026-10-28 10:00:00', '2026-10-27 09:30:00', NOW(), 4, 0),
(12, 2, 1,  1, '大屏需求电话：要 6 块屏联动',                   '2026-11-04 10:00:00', '2026-11-03 14:00:00', NOW(), 4, 0),
(13, 2, 1,  2, '大屏方案现场讲解，报价 12000',                  '2026-11-10 10:00:00', '2026-11-08 16:00:00', NOW(), 4, 0),
(14, 2, 2,  1, '首次电话回访，了解贸易进销存痛点',              NULL,                  '2026-10-22 11:00:00', NOW(), 4, 0),
(15, 2, 2,  3, '微信发行业方案合集',                            '2026-11-05 10:00:00', '2026-11-04 10:00:00', NOW(), 4, 0),
(16, 2, 2,  1, '电话跟进方案反馈：倾向标准版+短信包',            '2026-11-12 14:00:00', '2026-11-05 10:00:00', NOW(), 4, 0),
(17, 2, 3,  2, 'ERP 需求调研第一次',                            NULL,                  '2026-10-25 10:00:00', NOW(), 5, 0),
(18, 2, 3,  1, '方案报价确认，进入商务谈判',                    '2026-10-27 10:00:00', '2026-10-26 15:00:00', NOW(), 5, 0),
(19, 2, 3,  2, '签约 ERP 整体方案 39800',                       NULL,                  '2026-10-28 10:00:00', NOW(), 5, 0),
(20, 2, 3,  3, '上线回访，黄敏反馈导出功能建议',                '2026-11-10 10:00:00', '2026-11-07 11:00:00', NOW(), 5, 0),
(21, 2, 4,  1, '连锁店 CRM 场景电话沟通',                       NULL,                  '2026-11-01 10:00:00', NOW(), 4, 0),
(22, 2, 4,  3, '微信发短信包报价，等 11 月预算',                '2026-11-11 15:00:00', '2026-11-05 15:20:00', NOW(), 4, 0),
(23, 2, 5,  2, '技术验证启动会，IT 主管参加',                   '2026-11-06 10:00:00', '2026-11-05 10:00:00', NOW(), 2, 0),
(24, 2, 5,  1, '电话确认验证环境就绪',                          '2026-11-14 10:30:00', '2026-11-06 09:40:00', NOW(), 2, 0),
(25, 2, 6,  1, '轻量版试点电话沟通，约演示',                    '2026-11-06 10:00:00', '2026-11-04 14:00:00', NOW(), 2, 0),
(26, 2, 7,  2, '二期需求调研（门诊随访+会员）',                NULL,                  '2026-10-20 10:00:00', NOW(), 1, 0),
(27, 2, 7,  1, '二期方案汇报，院办通过',                        '2026-11-03 10:00:00', '2026-11-02 10:00:00', NOW(), 1, 0),
(28, 2, 7,  2, '签约后启动会',                                  NULL,                  '2026-11-09 10:00:00', NOW(), 1, 0),
(29, 2, 8,  1, '司机管理场景电话，需求偏定制',                  '2026-11-18 11:00:00', '2026-11-03 16:00:00', NOW(), 5, 0),
(30, 2, 12, 3, '微信发送文旅行业案例',                          '2026-11-25 14:00:00', '2026-11-02 11:00:00', NOW(), 2, 0);

-- ---------- 15. 商机 ----------
INSERT INTO crm_opportunity (id, name, customer_id, stage_group_id, stage_id, amount, expected_deal_date, end_status, owner_user_id, remark, create_time, update_time, creator, deleted) VALUES
(1, '云启 CRM 标准版年订',   1, 1, 3, 8800.00,  '2026-11-30', NULL, 4, '报价已出等预算',         '2026-10-27 10:00:00', NOW(), 4, 0),
(2, '云启数据大屏升级',      1, 1, 4, 12000.00, '2026-11-25', NULL, 4, '谈判中',                 '2026-11-03 14:00:00', NOW(), 4, 0),
(3, '蓝鲸 ERP 整体方案',     3, 1, 5, 39800.00, '2026-10-28', 1,    5, '已赢单签约',             '2026-10-24 14:00:00', NOW(), 5, 0),
(4, '蓝鲸续约扩容',          3, 1, 2, 8800.00,  '2026-12-15', NULL, 5, '明年续约',               '2026-11-07 11:00:00', NOW(), 5, 0),
(5, '中科智联试点',          5, 2, 7, 20000.00, '2026-12-31', NULL, 2, '大客户流程-技术验证中',  '2026-10-31 09:00:00', NOW(), 2, 0),
(6, '恒泰轻量版试点',        6, 1, 1, 8800.00,  '2026-12-10', NULL, 2, '初步接触',               '2026-11-02 10:00:00', NOW(), 2, 0),
(7, '明州医院 CRM 二期',     7, 1, 5, 120000.00,'2026-11-05', 1,    1, '已赢单，合同审批中',     '2026-10-20 10:00:00', NOW(), 1, 0),
(8, '锐创 POS+CRM 套件',     11,1, 4, 32000.00, '2026-10-25', 2,    5, '竞品低价抢单，输单',     '2026-10-17 10:00:00', NOW(), 5, 0);

-- ---------- 16. 合同 ----------
INSERT INTO crm_contract (id, contract_no, name, customer_id, opportunity_id, order_date, end_date, total_amount, received_amount, audit_status, owner_user_id, remark, create_time, update_time, creator, deleted) VALUES
(1, 'HT20261020-88001',  '云启科技 FreeCRM 标准版年订阅',     1, 1,    '2026-10-20', DATE_ADD(CURDATE(), INTERVAL 15 DAY), 8800.00,   8800.00,  2, 4, '含实施 1 人天',      '2026-10-20 11:00:00', NOW(), 4, 0),
(2, 'HT20261028-39801',  '蓝鲸智能 ERP 整体方案',            3, 3,    '2026-10-28', DATE_SUB(CURDATE(), INTERVAL 5 DAY),  39800.00,  20000.00, 2, 5, '三期付款，已逾期未收完', '2026-10-28 10:00:00', NOW(), 5, 0),
(3, 'HT20261105-12001',  '明州医院 CRM 二期建设',            7, 7,    '2026-11-05', NULL, 120000.00, 0.00,     1, 1, '审批中（张三待审）', '2026-11-05 10:00:00', NOW(), 1, 0),
(4, 'HT20261101-88002',  '恒泰机械轻量版试点',               6, 6,    '2026-11-01', NULL, 8800.00,   0.00,     0, 2, '草稿，待提交',       '2026-11-04 14:00:00', NOW(), 2, 0),
(5, 'HT20261015-32001',  '锐创电子 POS 收银套件',            11,8,    '2026-10-15', NULL, 32000.00,  0.00,     3, 5, '审批驳回：预算不足', '2026-10-15 10:00:00', NOW(), 5, 0),
(6, 'HT20261108-60001',  '优选百货营销短信包',               4, NULL, '2026-11-08', NULL, 6000.00,   0.00,     0, 4, '草稿，等客户预算',   '2026-11-08 10:00:00', NOW(), 4, 0);

-- ---------- 17. 回款计划 ----------
INSERT INTO crm_payment_plan (id, contract_id, period_no, return_time, amount, remark, create_time, update_time, creator, deleted) VALUES
(1, 2, 1, '2026-11-05', 20000.00, '首付 50%（已回）',   NOW(), NOW(), 5, 0),
(2, 2, 2, '2026-12-05', 9950.00,  '二期 25%',           NOW(), NOW(), 5, 0),
(3, 2, 3, '2027-01-05', 9950.00,  '尾款 25%',           NOW(), NOW(), 5, 0);

-- ---------- 18. 回款 ----------
INSERT INTO crm_payment (id, contract_id, plan_id, return_time, amount, method, audit_status, remark, create_time, update_time, creator, deleted) VALUES
(1, 1, NULL, '2026-10-21', 8800.00,  2, 1, '一次性全款',                     '2026-10-21 10:00:00', NOW(), 4, 0),
(2, 2, 1,    '2026-11-05', 20000.00, 2, 1, '首付，对应计划第 1 期',           '2026-11-05 10:00:00', NOW(), 5, 0),
(3, 2, 2,    '2026-11-10', 9950.00,  2, 0, '二期款，审批中（吴敏待审）',      '2026-11-09 16:00:00', NOW(), 5, 0),
(4, 2, 3,    '2026-11-07', 9950.00,  1, 2, '票据信息有误驳回，重新提交中',    '2026-11-07 10:00:00', NOW(), 5, 0);

-- ---------- 19. 请假 ----------
INSERT INTO oa_leave (id, user_id, type, start_time, end_time, days, reason, audit_status, create_time, update_time, creator, deleted) VALUES
(1, 2, 4, '2026-11-12 09:00:00', '2026-11-12 18:00:00', 1, '调休一天',           1, '2026-11-08 10:00:00', NOW(), 2, 0),
(2, 5, 2, '2026-11-13 09:00:00', '2026-11-14 18:00:00', 2, '感冒发烧，病假',     0, '2026-11-09 09:00:00', NOW(), 5, 0),
(3, 6, 3, '2026-11-03 09:00:00', '2026-11-05 18:00:00', 3, '年假出行',           2, '2026-10-30 14:00:00', NOW(), 6, 0),
(4, 4, 1, '2026-10-28 09:00:00', '2026-10-28 18:00:00', 1, '家中有事',           1, '2026-10-27 16:00:00', NOW(), 4, 0);

-- ---------- 20. 流程实例与任务（与上面业务状态一一对应） ----------
INSERT INTO oa_flow_instance (id, flow_def_id, biz_type, biz_id, biz_title, applicant_user_id, current_level, status, create_time, update_time, creator, deleted) VALUES
(1, 1, 1, 1, '合同审批：HT20261020-88001',  4, 2, 1, '2026-10-20 11:00:00', '2026-10-20 14:00:00', 4, 0),
(2, 1, 1, 2, '合同审批：HT20261028-39801',  5, 2, 1, '2026-10-28 10:00:00', '2026-10-28 16:00:00', 5, 0),
(3, 1, 1, 3, '合同审批：HT20261105-12001',  1, 1, 0, '2026-11-05 10:00:00', '2026-11-05 10:00:00', 1, 0),
(4, 1, 1, 5, '合同审批：HT20261015-32001',  5, 1, 2, '2026-10-15 10:00:00', '2026-10-16 09:00:00', 5, 0),
(5, 2, 2, 2, '回款审批：合同 HT20261028-39801 ￥20000', 5, 2, 1, '2026-11-05 10:00:00', '2026-11-06 10:00:00', 5, 0),
(6, 2, 2, 3, '回款审批：合同 HT20261028-39801 ￥9950',  5, 1, 0, '2026-11-09 16:00:00', '2026-11-09 16:00:00', 5, 0),
(7, 2, 2, 4, '回款审批：合同 HT20261028-39801 ￥9950',  5, 1, 2, '2026-11-07 10:00:00', '2026-11-07 15:00:00', 5, 0),
(8, 3, 3, 1, '请假 1 天：调休一天',         2, 2, 1, '2026-11-08 10:00:00', '2026-11-08 15:00:00', 2, 0),
(9, 3, 3, 2, '请假 2 天：感冒发烧，病假',   5, 1, 0, '2026-11-09 09:00:00', '2026-11-09 09:00:00', 5, 0),
(10,3, 3, 4, '请假 1 天：家中有事',         4, 2, 1, '2026-10-27 16:00:00', '2026-10-27 18:00:00', 4, 0),
(11,3, 3, 3, '请假 3 天：年假出行',         6, 1, 2, '2026-10-30 14:00:00', '2026-10-30 17:00:00', 6, 0);

INSERT INTO oa_flow_task (id, instance_id, node_level, approver_user_id, status, opinion, audit_time, create_time, update_time, creator, deleted) VALUES
(1,  1, 1, 2, 1, '同意，金额无误',       '2026-10-20 13:00:00', '2026-10-20 11:00:00', NOW(), 4, 0),
(2,  1, 2, 1, 1, '通过',                 '2026-10-20 14:00:00', '2026-10-20 13:00:00', NOW(), 4, 0),
(3,  2, 1, 2, 1, '同意',                 '2026-10-28 14:00:00', '2026-10-28 10:00:00', NOW(), 5, 0),
(4,  2, 2, 1, 1, '通过',                 '2026-10-28 16:00:00', '2026-10-28 14:00:00', NOW(), 5, 0),
(5,  3, 1, 2, 0, NULL,                   NULL,                  '2026-11-05 10:00:00', NOW(), 1, 0),
(6,  4, 1, 2, 2, '客户预算不足，建议改轻量方案', '2026-10-16 09:00:00', '2026-10-15 10:00:00', NOW(), 5, 0),
(7,  5, 1, 8, 1, '收款水单已核',         '2026-11-05 16:00:00', '2026-11-05 10:00:00', NOW(), 5, 0),
(8,  5, 2, 1, 1, '通过',                 '2026-11-06 10:00:00', '2026-11-05 16:00:00', NOW(), 5, 0),
(9,  6, 1, 8, 0, NULL,                   NULL,                  '2026-11-09 16:00:00', NOW(), 5, 0),
(10, 7, 1, 8, 2, '票据信息有误，请重新提交', '2026-11-07 15:00:00', '2026-11-07 10:00:00', NOW(), 5, 0),
(11, 8, 1, 7, 1, '同意',                 '2026-11-08 14:00:00', '2026-11-08 10:00:00', NOW(), 2, 0),
(12, 8, 2, 1, 1, '通过',                 '2026-11-08 15:00:00', '2026-11-08 14:00:00', NOW(), 2, 0),
(13, 9, 1, 7, 0, NULL,                   NULL,                  '2026-11-09 09:00:00', NOW(), 5, 0),
(14, 10,1, 7, 1, '同意',                 '2026-10-27 17:00:00', '2026-10-27 16:00:00', NOW(), 4, 0),
(15, 10,2, 1, 1, '通过',                 '2026-10-27 18:00:00', '2026-10-27 17:00:00', NOW(), 4, 0),
(16, 11,1, 7, 2, '项目上线期间，暂不批', '2026-10-30 17:00:00', '2026-10-30 14:00:00', NOW(), 6, 0);
