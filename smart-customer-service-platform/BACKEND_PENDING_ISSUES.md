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
