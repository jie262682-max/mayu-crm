INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(138, 81, '表单管理', 2, 'form', 'oa/form/index', 'oa:flow:save', 'DocumentAdd', 5, 1, NOW(), NOW(), 1, 0),
(139, 81, '我的申请', 2, 'apply', 'oa/apply/index', NULL, 'Tickets', 6, 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE m.id IN (138, 139) AND m.deleted = 0 AND r.id IN (1, 2, 5, 6)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
