-- 知识库 / RAG 模块表结构。
-- 向量本体存储在 Elasticsearch；MySQL 仅保存业务内容、切片元数据和索引任务状态。

CREATE TABLE IF NOT EXISTS knowledge_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(255) NOT NULL COMMENT '知识文章标题',
    summary VARCHAR(1000) NULL COMMENT '人工或AI摘要',
    content LONGTEXT NOT NULL COMMENT 'Markdown或纯文本正文',
    category VARCHAR(64) NOT NULL DEFAULT 'GENERAL' COMMENT 'ORDER/PAYMENT/REFUND/LOGISTICS/ACCOUNT/TECHNICAL/COMPLAINT/GENERAL',
    tags JSON NULL COMMENT '标签数组，例如 ["验证码","登录"]',
    source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL-人工编辑，FILE-由上传文件生成',
    file_id BIGINT NULL COMMENT '来源文件ID，关联 file_resource.id',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    version INT NOT NULL DEFAULT 1 COMMENT '每次发布或正文更新时递增',
    published_by BIGINT NULL COMMENT '发布人用户ID',
    published_at DATETIME NULL COMMENT '发布时间',
    created_by BIGINT NOT NULL COMMENT '创建人用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    KEY idx_knowledge_article_status_category (deleted, status, category),
    KEY idx_knowledge_article_file (file_id),
    KEY idx_knowledge_article_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文章';

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    file_id BIGINT NOT NULL COMMENT '关联 file_resource.id',
    document_name VARCHAR(255) NOT NULL COMMENT '导入时的文件名快照',
    content_type VARCHAR(128) NULL COMMENT '文件MIME类型',
    file_size BIGINT NULL COMMENT '文件大小，字节',
    extracted_content LONGTEXT NULL COMMENT '解析得到的原始文本',
    parser_type VARCHAR(64) NULL COMMENT '解析器标识，例如 TIKA/PDFBOX',
    status VARCHAR(16) NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PARSING/PARSED/INDEXED/FAILED/OFFLINE',
    error_message VARCHAR(1000) NULL COMMENT '解析或索引失败原因',
    created_by BIGINT NOT NULL COMMENT '上传或导入人用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    UNIQUE KEY uk_knowledge_document_file (file_id),
    KEY idx_knowledge_document_status (deleted, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库导入文件';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    source_type VARCHAR(16) NOT NULL COMMENT 'ARTICLE/DOCUMENT',
    source_id BIGINT NOT NULL COMMENT '来源文章或导入文件的业务ID',
    chunk_no INT NOT NULL COMMENT '同一来源内从0开始的切片序号',
    content MEDIUMTEXT NOT NULL COMMENT '切片正文',
    content_hash CHAR(64) NOT NULL COMMENT 'SHA-256内容摘要，用于避免重复索引',
    char_count INT NOT NULL DEFAULT 0 COMMENT '字符数',
    token_count INT NULL COMMENT '模型Token数，可在切片时计算',
    metadata JSON NULL COMMENT '标题、分类、标签等供ES检索过滤的元数据',
    es_document_id VARCHAR(128) NULL COMMENT 'Elasticsearch中的文档ID',
    embedding_model VARCHAR(128) NULL COMMENT '生成向量所用模型',
    index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/INDEXED/FAILED/OFFLINE',
    index_error VARCHAR(1000) NULL COMMENT '向量化或写入ES失败原因',
    indexed_at DATETIME NULL COMMENT '最后成功索引时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    UNIQUE KEY uk_knowledge_chunk_source_no (source_type, source_id, chunk_no),
    KEY idx_knowledge_chunk_index_status (deleted, index_status),
    KEY idx_knowledge_chunk_es_document (es_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库检索切片';

CREATE TABLE IF NOT EXISTS knowledge_index_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    source_type VARCHAR(16) NOT NULL COMMENT 'ARTICLE/DOCUMENT',
    source_id BIGINT NOT NULL COMMENT '来源文章或导入文件的业务ID',
    task_type VARCHAR(16) NOT NULL COMMENT 'PARSE/CHUNK/EMBED/INDEX/DELETE',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    error_message VARCHAR(1000) NULL COMMENT '失败原因',
    scheduled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计划执行时间',
    started_at DATETIME NULL COMMENT '开始执行时间',
    finished_at DATETIME NULL COMMENT '结束执行时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_knowledge_index_task_pending (status, scheduled_at),
    KEY idx_knowledge_index_task_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库解析与索引任务';
