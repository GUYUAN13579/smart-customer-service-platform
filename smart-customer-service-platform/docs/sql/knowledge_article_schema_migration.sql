-- 将旧版 knowledge_article 表迁移至当前知识库模块实体结构。
-- 执行前请备份生产环境数据；本地开发库当前没有历史知识文章。

ALTER TABLE knowledge_article
    DROP INDEX idx_article_category_status,
    DROP COLUMN category_id,
    DROP COLUMN view_count,
    DROP COLUMN hit_count,
    MODIFY COLUMN summary VARCHAR(1000) NULL COMMENT '人工或AI摘要',
    MODIFY COLUMN tags JSON NULL COMMENT '标签数组，例如 ["验证码","登录"]',
    ADD COLUMN category VARCHAR(64) NOT NULL DEFAULT 'GENERAL' COMMENT 'ORDER/PAYMENT/REFUND/LOGISTICS/ACCOUNT/TECHNICAL/COMPLAINT/GENERAL' AFTER content,
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL-人工编辑，FILE-由上传文件生成' AFTER tags,
    ADD COLUMN file_id BIGINT NULL COMMENT '来源文件ID，关联 file_resource.id' AFTER source_type,
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '每次发布或正文更新时递增' AFTER status,
    ADD COLUMN published_by BIGINT NULL COMMENT '发布人用户ID' AFTER version,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是' AFTER updated_at,
    ADD KEY idx_knowledge_article_status_category (deleted, status, category),
    ADD KEY idx_knowledge_article_file (file_id),
    ADD KEY idx_knowledge_article_published_at (published_at);
