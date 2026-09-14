-- 工单审核相关接口权限

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ticket:pending-review',
    '待审核工单列表',
    'API',
    '/api/v1/tickets/pending-review',
    NULL,
    501,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:pending-review'
);

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ticket:detail',
    '工单详情',
    'API',
    '/api/v1/tickets/{id}',
    NULL,
    502,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:detail'
);

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ticket:review',
    '工单审核',
    'API',
    '/api/v1/tickets/{id}/review',
    NULL,
    503,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:review'
);

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('ticket:pending-review', 'ticket:detail', 'ticket:review')
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
