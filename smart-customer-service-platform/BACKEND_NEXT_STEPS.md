# 后端后续开发路线

当前已完成：

- 注册
- 登录
- refreshToken 刷新 accessToken
- logout 退出登录
- JWT Filter 鉴权
- Redis 保存 refreshToken 与权限缓存
- 派单规则基础 CRUD

接下来按下面顺序推进。

## 2. users/me + menus/me

目标：完成“当前登录用户信息”和“当前登录用户菜单权限”接口。

接口：

- `GET /api/v1/users/me`
- `GET /api/v1/menus/me`

主要能力：

- 从 Spring Security 中获取当前登录用户。
- 返回当前用户基础资料、角色编码、权限编码。
- 根据当前用户权限，筛选出 `resource_type = MENU` 的权限。
- 组织菜单树，供前端动态展示侧边栏。

完成后效果：

- 前端可以登录后获取当前用户信息。
- 前端可以根据后端返回的菜单权限渲染页面入口。
- 后续所有业务接口都可以使用当前用户 ID 写入 `created_by`、`operator_id` 等字段。

## 3. 客户管理 CRUD

目标：完成客户基础资料管理。

接口：

- `POST /api/v1/customers`
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}`

主要能力：

- 创建客户。
- 分页查询客户。
- 查看客户详情。
- 修改客户资料。
- 逻辑删除客户。

注意点：

- 手机号、邮箱、客户编号要考虑唯一性。
- 删除建议使用逻辑删除。
- 创建、修改、删除建议写入 `audit_log`。

## 4. 会话中心

目标：完成客服会话的基础链路。

接口：

- `POST /api/v1/conversations`
- `GET /api/v1/conversations/{id}`
- `GET /api/v1/conversations/{id}/messages`
- `POST /api/v1/conversations/{id}/messages`

主要能力：

- 创建会话。
- 保存用户、AI、人工客服消息。
- 查询会话详情。
- 查询会话消息历史。
- 维护会话状态：`ACTIVE`、`TAKEN_OVER`、`CLOSED`。

注意点：

- 活跃会话上下文可以缓存到 Redis。
- 后续 AI 问答和人工接管都依赖会话中心。

## 5. AI 工单草稿

目标：把用户自然语言问题转成结构化工单草稿。

接口：

- `POST /api/v1/ai/chat`
- `POST /api/v1/ai/ticket-draft`

主要能力：

- AI 会话问答。
- 意图识别。
- 优先级建议。
- 知识库检索。
- 生成工单草稿。
- 记录 AI 工具调用日志。

注意点：

- AI 生成结果只能作为草稿，不建议直接创建正式工单。
- 低置信度时应提示转人工。
- 需要记录 `ai_tool_call_log`，方便面试时讲审计链路。

## 6. 工单创建 / 查询

目标：完成工单主流程的第一阶段。

接口：

- `POST /api/v1/tickets`
- `GET /api/v1/tickets`
- `GET /api/v1/tickets/{id}`

主要能力：

- 从 AI 草稿或人工输入创建工单。
- 分页查询工单。
- 查看工单详情。
- 写入工单事件 `ticket_event`。
- 写入审计日志 `audit_log`。

注意点：

- 创建工单建议支持 `Idempotency-Key` 防重复提交。
- 工单状态先从 `NEW` 开始。
- 工单详情最好包含客户、会话、事件记录等信息。

## 7. 派单规则真正参与派单

目标：让已完成的派单规则 CRUD 真正参与工单派单。

接口：

- `POST /api/v1/tickets/{id}/assign`

主要能力：

- 根据工单分类、优先级匹配派单规则。
- 按规则权重选择技能组。
- 按技能组成员负载选择处理人。
- 更新工单 `assignee_id`、`skill_group_id`、`status`。
- 写入 `ticket_event`。

注意点：

- 派单接口建议支持幂等。
- 自动派单失败时允许人工派单兜底。
- 规则权重越高，优先级越高。

## 8. SLA / MQ / 通知

目标：体现真实工单系统的中间件能力。

主要能力：

- RabbitMQ 解耦工单创建、派单、SLA 提醒和通知发送。
- SLA 策略按分类、优先级匹配。
- 创建工单或派单后生成响应/解决截止时间。
- 到期前提醒。
- 超时升级。
- 写入站内通知 `notification`。

建议消息：

- `ticket.create`
- `ticket.assign`
- `ticket.sla.delay`
- `ticket.escalate`
- `notification.send`

注意点：

- MQ 消费要考虑重复消费。
- SLA 提醒要考虑防重复提醒。
- 通知表可作为后续前端消息中心的数据来源。

## 推荐节奏

建议每个阶段都按同一套顺序开发：

1. 明确接口请求和响应 DTO / VO。
2. 写 Entity 和 Mapper。
3. 写 Service 接口。
4. 写 Service 实现。
5. 写 Controller。
6. 加权限注解。
7. 写基础测试。
8. 前后端联调。

当前下一步优先做：`users/me + menus/me`。
