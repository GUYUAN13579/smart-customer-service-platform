# 2026-09-06 工作总结

## 今日完成内容

### 1. 明确知识库当前开发位置

对项目规划、日报、权限 SQL 和当前代码进行了交叉检查，确认当前主业务闭环已基本完成，知识库模块推进到：

```text
知识文章管理
-> 已完成

纯文本文件导入与解析
-> 已完成

文章 / 文件知识切片
-> 已完成

索引任务管理与手动索引执行
-> 已完成代码

知识检索与 RAG 注入
-> 待完成
```

### 2. 补齐 knowledge_chunk 表迁移方案

当前数据库中的旧 `knowledge_chunk` 表仅支持：

```text
article_id
chunk_index
chunk_text
embedding_id
```

与当前统一支持文章和知识文件的 `KnowledgeChunk` 实体不一致。

已给出迁移 SQL，目标字段包括：

```text
source_type / source_id / chunk_no
content / content_hash / char_count / token_count / metadata
es_document_id / embedding_model
index_status / index_error / indexed_at
created_at / updated_at / deleted
```

迁移后的来源语义：

```text
ARTICLE  -> source_id 对应 knowledge_article.id
DOCUMENT -> source_id 对应 knowledge_document.id
```

需要在测试数据库执行迁移 SQL 后，再调用知识切片和索引相关接口。

### 3. 完成知识切片管理接口

已完成：

```http
POST /api/v1/knowledge/chunks
POST /api/v1/knowledge/articles/{id}/chunks/rebuild
POST /api/v1/knowledge/documents/{id}/chunks/rebuild
GET  /api/v1/knowledge/chunks
GET  /api/v1/knowledge/chunks/{id}
```

#### 3.1 完成分页与详情查询

`pageChunks` 已调整：

- `sourceType` 和 `sourceId` 必须同时传入，防止文章 ID 与文件 ID 相同导致结果混合。
- `indexStatus` 改为可选筛选条件。
- 固定排除逻辑删除切片。
- 按 `chunkNo` 升序返回，保证前端能恢复原文片段顺序。
- 空结果正常返回空分页，不再抛出异常。

`getChunk` 已补充逻辑删除校验。

#### 3.2 完成文章切片重建

`rebuildArticleChunks` 已实现：

- 校验文章存在、未删除、状态为 `PUBLISHED` 且正文不为空。
- 使用 Spring AI `TokenTextSplitter` 按 500 Token 切分文本。
- 在事务中物理删除旧切片，再从 `chunkNo = 0` 插入新切片。
- 计算每条切片的 SHA-256 `contentHash`。
- 写入文章标题、分类和标签 JSON 元数据。
- 新切片初始状态为 `PENDING`，等待后续索引。
- 返回来源、切片数量和重建时间。

物理删除旧切片的原因：数据库唯一键为

```text
(source_type, source_id, chunk_no)
```

重建时编号会重新从 0 开始；若仅逻辑删除旧记录，新旧序号会发生唯一键冲突。

#### 3.3 完成文件切片重建

`rebuildDocumentChunks` 已实现，流程与文章切片一致：

- 校验知识文件存在、未删除、状态为 `PARSED`。
- 使用 `extractedContent` 作为切分文本。
- 写入 `sourceType = DOCUMENT`。
- 写入文件名、文件 ID、MIME 类型、解析器类型等元数据。
- 新切片初始状态同样为 `PENDING`。

#### 3.4 完成手动新增单条切片

新增：

```http
POST /api/v1/knowledge/chunks
```

规则：

- 文章来源必须已经发布，文件来源必须已经解析。
- 服务端自动查询同一来源的最大 `chunkNo` 并追加编号。
- 客户端提供的 `metadata` 必须是合法 JSON 对象；不传时保存为 `{}`。
- 自动计算内容哈希、字符数，并设为 `PENDING`。

### 4. 完成知识索引任务管理接口

已完成：

```http
POST /api/v1/knowledge/articles/{id}/index
POST /api/v1/knowledge/documents/{id}/index

GET  /api/v1/knowledge/index-tasks
GET  /api/v1/knowledge/index-tasks/{id}

POST /api/v1/knowledge/index-tasks/{id}/retry
```

任务创建规则：

- 文章必须处于 `PUBLISHED`，文件必须处于 `PARSED`。
- 来源下必须存在至少一条 `PENDING` 切片。
- 同一来源不能重复创建 `PENDING` 或 `PROCESSING` 的 `INDEX` 任务。
- 创建时仅写入 `knowledge_index_task`，任务初始状态为 `PENDING`。
- 任务创建阶段不会提前把切片改为 `PROCESSING`。

任务状态流转：

```text
PENDING
-> PROCESSING
-> SUCCESS

PROCESSING
-> FAILED

FAILED
-> PENDING（手动重试，retryCount + 1）
```

重试时会同时将关联的失败切片从 `FAILED` 恢复为 `PENDING`，供执行器重新领取。

### 5. 接入百炼 qwen3.7-text-embedding 与 Elasticsearch

确认百炼 `qwen3.7-text-embedding` 支持 OpenAI 兼容 Embeddings API，并支持多种向量维度。当前项目统一使用 1024 维：

```yaml
spring.ai.openai.embedding.options.model: qwen3.7-text-embedding
spring.ai.openai.embedding.options.dimensions: 1024
spring.ai.openai.embedding.options.encoding-format: float

app.knowledge.index.name: knowledge_chunk_v1
app.knowledge.index.vector-dimensions: 1024
```

新增：

```text
KnowledgeIndexProperties
KnowledgeEsIndexService
KnowledgeEsIndexServiceImpl
```

`KnowledgeEsIndexServiceImpl` 会在首次执行时检查并自动创建 ES 索引 `knowledge_chunk_v1`。

Mapping 包含：

```text
chunkId          long
sourceType       keyword
sourceId         long
chunkNo          integer
content          text
contentHash      keyword
metadata         keyword，仅存储
embeddingModel   keyword
createdAt        date
embedding        dense_vector，1024 维，cosine 相似度
```

### 6. 完成手动执行索引任务接口

新增：

```http
POST /api/v1/knowledge/index-tasks/{id}/execute
```

执行流程：

```text
task PENDING
-> 条件更新为 PROCESSING，防止重复执行
-> 查询来源下所有 PENDING 切片
-> 对每条切片调用 EmbeddingModel.embed(content)
-> 校验返回向量为 1024 维
-> 写入 Elasticsearch
-> 回写 chunk：
   esDocumentId
   embeddingModel
   indexedAt
   indexStatus = INDEXED
-> task = SUCCESS
```

失败处理：

```text
执行中的切片 -> FAILED
chunk.indexError -> 错误原因

索引任务 -> FAILED
task.errorMessage -> 错误原因
```

当前执行接口采用同步手动调用，适合先完成联调。后续可升级为 MQ 消费者或定时任务自动执行。

### 7. 权限与验证

新增或更新：

```text
docs/sql/knowledge_chunk_permission_patch.sql
docs/sql/knowledge_index_task_permission_patch.sql
```

权限授予：

```text
ADMIN
SUPERVISOR
```

新增权限 SQL 执行后需要重新登录，确保权限进入 JWT。

后端多次执行 Maven 编译，均通过。

## 当前知识库完整流程

```text
新增或发布知识文章 / 导入并解析纯文本文件
-> 重建或手动新增知识切片
-> chunk.indexStatus = PENDING
-> 创建 INDEX 任务
-> task.status = PENDING
-> 手动执行索引任务
-> 百炼生成 1024 维向量
-> 写入 Elasticsearch
-> chunk.indexStatus = INDEXED
-> task.status = SUCCESS
```

## 待执行联调事项

以下事项尚未验证，不能视为已完成：

- 执行 `knowledge_chunk` 表结构迁移 SQL。
- 执行知识切片和索引任务权限 SQL，并重新登录验证权限。
- 将 `application-local.yml` 中 Elasticsearch 地址配置为真实可访问的 ES 服务。
- 通过一篇 `PUBLISHED` 文章实际验证：重建切片 -> 创建任务 -> 执行任务。
- 检查 `knowledge_chunk_v1` 是否创建成功、向量维度是否为 1024、切片状态是否回写为 `INDEXED`。
- 验证百炼 API Key 是否已开通 `qwen3.7-text-embedding` 模型权限。
- 构造 ES 或百炼调用失败场景，验证任务和切片能正确进入 `FAILED`，并能通过 retry 恢复。

## 后续优先完成内容

### 1. 知识语义检索接口

下一步优先实现：

```http
GET /api/v1/knowledge/search
```

建议参数：

```text
query       必填，用户问题
category    可选，知识分类过滤
topK        可选，默认 5
sourceType  可选，ARTICLE / DOCUMENT
```

流程：

```text
query
-> 调用 qwen3.7-text-embedding 生成查询向量
-> ES kNN / cosine 检索 knowledge_chunk_v1
-> 按来源、分类或状态过滤
-> 返回切片正文、相似度、文章或文件来源信息
```

注意：查询文本与知识库文档属于非对称检索场景。后续可结合百炼的查询类型或指令参数，进一步提升召回质量。

### 2. RAG 接入 AI 链路

在以下调用 AI 前增加知识检索：

```text
AiService.chat
AI 自动回复
AI 工单草稿生成
```

目标流程：

```text
用户问题
-> 知识语义检索 TopK
-> 将命中片段组织为受限上下文
-> 注入 AI Prompt
-> AI 返回回答和知识引用来源
```

需要控制：

- 只使用已索引的切片。
- 限制注入片段数量和总文本长度，避免上下文膨胀。
- 无命中时让 AI 明确说明信息不足，并提示转人工。
- 前端展示来源标题、命中片段和相似度，方便客服核验。

### 3. 索引执行异步化

当前 `/execute` 为同步测试接口。后续可：

```text
创建任务后发布 MQ 消息
-> 消费者领取 PENDING 任务
-> 调用 Embedding / ES
-> 失败重试与死信处理
```

也可以先使用定时任务扫描 `scheduled_at <= now` 的 `PENDING` 索引任务。

### 4. 知识库前端完善

- 增加文件导入、解析、切片重建、索引任务创建与执行入口。
- 展示切片列表、索引状态、失败原因和重试按钮。
- 增加知识检索调试页，验证命中片段与相似度。
- 在 AI 辅助回答区域展示检索引用。

### 5. 其他项目待完成增强

- 完成自动派单真实数据联调：技能组、成员、分类码和 assignment_rule。
- 升级正式客户鉴权，并校验客户与会话归属。
- 将前端串行触发的 AI 自动回复升级为后端事件或 RabbitMQ 异步调用。
- 完成 SLA 延迟消息、死信、失败重试队列的端到端验证。
- 增加知识库、自动派单、权限越权、文件上传、MQ 重复消费等集成测试。
- 将数据库、Redis、RabbitMQ、MinIO、AI Key 等敏感配置迁移到环境变量或密钥管理。
- 增加 Docker Compose、部署脚本、README 架构图和演示说明。
