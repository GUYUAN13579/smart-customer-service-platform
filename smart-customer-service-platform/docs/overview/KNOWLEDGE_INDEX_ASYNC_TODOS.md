# 知识索引异步化说明

## 当前完成情况

已完成：

- `knowledge.index.exchange`、正常消费队列和延迟队列。
- 创建任务、手动重试后的事务提交再投递。
- 消费者幂等领取与任务执行。
- Embedding / ES 等系统异常后的自动退避重试。

当前自动重试间隔为 1 分钟、2 分钟、4 分钟，最多由 `maxRetryCount` 控制；达到上限后任务保留为 `FAILED`，由管理员手动处理。

仍待增强：知识索引消息的发布确认失败回写，以及独立失败队列的运营查询。

## 目标

将当前由管理员手动调用的索引执行接口：

```text
POST /api/v1/knowledge/index-tasks/{id}/execute
```

改造为“创建或重试任务后自动异步执行”。原接口保留，用于管理员排障和手工补偿。

最终链路：

```text
创建 / 重试知识索引任务
-> MySQL 事务提交
-> 生产者发布 taskId 到 RabbitMQ
-> 消费者领取 PENDING 任务
-> 复用 executeIndexTask(taskId)
-> 百炼 Embedding
-> Elasticsearch 写入
-> SUCCESS 或 FAILED
```

## 已实现的源码位置

`KnowledgeIndexTaskServiceImpl` 负责以下动作：

1. `createPendingIndexTask`：新建任务成功后，事务提交完成时发布消息。
2. `retryIndexTask`：失败任务恢复为 `PENDING` 后，事务提交完成时发布消息。
3. `markIndexTaskFailed`：系统错误时触发自动退避重试。

## 需要新增的 MQ 基础设施

建议沿用项目现有 `SlaAlertMqConfig` 的 Direct Exchange 风格，新增以下资源：

```text
Exchange: knowledge.index.exchange
Queue:    knowledge.index.queue
Key:      knowledge.index.execute
```

建议新增或补充：

```text
MqConstants
KnowledgeIndexMqConfig
mq.message.KnowledgeIndexTaskMessage
mq.producer.KnowledgeIndexTaskMessageProducer
mq.consumer.KnowledgeIndexTaskMessageConsumer
```

消息体只需要包含 `taskId`。不要把切片正文、向量或完整任务对象塞进消息中，因为它们可能在消息等待期间过期或变更。

## 生产者 TODO

生产者应接收 `taskId`，以 JSON 消息发送到 `knowledge.index.exchange`。

关键要求：

- 使用 RabbitMQ 发布确认和返回回调，确认交换机和路由键配置正确。
- 在 `TransactionSynchronizationManager.registerSynchronization(... afterCommit())` 中调用生产者。
- 事务回滚时不能发送消息。
- `createPendingIndexTask` 和 `retryIndexTask` 都要使用同一个“提交后发布”辅助方法，避免逻辑重复。

## 消费者 TODO

消费者监听 `knowledge.index.queue`，收到消息后：

1. 校验消息和 `taskId`。
2. 调用 `knowledgeIndexTaskService.executeIndexTask(taskId)`。
3. `executeIndexTask` 已通过 `PENDING -> PROCESSING` 条件更新抢占任务，因此重复消息或多实例竞争时，后到消费者会安全失败，不会重复索引。
4. 成功时正常确认消息。
5. 业务异常时不要吞掉异常；先让消息进入项目定义的重试或死信策略。

## 失败重试建议

第一版可以采用：

```text
消费者异常
-> 任务与 PROCESSING 切片标记 FAILED
-> 消息进入失败队列
-> 管理员调用 retry 接口
-> retry 接口将状态恢复为 PENDING
-> afterCommit 再次发布消息
```

这样与你当前已有的 `retryIndexTask` 语义最一致，也便于排查。

后续再增强为自动重试时，必须满足：

- 每次重试递增 `retryCount`。
- 不超过 `maxRetryCount`。
- 使用延迟队列或死信队列控制退避时间。
- 达到上限后保留 `FAILED` 和完整错误信息，供管理员在知识库页面处理。

## 联调验收

完成后用一个新文档验证：

1. 上传、导入、解析、重建切片。
2. 创建索引任务后不再手动调用 `/execute`。
3. 观察任务从 `PENDING` 自动变为 `PROCESSING`，最终变为 `SUCCESS`。
4. 检查 `knowledge_chunk.index_status = INDEXED` 且 `es_document_id` 不为空。
5. 调用 `/api/v1/knowledge/search`，确认能命中新文档。
6. 故意停止 ES 或使用错误 Embedding 配置，确认任务进入 `FAILED`，并能通过 retry 重新成功。
