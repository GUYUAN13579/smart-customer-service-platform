# 后端待解决清单

这个文件用于记录当前为了先跑通主链路而暂时简化的设计点，避免后续进入派单、SLA、通知等模块时遗漏。

## 会话中心

### 1. 会话创建时 current_agent_id 暂不应固定为当前登录用户

当前讨论点：

- `createSession` 如果直接把 `SecurityUtils.getCurrentUserId()` 写入 `conversation_session.current_agent_id`，语义上更像“创建会话即人工接管”。
- 真实系统里，会话刚创建时可能仍由 AI 托管，或者进入待分配队列。
- 后续完成派单/接管逻辑后，`current_agent_id` 应由派单结果或人工接管动作写入。

后续建议：

- 创建会话时默认 `current_agent_id = null`。
- `status` 初始值保持 `ACTIVE`，表示会话进行中。
- 新增人工接管接口时再写入当前客服 ID，例如 `POST /api/v1/conversations/{id}/take-over`。
- 工单派单完成后，如果需要同步会话处理人，再由派单服务更新 `current_agent_id`。

### 2. 会话编号生成需要增强唯一性

当前可以先使用时间戳生成 `session_no`，但高并发下可能碰撞。

后续建议：

- 使用统一编号工具类。
- 或在时间戳后追加随机数。
- 或使用数据库唯一键冲突重试。

### 3. 会话状态流转需要集中管理

后续需要明确状态含义：

- `ACTIVE`：会话进行中，可能由 AI 或系统托管。
- `TAKEN_OVER`：已被人工客服接管。
- `CLOSED`：会话关闭，不允许继续发送普通消息。

状态流转建议：

- `ACTIVE -> TAKEN_OVER`
- `ACTIVE -> CLOSED`
- `TAKEN_OVER -> CLOSED`

### 4. 会话消息发送后可考虑触发后续动作

当前发送消息只需要插入 `conversation_message` 并更新 `last_message_at`。

后续可扩展：

- 客户消息触发 AI 回复。
- AI 低置信度触发人工接管提醒。
- 关键意图触发工单草稿生成。
- 消息事件进入 MQ。

## AI 能力

### 1. AI 自动回复与转人工工单流程调整

当前决策：

- 项目最终流程调整为“AI 优先回复，人工兜底”。
- 用户发起咨询后，系统先创建会话和消息，由 AI 自动回复。
- 当用户点击转人工、输入转人工相关关键词，或 AI 判断无法解决时，进入转人工流程。
- 转人工时系统创建待审核工单，AI 负责填充标题、标准化摘要、分类、优先级、建议处理动作等字段。
- `original_content` 保存用户原始诉求，`ai_summary` 保存 AI 标准化总结，两者需要分开展示和保存。
- 工单进入 `PENDING_REVIEW` 状态后，由 `ADMIN` 或 `SUPERVISOR` 审核。
- 审核通过后再派给客服，客服接管会话并处理。

关键原则：

- AI 只提供建议内容，不直接决定审核结果、派单结果和最终业务状态。
- 工单创建、审核、派单、状态流转必须由后端业务代码控制。
- 审核员需要能修改 AI 生成的字段，避免 AI 总结错误直接进入后续流程。

后续建议：

- 新增独立接口，例如 `POST /api/v1/ai/conversations/{sessionId}/auto-reply`。
- 新增转人工接口，例如 `POST /api/v1/conversations/{sessionId}/manual-transfer`。
- 新增 AI 工单草稿接口，例如 `POST /api/v1/ai/conversations/{sessionId}/ticket-draft`。
- 自动回复必须受会话 `ai_enabled` 开关控制。
- 自动回复前先读取会话上下文，后续知识库完成后再加入知识库命中内容。
- AI 回复要写入 `conversation_message`，`sender_type = AI`。
- AI 调用过程和最终发送动作都要写入日志，方便审计和排查。
- 低置信度、知识库无命中、用户要求人工、连续多轮未解决时，应自动进入或提示进入转人工流程。
