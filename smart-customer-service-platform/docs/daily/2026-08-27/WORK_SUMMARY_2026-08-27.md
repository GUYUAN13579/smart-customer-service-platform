# 2026-08-27 工作总结

## 今日完成内容

### 1. SLA 概念与后端建设方向确认

今天明确了 SLA 在当前智能客服与工单系统中的定位：

- SLA 用于约定工单的首次响应时限和解决时限。
- 不同工单分类、优先级可以配置不同 SLA 策略。
- SLA 策略不会单独产生业务价值，必须应用到工单上，生成工单自己的截止时间。
- 后续 RabbitMQ、定时任务、通知模块都会围绕这些截止时间工作。

### 2. SLA 策略管理 CRUD 完成

完成了 SLA 策略的基础管理接口：

```http
POST   /api/v1/sla/policies
GET    /api/v1/sla/policies
GET    /api/v1/sla/policies/{id}
PUT    /api/v1/sla/policies/{id}
DELETE /api/v1/sla/policies/{id}
```

完成内容包括：

- `SlaPolicy` 实体类
- `SlaPolicyCreateRequest`
- `SlaPolicyUpdateRequest`
- `SlaPolicyQueryRequest`
- `SlaPolicyVO`
- `SlaPolicyMapper`
- `SlaPolicyService`
- `SlaPolicyServiceImpl`
- `SlaPolicyController`
- `sla_policy` 建表 SQL
- SLA 策略相关权限 SQL

核心业务逻辑：

- 创建 SLA 策略时，校验同分类、同优先级是否已有启用策略。
- 修改 SLA 策略时，排除自身后再校验重复。
- 删除 SLA 策略使用软删除。
- 分页查询支持关键词、分类、优先级、启用状态筛选。
- 查询只返回 `deleted = 0` 的数据。

### 3. SLA 应用到工单接口骨架与部分逻辑完成

新增接口：

```http
POST /api/v1/tickets/{id}/apply-sla
```

该接口用于把 SLA 策略应用到具体工单上，生成：

- `ticket.first_response_deadline`
- `ticket.resolve_deadline`

新增内容包括：

- `TicketApplySlaRequest`
- `TicketApplySlaVO`
- `ticket:apply-sla` 权限 SQL
- `TicketController.applySla`
- `TicketService.applySla`
- `TicketServiceImpl.applySla`

当前 `applySla` 已经包含：

- 查询工单。
- 校验工单存在、未删除、未关闭、未驳回。
- 判断是否允许覆盖已有 SLA 时间。
- 按 `category + priority` 查找启用 SLA 策略。
- 如果找不到完全匹配，则查找 `category IS NULL + priority` 的默认策略。
- 找不到 SLA 策略时抛出明确异常。
- 使用 `baseTime` 或当前时间计算首次响应截止时间与解决截止时间。
- 更新工单 SLA 时间。
- 插入工单操作日志，操作类型为 `APPLY_SLA`。
- 返回 `TicketApplySlaVO`。

### 4. 编译验证

今日新增和修改的后端代码已完成 Maven 编译验证。

编译命令：

```bash
/Users/chen/Documents/Codex/2026-08-13/wo/.tools/maven/apache-maven-3.9.9/bin/mvn -s /Users/chen/Documents/Codex/2026-08-13/wo/.tools/maven/settings-central.xml -q -DskipTests compile
```

结果：编译通过。

## 今日涉及文件

### SLA 策略模块

- `src/main/java/com/example/smartcustomerservice/domain/entity/SlaPolicy.java`
- `src/main/java/com/example/smartcustomerservice/domain/dto/SlaPolicyCreateRequest.java`
- `src/main/java/com/example/smartcustomerservice/domain/dto/SlaPolicyUpdateRequest.java`
- `src/main/java/com/example/smartcustomerservice/domain/dto/SlaPolicyQueryRequest.java`
- `src/main/java/com/example/smartcustomerservice/domain/vo/SlaPolicyVO.java`
- `src/main/java/com/example/smartcustomerservice/mapper/sla/SlaPolicyMapper.java`
- `src/main/java/com/example/smartcustomerservice/service/sla/SlaPolicyService.java`
- `src/main/java/com/example/smartcustomerservice/service/impl/sla/SlaPolicyServiceImpl.java`
- `src/main/java/com/example/smartcustomerservice/controller/sla/SlaPolicyController.java`
- `docs/sql/sla_policy_patch.sql`

### 工单 SLA 应用模块

- `src/main/java/com/example/smartcustomerservice/domain/dto/TicketApplySlaRequest.java`
- `src/main/java/com/example/smartcustomerservice/domain/vo/TicketApplySlaVO.java`
- `src/main/java/com/example/smartcustomerservice/controller/ticket/TicketController.java`
- `src/main/java/com/example/smartcustomerservice/service/ticket/TicketService.java`
- `src/main/java/com/example/smartcustomerservice/service/impl/ticket/TicketServiceImpl.java`
- `docs/sql/ticket_apply_sla_permission_patch.sql`

## SLA 在系统中的整体流程

### 1. 管理员配置 SLA 策略

管理员或主管在后台配置 SLA 策略，例如：

| 工单分类 | 优先级 | 首次响应时限 | 解决时限 |
| --- | --- | --- | --- |
| REFUND | P1 | 10 分钟 | 120 分钟 |
| REFUND | P2 | 30 分钟 | 480 分钟 |
| null | P3 | 120 分钟 | 1440 分钟 |

其中 `category = null` 可以作为默认策略：当系统找不到某个分类的专门策略时，就使用该优先级的默认策略。

### 2. 用户咨询，AI 优先自动回复

用户在用户测试页或真实客户端发起问题后，系统先进入会话中心。

如果 AI 能回答：

- AI 自动回复用户。
- 不一定马上创建工单。

如果用户不满意，点击转人工或输入转人工：

- 系统生成 AI 工单草稿。
- 草稿包含用户原始问题、AI 总结、建议处理方案、分类、优先级等。
- 管理员或主管审核。

### 3. 审核通过后形成正式待派单工单

审核人员确认 AI 工单草稿后，工单进入：

```text
WAITING_ASSIGN
```

此时工单已经有：

- `category`
- `priority`
- `customer_id`
- `session_id`
- `original_content`
- `ai_summary`
- `suggested_action`

### 4. 应用 SLA 策略

系统根据工单的：

```text
category + priority
```

查找启用中的 SLA 策略。

优先级：

1. 先找完全匹配：`category = 工单分类 AND priority = 工单优先级`
2. 找不到时，找默认策略：`category IS NULL AND priority = 工单优先级`
3. 仍然找不到时，提示没有匹配的 SLA 策略

找到策略后，系统计算：

```text
first_response_deadline = baseTime + first_response_minutes
resolve_deadline = baseTime + resolve_minutes
```

然后写入 `ticket` 表。

### 5. 派单与客服处理

工单被派给客服后进入：

```text
ASSIGNED
```

客服点击开始处理后进入：

```text
PROCESSING
```

如果这是客服第一次真正响应工单，后续可以补充逻辑：

```text
first_responded_at = 当前时间
```

这个字段用于判断首次响应是否超时。

### 6. 工单解决与关闭

客服解决工单后进入：

```text
RESOLVED
```

管理员、主管或负责客服关闭工单后进入：

```text
CLOSED
```

系统可以通过 `resolve_deadline` 和 `closed_at` 或最终状态判断解决是否超时。

## RabbitMQ 在 SLA 中的作用

RabbitMQ 的作用不是保存 SLA 策略，也不是直接计算 SLA，而是负责“到时间提醒”和“异步处理”。

### 1. 为什么需要 RabbitMQ

如果没有 RabbitMQ，系统要知道某个工单快超时或已超时，通常只能靠定时任务不断扫描数据库：

```text
每隔 1 分钟查一次 ticket 表
找出即将超时或已经超时的工单
发送提醒
```

这种方式简单，但缺点是：

- 数据量大时数据库压力会增加。
- 扫描频率太低会提醒不及时。
- 扫描频率太高会浪费资源。

RabbitMQ 可以让系统在工单应用 SLA 时，就提前投递一条“延迟消息”。

### 2. RabbitMQ 在当前项目中的推荐流程

当系统执行 `apply-sla` 成功后：

1. 工单写入 `first_response_deadline` 和 `resolve_deadline`。
2. 系统向 RabbitMQ 发送延迟消息。
3. 消息不会立刻被消费，而是等到指定时间才进入队列。
4. 到时间后，消费者收到消息。
5. 消费者重新查询数据库确认工单当前状态。
6. 如果工单确实还没响应或还没解决，就发送通知、写日志、标记 SLA 风险。

### 3. 首次响应提醒示例

假设 SLA 要求 30 分钟内首次响应。

系统可以发送两类消息：

```text
25 分钟后：提醒快要首次响应超时
30 分钟后：检查是否已经首次响应超时
```

消费者收到消息后，不直接相信消息本身，而是重新查数据库：

```text
如果 first_responded_at 为空，并且 now > first_response_deadline
说明首次响应已超时
```

### 4. 解决时限提醒示例

假设 SLA 要求 8 小时内解决。

系统可以发送：

```text
7 小时 30 分钟后：提醒即将解决超时
8 小时后：检查是否解决超时
```

消费者判断：

```text
如果 status 不是 RESOLVED 或 CLOSED，并且 now > resolve_deadline
说明解决已超时
```

### 5. RabbitMQ 消息里应该放什么

消息里不要放完整工单数据，只放必要信息：

```json
{
  "ticketId": 1,
  "deadlineType": "FIRST_RESPONSE",
  "eventType": "OVERDUE_CHECK",
  "deadlineAt": "2026-08-27T18:30:00"
}
```

原因：

- 工单状态可能在消息等待期间发生变化。
- 消费时必须重新查数据库，确保判断的是最新状态。
- 消息越小越稳定。

## 后续要做的内容

### 1. SLA 风险查询接口

查询即将超时的工单：

```http
GET /api/v1/sla/tickets/risk
```

推荐筛选条件：

- `type`：`FIRST_RESPONSE` / `RESOLVE`
- `withinMinutes`
- `priority`
- `category`
- `assigneeId`

### 2. SLA 超时查询接口

查询已经超时的工单：

```http
GET /api/v1/sla/tickets/overdue
```

判断逻辑：

- 首响超时：`first_responded_at IS NULL AND now > first_response_deadline`
- 解决超时：`status NOT IN ('RESOLVED', 'CLOSED') AND now > resolve_deadline`

### 3. RabbitMQ 延迟消息

后续需要实现：

- SLA 延迟消息发送器
- SLA 延迟消息消费者
- 首响即将超时提醒
- 首响已超时检查
- 解决即将超时提醒
- 解决已超时检查

### 4. SLA 通知与日志

后续可以补充：

- 通知管理员/主管
- 通知负责客服
- 写入工单操作日志
- 写入通知表
- 前端展示 SLA 风险标记

### 5. 自动派单与 SLA 联动

后续自动派单完成后，可以让流程变成：

```text
审核通过 -> 自动匹配技能组 -> 自动派单 -> 自动应用 SLA -> 发送 MQ 延迟检查消息
```

这会让工单系统更接近真实企业项目。
