package com.example.smartcustomerservice.service.knowledge;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleQueryRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleUpdateRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeArticleVO;

public interface KnowledgeArticleService {

    KnowledgeArticleVO createArticle(KnowledgeArticleCreateRequest request);

    KnowledgeArticleVO updateArticle(Long id, KnowledgeArticleUpdateRequest request);

    Boolean deleteArticle(Long id);

    KnowledgeArticleVO getArticle(Long id);

    PageResult<KnowledgeArticleVO> pageArticles(KnowledgeArticleQueryRequest request);

    KnowledgeArticleVO publishArticle(Long id);

    KnowledgeArticleVO offlineArticle(Long id);
}
