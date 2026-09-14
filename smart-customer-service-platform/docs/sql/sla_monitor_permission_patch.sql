-- SLA 监控接口权限

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-monitor:risk', '查询SLA即将超时工单', 'API', '/api/v1/sla/tickets/risk', NULL, 265, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-monitor:risk');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-monitor:overdue', '查询SLA已超时工单', 'API', '/api/v1/sla/tickets/overdue', NULL, 266, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-monitor:overdue');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('sla-monitor:risk', 'sla-monitor:overdue')
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
