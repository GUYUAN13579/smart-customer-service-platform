-- 知识库纯文本文件管理权限，授予管理员与主管。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT permission_code, permission_name, 'API', resource_path, NULL, sort_order, NOW()
FROM (
    SELECT 'knowledge:document:create' AS permission_code, '导入知识库文件' AS permission_name,
           '/api/v1/knowledge/documents' AS resource_path, 760 AS sort_order
    UNION ALL SELECT 'knowledge:document:list', '查询知识库文件', '/api/v1/knowledge/documents', 761
    UNION ALL SELECT 'knowledge:document:detail', '查看知识库文件详情', '/api/v1/knowledge/documents/{id}', 762
    UNION ALL SELECT 'knowledge:document:parse', '解析知识库文件', '/api/v1/knowledge/documents/{id}/parse', 763
    UNION ALL SELECT 'knowledge:document:retry', '重试解析知识库文件', '/api/v1/knowledge/documents/{id}/retry-parse', 764
    UNION ALL SELECT 'knowledge:document:offline', '下线知识库文件', '/api/v1/knowledge/documents/{id}/offline', 765
    UNION ALL SELECT 'knowledge:document:delete', '删除知识库文件', '/api/v1/knowledge/documents/{id}', 766
) permissions
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'knowledge:document:create', 'knowledge:document:list', 'knowledge:document:detail',
    'knowledge:document:parse', 'knowledge:document:retry', 'knowledge:document:offline',
    'knowledge:document:delete'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
