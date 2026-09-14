-- 对齐早期 sla_policy 表与当前 SlaPolicy 实体及 SlaPolicyServiceImpl 的字段约定。
-- 旧表缺少这两个字段时，策略查询会因全局逻辑删除条件引用 deleted 而失败。
ALTER TABLE sla_policy
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除' AFTER updated_at;

CREATE INDEX idx_sla_policy_enabled_deleted ON sla_policy (enabled, deleted);
