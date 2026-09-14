# 智能客服与工单自动化平台实施规划

## 1. 项目定位

本项目面向咨询、报障、售后和运维支持场景。用户从 Web、App、微信、电话等渠道进入会话后，系统通过 AI 识别意图、检索知识库、生成工单草稿，并通过规则或人工方式完成建单、派单、处理、SLA 监控、关闭、质检和统计。

简历表达重点：这不是普通 CRUD 项目，而是一个包含 AI Agent、缓存、消息队列、搜索引擎、状态机、幂等、审计和运营看板的业务闭环系统。

## 2. 推荐开发顺序

### 第一阶段：工程骨架与基础能力

目标：让项目具备可启动、可调试、可扩展的基础。

任务：
- 新建 Spring Boot 项目，确定 Controller、Service、Mapper 三层结构。
- 接入 MyBatis-Plus、MySQL、Redis、RabbitMQ、Elasticsearch、Swagger、Actuator。
- 定义统一返回体、分页对象、全局异常处理、业务错误码。
- 设计基础枚举：工单状态、优先级、渠道类型、消息发送方、知识状态、操作人类型。
- 初始化数据库建表脚本，优先建核心表。

验收：
- 项目可以启动。
- Swagger 页面可以打开。
- 数据库连接、Redis 连接、健康检查可以验证。

### 第二阶段：认证、用户与权限

目标：补齐后台管理和客服工作台的基础身份能力。

任务：
- 实现登录、刷新令牌、退出登录。
- 实现当前用户信息、当前用户菜单权限。
- 建立用户、角色、权限、用户角色关联表。
- 接入 Spring Security + JWT。
- 写入 audit_log，记录登录、登出、关键写操作。

验收：
- 用户可以登录并拿到 Token。
- 受保护接口必须携带 Token。
- 不同角色可看到不同菜单或权限点。

### 第三阶段：客户、渠道与会话中心

目标：完成客服系统的入口链路。

任务：
- 实现客户创建、客户分页查询、客户详情。
- 实现渠道账号绑定。
- 实现会话创建、会话消息写入、会话详情查询。
- 用 Redis 缓存活跃会话上下文，Key 建议为 `cs:session:{sessionId}`，TTL 2 小时。
- 预留人工接管字段：`ai_enabled`、`current_agent_id`、`status`。

验收：
- 可以创建客户和会话。
- 可以保存用户、AI、人工客服消息。
- 会话上下文能从 Redis 读取。

### 第四阶段：AI 问答与工单草稿

目标：做出最有简历辨识度的 AI 链路。

任务：
- 接入 Spring AI。
- 定义 AI 工具：`classifyTicket`、`suggestPriority`、`searchKnowledgeBase`、`createDraft`。
- 实现 `/api/v1/ai/chat`，返回回答、意图、置信度、工具调用列表。
- 实现 `/api/v1/ai/ticket-draft`，把自然语言转成结构化工单草稿。
- 所有 AI 工具调用写入 `ai_tool_call_log`。
- 对低置信度结果设计兜底策略：转人工、只返回建议、不自动建单。

验收：
- 输入“验证码收不到”可生成分类、优先级和工单草稿。
- AI 调用过程可追踪。
- AI 结果不会直接越权修改业务数据。

### 第五阶段：知识库与 Elasticsearch 检索

目标：让 AI 回答有来源、有依据。

任务：
- 实现知识分类、知识文章、知识片段表。
- 支持文章草稿、发布、下线。
- 发布后同步到 Elasticsearch。
- 实现 `/api/v1/knowledge/search`。
- Redis 缓存热点知识，Key 建议为 `cs:faq:hot:{category}`，TTL 30 分钟。
- 检索结果返回标题、片段、分数和来源。

验收：
- 可以维护 FAQ/知识文章。
- 用户问题能检索出相关知识。
- AI 回答可以引用知识来源。

### 第六阶段：工单生命周期

目标：完成核心业务状态机。

任务：
- 实现创建工单、查询详情、分页查询。
- 实现状态流转：NEW、ASSIGNED、PROCESSING、HOLD、CLOSED、CANCELLED。
- 写操作支持 `Idempotency-Key`，创建工单幂等 Key 建议为 `cs:ticket:idempotent:{key}`，TTL 10 分钟。
- 工单状态变化写入 `ticket_event`。
- 关键操作写入 `audit_log`。

验收：
- 可以从 AI 草稿正式创建工单。
- 工单每次状态变化都有事件记录。
- 重复提交不会创建重复工单。

### 第七阶段：自动派单、SLA 与通知

目标：体现中间件和复杂业务规则能力。

任务：
- 设计技能组、技能组成员、派单规则。
- 实现自动派单：按分类、优先级、技能组、当前负载选择处理人。
- RabbitMQ 解耦工单创建、派单、SLA 提醒、通知发送。
- SLA 策略按分类和优先级配置首次响应时限、解决时限。
- 使用延迟消息或定时任务生成 `sla_alert`。
- 发送站内通知，写入 `notification`。

验收：
- 新工单可自动分配给合适技能组或处理人。
- 即将超时和已超时工单能生成提醒。
- 消息重复消费不影响最终结果。

### 第八阶段：人工接管、质检与运营看板

目标：补全真实客服系统的管理能力。

任务：
- 实现人工接管和释放接管。
- 结束工单后按规则生成质检任务。
- 可加入 AI 辅助质检：敏感词、满意度、处理时长、回复质量。
- 实现运营看板：工单数量、关闭数量、平均首次响应时间、AI 解决率、满意度。
- 对统计接口做必要索引和聚合优化。

验收：
- AI 低置信度或用户要求人工时可以切换。
- 已关闭工单可进入质检池。
- 看板能展示关键运营指标。

### 第九阶段：测试、压测与简历包装

目标：把项目变成能讲、能演示、能经得住追问的作品。

任务：
- 为核心 Service 写单元测试。
- 为建单、派单、SLA 写集成测试。
- 准备 Postman 或 Swagger 演示流程。
- 补充 README：业务背景、架构图、核心流程、技术难点、启动方式。
- 使用 JMeter 或 Gatling 做简单压测。
- 准备面试话术。

验收：
- 面试时能 5 分钟讲清楚架构。
- 能现场演示一条完整链路。
- 能回答幂等、状态机、MQ 重试、AI 兜底、ES 检索、Redis 缓存一致性等问题。

## 3. 建议优先实现的接口

第一批最小闭环：
- `POST /api/v1/auth/login`
- `POST /api/v1/conversations`
- `POST /api/v1/ai/chat`
- `POST /api/v1/ai/ticket-draft`
- `POST /api/v1/tickets`
- `POST /api/v1/tickets/{id}/assign`
- `GET /api/v1/tickets/{id}`
- `POST /api/v1/knowledge/search`
- `GET /api/v1/statistics/dashboard`

第二批增强能力：
- 客户分页查询
- 会话消息历史
- 工单分页查询
- 工单转派、挂起、关闭
- 知识文章发布和同步 ES
- SLA 策略配置
- 通知列表
- 质检任务列表

## 4. 核心数据表优先级

第一批：
- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `customer`
- `conversation_session`
- `conversation_message`
- `ticket`
- `ticket_event`
- `audit_log`

第二批：
- `ai_tool_call_log`
- `knowledge_category`
- `knowledge_article`
- `knowledge_chunk`
- `skill_group`
- `skill_group_member`
- `assignment_rule`
- `sla_policy`
- `sla_alert`
- `notification`
- `quality_task`
- `file_resource`

## 5. 面试可讲技术点

- Redis 会话缓存：减少数据库压力，保证多轮会话上下文连续。
- Redis 幂等锁：避免重复点击或重试导致重复建单、重复派单。
- RabbitMQ 异步解耦：建单后派单、SLA、通知异步处理，降低主链路耗时。
- 工单状态机：用明确状态和事件表保证流转可控、可追踪。
- Elasticsearch 知识检索：支持关键词、标签、权重和高亮，提高知识命中率。
- Spring AI Tool Calling：让 AI 只通过白名单工具执行业务动作，方便审计和兜底。
- 审计日志：记录关键写操作，便于排查问题和满足管理要求。
- SLA 延迟提醒：体现真实业务规则，而不是简单增删改查。
