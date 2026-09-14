-- 知识索引任务权限，授予管理员与主管。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT permission_code, permission_name, 'API', resource_path, NULL, sort_order, NOW()
FROM (
    SELECT 'knowledge:index:create' AS permission_code, '创建知识索引任务' AS permission_name,
           '/api/v1/knowledge/articles/{id}/index' AS resource_path, 780 AS sort_order
    UNION ALL SELECT 'knowledge:index:list', '查询知识索引任务', '/api/v1/knowledge/index-tasks', 781
    UNION ALL SELECT 'knowledge:index:detail', '查看知识索引任务详情', '/api/v1/knowledge/index-tasks/{id}', 782
    UNION ALL SELECT 'knowledge:index:retry', '重试知识索引任务', '/api/v1/knowledge/index-tasks/{id}/retry', 783
    UNION ALL SELECT 'knowledge:index:execute', '执行知识索引任务', '/api/v1/knowledge/index-tasks/{id}/execute', 784
) permissions
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'knowledge:index:create', 'knowledge:index:list', 'knowledge:index:detail', 'knowledge:index:retry',
    'knowledge:index:execute'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
