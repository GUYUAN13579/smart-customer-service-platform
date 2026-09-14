# 2026-08-31 工作总结

## 今日完成内容

### 1. 新增 SLA 自动告警管理接口骨架

新增接口：

```http
GET  /api/v1/sla/alerts
GET  /api/v1/sla/alerts/{id}
POST /api/v1/sla/alerts/{id}/retry
```

新增代码：

- `SlaAlert` 实体类。
- `SlaAlertQueryRequest` 查询请求对象。
- `SlaAlertVO` 返回对象。
- `SlaAlertMapper`。
- `SlaAlertService`、`SlaAlertServiceImpl`。
- `SlaAlertController`。
- `docs/sql/sla_alert_permission_patch.sql`。

`sla_alert` 记录自动提醒事件，用于告警审计、消息幂等、失败重试和后续运营查询。

### 2. 完成 SLA 告警记录查询与失败告警重置

完成 `pageAlerts`：

- 支持按 `ticketId`、`alertType`、`alertLevel`、`status`、计划时间范围分页筛选。
- 查询字段为空时不会生成错误的 `= null` 条件。
- 按 `scheduledAt`、`id` 倒序返回。

完成 `getAlert`：

- 校验告警 ID。
- 查询并返回单条告警记录。

完成 `retryAlert` 的数据库状态重置：

- 仅允许 `FAILED` 状态重试。
- 将状态改为 `PENDING`。
- 重置计划时间为当前时间。
- 清空失败原因和发送时间。
- `retryCount + 1`。
- 使用状态条件更新避免并发重复重试。

### 3. 确认 RabbitMQ 延迟方案

服务器 RabbitMQ 版本为 `4.3.4`。

确认不使用 `rabbitmq_delayed_message_exchange` 插件，因为该插件不适配 RabbitMQ 4.3。

本项目改用：

```text
消息级 TTL + 死信交换机（DLX）+ 正常消费队列
```

消息流：

```text
生产者
-> sla.alert.delay.exchange
-> sla.alert.delay.queue
-> TTL 到期
-> sla.alert.dead-letter.exchange
-> sla.alert.trigger.queue
-> 消费者
```

### 4. 完成 SLA 自动提醒 RabbitMQ 基础设施

新增：

- `SlaAlertMqConfig`。
- `SlaAlertMessage`，消息体只携带 `alertId`。
- `SlaAlertMessageProducer`。
- `SlaAlertMessageConsumer`。

在 `MqConstants` 中集中维护以下名称：

- 延迟交换机、延迟队列、延迟路由键。
- 死信交换机。
- 消费队列、消费路由键。
- 告警 ID 消息头和关联 ID 前缀。

`SlaAlertMqConfig` 已完成：

- 延迟交换机声明。
- 延迟队列声明。
- 延迟队列配置 `x-dead-letter-exchange` 与 `x-dead-letter-routing-key`。
- 死信交换机声明。
- 正常消费队列声明。
- 两段 Binding 声明。
- `Jackson2JsonMessageConverter`。
- `@EnableRabbit`。

### 5. 完成消息生产者与发布确认

`sendDelayedAlert` 已完成：

- 校验 `SlaAlertMessage` 和 `alertId`。
- 校验延迟时间非负。
- 将消息发送到延迟交换机。
- 使用 `MessagePostProcessor` 为每一条消息设置 TTL。
- 使用 `CorrelationData` 为每次投递设置唯一关联 ID。
- 在消息头中写入 `x-sla-alert-id`。

本地配置已开启：

```yaml
publisher-confirm-type: correlated
publisher-returns: true
template:
  mandatory: true
```

新增 `SlaAlertPublisherConfirmHandler`：

- Broker 未确认消息到达交换机时，将 `sla_alert` 标记为 `FAILED`。
- 消息到达交换机但无法路由到队列时，也标记为 `FAILED`。
- 记录失败原因到 `last_error_message`。
- 只更新仍是 `PENDING` 的告警，避免覆盖后续状态。

### 6. 完成 applySla 与自动告警计划联动

`TicketServiceImpl.applySla` 已补充：

```text
应用 SLA
-> 更新工单首响/解决截止时间
-> 插入 4 条 PENDING 的 sla_alert
-> 数据库事务提交成功
-> 分别调用 sendDelayedAlert 投递延迟消息
```

创建的告警类型：

```text
FIRST_RESPONSE + RISK    ：首响截止前 30 分钟
FIRST_RESPONSE + OVERDUE ：首响截止时间
RESOLVE + RISK           ：解决截止前 30 分钟
RESOLVE + OVERDUE        ：解决截止时间
```

使用 `TransactionSynchronizationManager.registerSynchronization(...)` 的 `afterCommit()` 回调投递消息。

这样能够确保消费者收到 `alertId` 时，数据库中的 `sla_alert` 已经提交可查。

如果事务提交后同步投递异常，会将对应告警更新为 `FAILED`。

同时修复：

- `overwrite` 为空时可能出现的空指针。
- 工单更新结果未校验的问题。

### 7. 完成自动提醒消费者主流程

`SlaAlertMessageConsumer.consumeSlaAlert` 已完成：

- 使用 `@RabbitListener` 监听 `sla.alert.trigger.queue`。
- 使用 `@Transactional` 保证通知、操作日志、告警状态更新一致。
- 根据 `alertId` 查询 `sla_alert`，再根据 `ticketId` 查询工单。
- 通过 `PENDING -> PROCESSING` 的条件更新领取告警，防止重复消费时重复发送通知。
- 重新查询并判断工单是否仍需要提醒。
- 工单已首响、已解决、已关闭、已驳回、截止时间已变更或未分配处理人时，更新为 `SKIPPED`。
- 创建通知记录，通知类型为 `SLA_RISK` 或 `SLA_OVERDUE`。
- 创建工单操作日志，操作类型为 `SLA_AUTO_ALERT`，操作人为空表示系统行为。
- 通知成功后将告警更新为 `SENT`，保存 `notificationId`、`sentAt`。

## 当前 SLA 模块进度

### 已完成

- SLA 策略 CRUD。
- SLA 应用到工单。
- SLA 快超时和已超时工单查询。
- 通知中心：分页、未读数、单条已读、全部已读。
- SLA 手动提醒。
- SLA 自动告警记录管理：分页、详情、失败状态重置。
- TTL + DLX RabbitMQ 基础设施。
- 延迟消息生产者。
- RabbitMQ 发布确认与路由失败回调。
- `applySla` 自动创建并投递四类告警。
- 自动提醒消费者：校验、通知、日志与状态流转。

### 待完成

- 将 `retryAlert` 与 `SlaAlertMessageProducer` 联动，真正重新投递失败告警。
- 为消费者增加重试策略、最大重试次数和消费失败死信队列。
- 增加定时扫描兜底，处理消息投递后长期处于 `PENDING` / `PROCESSING` 的告警。
- 为自动提醒编写集成测试。
- 验证 RabbitMQ 控制台中交换机、队列、Binding 是否已由应用成功创建。
- 补齐 SLA 监控和通知中心前端页面。

## 后续开发建议

### 1. 完成失败告警的真正重试

修改 `retryAlert`：

```text
FAILED
-> 更新为 PENDING，retryCount + 1
-> 调用 sendDelayedAlert(new SlaAlertMessage(alertId), 0)
-> 再次进入自动提醒消费者
```

建议使用事务提交后再投递，和 `applySla` 保持一致。

### 2. 增加消费者失败处理

当前消费者业务异常会回滚数据库操作。后续可增加：

- RabbitMQ 消费重试次数。
- 消费失败死信队列。
- 最大次数后将告警状态标记为 `FAILED`。
- 保存失败原因，允许管理员从告警页面手动重试。

### 3. 编写集成测试

建议至少测试：

- `applySla` 后是否生成 4 条 `sla_alert`。
- 延迟消息是否进入正确的交换机和队列。
- 首响风险、首响超时、解决风险、解决超时四种消费者判断。
- 工单已处理时告警是否变为 `SKIPPED`。
- 自动通知和工单操作日志是否成功写入。
- 发布失败或路由失败时告警是否变为 `FAILED`。

### 4. 后续增强项

- 告警筛选前端页面。
- 通知中心前端入口与未读角标。
- SLA 数据看板：超时数量、平均响应时长、按优先级/分类统计。
- 定时扫描兜底任务。
