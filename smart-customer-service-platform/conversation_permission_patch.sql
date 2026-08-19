-- 会话中心新增接口权限
-- 如果你的 sys_permission.permission_code 已经有唯一索引，可以直接使用 INSERT IGNORE。

INSERT IGNORE INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
VALUES
    ('conversation:list', '会话分页查询', 'API', '/api/v1/conversations', NULL, 301, NOW()),
    ('conversation:release', '退出接管会话', 'API', '/api/v1/conversations/{id}/release-take-over', NULL, 305, NOW()),
    ('conversation:close', '关闭会话', 'API', '/api/v1/conversations/{id}/close', NULL, 306, NOW());

-- 常见角色授权：管理员、主管、客服都可以查看会话列表、退出接管、关闭会话。
-- 如果你的角色编码不是 ADMIN/SUPERVISOR/AGENT，请按实际 role_code 修改。

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('conversation:list', 'conversation:release', 'conversation:close')
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT');
