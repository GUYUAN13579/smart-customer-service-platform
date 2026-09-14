package com.example.smartcustomerservice.service.knowledge;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeIndexTaskQueryRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeIndexTaskVO;

public interface KnowledgeIndexTaskService {

    KnowledgeIndexTaskVO createArticleIndexTask(Long articleId);

    KnowledgeIndexTaskVO createDocumentIndexTask(Long documentId);

    PageResult<KnowledgeIndexTaskVO> pageIndexTasks(KnowledgeIndexTaskQueryRequest request);

    KnowledgeIndexTaskVO getIndexTask(Long id);

    KnowledgeIndexTaskVO retryIndexTask(Long id);

    KnowledgeIndexTaskVO executeIndexTask(Long id);
}
