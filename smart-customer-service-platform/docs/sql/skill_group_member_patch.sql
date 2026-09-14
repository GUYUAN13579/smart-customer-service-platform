-- 技能组成员管理所需索引与权限。
-- 先执行 SHOW INDEX FROM skill_group_member / skill_group；只有对应索引不存在时再执行下列 ALTER TABLE。

-- ALTER TABLE skill_group_member
--     ADD UNIQUE KEY uk_skill_group_member_group_user (skill_group_id, user_id),
--     ADD KEY idx_skill_group_member_group_status (skill_group_id, status),
--     ADD KEY idx_skill_group_member_user_status (user_id, status);

-- ALTER TABLE skill_group
--     ADD UNIQUE KEY uk_skill_group_group_name (group_name),
--     ADD KEY idx_skill_group_status_category (status, category);

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group:create', '新增技能组', 'API', '/api/v1/skill-groups', NULL, 225, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group:create');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group:update', '修改技能组', 'API', '/api/v1/skill-groups/{id}', NULL, 226, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group:update');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group:delete', '删除技能组', 'API', '/api/v1/skill-groups/{id}', NULL, 227, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group:delete');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group:detail', '查询技能组详情', 'API', '/api/v1/skill-groups/{id}', NULL, 228, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group:detail');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group:list', '查询技能组', 'API', '/api/v1/skill-groups', NULL, 229, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group:list');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group-member:create', '新增技能组成员', 'API', '/api/v1/skill-groups/{skillGroupId}/members', NULL, 230, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group-member:create');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group-member:update', '修改技能组成员', 'API', '/api/v1/skill-groups/{skillGroupId}/members/{memberId}', NULL, 231, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group-member:update');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group-member:delete', '移除技能组成员', 'API', '/api/v1/skill-groups/{skillGroupId}/members/{memberId}', NULL, 232, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group-member:delete');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group-member:detail', '查询技能组成员详情', 'API', '/api/v1/skill-groups/{skillGroupId}/members/{memberId}', NULL, 233, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group-member:detail');

INSERT INTO sys_permission
    (permission_code, permission_name, resource_type, resource_path, parent_id, sort_order, created_at)
SELECT 'skill-group-member:list', '查询技能组成员', 'API', '/api/v1/skill-groups/{skillGroupId}/members', NULL, 234, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'skill-group-member:list');

INSERT INTO sys_role_permission
    (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'skill-group:create',
    'skill-group:update',
    'skill-group:delete',
    'skill-group:detail',
    'skill-group:list',
    'skill-group-member:create',
    'skill-group-member:update',
    'skill-group-member:delete',
    'skill-group-member:detail',
    'skill-group-member:list'
)
WHERE r.role_code IN ('ADMIN', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
