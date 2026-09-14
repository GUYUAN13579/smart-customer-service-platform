-- 知识切片管理权限，授予管理员与主管。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT permission_code, permission_name, 'API', resource_path, NULL, sort_order, NOW()
FROM (
    SELECT 'knowledge:chunk:create' AS permission_code, '新增知识切片' AS permission_name,
           '/api/v1/knowledge/chunks' AS resource_path, 769 AS sort_order
    UNION ALL SELECT 'knowledge:chunk:rebuild', '重建知识切片', '/api/v1/knowledge/articles/{id}/chunks/rebuild', 770
    UNION ALL SELECT 'knowledge:chunk:list', '查询知识切片', '/api/v1/knowledge/chunks', 771
    UNION ALL SELECT 'knowledge:chunk:detail', '查看知识切片详情', '/api/v1/knowledge/chunks/{id}', 772
) permissions
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'knowledge:chunk:create', 'knowledge:chunk:rebuild', 'knowledge:chunk:list', 'knowledge:chunk:detail'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
