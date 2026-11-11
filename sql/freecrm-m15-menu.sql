-- M11-M15 新菜单（幂等）：系统设置 / 操作日志 / 登录日志 / 我的消息
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, create_time, update_time, creator, deleted) VALUES
(124, 79, '系统设置', 2, 'settings', 'system/settings/index', 'sys:user:list', 'Tools',    5, 1, NOW(), NOW(), 1, 0),
(125, 79, '操作日志', 2, 'operlog',  'system/operlog/index',  'sys:user:list', 'Notebook', 6, 1, NOW(), NOW(), 1, 0),
(126, 79, '登录日志', 2, 'loginlog', 'system/loginlog/index', 'sys:user:list', 'Key',      7, 1, NOW(), NOW(), 1, 0),
(127, 79, '我的消息', 2, 'notice',   'system/notice/index',   NULL,            'Bell',     8, 1, NOW(), NOW(), 1, 0)
ON DUPLICATE KEY UPDATE deleted = 0;

-- 超管授权全部；我的消息给全部角色（人人可用）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m WHERE m.id IN (124, 125, 126, 127) AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 127 FROM sys_role r WHERE r.id > 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = 127);
