-- SLA 自动告警接口权限。sla_alert 表结构已由数据库迁移单独维护。

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-alert:list', '查询SLA自动告警', 'API', '/api/v1/sla/alerts', NULL, 275, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-alert:list');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-alert:detail', '查询SLA自动告警详情', 'API', '/api/v1/sla/alerts/{id}', NULL, 276, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-alert:detail');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-alert:retry', '重试SLA自动告警', 'API', '/api/v1/sla/alerts/{id}/retry', NULL, 277, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-alert:retry');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('sla-alert:list', 'sla-alert:detail', 'sla-alert:retry')
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
