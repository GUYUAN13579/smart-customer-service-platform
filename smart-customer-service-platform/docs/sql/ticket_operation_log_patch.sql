-- 工单操作日志表与权限

CREATE TABLE IF NOT EXISTS ticket_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '操作日志ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    operator_id BIGINT COMMENT '操作人用户ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型：CREATE/REVIEW_APPROVE/REVIEW_REJECT/ASSIGN/START/ADD_RECORD/RESOLVE/CLOSE',
    from_status VARCHAR(32) COMMENT '操作前工单状态',
    to_status VARCHAR(32) COMMENT '操作后工单状态',
    operation_content VARCHAR(1000) COMMENT '操作说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志表';

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'ticket:operation-log:list', '查询工单操作日志', 'API', '/api/v1/tickets/{id}/operation-logs', NULL, 526, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'ticket:operation-log:list');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'ticket:operation-log:list'
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
