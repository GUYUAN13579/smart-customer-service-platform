-- 知识文章管理权限，授予管理员与主管。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT permission_code, permission_name, 'API', resource_path, NULL, sort_order, NOW()
FROM (
    SELECT 'knowledge:article:create' AS permission_code, '新增知识文章' AS permission_name,
           '/api/v1/knowledge/articles' AS resource_path, 750 AS sort_order
    UNION ALL SELECT 'knowledge:article:update', '修改知识文章', '/api/v1/knowledge/articles/{id}', 751
    UNION ALL SELECT 'knowledge:article:delete', '删除知识文章', '/api/v1/knowledge/articles/{id}', 752
    UNION ALL SELECT 'knowledge:article:detail', '查看知识文章详情', '/api/v1/knowledge/articles/{id}', 753
    UNION ALL SELECT 'knowledge:article:list', '分页查询知识文章', '/api/v1/knowledge/articles', 754
    UNION ALL SELECT 'knowledge:article:publish', '发布知识文章', '/api/v1/knowledge/articles/{id}/publish', 755
    UNION ALL SELECT 'knowledge:article:offline', '下线知识文章', '/api/v1/knowledge/articles/{id}/offline', 756
) permissions
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'knowledge:article:create', 'knowledge:article:update', 'knowledge:article:delete',
    'knowledge:article:detail', 'knowledge:article:list', 'knowledge:article:publish',
    'knowledge:article:offline'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
