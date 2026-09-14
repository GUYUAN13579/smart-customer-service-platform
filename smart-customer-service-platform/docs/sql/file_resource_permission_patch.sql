-- 客服会话上传附件所需权限，授予客服、主管和管理员。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT permission_code, permission_name, 'API', resource_path, NULL, sort_order, NOW()
FROM (
    SELECT 'file:upload' AS permission_code, '上传文件' AS permission_name,
           '/api/v1/files/upload' AS resource_path, 420 AS sort_order
    UNION ALL
    SELECT 'file:image:upload', '上传图片', '/api/v1/files/images/upload', 421
    UNION ALL
    SELECT 'file:detail', '查看文件资源', '/api/v1/files/{id}', 422
) permissions
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('file:upload', 'file:image:upload', 'file:detail')
WHERE r.role_code IN ('AGENT', 'SUPERVISOR', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
