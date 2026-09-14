package com.example.smartcustomerservice.service.impl.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleQueryRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeArticleUpdateRequest;
import com.example.smartcustomerservice.domain.entity.KnowledgeArticle;
import com.example.smartcustomerservice.domain.vo.KnowledgeArticleVO;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeArticleMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.knowledge.KnowledgeArticleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final ObjectMapper objectMapper;

    public KnowledgeArticleServiceImpl(KnowledgeArticleMapper knowledgeArticleMapper, ObjectMapper objectMapper) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO createArticle(KnowledgeArticleCreateRequest request) {
        Long currentUserId = requireCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.getTitle().trim());
        article.setSummary(blankToNull(request.getSummary()));
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setTags(writeTags(request.getTags()));
        article.setSourceType("MANUAL");
        article.setStatus("DRAFT");
        article.setVersion(1);
        article.setCreatedBy(currentUserId);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article.setDeleted(0);
        if (knowledgeArticleMapper.insert(article) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "新增知识文章失败");
        }
        return toVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO updateArticle(Long id, KnowledgeArticleUpdateRequest request) {
        KnowledgeArticle article = getEntity(id);
        article.setTitle(request.getTitle().trim());
        article.setSummary(blankToNull(request.getSummary()));
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setTags(writeTags(request.getTags()));
        article.setUpdatedAt(LocalDateTime.now());
        if (knowledgeArticleMapper.updateById(article) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "知识文章已变化，请刷新后重试");
        }
        return toVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteArticle(Long id) {
        getEntity(id);
        return knowledgeArticleMapper.update(null, new LambdaUpdateWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getId, id)
                .eq(KnowledgeArticle::getDeleted, 0)
                .set(KnowledgeArticle::getDeleted, 1)
                .set(KnowledgeArticle::getStatus, "OFFLINE")
                .set(KnowledgeArticle::getUpdatedAt, LocalDateTime.now())) == 1;
    }

    @Override
    public KnowledgeArticleVO getArticle(Long id) {
        return toVO(getEntity(id));
    }

    @Override
    public PageResult<KnowledgeArticleVO> pageArticles(KnowledgeArticleQueryRequest request) {
        KnowledgeArticleQueryRequest safeRequest = request == null ? new KnowledgeArticleQueryRequest() : request;
        Page<KnowledgeArticle> page = knowledgeArticleMapper.selectPage(
                new Page<>(safeRequest.getPage(), safeRequest.getSize()),
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getDeleted, 0)
                        .and(StringUtils.hasText(safeRequest.getKeyword()), wrapper -> wrapper
                                .like(KnowledgeArticle::getTitle, safeRequest.getKeyword())
                                .or()
                                .like(KnowledgeArticle::getSummary, safeRequest.getKeyword()))
                        .eq(StringUtils.hasText(safeRequest.getCategory()), KnowledgeArticle::getCategory, safeRequest.getCategory())
                        .eq(StringUtils.hasText(safeRequest.getStatus()), KnowledgeArticle::getStatus, safeRequest.getStatus())
                        .orderByDesc(KnowledgeArticle::getUpdatedAt)
                        .orderByDesc(KnowledgeArticle::getId)
        );
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO publishArticle(Long id) {
        KnowledgeArticle article = getEntity(id);
        LocalDateTime now = LocalDateTime.now();
        article.setStatus("PUBLISHED");
        article.setPublishedBy(requireCurrentUserId());
        article.setPublishedAt(now);
        article.setVersion(article.getVersion() == null ? 1 : article.getVersion() + 1);
        article.setUpdatedAt(now);
        if (knowledgeArticleMapper.updateById(article) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "知识文章状态已变化，请刷新后重试");
        }
        return toVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO offlineArticle(Long id) {
        KnowledgeArticle article = getEntity(id);
        article.setStatus("OFFLINE");
        article.setUpdatedAt(LocalDateTime.now());
        if (knowledgeArticleMapper.updateById(article) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "知识文章状态已变化，请刷新后重试");
        }
        return toVO(article);
    }

    private KnowledgeArticle getEntity(Long id) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null || Integer.valueOf(1).equals(article.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识文章不存在");
        }
        return article;
    }

    private KnowledgeArticleVO toVO(KnowledgeArticle article) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCategory(article.getCategory());
        vo.setTags(readTags(article.getTags()));
        vo.setSourceType(article.getSourceType());
        vo.setFileId(article.getFileId());
        vo.setStatus(article.getStatus());
        vo.setVersion(article.getVersion());
        vo.setPublishedBy(article.getPublishedBy());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setCreatedBy(article.getCreatedBy());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文章标签格式不正确");
        }
    }

    private List<String> readTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tags, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Long requireCurrentUserId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前登录用户");
        }
        return currentUserId;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
