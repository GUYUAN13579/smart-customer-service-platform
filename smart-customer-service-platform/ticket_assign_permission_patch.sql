-- 工单派单相关接口权限

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ticket:waiting-assign',
    '待派单工单列表',
    'API',
    '/api/v1/tickets/waiting-assign',
    NULL,
    504,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:waiting-assign'
);

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ticket:assign',
    '工单派单',
    'API',
    '/api/v1/tickets/{id}/assign',
    NULL,
    505,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:assign'
);

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ticket:my',
    '我的工单',
    'API',
    '/api/v1/tickets/my',
    NULL,
    506,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:my'
);

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('ticket:waiting-assign', 'ticket:assign')
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'ticket:my'
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
