-- 学生成绩管理模块权限分配脚本
-- 此脚本将学生成绩管理相关的权限分配给管理员角色

-- 先查询出学生成绩管理相关的所有权限点的menu_id
-- 然后将这些menu_id与管理员角色(role_id=1)关联起来

-- 分配权限到管理员角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT '1', menu_id
FROM sys_menu
WHERE perms LIKE 'student:performance:%';

-- 提示：执行完成后，刷新页面或重新登录系统即可生效
-- 如果需要为其他角色分配权限，请修改role_id的值（例如普通角色的role_id通常为2）