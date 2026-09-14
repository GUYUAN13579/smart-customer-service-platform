-- 通知中心表、通知接口权限与 SLA 手动提醒权限

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人用户ID',
    type VARCHAR(32) NOT NULL COMMENT '通知类型：SLA_RISK/SLA_OVERDUE/TICKET_ASSIGNED/SYSTEM',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    business_type VARCHAR(32) COMMENT '关联业务类型：TICKET/SLA/CONVERSATION/SYSTEM',
    business_id BIGINT COMMENT '关联业务ID',
    read_status VARCHAR(16) NOT NULL DEFAULT 'UNREAD' COMMENT '读取状态：UNREAD/READ',
    read_at DATETIME COMMENT '读取时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_receiver_read_created (receiver_id, read_status, created_at),
    INDEX idx_type_created (type, created_at),
    INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'notification:list', '查询个人通知', 'API', '/api/v1/notifications', NULL, 270, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:list');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'notification:unread-count', '查询未读通知数', 'API', '/api/v1/notifications/unread-count', NULL, 271, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:unread-count');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'notification:read', '标记通知已读', 'API', '/api/v1/notifications/{id}/read', NULL, 272, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:read');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'notification:read-all', '全部通知已读', 'API', '/api/v1/notifications/read-all', NULL, 273, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'notification:read-all');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'sla-monitor:remind', '发送SLA手动提醒', 'API', '/api/v1/sla/tickets/{ticketId}/remind', NULL, 274, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'sla-monitor:remind');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'notification:list',
    'notification:unread-count',
    'notification:read',
    'notification:read-all'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR', 'AGENT')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'sla-monitor:remind'
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
