-- SLA 告警兜底扫描手动触发权限。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-alert:recovery:scan', '手动执行SLA告警兜底扫描', 'API', '/api/v1/sla/alerts/recovery/scan', NULL, 278, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'sla-alert:recovery:scan'
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'sla-alert:recovery:scan'
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
