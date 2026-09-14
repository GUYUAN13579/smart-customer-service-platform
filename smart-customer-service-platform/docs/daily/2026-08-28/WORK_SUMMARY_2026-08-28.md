# 2026-08-28 工作总结

## 今日完成内容

### 1. 回顾 SLA 模块当前进度

今天先阅读了 `2026-08-27` 的工作总结，确认昨天已经完成：

- SLA 策略管理 CRUD。
- SLA 应用到工单接口。
- 工单中已经具备 `first_response_deadline`、`resolve_deadline`、`first_responded_at` 等 SLA 相关字段。
- 后续目标是进入 SLA 监控、通知和 RabbitMQ 自动提醒阶段。

### 2. 新增 SLA 监控接口骨架

今天新增了 SLA 监控相关接口，用于查询快超时和已超时工单。

新增接口：

```http
GET /api/v1/sla/tickets/risk
GET /api/v1/sla/tickets/overdue
```

接口作用：

- `/risk`：查询即将违反 SLA 的工单。
- `/overdue`：查询已经违反 SLA 的工单。

新增代码文件：

- `src/main/java/com/example/smartcustomerservice/domain/dto/SlaTicketMonitorQueryRequest.java`
- `src/main/java/com/example/smartcustomerservice/domain/vo/SlaTicketMonitorVO.java`
- `src/main/java/com/example/smartcustomerservice/service/sla/SlaMonitorService.java`
- `src/main/java/com/example/smartcustomerservice/service/impl/sla/SlaMonitorServiceImpl.java`
- `src/main/java/com/example/smartcustomerservice/controller/sla/SlaMonitorController.java`
- `docs/sql/sla_monitor_permission_patch.sql`

没有新增 Mapper，因为 SLA 监控查询基于现有 `ticket` 表，直接复用 `TicketMapper`。

### 3. 完成快超时工单查询逻辑

完成了 `pageRiskTickets` 方法。

支持查询条件：

- `type`：`FIRST_RESPONSE` / `RESOLVE`
- `withinMinutes`：未来多少分钟内即将超时，默认 30 分钟
- `priority`
- `category`
- `assigneeId`
- `page`
- `size`

核心逻辑：

```text
type = FIRST_RESPONSE:
查询 first_response_deadline 在 now 到 now + withinMinutes 之间的工单

type = RESOLVE:
查询 resolve_deadline 在 now 到 now + withinMinutes 之间的工单

type 为空:
查询首响快超时或解决快超时的工单
```

返回时会计算：

- `deadlineAt`
- `remainingMinutes`
- `riskType`

### 4. 完成已超时工单查询逻辑

完成了 `pageOverdueTickets` 方法。

核心逻辑：

```text
首响超时:
first_responded_at IS NULL
AND first_response_deadline < now

解决超时:
status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
AND resolve_deadline < now
```

如果 `type` 为空，则同时查询首响超时和解决超时。

返回时会计算：

- `deadlineAt`
- `overdueMinutes`
- `riskType`

其中：

- 快超时接口使用 `remainingMinutes`
- 已超时接口使用 `overdueMinutes`

### 5. 学习并修正 MyBatis-Plus 中 OR 条件写法

今天重点讲解并使用了 MyBatis-Plus 的 `.or()` 和嵌套 `.and(...)`。

错误风险写法：

```java
wrapper.eq(Ticket::getDeleted, 0)
        .between(Ticket::getFirstResponseDeadline, now, deadlineBefore)
        .or()
        .between(Ticket::getResolveDeadline, now, deadlineBefore);
```

这类写法可能导致 `OR` 影响外层条件，使 `deleted = 0` 等条件失效。

推荐写法：

```java
wrapper.and(item -> item
        .between(Ticket::getFirstResponseDeadline, now, deadlineBefore)
        .or()
        .between(Ticket::getResolveDeadline, now, deadlineBefore));
```

带多个条件组时推荐写成：

```java
wrapper.and(item -> item
        .and(first -> first
                .isNull(Ticket::getFirstRespondedAt)
                .lt(Ticket::getFirstResponseDeadline, now))
        .or()
        .and(resolve -> resolve
                .notIn(Ticket::getStatus, "RESOLVED", "CLOSED", "REJECTED")
                .lt(Ticket::getResolveDeadline, now)));
```

这表示：

```sql
AND (
  (
    first_responded_at IS NULL
    AND first_response_deadline < now
  )
  OR
  (
    status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
    AND resolve_deadline < now
  )
)
```

今天也明确了：

- `item -> item`
- `first -> first`
- `resolve -> resolve`

这些都只是 Java lambda 参数名，不是固定关键字，可以换成 `w`、`q`、`wrapper` 等名字。

### 6. 编译验证

今日后端修改完成后，已执行 Maven 编译验证。

编译命令：

```bash
/Users/chen/Documents/Codex/2026-08-13/wo/.tools/maven/apache-maven-3.9.9/bin/mvn -s /Users/chen/Documents/Codex/2026-08-13/wo/.tools/maven/settings-central.xml -q -DskipTests compile
```

结果：编译通过。

## 当前 SLA 模块进度

目前 SLA 模块已经包含：

### 已完成

- SLA 策略配置 CRUD。
- SLA 应用到工单。
- SLA 快超时查询。
- SLA 已超时查询。

### 尚未完成

- SLA 通知记录。
- SLA 手动提醒。
- SLA 自动提醒。
- RabbitMQ 延迟消息发送。
- RabbitMQ 延迟消息消费。
- 前端 SLA 监控页面。
- 前端通知中心。

## 后续要做的内容

### 1. 通知模块

下一步建议先做通知模块，因为 SLA 监控已经能查出风险工单，接下来应该能提醒对应人员。

建议接口：

```http
GET /api/v1/notifications
GET /api/v1/notifications/unread-count
PUT /api/v1/notifications/{id}/read
PUT /api/v1/notifications/read-all
```

通知表建议字段：

- `id`
- `receiver_id`
- `type`
- `title`
- `content`
- `business_type`
- `business_id`
- `read_status`
- `read_at`
- `created_at`
- `deleted`

### 2. SLA 手动提醒接口

建议接口：

```http
POST /api/v1/sla/tickets/{ticketId}/remind
```

作用：

- 管理员或主管在 SLA 监控页面看到快超时或已超时工单后，可以手动提醒负责人。
- 该接口会插入一条通知记录。
- 同时可以插入一条工单操作日志。

请求字段建议：

- `remindType`
- `receiverId`
- `content`

### 3. RabbitMQ 自动提醒

后续在 `apply-sla` 成功后，可以发送 RabbitMQ 延迟消息。

推荐消息类型：

- 首响即将超时提醒
- 首响已超时检查
- 解决即将超时提醒
- 解决已超时检查

消息中只放必要字段：

```json
{
  "ticketId": 1,
  "deadlineType": "FIRST_RESPONSE",
  "eventType": "OVERDUE_CHECK",
  "deadlineAt": "2026-08-28T18:30:00"
}
```

消费者收到消息后必须重新查询数据库，不能直接相信消息里的旧状态。

### 4. 前端 SLA 页面

后续前端可以新增：

- SLA 策略管理页面
- SLA 监控页面
- 快超时工单列表
- 已超时工单列表
- 手动提醒按钮
- 通知中心入口

## 下一步推荐

下一步建议从通知模块开始：

```text
notification 表设计
Notification 实体类
NotificationMapper
NotificationQueryRequest
NotificationVO
NotificationService
NotificationController
NotificationServiceImpl
```

通知模块完成后，再接：

```text
SLA 手动提醒 -> RabbitMQ 自动提醒 -> 前端通知中心
```
