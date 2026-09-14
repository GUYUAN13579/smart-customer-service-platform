package com.example.smartcustomerservice.service.impl.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.KnowledgeSearchRequest;
import com.example.smartcustomerservice.domain.entity.KnowledgeArticle;
import com.example.smartcustomerservice.domain.entity.KnowledgeDocument;
import com.example.smartcustomerservice.domain.vo.KnowledgeSearchVO;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeArticleMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeDocumentMapper;
import com.example.smartcustomerservice.service.knowledge.KnowledgeEsIndexService;
import com.example.smartcustomerservice.service.knowledge.KnowledgeSearchService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeEsIndexService knowledgeEsIndexService;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeSearchServiceImpl(EmbeddingModel embeddingModel,
                                      KnowledgeEsIndexService knowledgeEsIndexService,
                                      KnowledgeArticleMapper knowledgeArticleMapper,
                                      KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.embeddingModel = embeddingModel;
        this.knowledgeEsIndexService = knowledgeEsIndexService;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    @Override
    public List<KnowledgeSearchVO> search(KnowledgeSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "检索内容不能为空");
        }

        String query = request.getQuery().trim();
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        String sourceType = StringUtils.hasText(request.getSourceType()) ? request.getSourceType() : null;
        String category = StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : null;

        /*
         * 分类属于文章来源信息而不是当前 ES 顶层字段。带分类检索时多取一些候选，
         * 再在补全来源信息后过滤，尽量避免前几个候选恰好都不属于目标分类导致无结果。
         */
        int candidateSize = category == null ? topK : Math.min(topK * 5, 100);
        float[] queryEmbedding = embeddingModel.embed(query);
        List<KnowledgeSearchVO> candidates = knowledgeEsIndexService.search(
                queryEmbedding, sourceType, candidateSize);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, KnowledgeArticle> articleMap = findPublishedArticles(candidates);
        Map<Long, KnowledgeDocument> documentMap = findParsedDocuments(candidates);
        List<KnowledgeSearchVO> results = new ArrayList<>();
        for (KnowledgeSearchVO candidate : candidates) {
            if ("ARTICLE".equals(candidate.getSourceType())) {
                KnowledgeArticle article = articleMap.get(candidate.getSourceId());
                if (article == null || (category != null && !category.equals(article.getCategory()))) {
                    continue;
                }
                candidate.setSourceTitle(article.getTitle());
                candidate.setCategory(article.getCategory());
            } else if ("DOCUMENT".equals(candidate.getSourceType())) {
                KnowledgeDocument document = documentMap.get(candidate.getSourceId());
                // 知识文件暂未设计分类字段，指定 category 时不将文件切片计入结果。
                if (document == null || category != null) {
                    continue;
                }
                candidate.setSourceTitle(document.getDocumentName());
            } else {
                continue;
            }

            results.add(candidate);
            if (results.size() == topK) {
                break;
            }
        }
        return results;
    }

    private Map<Long, KnowledgeArticle> findPublishedArticles(List<KnowledgeSearchVO> candidates) {
        Set<Long> articleIds = candidates.stream()
                .filter(item -> "ARTICLE".equals(item.getSourceType()))
                .map(KnowledgeSearchVO::getSourceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                        .in(KnowledgeArticle::getId, articleIds)
                        .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                        .eq(KnowledgeArticle::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(KnowledgeArticle::getId, article -> article,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, KnowledgeDocument> findParsedDocuments(List<KnowledgeSearchVO> candidates) {
        Set<Long> documentIds = candidates.stream()
                .filter(item -> "DOCUMENT".equals(item.getSourceType()))
                .map(KnowledgeSearchVO::getSourceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                        .in(KnowledgeDocument::getId, documentIds)
                        .eq(KnowledgeDocument::getStatus, "PARSED")
                        .eq(KnowledgeDocument::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, document -> document,
                        (left, right) -> left, LinkedHashMap::new));
    }
}
