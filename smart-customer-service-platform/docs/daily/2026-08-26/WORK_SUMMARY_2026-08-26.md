# 2026-08-26 工作总结

## 今日目标

继续完善智能客服与工单自动化平台后端，将工单从“审核、派单、我的工单”推进到“处理、记录、解决、关闭”，让工单主流程更加完整。

## 今日完成内容

### 1. 明确下一阶段后端接口

在阅读 `WORK_SUMMARY_2026-08-23.md` 和项目现有代码后，确认当前系统已经完成：

```text
用户会话
-> AI 自动回复 / AI 辅助回答
-> AI 工单草稿
-> 创建待审核工单
-> 审核
-> 待派单
-> 派单
-> 我的工单
```

今日继续补齐后续链路：

```text
我的工单
-> 开始处理
-> 添加处理记录
-> 查询处理记录
-> 解决工单
-> 关闭工单
```

### 2. 新增工单处理记录表

新增 SQL 文件：

- `docs/sql/ticket_process_flow_patch.sql`

新增表：

- `ticket_process_record`

该表用于记录客服处理工单过程中的业务动作，例如：

- 开始处理
- 添加备注
- 联系客户
- 等待处理
- 解决工单
- 关闭工单

核心字段：

- `ticket_id`：关联工单 ID。
- `operator_id`：操作人用户 ID。
- `record_type`：处理记录类型。
- `content`：处理内容。
- `visible_to_customer`：是否对客户可见。
- `created_at`：创建时间。
- `deleted`：逻辑删除标记。

同时在 SQL 中补充了对应接口权限：

- `ticket:start`
- `ticket:record:add`
- `ticket:record:list`
- `ticket:resolve`
- `ticket:close`

### 3. 新增工单处理流转后端结构

新增实体类：

- `TicketProcessRecord`

新增 Mapper：

- `TicketProcessRecordMapper`

新增请求 DTO：

- `TicketStartRequest`
- `TicketProcessRecordCreateRequest`
- `TicketProcessRecordQueryRequest`
- `TicketResolveRequest`
- `TicketCloseRequest`

新增响应 VO：

- `TicketProcessRecordVO`

新增接口：

```text
PUT  /api/v1/tickets/{id}/start
POST /api/v1/tickets/{id}/records
GET  /api/v1/tickets/{id}/records
PUT  /api/v1/tickets/{id}/resolve
PUT  /api/v1/tickets/{id}/close
```

对应接入位置：

- `TicketController`
- `TicketService`
- `TicketServiceImpl`

### 4. 完成 startTicket 方法

完成接口：

```text
PUT /api/v1/tickets/{id}/start
```

主要逻辑：

- 查询工单。
- 校验工单存在且未删除。
- 获取当前登录用户 ID。
- 校验当前用户必须是该工单处理人。
- 只允许 `ASSIGNED -> PROCESSING`。
- 使用 `LambdaUpdateWrapper` 加状态条件更新，避免并发重复开始处理。
- 插入 `START` 类型处理记录。
- 返回最新 `TicketVO`。

本次修正点：

- `Long` 比较改为 `Objects.equals()`。
- `deleted` 判断改为空指针安全写法。
- 更新时增加 `assigneeId`、`status`、`deleted` 条件。
- 判断更新影响行数。
- 处理记录内容优先使用 `request.remark`，否则默认“客服开始处理工单”。
- `visibleToCustomer` 默认设置为 `0`。

### 5. 完成 addTicketProcessRecord 方法

完成接口：

```text
POST /api/v1/tickets/{id}/records
```

主要逻辑：

- 查询工单。
- 校验工单存在且未删除。
- 获取当前登录用户 ID。
- `ADMIN` / `SUPERVISOR` 可以添加处理记录。
- 普通客服只能给自己负责的工单添加处理记录。
- 只允许 `ASSIGNED` / `PROCESSING` 状态添加处理记录。
- 插入 `ticket_process_record`。
- 更新工单 `updated_at`。
- 返回 `TicketProcessRecordVO`。

本次修正点：

- 补充 `request == null` 校验。
- 补充 `operatorId`。
- 补充 `createdAt`。
- 不再用 `roleId >= 3` 判断权限。
- 改用当前登录用户上下文中的 `roleCodes` 判断是否为管理员或主管。
- `visibleToCustomer` 加入 `0 / 1` 参数校验。

### 6. 完成 pageTicketProcessRecords 方法

完成接口：

```text
GET /api/v1/tickets/{id}/records
```

主要逻辑：

- 查询工单。
- 校验工单存在且未删除。
- 分页查询 `ticket_process_record`。
- 固定筛选 `ticket_id` 和 `deleted = 0`。
- 支持按 `recordType` 筛选。
- 支持按 `visibleToCustomer` 筛选。
- 按 `createdAt` 和 `id` 正序排序，方便前端展示时间线。
- 返回 `PageResult<TicketProcessRecordVO>`。

### 7. 完成 resolveTicket 方法

完成接口：

```text
PUT /api/v1/tickets/{id}/resolve
```

主要逻辑：

- 查询工单。
- 校验工单存在且未删除。
- 校验当前用户必须是该工单处理人。
- 只允许 `PROCESSING -> RESOLVED`。
- 使用 `LambdaUpdateWrapper` 加状态、处理人、删除标记条件更新。
- 插入 `RESOLVE` 类型处理记录。
- 返回最新 `TicketVO`。

本次修正点：

- 补充 `request == null` 校验。
- `Long` 比较改为 `Objects.equals()`。
- 更新时补充 `assigneeId` 和 `deleted` 条件。
- 判断更新影响行数必须为 `1`。
- 插入处理记录时补充 `createdAt`。
- 判断处理记录插入结果。
- `visibleToCustomer` 默认保持为 `1`。
- `TicketResolveRequest.visibleToCustomer` 加入 `0 / 1` 参数校验。

### 8. 完成 closeTicket 方法

完成接口：

```text
PUT /api/v1/tickets/{id}/close
```

主要逻辑：

- 查询工单。
- 校验工单存在且未删除。
- 普通客服只能关闭自己负责且 `RESOLVED` 状态的工单。
- `ADMIN` / `SUPERVISOR` 可以关闭 `ASSIGNED` / `PROCESSING` / `RESOLVED` 状态工单。
- 更新工单状态为 `CLOSED`。
- 设置 `closedAt` 和 `updatedAt`。
- 插入 `CLOSE` 类型处理记录。
- 同步关闭关联会话。
- 插入会话系统消息。
- 返回最新 `TicketVO`。

### 9. 新增工单操作日志表与查询接口

新增 SQL 文件：

- `docs/sql/ticket_operation_log_patch.sql`

新增表：

- `ticket_operation_log`

该表用于记录工单状态流转和关键系统动作，偏审计用途。

它和 `ticket_process_record` 的区别：

- `ticket_process_record`：记录客服真实处理过程，例如“已联系客户”“等待财务确认”。
- `ticket_operation_log`：记录系统状态变化，例如“工单从 ASSIGNED 变为 PROCESSING”。

新增实体类：

- `TicketOperationLog`

新增 Mapper：

- `TicketOperationLogMapper`

新增请求 DTO：

- `TicketOperationLogQueryRequest`

新增响应 VO：

- `TicketOperationLogVO`

新增查询接口：

```text
GET /api/v1/tickets/{id}/operation-logs
```

主要逻辑：

- 查询工单。
- 校验工单存在且未删除。
- 分页查询 `ticket_operation_log`。
- 支持按 `operationType` 筛选。
- 按 `createdAt` 和 `id` 正序排序。
- 返回 `PageResult<TicketOperationLogVO>`。

新增权限：

- `ticket:operation-log:list`

### 10. 在现有工单动作中预留操作日志插入位置

按照要求，没有直接把所有操作日志插入业务写满，而是在关键业务成功位置留下了注释：

```java
// TODO 插入工单操作日志：...
```

已预留的位置包括：

- `manualTransfer`
- `reviewTicket`
- `assignTicket`
- `startTicket`
- `addTicketProcessRecord`
- `resolveTicket`
- `closeTicket`

后续可以在这些位置逐步调用统一的操作日志写入方法。

## 今日新增或修改的主要文件

### SQL

- `docs/sql/ticket_process_flow_patch.sql`
- `docs/sql/ticket_operation_log_patch.sql`

### Entity

- `TicketProcessRecord`
- `TicketOperationLog`

### Mapper

- `TicketProcessRecordMapper`
- `TicketOperationLogMapper`

### DTO

- `TicketStartRequest`
- `TicketProcessRecordCreateRequest`
- `TicketProcessRecordQueryRequest`
- `TicketResolveRequest`
- `TicketCloseRequest`
- `TicketOperationLogQueryRequest`

### VO

- `TicketProcessRecordVO`
- `TicketOperationLogVO`

### Controller / Service

- `TicketController`
- `TicketService`
- `TicketServiceImpl`

## 当前工单主流程状态

目前工单主流程已经基本闭环：

```text
AI 工单草稿
-> 创建待审核工单
-> 审核通过
-> 待派单
-> 派单
-> 我的工单
-> 开始处理
-> 添加处理记录
-> 查询处理记录
-> 解决工单
-> 关闭工单
```

当前已经具备比较完整的工单系统核心业务骨架。

## 后续要做的事情

### 1. 实现工单操作日志写入

虽然今天已经新增了 `ticket_operation_log` 表和查询接口，但实际插入逻辑还没有写入。

下一步建议新增一个私有方法：

```java
private void insertTicketOperationLog(
        Long ticketId,
        Long operatorId,
        String operationType,
        String fromStatus,
        String toStatus,
        String operationContent,
        LocalDateTime createdAt
)
```

然后在已经预留的 TODO 位置逐步调用。

### 2. 前端工单中心接入处理流转接口

后端处理流转接口完成后，前端工单中心可以继续补：

- 开始处理按钮
- 添加处理记录弹窗
- 处理记录时间线
- 操作日志时间线
- 解决工单按钮
- 关闭工单按钮

### 3. 派单规则真正参与派单

当前派单仍然是手动指定客服。

后续可以实现：

- 根据工单分类匹配技能组。
- 根据技能组筛选客服。
- 根据客服当前工单数量做简单负载均衡。
- 自动派单接口。
- 自动派单失败后进入人工派单。

### 4. SLA 模块

工单主流程闭环后，建议进入 SLA。

可实现内容：

- 创建或派单时计算首次响应截止时间。
- 创建或派单时计算解决截止时间。
- 客服首次处理或首次回复时记录 `firstRespondedAt`。
- 查询即将超时工单。
- 查询已超时工单。
- SLA 超时标记。

### 5. 通知模块

后续可以新增站内通知：

- 工单派单通知。
- 工单即将超时通知。
- 工单超时通知。
- 工单关闭通知。
- 当前用户未读通知列表。
- 标记通知已读。

### 6. RabbitMQ 异步处理

RabbitMQ 可以放到 SLA 和通知之后接入。

适合异步化的场景：

- 派单后异步通知客服。
- SLA 延迟检查。
- 通知异步写入。
- AI 调用日志异步落库。

### 7. Dashboard 数据统计

后续可以补充统计接口：

- 今日新增工单数。
- 待审核工单数。
- 待派单工单数。
- 处理中工单数。
- 已关闭工单数。
- 超时工单数。
- 客服处理量排行。
- 工单分类占比。

## 下次建议

下一次建议先做：

```text
工单操作日志写入方法 + 在现有工单状态流转中真正插入日志
```

这一步完成后，工单详情页就可以同时展示：

- 业务处理记录
- 系统操作日志

这样项目的可解释性、审计链路和面试展示价值都会更强。
