-- M17: EAM 资产管理菜单（幂等）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(128, 0,   'EAM 资产管理', 1, '/eam', 'Layout', NULL, 'Box', 15, 1, NOW(), NOW(), 1, 0),
(129, 128, '资产台账', 2, 'asset', 'eam/asset/index', 'eam:asset:list', 'Monitor', 1, 1, NOW(), NOW(), 1, 0),
(130, 128, '资产分类', 2, 'category', 'eam/category/index', 'eam:asset:list', 'Grid', 2, 1, NOW(), NOW(), 1, 0),
(131, 128, '变更记录', 2, 'change', 'eam/change/index', 'eam:asset:list', 'Switch', 3, 1, NOW(), NOW(), 1, 0),
(132, 128, '资产盘点', 2, 'check', 'eam/check/index', 'eam:asset:list', 'Search', 4, 1, NOW(), NOW(), 1, 0),
(133, 128, '折旧记录', 2, 'depreciation', 'eam/depreciation/index', 'eam:asset:list', 'Timer', 5, 1, NOW(), NOW(), 1, 0),
(134, 129, '资产新增/编辑', 3, '', NULL, 'eam:asset:save', NULL, 1, 0, NOW(), NOW(), 1, 0),
(135, 129, '资产删除', 3, '', NULL, 'eam:asset:delete', NULL, 2, 0, NOW(), NOW(), 1, 0),
(136, 129, '领用/归还/维修/报废', 3, '', NULL, 'eam:asset:change', NULL, 3, 0, NOW(), NOW(), 1, 0),
(137, 132, '盘点操作', 3, '', NULL, 'eam:check:save', NULL, 1, 0, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

-- 超管全量；人事全量；销售经理（领用归还移动端）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.id BETWEEN 128 AND 137 AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 5, m.id FROM sys_menu m WHERE m.id BETWEEN 128 AND 137 AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 5 AND rm.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.id FROM sys_menu m WHERE m.id IN (128, 129, 131, 136) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 2 AND rm.menu_id = m.id);
