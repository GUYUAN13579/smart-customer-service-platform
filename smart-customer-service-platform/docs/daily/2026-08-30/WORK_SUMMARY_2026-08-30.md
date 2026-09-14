# 2026-08-30 工作总结

## 今日完成内容

### 1. 完成通知中心基础结构

新增了通知中心相关的实体类、请求对象、返回对象、Mapper、Service 和 Controller。

新增接口：

```http
GET /api/v1/notifications
GET /api/v1/notifications/unread-count
PUT /api/v1/notifications/{id}/read
PUT /api/v1/notifications/read-all
```

通知表为 `notification`，核心字段包括：

- `receiver_id`：通知接收人。
- `type`：通知类型，例如 `SLA_RISK`、`SLA_OVERDUE`。
- `business_type`、`business_id`：关联业务对象，例如某一张工单。
- `read_status`、`read_at`：通知已读状态与已读时间。
- `deleted`：逻辑删除标记。

对应 SQL 文件：

- `docs/sql/notification_patch.sql`

其中包含通知表、索引、通知接口权限以及 SLA 手动提醒权限。

### 2. 完成通知分页查询

完成 `pageNotifications` 方法，查询规则如下：

- 只能查询当前登录用户自己的通知，用户 ID 从 `SecurityUtils.getCurrentUserId()` 获取。
- 默认排除逻辑删除通知。
- 支持按 `readStatus`、`type`、`businessType` 筛选。
- 可选筛选字段为空时不会拼接错误的 `= null` 条件。
- 按 `createdAt`、`id` 倒序，最新通知优先展示。
- 正确返回当前页码、每页大小和总条数。

### 3. 完成未读数量查询与单条已读

完成 `getUnreadCount` 和 `markAsRead` 方法。

未读数量查询：

- 统计当前用户、未删除、`read_status = UNREAD` 的通知数量。

单条已读：

- 先校验通知属于当前用户且未被逻辑删除。
- 已读通知重复调用时直接返回成功，保持接口幂等。
- 更新时限定原状态为 `UNREAD`，避免并发情况下覆盖异常状态。
- 通知不存在、属于其他用户或已删除时，返回“通知不存在”。

### 4. 完成 SLA 手动提醒接口

完成接口：

```http
POST /api/v1/sla/tickets/{ticketId}/remind
```

`remindTicket` 的处理流程：

```text
管理员或主管发起提醒
-> 校验工单、当前用户和接收人
-> 创建 notification 通知记录
-> 创建 ticket_operation_log 工单操作日志
-> 返回 SlaTicketRemindVO
```

已实现的校验包括：

- 工单必须存在且未逻辑删除。
- 已关闭、已驳回工单不允许提醒。
- 已完成首次响应的工单不允许发送首响提醒。
- 已解决工单不允许发送解决提醒。
- 接收人必须存在、未删除且状态启用。
- 为空的提醒内容会自动生成默认文案。

通知与操作日志使用同一个事务；任意一步插入失败都会回滚，避免只创建通知或只创建日志的半完成数据。

操作日志规则：

```text
operation_type = SLA_REMIND
from_status = 当前工单状态
to_status = 当前工单状态
operator_id = 当前登录管理员或主管
```

### 5. 明确 sla_alert 的后续作用

今天明确了 `sla_alert` 和 `notification` 的职责区别：

- `notification`：面向具体用户展示的消息，可标记已读。
- `sla_alert`：系统自动 SLA 告警事件记录，用于告警审计、失败重试和幂等控制。

当前手动提醒阶段只需写入 `notification + ticket_operation_log`。

后续接入 RabbitMQ 自动提醒后，`sla_alert` 用于防止同一工单、同一告警类型因消息重试而重复发送通知。

### 6. 编译验证

今日后端修改后已执行 Maven 编译验证，结果通过。

## 当前 SLA 模块进度

### 已完成

- SLA 策略 CRUD。
- SLA 应用到工单。
- SLA 快超时工单查询。
- SLA 已超时工单查询。
- 通知分页查询。
- 未读通知数量查询。
- 单条通知标记已读。
- SLA 手动提醒。

### 待完成

- 全部通知标记已读 `markAllAsRead`。
- `sla_alert` 自动告警记录表。
- RabbitMQ 延迟消息生产者。
- RabbitMQ 自动提醒消费者。
- 自动提醒失败重试与告警记录查询。
- 定时扫描兜底任务。
- SLA 监控与通知中心前端页面。

## 后续开发建议

### 1. 完成全部通知已读

接口：

```http
PUT /api/v1/notifications/read-all
```

只更新当前用户、未删除且状态为 `UNREAD` 的通知。即使当前没有未读通知，也应直接返回成功，保持幂等。

### 2. 设计 sla_alert 表和自动提醒消息模型

建议先新增 `sla_alert` 表，用来记录系统告警事件。

建议字段：

```text
id
ticket_id
alert_type
deadline_at
triggered_at
alert_status
notification_id
retry_count
last_error_message
created_at
updated_at
```

推荐自动提醒消息内容：

```json
{
  "ticketId": 1,
  "deadlineType": "FIRST_RESPONSE",
  "eventType": "RISK_REMIND",
  "deadlineAt": "2026-08-30T18:30:00"
}
```

### 3. 接入 RabbitMQ 自动提醒

整体流程：

```text
applySla 成功
-> 按首响/解决截止时间发送延迟消息
-> 消费者收到消息后重新查询工单
-> 判断是否仍需提醒
-> 写入 sla_alert，保证幂等
-> 创建 notification
-> 写入 ticket_operation_log（SLA_AUTO_ALERT）
```

消费者不能直接相信消息中的旧状态，必须重新查询数据库，避免工单已经首响、解决或关闭后仍发送提醒。

### 4. 后续增强项

- 新增 `GET /api/v1/sla/alerts`，查询自动告警记录。
- 新增 `POST /api/v1/sla/alerts/{id}/retry`，补偿失败告警。
- 增加定时任务作为 RabbitMQ 的兜底扫描。
- 补齐 SLA 监控和通知中心前端页面。
