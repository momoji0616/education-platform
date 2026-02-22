-- 学生成绩管理模块权限初始化SQL脚本
-- 使用前请确保已连接到RuoYi数据库
-- 执行此脚本将在sys_menu表中注册学生成绩管理模块所需的所有权限点

-- 1. 首先插入一个菜单（作为一级菜单）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('学生成绩管理', '0', 4, 'student', 'student/performance/index', 1, 0, 'C', '0', '0', 'student:performance:list', 'data-analysis', 'admin', NOW(), '', NULL, '学生成绩管理菜单');

-- 2. 插入列表权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('成绩列表', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 1, 'list', '', 1, 0, 'F', '0', '0', 'student:performance:list', '#', 'admin', NOW(), '', NULL, '查看学生成绩列表');

-- 3. 插入查询详情权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询成绩', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 2, 'query', '', 1, 0, 'F', '0', '0', 'student:performance:query', '#', 'admin', NOW(), '', NULL, '查询学生成绩详情');

-- 4. 插入新增权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('新增成绩', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 3, 'add', '', 1, 0, 'F', '0', '0', 'student:performance:add', '#', 'admin', NOW(), '', NULL, '新增学生成绩信息');

-- 5. 插入修改权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('修改成绩', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 4, 'edit', '', 1, 0, 'F', '0', '0', 'student:performance:edit', '#', 'admin', NOW(), '', NULL, '修改学生成绩信息');

-- 6. 插入删除权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除成绩', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 5, 'remove', '', 1, 0, 'F', '0', '0', 'student:performance:remove', '#', 'admin', NOW(), '', NULL, '删除学生成绩信息');

-- 7. 插入导出权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出成绩', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 6, 'export', '', 1, 0, 'F', '0', '0', 'student:performance:export', '#', 'admin', NOW(), '', NULL, '导出学生成绩数据');

-- 8. 插入导入权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导入成绩', (SELECT menu_id FROM sys_menu WHERE menu_name = '学生成绩管理' AND parent_id = '2000'), 7, 'import', '', 1, 0, 'F', '0', '0', 'student:performance:import', '#', 'admin', NOW(), '', NULL, '导入学生成绩数据和下载模板');

-- 提示：执行完成后，请使用管理员账号登录系统，在 系统管理->角色管理 中为相应角色分配以上权限。
-- 如果菜单没有显示，请检查parent_id是否正确，或者在 系统管理->菜单管理 中手动添加菜单。