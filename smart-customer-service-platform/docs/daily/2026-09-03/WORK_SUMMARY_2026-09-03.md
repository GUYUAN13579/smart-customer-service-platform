# 2026-09-03 工作总结

## 今日完成内容

### 1. 完成运营统计与管理看板

补齐并接入了运营统计模块，当前提供以下接口：

```http
GET /api/v1/statistics/dashboard
GET /api/v1/statistics/agent-workloads
GET /api/v1/statistics/ticket-trends?days=7
GET /api/v1/statistics/sla-performance
```

统计内容包括：

- 工单总量、当日新增、处理中、待审核、待派单、SLA 风险与超时数量。
- 客服在各技能组内的 `ASSIGNED`、`PROCESSING`、总活跃工单数、最大承接量和负载率。
- 最近 1 至 90 天的新增、解决、关闭工单趋势；解决与关闭数据依据 `ticket_operation_log` 的 `RESOLVE`、`CLOSE` 日志统计，避免把普通更新误算成状态流转。
- 首次响应与解决 SLA 的已考核、达标、违约数量及达标率。

前端运营看板已接入真实数据，新增工单趋势图、SLA 达标进度和客服实时负载表。

新增统计权限脚本：

```text
docs/sql/statistics_permission_patch.sql
```

### 2. 定位自动派单未命中规则的问题

联调发现工单审核通过后仍处于 `WAITING_ASSIGN`，自动派单预览返回：

```text
未找到匹配的启用派单规则
```

确认自动派单并非没有触发，而是规则采用精确匹配：

```text
ticket.category + ticket.priority + assignment_rule.enabled = 1
```

原 AI 草稿输出了“账号与安全”之类的自由中文分类，而派单规则需要稳定的分类码，因此无法匹配。

### 3. 统一 AI 工单分类并增加后端兜底

修改 `ai-prompts.yml` 中的工单草稿提示词，要求模型只能输出以下分类：

```text
ORDER
PAYMENT
REFUND
LOGISTICS
ACCOUNT
TECHNICAL
COMPLAINT
GENERAL
```

同时在 `AiServiceImpl` 中增加分类标准化：即使模型输出中文、`UNKNOWN` 或其他未知值，也统一降级为 `GENERAL`。

这样 AI 工单分类、技能组分类、派单规则分类和 SLA 分类可以共享同一组受控值。历史工单不会被自动迁移；需要在审核时修正分类，或更新数据库中的规则配置。

### 4. 补齐客服端附件上传权限脚本

排查客服会话页面图片上传 `403`：

- 用户端测试页面使用 `/api/v1/customer-test/**`，该路径被放行。
- 客服端使用 `/api/v1/files/images/upload`，要求 `file:image:upload` 权限。

新增脚本：

```text
docs/sql/file_resource_permission_patch.sql
```

脚本将 `file:upload`、`file:image:upload`、`file:detail` 授予 `AGENT`、`SUPERVISOR`、`ADMIN`。执行后需要重新登录，使新权限写入 JWT。

### 5. 完成知识库表设计

新增知识库 / RAG 表结构脚本：

```text
docs/sql/knowledge_base_schema.sql
```

包含四张表：

```text
knowledge_article
-> 人工维护的知识文章，包含正文、分类、标签、发布状态和版本。

knowledge_document
-> 上传到知识库的原始文件及文本解析结果。

knowledge_chunk
-> 文章或文件切分后的检索片段，以及 ES 索引状态和向量模型信息。

knowledge_index_task
-> 记录解析、切片、向量化、写入或删除 ES 索引等异步任务与重试状态。
```

向量内容计划存储在 Elasticsearch，MySQL 保存知识内容、切片元数据和处理任务状态。

### 6. 完成知识文章 CRUD、发布与前端管理页

后端已完成知识文章基础接口：

```http
POST   /api/v1/knowledge/articles
PUT    /api/v1/knowledge/articles/{id}
DELETE /api/v1/knowledge/articles/{id}
GET    /api/v1/knowledge/articles/{id}
GET    /api/v1/knowledge/articles
POST   /api/v1/knowledge/articles/{id}/publish
POST   /api/v1/knowledge/articles/{id}/offline
```

实现内容：

- 新增 `KnowledgeArticle` 实体、DTO、VO、Mapper、Service、ServiceImpl、Controller。
- 支持关键词、分类、状态分页查询。
- 标签以 JSON 数组保存，接口返回 `List<String>`。
- 新建文章默认 `DRAFT`；发布后为 `PUBLISHED`；下线为 `OFFLINE`；删除采用逻辑删除并同步下线。
- 发布时记录发布人、发布时间并递增版本号。

新增权限脚本：

```text
docs/sql/knowledge_article_permission_patch.sql
```

文章管理权限授予 `ADMIN` 与 `SUPERVISOR`。

前端知识库页已替换为真实文章管理页面：

- 关键词、分类、状态筛选与分页。
- 新增、编辑、发布、下线、删除。
- 标签可通过逗号输入并以标签样式展示。
- 路由与侧边栏仅对 `ADMIN`、`SUPERVISOR` 展示。
- 移除了尚未实现的语义检索演示入口，避免前端调用不存在的接口。

### 7. 验证结果

- 后端执行 Maven 编译，通过。
- 前端执行 `npm run build`，通过。

## 当前核心业务闭环

```text
客户发送咨询
-> AI 自动/辅助回复
-> 客户转人工
-> AI 生成标准化工单草稿
-> 审核员审核并校正分类与优先级
-> 自动匹配派单规则和技能组
-> 分配客服、应用 SLA、创建提醒
-> 客服接管会话并回复
-> 记录首次响应
-> 客服确认问题已解决
-> 工单 RESOLVED、会话 CLOSED
-> 通知与运营统计看板展示
```

## 后续优先完成内容

### 1. 知识库文件导入与解析

- 新增知识库文件上传 / 导入接口，复用已有 `file_resource` 和 MinIO 存储。
- 创建 `knowledge_document`，保存文件元数据和解析状态。
- 接入文本、Markdown、PDF、Word 等常见文件解析能力。
- 对解析失败记录失败原因，并支持重新解析。

### 2. 知识切片与索引任务

- 设计按标题、段落和长度切分文章 / 文件的切片策略。
- 写入 `knowledge_chunk`，计算内容哈希，防止重复切片。
- 创建 `knowledge_index_task` 任务，支持待执行、处理中、成功、失败与重试。
- 后续通过 RabbitMQ 异步执行耗时的解析、切片和索引操作。

### 3. 向量化、Elasticsearch 与 RAG 检索

- 确定 Embedding 模型和 Elasticsearch 向量索引 Mapping。
- 将已发布文章的切片向量写入 Elasticsearch。
- 提供知识检索接口，支持关键词、分类过滤、TopK 和相似度分数。
- 在 AI 回复、AI 自动回复、AI 工单草稿生成前检索相关知识片段。
- 返回文章标题、片段和引用来源，供客服核验 AI 回答。

### 4. 自动派单联调与配置完善

- 执行或补齐分类码对应的技能组和 `assignment_rule` 数据。
- 使用 `ACCOUNT / P2` 等实际工单验证审核后自动派单。
- 验证技能组启用状态、客服成员状态、AGENT 角色和最大承接量限制。
- 处理历史中文分类工单的审核修正或数据迁移。

### 5. 其余工程化增强

- 将客户测试接口升级为正式客户登录与会话归属校验。
- 将前端串行触发的 AI 自动回复升级为后端事件或 MQ 异步触发。
- 完成 RabbitMQ 延迟、死信、重试队列的真实 SLA 告警端到端验证。
- 增加自动派单、鉴权越权、消息重复消费、文件上传、知识检索等集成测试。
- 将数据库、Redis、RabbitMQ、MinIO、AI 密钥迁移到环境变量或密钥管理。
- 准备 Docker Compose 或部署脚本，统一运行前后端和基础服务。
