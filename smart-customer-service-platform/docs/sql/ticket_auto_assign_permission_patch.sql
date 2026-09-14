-- 自动派单与派单预览接口权限。

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:auto-assign:preview', '预览工单自动派单结果', 'API', '/api/v1/tickets/{id}/assignment-preview', NULL, 532, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:auto-assign:preview');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:auto-assign', '执行工单自动派单', 'API', '/api/v1/tickets/{id}/auto-assign', NULL, 533, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:auto-assign');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('ticket:auto-assign:preview', 'ticket:auto-assign')
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
