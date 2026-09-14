package com.example.smartcustomerservice.service.impl.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.KnowledgeIndexTaskQueryRequest;
import com.example.smartcustomerservice.domain.entity.KnowledgeArticle;
import com.example.smartcustomerservice.domain.entity.KnowledgeChunk;
import com.example.smartcustomerservice.domain.entity.KnowledgeDocument;
import com.example.smartcustomerservice.domain.entity.KnowledgeIndexTask;
import com.example.smartcustomerservice.domain.vo.KnowledgeIndexTaskVO;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeArticleMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeChunkMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeDocumentMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeIndexTaskMapper;
import com.example.smartcustomerservice.mq.message.KnowledgeTaskMessage;
import com.example.smartcustomerservice.mq.producer.KnowledgeIndexTaskMessageProducer;
import com.example.smartcustomerservice.service.knowledge.KnowledgeEsIndexService;
import com.example.smartcustomerservice.service.knowledge.KnowledgeIndexTaskService;
import com.example.smartcustomerservice.service.knowledge.KnowledgeIndexTaskRetryService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeIndexTaskServiceImpl implements KnowledgeIndexTaskService {

    private final KnowledgeIndexTaskMapper knowledgeIndexTaskMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final EmbeddingModel embeddingModel;
    private final OpenAiEmbeddingProperties embeddingProperties;
    private final KnowledgeEsIndexService knowledgeEsIndexService;
    private final KnowledgeIndexTaskMessageProducer knowledgeIndexTaskMessageProducer;
    private final KnowledgeIndexTaskRetryService knowledgeIndexTaskRetryService;

    public KnowledgeIndexTaskServiceImpl(KnowledgeIndexTaskMapper knowledgeIndexTaskMapper,
                                         KnowledgeChunkMapper knowledgeChunkMapper,
                                         KnowledgeArticleMapper knowledgeArticleMapper,
                                         KnowledgeDocumentMapper knowledgeDocumentMapper,
                                         EmbeddingModel embeddingModel,
                                         OpenAiEmbeddingProperties embeddingProperties,
                                         KnowledgeEsIndexService knowledgeEsIndexService,
                                         KnowledgeIndexTaskMessageProducer knowledgeIndexTaskMessageProducer,
                                         KnowledgeIndexTaskRetryService knowledgeIndexTaskRetryService) {
        this.knowledgeIndexTaskMapper = knowledgeIndexTaskMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.embeddingModel = embeddingModel;
        this.embeddingProperties = embeddingProperties;
        this.knowledgeEsIndexService = knowledgeEsIndexService;
        this.knowledgeIndexTaskMessageProducer = knowledgeIndexTaskMessageProducer;
        this.knowledgeIndexTaskRetryService = knowledgeIndexTaskRetryService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeIndexTaskVO createArticleIndexTask(Long articleId) {
        // 为已发布文章的待索引切片创建 INDEX 任务。
        KnowledgeArticle article = knowledgeArticleMapper.selectById(articleId);
        if (article == null || Integer.valueOf(1).equals(article.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识文章不存在");
        }
        if (!"PUBLISHED".equals(article.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有已发布文章可以创建索引任务");
        }
        return createPendingIndexTask("ARTICLE", articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeIndexTaskVO createDocumentIndexTask(Long documentId) {
        // 为已解析文件的待索引切片创建 INDEX 任务。
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在");
        }
        if (!"PARSED".equals(document.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有已解析文件可以创建索引任务");
        }
        return createPendingIndexTask("DOCUMENT", documentId);
    }

    @Override
    public PageResult<KnowledgeIndexTaskVO> pageIndexTasks(KnowledgeIndexTaskQueryRequest request) {
        // 按来源、任务类型和状态分页查询索引任务。
        KnowledgeIndexTaskQueryRequest safeRequest = request == null
                ? new KnowledgeIndexTaskQueryRequest()
                : request;
        Page<KnowledgeIndexTask> page = knowledgeIndexTaskMapper.selectPage(
                new Page<>(safeRequest.getPage(), safeRequest.getSize()),
                new LambdaQueryWrapper<KnowledgeIndexTask>()
                        .eq(StringUtils.hasText(safeRequest.getSourceType()),
                                KnowledgeIndexTask::getSourceType, safeRequest.getSourceType())
                        .eq(safeRequest.getSourceId() != null,
                                KnowledgeIndexTask::getSourceId, safeRequest.getSourceId())
                        .eq(StringUtils.hasText(safeRequest.getTaskType()),
                                KnowledgeIndexTask::getTaskType, safeRequest.getTaskType())
                        .eq(StringUtils.hasText(safeRequest.getStatus()),
                                KnowledgeIndexTask::getStatus, safeRequest.getStatus())
                        .orderByDesc(KnowledgeIndexTask::getCreatedAt)
                        .orderByDesc(KnowledgeIndexTask::getId)
        );
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public KnowledgeIndexTaskVO getIndexTask(Long id) {
        // 查询单个索引任务的执行状态和失败原因。
        return toVO(getTask(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeIndexTaskVO retryIndexTask(Long id) {
        // 管理员人工重试会开启新的自动重试周期，不受上一轮自动重试次数限制。
        KnowledgeIndexTask task = getTask(id);
        if (!"FAILED".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有失败的索引任务可以重试");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = knowledgeIndexTaskMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexTask>()
                .eq(KnowledgeIndexTask::getId, id)
                .eq(KnowledgeIndexTask::getStatus, "FAILED")
                .set(KnowledgeIndexTask::getStatus, "PENDING")
                .set(KnowledgeIndexTask::getRetryCount, 0)
                .set(KnowledgeIndexTask::getErrorMessage, null)
                .set(KnowledgeIndexTask::getScheduledAt, now)
                .set(KnowledgeIndexTask::getStartedAt, null)
                .set(KnowledgeIndexTask::getFinishedAt, null)
                .set(KnowledgeIndexTask::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "索引任务状态已变化，请刷新后重试");
        }

        /*
         * 未来索引执行器失败时会把关联切片标记为 FAILED。
         * 重试任务时一并恢复为 PENDING，执行器才能再次领取这些切片。
         */
        knowledgeChunkMapper.update(null, new LambdaUpdateWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, task.getSourceType())
                .eq(KnowledgeChunk::getSourceId, task.getSourceId())
                .eq(KnowledgeChunk::getDeleted, 0)
                .eq(KnowledgeChunk::getIndexStatus, "FAILED")
                .set(KnowledgeChunk::getIndexStatus, "PENDING")
                .set(KnowledgeChunk::getIndexError, null)
                .set(KnowledgeChunk::getUpdatedAt, now));

        publishTaskAfterCommit(task.getId());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setErrorMessage(null);
        task.setScheduledAt(now);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setUpdatedAt(now);
        return toVO(task);

    }

    @Override
    public KnowledgeIndexTaskVO executeIndexTask(Long id) {
        // 消费者与管理员手动补偿接口共用本方法，条件更新保证重复消息不会重复索引。
        KnowledgeIndexTask task = getTask(id);
        LocalDateTime startedAt = LocalDateTime.now();

        /*
         * 使用条件更新领取任务。两个请求同时执行同一任务时，只有一个请求能把
         * PENDING 改为 PROCESSING，另一个请求会得到状态冲突，避免重复调用模型和重复写 ES。
         */
        int claimed = knowledgeIndexTaskMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexTask>()
                .eq(KnowledgeIndexTask::getId, id)
                .eq(KnowledgeIndexTask::getStatus, "PENDING")
                .set(KnowledgeIndexTask::getStatus, "PROCESSING")
                .set(KnowledgeIndexTask::getStartedAt, startedAt)
                .set(KnowledgeIndexTask::getUpdatedAt, startedAt));
        if (claimed != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "索引任务不是待执行状态");
        }

        task.setStatus("PROCESSING");
        task.setStartedAt(startedAt);
        task.setUpdatedAt(startedAt);
        try {
            knowledgeEsIndexService.ensureIndex();
            List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                    .eq(KnowledgeChunk::getSourceType, task.getSourceType())
                    .eq(KnowledgeChunk::getSourceId, task.getSourceId())
                    .eq(KnowledgeChunk::getDeleted, 0)
                    .eq(KnowledgeChunk::getIndexStatus, "PENDING")
                    .orderByAsc(KnowledgeChunk::getChunkNo));
            if (chunks.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "当前索引任务没有待处理切片");
            }

            String embeddingModelName = embeddingProperties.getOptions().getModel();
            for (KnowledgeChunk chunk : chunks) {
                int chunkClaimed = knowledgeChunkMapper.update(null, new LambdaUpdateWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getId, chunk.getId())
                        .eq(KnowledgeChunk::getDeleted, 0)
                        .eq(KnowledgeChunk::getIndexStatus, "PENDING")
                        .set(KnowledgeChunk::getIndexStatus, "PROCESSING")
                        .set(KnowledgeChunk::getUpdatedAt, LocalDateTime.now()));
                if (chunkClaimed != 1) {
                    continue;
                }

                float[] vector = embeddingModel.embed(chunk.getContent());
                if (vector == null || vector.length == 0) {
                    throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Embedding 模型未返回有效向量");
                }

                String esDocumentId = knowledgeEsIndexService.indexChunk(chunk, vector, embeddingModelName);
                LocalDateTime indexedAt = LocalDateTime.now();
                int chunkUpdated = knowledgeChunkMapper.update(null, new LambdaUpdateWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getId, chunk.getId())
                        .eq(KnowledgeChunk::getIndexStatus, "PROCESSING")
                        .set(KnowledgeChunk::getEsDocumentId, esDocumentId)
                        .set(KnowledgeChunk::getEmbeddingModel, embeddingModelName)
                        .set(KnowledgeChunk::getIndexStatus, "INDEXED")
                        .set(KnowledgeChunk::getIndexError, null)
                        .set(KnowledgeChunk::getIndexedAt, indexedAt)
                        .set(KnowledgeChunk::getUpdatedAt, indexedAt));
                if (chunkUpdated != 1) {
                    throw new BusinessException(ResultCode.CONFLICT, "知识切片状态已变化，请重新执行索引任务");
                }
            }

            LocalDateTime finishedAt = LocalDateTime.now();
            knowledgeIndexTaskMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexTask>()
                    .eq(KnowledgeIndexTask::getId, id)
                    .eq(KnowledgeIndexTask::getStatus, "PROCESSING")
                    .set(KnowledgeIndexTask::getStatus, "SUCCESS")
                    .set(KnowledgeIndexTask::getFinishedAt, finishedAt)
                    .set(KnowledgeIndexTask::getErrorMessage, null)
                    .set(KnowledgeIndexTask::getUpdatedAt, finishedAt));
            task.setStatus("SUCCESS");
            task.setFinishedAt(finishedAt);
            task.setErrorMessage(null);
            task.setUpdatedAt(finishedAt);
            return toVO(task);
        } catch (Exception e) {
            boolean autoRetryScheduled = markIndexTaskFailed(task, e);
            if (autoRetryScheduled) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "索引执行失败，系统已安排自动重试");
            }
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "执行知识索引任务失败");
        }
    }

    private KnowledgeIndexTaskVO createPendingIndexTask(String sourceType, Long sourceId) {
        Long pendingChunkCount = knowledgeChunkMapper.selectCount(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, sourceType)
                .eq(KnowledgeChunk::getSourceId, sourceId)
                .eq(KnowledgeChunk::getIndexStatus, "PENDING")
                .eq(KnowledgeChunk::getDeleted, 0));
        if (pendingChunkCount == null || pendingChunkCount == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前来源没有待索引切片");
        }

        Long activeTaskCount = knowledgeIndexTaskMapper.selectCount(new LambdaQueryWrapper<KnowledgeIndexTask>()
                .eq(KnowledgeIndexTask::getSourceType, sourceType)
                .eq(KnowledgeIndexTask::getSourceId, sourceId)
                .eq(KnowledgeIndexTask::getTaskType, "INDEX")
                .in(KnowledgeIndexTask::getStatus, "PENDING", "PROCESSING"));
        if (activeTaskCount != null && activeTaskCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "当前来源已有进行中的索引任务");
        }

        LocalDateTime now = LocalDateTime.now();
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setSourceType(sourceType);
        task.setSourceId(sourceId);
        task.setTaskType("INDEX");
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setScheduledAt(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        if (knowledgeIndexTaskMapper.insert(task) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "创建知识索引任务失败");
        }
        publishTaskAfterCommit(task.getId());
        return toVO(task);
    }

    private KnowledgeIndexTask getTask(Long id) {
        KnowledgeIndexTask task = knowledgeIndexTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识索引任务不存在");
        }
        return task;
    }

    private boolean markIndexTaskFailed(KnowledgeIndexTask task, Exception e) {
        LocalDateTime failedAt = LocalDateTime.now();
        String errorMessage = truncateError(e.getMessage());
        knowledgeChunkMapper.update(null, new LambdaUpdateWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, task.getSourceType())
                .eq(KnowledgeChunk::getSourceId, task.getSourceId())
                .eq(KnowledgeChunk::getDeleted, 0)
                .eq(KnowledgeChunk::getIndexStatus, "PROCESSING")
                .set(KnowledgeChunk::getIndexStatus, "FAILED")
                .set(KnowledgeChunk::getIndexError, errorMessage)
                .set(KnowledgeChunk::getUpdatedAt, failedAt));
        int taskUpdated = knowledgeIndexTaskMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexTask>()
                .eq(KnowledgeIndexTask::getId, task.getId())
                .eq(KnowledgeIndexTask::getStatus, "PROCESSING")
                .set(KnowledgeIndexTask::getStatus, "FAILED")
                .set(KnowledgeIndexTask::getErrorMessage, errorMessage)
                .set(KnowledgeIndexTask::getFinishedAt, failedAt)
                .set(KnowledgeIndexTask::getUpdatedAt, failedAt));
        return taskUpdated == 1 && shouldAutoRetry(e)
                && knowledgeIndexTaskRetryService.scheduleAutomaticRetry(task.getId());
    }

    private boolean shouldAutoRetry(Exception exception) {
        return !(exception instanceof BusinessException businessException)
                || ResultCode.INTERNAL_SERVER_ERROR.getCode().equals(businessException.getCode());
    }

    private void publishTaskAfterCommit(Long taskId) {
        Runnable publishAction = () -> knowledgeIndexTaskMessageProducer
                .sendKnowledgeIndexTask(new KnowledgeTaskMessage(taskId));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishAction.run();
            }
        });
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知索引异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private KnowledgeIndexTaskVO toVO(KnowledgeIndexTask task) {
        KnowledgeIndexTaskVO vo = new KnowledgeIndexTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
