-- 工单处理流转表与权限

CREATE TABLE IF NOT EXISTS ticket_process_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '处理记录ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    operator_id BIGINT NOT NULL COMMENT '操作人用户ID',
    record_type VARCHAR(32) NOT NULL COMMENT '记录类型：START/NOTE/CONTACT_CUSTOMER/PROCESS/RESOLVE/CLOSE/OTHER',
    content TEXT NOT NULL COMMENT '处理内容',
    visible_to_customer TINYINT NOT NULL DEFAULT 0 COMMENT '客户是否可见：0否 1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_record_type (record_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单处理记录表';

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:start', '开始处理工单', 'API', '/api/v1/tickets/{id}/start', NULL, 521, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:start');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:record:add', '新增工单处理记录', 'API', '/api/v1/tickets/{id}/records', NULL, 522, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:record:add');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:record:list', '查询工单处理记录', 'API', '/api/v1/tickets/{id}/records', NULL, 523, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:record:list');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:resolve', '解决工单', 'API', '/api/v1/tickets/{id}/resolve', NULL, 524, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:resolve');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:close', '关闭工单', 'API', '/api/v1/tickets/{id}/close', NULL, 525, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:close');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'ticket:start',
    'ticket:record:add',
    'ticket:record:list',
    'ticket:resolve',
    'ticket:close'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
