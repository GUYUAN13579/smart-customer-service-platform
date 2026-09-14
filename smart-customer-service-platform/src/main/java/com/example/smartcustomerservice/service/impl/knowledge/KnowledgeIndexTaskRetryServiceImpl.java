package com.example.smartcustomerservice.service.impl.knowledge;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smartcustomerservice.config.properties.KnowledgeIndexProperties;
import com.example.smartcustomerservice.domain.entity.KnowledgeChunk;
import com.example.smartcustomerservice.domain.entity.KnowledgeIndexTask;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeChunkMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeIndexTaskMapper;
import com.example.smartcustomerservice.mq.message.KnowledgeTaskMessage;
import com.example.smartcustomerservice.mq.producer.KnowledgeIndexTaskMessageProducer;
import com.example.smartcustomerservice.service.knowledge.KnowledgeIndexTaskRetryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
public class KnowledgeIndexTaskRetryServiceImpl implements KnowledgeIndexTaskRetryService {

    private final KnowledgeIndexTaskMapper knowledgeIndexTaskMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeIndexTaskMessageProducer knowledgeIndexTaskMessageProducer;
    private final KnowledgeIndexProperties knowledgeIndexProperties;

    public KnowledgeIndexTaskRetryServiceImpl(KnowledgeIndexTaskMapper knowledgeIndexTaskMapper,
                                              KnowledgeChunkMapper knowledgeChunkMapper,
                                              KnowledgeIndexTaskMessageProducer knowledgeIndexTaskMessageProducer,
                                              KnowledgeIndexProperties knowledgeIndexProperties) {
        this.knowledgeIndexTaskMapper = knowledgeIndexTaskMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeIndexTaskMessageProducer = knowledgeIndexTaskMessageProducer;
        this.knowledgeIndexProperties = knowledgeIndexProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scheduleAutomaticRetry(Long taskId) {
        if (!knowledgeIndexProperties.isAutoRetryEnabled() || taskId == null) {
            return false;
        }

        KnowledgeIndexTask task = knowledgeIndexTaskMapper.selectById(taskId);
        if (task == null || !"FAILED".equals(task.getStatus())) {
            return false;
        }

        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetryCount = task.getMaxRetryCount() == null ? 3 : task.getMaxRetryCount();
        if (retryCount >= maxRetryCount) {
            return false;
        }

        int nextRetryCount = retryCount + 1;
        long delayMillis = calculateDelayMillis(nextRetryCount);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = now.plusNanos(delayMillis * 1_000_000L);
        int updated = knowledgeIndexTaskMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexTask>()
                .eq(KnowledgeIndexTask::getId, taskId)
                .eq(KnowledgeIndexTask::getStatus, "FAILED")
                .set(KnowledgeIndexTask::getStatus, "PENDING")
                .set(KnowledgeIndexTask::getRetryCount, nextRetryCount)
                .set(KnowledgeIndexTask::getErrorMessage, "自动重试已调度，预计 " + scheduledAt + " 执行")
                .set(KnowledgeIndexTask::getScheduledAt, scheduledAt)
                .set(KnowledgeIndexTask::getStartedAt, null)
                .set(KnowledgeIndexTask::getFinishedAt, null)
                .set(KnowledgeIndexTask::getUpdatedAt, now));
        if (updated != 1) {
            return false;
        }

        knowledgeChunkMapper.update(null, new LambdaUpdateWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, task.getSourceType())
                .eq(KnowledgeChunk::getSourceId, task.getSourceId())
                .eq(KnowledgeChunk::getDeleted, 0)
                .eq(KnowledgeChunk::getIndexStatus, "FAILED")
                .set(KnowledgeChunk::getIndexStatus, "PENDING")
                .set(KnowledgeChunk::getIndexError, null)
                .set(KnowledgeChunk::getUpdatedAt, now));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                knowledgeIndexTaskMessageProducer.sendDelayedKnowledgeIndexTask(
                        new KnowledgeTaskMessage(taskId), delayMillis);
            }
        });
        return true;
    }

    private long calculateDelayMillis(int retryCount) {
        long initialDelay = Math.max(1L, knowledgeIndexProperties.getAutoRetryInitialDelayMs());
        long maxDelay = Math.max(initialDelay, knowledgeIndexProperties.getAutoRetryMaxDelayMs());
        int exponent = Math.min(Math.max(retryCount - 1, 0), 20);
        long multiplier = 1L << exponent;
        return Math.min(initialDelay * multiplier, maxDelay);
    }
}
