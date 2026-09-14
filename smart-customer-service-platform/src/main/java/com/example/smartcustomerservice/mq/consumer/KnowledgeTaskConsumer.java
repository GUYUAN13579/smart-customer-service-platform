package com.example.smartcustomerservice.mq.consumer;

import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.entity.KnowledgeIndexTask;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeIndexTaskMapper;
import com.example.smartcustomerservice.mq.message.KnowledgeTaskMessage;
import com.example.smartcustomerservice.service.knowledge.KnowledgeIndexTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeTaskConsumer {

    private final KnowledgeIndexTaskMapper knowledgeIndexTaskMapper;
    private final KnowledgeIndexTaskService knowledgeIndexTaskService;

    public KnowledgeTaskConsumer(KnowledgeIndexTaskMapper knowledgeIndexTaskMapper,
                                 KnowledgeIndexTaskService knowledgeIndexTaskService){
        this.knowledgeIndexTaskMapper = knowledgeIndexTaskMapper;
        this.knowledgeIndexTaskService = knowledgeIndexTaskService;
    }


    @RabbitListener(queues = MqConstants.KNOWLEDGE_INDEX_QUEUE)
    public void consumeKnowledgeTask(KnowledgeTaskMessage message) {
        if (message == null || message.getTaskId() == null) {
            return;
        }

        KnowledgeIndexTask task = knowledgeIndexTaskMapper.selectById(message.getTaskId());
        // 已被其他消费者领取、已完成、已失败或已删除的任务都是重复消息，直接确认即可。
        if (task == null || !"PENDING".equals(task.getStatus())) {
            return;
        }

        try {
            knowledgeIndexTaskService.executeIndexTask(message.getTaskId());
        } catch (BusinessException exception) {
            // 查询与领取之间可能被其他消费者抢先处理；状态冲突属于正常幂等结果。
            if (ResultCode.CONFLICT.getCode().equals(exception.getCode())) {
                return;
            }
            // 自动重试已把任务恢复为 PENDING 并投递了延迟消息，确认当前原始消息即可。
            KnowledgeIndexTask latestTask = knowledgeIndexTaskMapper.selectById(message.getTaskId());
            if (latestTask != null && "PENDING".equals(latestTask.getStatus())) {
                return;
            }
            // 其他异常继续抛出，任务已由执行服务标记 FAILED，可经 retry 接口再次投递。
            throw exception;
        }
    }
}
