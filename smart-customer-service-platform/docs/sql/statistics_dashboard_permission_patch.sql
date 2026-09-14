-- 运营看板统计接口权限，授予管理员与主管。
-- 看板会同时请求 dashboard、客服负载、工单趋势和 SLA 表现四个接口，
-- 因此必须一次性授予全部四项权限，避免出现部分区域加载失败。
INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT permission_code, permission_name, 'API', resource_path, NULL, sort_order, NOW()
FROM (
    SELECT 'statistics:dashboard' AS permission_code, '查看运营看板统计' AS permission_name,
           '/api/v1/statistics/dashboard' AS resource_path, 700 AS sort_order
    UNION ALL
    SELECT 'statistics:agent-workload', '查看客服负载统计', '/api/v1/statistics/agent-workloads', 701
    UNION ALL
    SELECT 'statistics:ticket-trend', '查看工单趋势统计', '/api/v1/statistics/ticket-trends', 702
    UNION ALL
    SELECT 'statistics:sla-performance', '查看SLA达标统计', '/api/v1/statistics/sla-performance', 703
) permissions
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'statistics:dashboard', 'statistics:agent-workload',
    'statistics:ticket-trend', 'statistics:sla-performance'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
