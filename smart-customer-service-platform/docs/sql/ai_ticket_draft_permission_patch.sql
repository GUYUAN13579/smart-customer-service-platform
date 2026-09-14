-- AI 工单草稿接口权限

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT
    'ai:ticket-draft',
    'AI 生成工单草稿',
    'API',
    '/api/v1/ai/conversations/{sessionId}/ticket-draft',
    NULL,
    404,
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE permission_code = 'ai:ticket-draft'
);

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'ai:ticket-draft'
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
