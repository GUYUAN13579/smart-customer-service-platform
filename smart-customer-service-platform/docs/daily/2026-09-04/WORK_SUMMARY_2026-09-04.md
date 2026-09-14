# 2026-09-04 工作总结

## 今日完成内容

### 1. 搭建知识库纯文本文件导入模块骨架

围绕 `knowledge_document` 表完成了纯文本文件导入与解析模块的基础分层代码：

```text
KnowledgeDocument 实体
KnowledgeDocumentCreateRequest
KnowledgeDocumentQueryRequest
KnowledgeDocumentVO
KnowledgeDocumentMapper
KnowledgeDocumentService
KnowledgeDocumentServiceImpl
KnowledgeDocumentController
```

新增接口：

```http
POST   /api/v1/knowledge/documents
GET    /api/v1/knowledge/documents
GET    /api/v1/knowledge/documents/{id}
POST   /api/v1/knowledge/documents/{id}/parse
POST   /api/v1/knowledge/documents/{id}/retry-parse
POST   /api/v1/knowledge/documents/{id}/offline
DELETE /api/v1/knowledge/documents/{id}
```

文件导入采用两段式流程：

```text
先通过现有 /api/v1/files/upload 上传至 MinIO
-> 获取 file_resource.id
-> 调用 POST /api/v1/knowledge/documents 导入知识库
-> 创建 knowledge_document 记录
```

当前版本只支持：

```text
.txt
.md
.csv
.json
```

文件类型通过原始文件名扩展名校验，并使用 `Locale.ROOT` 统一处理大小写。

### 2. 完成知识库文件导入 createDocument

`createDocument` 已完成以下处理：

- 根据 `fileId` 查询已有 `file_resource` 元数据。
- 校验文件是否属于允许导入的纯文本格式。
- 按 `fileId` 检查是否已存在知识库导入记录，避免唯一索引异常返回 500。
- 使用 `SecurityUtils.getCurrentUserId()` 记录实际导入人，而不是原文件上传人。
- 创建 `knowledge_document`，初始状态设为 `UPLOADED`。
- 保存文件名、MIME 类型、文件大小等元数据快照。

### 3. 完成知识库文件分页与详情查询

`pageDocuments` 已支持：

- 标准分页。
- 按文件名关键词模糊查询。
- 按文档状态筛选。
- 排除逻辑删除数据。
- 按创建时间倒序排序。

`getDocument` 会校验文档存在且未逻辑删除，再返回详情。

### 4. 完成纯文本解析与失败重试

完成了 `parseDocument` 和 `retryParseDocument`，并整理为共享的 `doParse` 解析流程。

当前状态流转：

```text
UPLOADED
-> PARSING
-> PARSED

PARSING
-> FAILED

FAILED
-> PARSING
-> PARSED 或 FAILED
```

实现要点：

- 普通解析仅允许 `UPLOADED` 状态。
- 重试解析仅允许 `FAILED` 状态。
- 先通过条件更新写入 `PARSING`，防止并发请求重复解析同一文件。
- 使用 `knowledgeDocument.fileId` 调用 `FileResourceService.getFileContent`，避免混淆知识文档 ID 与文件资源 ID。
- 使用 UTF-8 将 `byte[]` 转换为 `String`，正确保存到 `extracted_content`。
- 成功后写入 `parserType = PLAIN_TEXT`、`status = PARSED`。
- 读取文件、格式校验或内容为空等异常时，写入 `status = FAILED` 和截断后的 `errorMessage`，使重试接口可以正常使用。
- 解析时再次校验文件扩展名，避免绕过导入校验。

### 5. 完成下线与逻辑删除

完成：

```text
POST /api/v1/knowledge/documents/{id}/offline
DELETE /api/v1/knowledge/documents/{id}
```

规则：

- 下线后状态更新为 `OFFLINE`，文档不再允许走解析流程。
- 删除使用逻辑删除：`deleted = 1`，并同步将状态设为 `OFFLINE`。
- `PARSING` 状态的文档不能下线或删除，避免与解析过程产生状态竞争。
- 使用条件更新避免并发下重复操作。

### 6. 新增知识库文件权限脚本

新增：

```text
docs/sql/knowledge_document_permission_patch.sql
```

该脚本将知识库文件导入、查询、解析、重试、下线、删除权限授予：

```text
ADMIN
SUPERVISOR
```

执行权限脚本后需要重新登录，使新权限进入 JWT。

### 7. 编译验证

- 后端多次执行 Maven 编译，均通过。

## 当前知识库进度

```text
知识文章 CRUD / 发布 / 下线
-> 已完成

纯文本文件上传后导入 knowledge_document
-> 已完成

纯文本解析、失败状态与重试
-> 已完成

知识内容切片 knowledge_chunk
-> 待完成

向量化与 Elasticsearch 索引
-> 待完成

RAG 语义检索与 AI 上下文注入
-> 待完成
```

## 后续优先完成内容

### 1. 知识切片 knowledge_chunk

下一步优先实现文章和已解析文档的切片重建。

建议接口：

```http
POST /api/v1/knowledge/articles/{id}/chunks/rebuild
POST /api/v1/knowledge/documents/{id}/chunks/rebuild
GET  /api/v1/knowledge/chunks
GET  /api/v1/knowledge/chunks/{id}
```

第一版切片策略：

- 仅处理 `PUBLISHED` 文章和 `PARSED` 文档。
- 优先按标题、空行和段落拆分。
- 单个切片最大 800 字符。
- 相邻切片保留约 100 字符重叠。
- 计算 SHA-256 `contentHash`，避免相同内容重复索引。
- 切片初始 `indexStatus = PENDING`。

### 2. 知识索引任务与向量化

- 使用 `knowledge_index_task` 管理切片、向量化、索引和删除任务。
- 为任务补充待执行、处理中、成功、失败和重试逻辑。
- 确定 Embedding 模型。
- 创建 Elasticsearch 向量索引 Mapping。
- 将 `knowledge_chunk` 写入 Elasticsearch，并回写 `esDocumentId`、模型信息、索引状态。

### 3. RAG 语义检索与 AI 接入

- 提供知识检索接口，支持 query、分类过滤、TopK 和相似度分数。
- 只检索已发布文章与已完成索引的文件切片。
- 在 AI 普通问答、自动回复、工单草稿前注入相关知识上下文。
- 返回知识来源、文章标题和命中片段，供客服在前端核验。

### 4. 知识库前端完善

- 在知识库页面增加纯文本文件上传、导入、解析、重试和状态展示。
- 增加已解析文本预览。
- 后续增加切片列表、索引状态和检索结果引用展示。

### 5. 其他待完成增强

- 补齐分类码对应的技能组和 `assignment_rule` 数据，完成自动派单真实联调。
- 升级正式客户鉴权，校验客户与会话归属。
- 将前端触发的 AI 自动回复升级为后端事件或 MQ 异步调用。
- 完成 SLA 延迟消息、死信、重试队列的端到端验证。
- 增加知识库、自动派单、权限越权、消息重复消费和文件上传的集成测试。
- 将敏感配置迁移到环境变量或密钥管理，并准备 Docker Compose / 部署脚本。
