CREATE TABLE IF NOT EXISTS sla_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    policy_name VARCHAR(128) NOT NULL COMMENT 'SLA策略名称',
    category VARCHAR(64) NULL COMMENT '适用工单分类，为空表示不限分类',
    priority VARCHAR(16) NOT NULL COMMENT '适用优先级，如 P1/P2/P3/P4',
    first_response_minutes INT NOT NULL COMMENT '首次响应时限，单位分钟',
    resolve_minutes INT NOT NULL COMMENT '解决时限，单位分钟',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除，1已删除',
    KEY idx_sla_policy_category_priority (category, priority),
    KEY idx_sla_policy_enabled_deleted (enabled, deleted)
) COMMENT='SLA策略表';

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-policy:create', '创建SLA策略', 'API', '/api/v1/sla/policies', NULL, 260, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-policy:create');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-policy:update', '修改SLA策略', 'API', '/api/v1/sla/policies/{id}', NULL, 261, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-policy:update');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-policy:delete', '删除SLA策略', 'API', '/api/v1/sla/policies/{id}', NULL, 262, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-policy:delete');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-policy:detail', 'SLA策略详情', 'API', '/api/v1/sla/policies/{id}', NULL, 263, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-policy:detail');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-policy:list', 'SLA策略列表', 'API', '/api/v1/sla/policies', NULL, 264, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-policy:list');

INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'sla-policy:create',
    'sla-policy:update',
    'sla-policy:delete',
    'sla-policy:detail',
    'sla-policy:list'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
