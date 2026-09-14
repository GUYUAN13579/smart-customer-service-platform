-- 知识语义检索权限，授予可使用 AI 辅助能力的客服及管理角色。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'knowledge:search', '知识库语义检索', 'API', '/api/v1/knowledge/search', NULL, 790, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'knowledge:search'
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'knowledge:search'
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
