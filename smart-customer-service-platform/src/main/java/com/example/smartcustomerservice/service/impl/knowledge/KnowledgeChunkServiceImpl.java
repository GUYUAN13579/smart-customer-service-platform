package com.example.smartcustomerservice.service.impl.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.KnowledgeChunkCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeChunkQueryRequest;
import com.example.smartcustomerservice.domain.entity.KnowledgeArticle;
import com.example.smartcustomerservice.domain.entity.KnowledgeChunk;
import com.example.smartcustomerservice.domain.entity.KnowledgeDocument;
import com.example.smartcustomerservice.domain.vo.KnowledgeChunkRebuildVO;
import com.example.smartcustomerservice.domain.vo.KnowledgeChunkVO;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeArticleMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeChunkMapper;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeDocumentMapper;
import com.example.smartcustomerservice.service.knowledge.KnowledgeChunkService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final ObjectMapper objectMapper;

    public KnowledgeChunkServiceImpl(KnowledgeChunkMapper knowledgeChunkMapper,
                                     KnowledgeArticleMapper knowledgeArticleMapper,
                                     KnowledgeDocumentMapper knowledgeDocumentMapper,
                                     ObjectMapper objectMapper) {
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeChunkVO createChunk(KnowledgeChunkCreateRequest request) {
        validateChunkSource(request.getSourceType(), request.getSourceId());

        /*
         * 单条新增不由客户端指定 chunkNo，而是追加到该来源现有切片末尾，
         * 保证分页展示和后续拼接上下文时仍能按原文顺序读取。
         */
        KnowledgeChunk lastChunk = knowledgeChunkMapper.selectOne(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, request.getSourceType())
                .eq(KnowledgeChunk::getSourceId, request.getSourceId())
                .eq(KnowledgeChunk::getDeleted, 0)
                .orderByDesc(KnowledgeChunk::getChunkNo)
                .last("LIMIT 1"));
        int nextChunkNo = lastChunk == null ? 0 : lastChunk.getChunkNo() + 1;
        LocalDateTime now = LocalDateTime.now();
        String content = request.getContent().trim();

        KnowledgeChunk knowledgeChunk = new KnowledgeChunk();
        knowledgeChunk.setSourceType(request.getSourceType());
        knowledgeChunk.setSourceId(request.getSourceId());
        knowledgeChunk.setChunkNo(nextChunkNo);
        knowledgeChunk.setContent(content);
        knowledgeChunk.setContentHash(sha256(content));
        knowledgeChunk.setCharCount(content.length());
        knowledgeChunk.setMetadata(normalizeMetadata(request.getMetadata()));
        knowledgeChunk.setIndexStatus("PENDING");
        knowledgeChunk.setCreatedAt(now);
        knowledgeChunk.setUpdatedAt(now);
        knowledgeChunk.setDeleted(0);
        if (knowledgeChunkMapper.insert(knowledgeChunk) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "新增知识切片失败");
        }
        return toChunkVO(knowledgeChunk);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeChunkRebuildVO rebuildArticleChunks(Long articleId) {
        // 将已发布文章重建为待索引的知识切片。
        KnowledgeArticle knowledgeArticle = knowledgeArticleMapper.selectById(articleId);
        if (knowledgeArticle == null || Integer.valueOf(1).equals(knowledgeArticle.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识文章不存在");
        }
        if (!"PUBLISHED".equals(knowledgeArticle.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有已发布文章可以重建切片");
        }
        if (!StringUtils.hasText(knowledgeArticle.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文章正文为空，无法生成切片");
        }

        String content = knowledgeArticle.getContent();
        Document document = new Document(content);
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();
        List<Document> chunks = splitter.apply(List.of(document));
        String metadata = writeArticleMetadata(knowledgeArticle);
        LocalDateTime now = LocalDateTime.now();

        /*
         * 表中的唯一键是 (source_type, source_id, chunk_no)。重建时会重新从 0 编号，
         * 因此不能仅逻辑删除旧记录，否则新切片会与旧序号冲突。当前尚未接入 ES 索引，
         * 可以直接物理删除；后续接入 ES 后，需要先为旧切片创建 ES 删除任务。
         */
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, "ARTICLE")
                .eq(KnowledgeChunk::getSourceId, articleId));

        int chunkNo = 0;
        for (Document chunk : chunks) {
            String chunkContent = chunk.getText();
            if (!StringUtils.hasText(chunkContent)) {
                continue;
            }

            KnowledgeChunk knowledgeChunk = new KnowledgeChunk();
            knowledgeChunk.setSourceType("ARTICLE");
            knowledgeChunk.setSourceId(knowledgeArticle.getId());
            knowledgeChunk.setChunkNo(chunkNo++);
            knowledgeChunk.setContent(chunkContent);
            knowledgeChunk.setContentHash(sha256(chunkContent));
            knowledgeChunk.setCharCount(chunkContent.length());
            knowledgeChunk.setMetadata(metadata);
            knowledgeChunk.setIndexStatus("PENDING");
            knowledgeChunk.setCreatedAt(now);
            knowledgeChunk.setUpdatedAt(now);
            knowledgeChunk.setDeleted(0);
            if (knowledgeChunkMapper.insert(knowledgeChunk) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "新增知识切片失败");
            }
        }

        if (chunkNo == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文章正文无法生成有效切片");
        }

        KnowledgeChunkRebuildVO result = new KnowledgeChunkRebuildVO();
        result.setSourceType("ARTICLE");
        result.setSourceId(articleId);
        result.setChunkCount(chunkNo);
        result.setRebuiltAt(now);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeChunkRebuildVO rebuildDocumentChunks(Long documentId) {
        // 将已解析文件重建为待索引的知识切片。
        KnowledgeDocument knowledgeDocument = knowledgeDocumentMapper.selectById(documentId);
        if (knowledgeDocument == null || Integer.valueOf(1).equals(knowledgeDocument.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在");
        }
        if (!"PARSED".equals(knowledgeDocument.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有已解析文件可以重建切片");
        }
        if (!StringUtils.hasText(knowledgeDocument.getExtractedContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件解析内容为空，无法生成切片");
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();
        List<Document> chunks = splitter.apply(List.of(new Document(knowledgeDocument.getExtractedContent())));
        String metadata = writeDocumentMetadata(knowledgeDocument);
        LocalDateTime now = LocalDateTime.now();

        /*
         * 文件切片也从 chunkNo = 0 重新编号，因此与文章重建保持相同策略：
         * 先物理删除原 DOCUMENT 切片，再插入新记录，避免唯一键冲突和旧内容残留。
         */
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, "DOCUMENT")
                .eq(KnowledgeChunk::getSourceId, documentId));

        int chunkNo = 0;
        for (Document chunk : chunks) {
            String chunkContent = chunk.getText();
            if (!StringUtils.hasText(chunkContent)) {
                continue;
            }

            KnowledgeChunk knowledgeChunk = new KnowledgeChunk();
            knowledgeChunk.setSourceType("DOCUMENT");
            knowledgeChunk.setSourceId(knowledgeDocument.getId());
            knowledgeChunk.setChunkNo(chunkNo++);
            knowledgeChunk.setContent(chunkContent);
            knowledgeChunk.setContentHash(sha256(chunkContent));
            knowledgeChunk.setCharCount(chunkContent.length());
            knowledgeChunk.setMetadata(metadata);
            knowledgeChunk.setIndexStatus("PENDING");
            knowledgeChunk.setCreatedAt(now);
            knowledgeChunk.setUpdatedAt(now);
            knowledgeChunk.setDeleted(0);
            if (knowledgeChunkMapper.insert(knowledgeChunk) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "新增知识文件切片失败");
            }
        }

        if (chunkNo == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件解析内容无法生成有效切片");
        }

        KnowledgeChunkRebuildVO result = new KnowledgeChunkRebuildVO();
        result.setSourceType("DOCUMENT");
        result.setSourceId(documentId);
        result.setChunkCount(chunkNo);
        result.setRebuiltAt(now);
        return result;
    }

    @Override
    public PageResult<KnowledgeChunkVO> pageChunks(KnowledgeChunkQueryRequest request) {
        // 按来源和索引状态分页查询有效切片。
        KnowledgeChunkQueryRequest safeRequest = request == null
                ? new KnowledgeChunkQueryRequest()
                : request;
        if (safeRequest.getSourceId() == null || !StringUtils.hasText(safeRequest.getSourceType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "来源类型和来源ID不能为空");
        }
        LambdaQueryWrapper<KnowledgeChunk> lambdaQueryWrapper = new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getSourceType, safeRequest.getSourceType())
                .eq(KnowledgeChunk::getSourceId, safeRequest.getSourceId())
                .eq(StringUtils.hasText(safeRequest.getIndexStatus()),
                        KnowledgeChunk::getIndexStatus, safeRequest.getIndexStatus())
                .eq(KnowledgeChunk::getDeleted, 0)
                .orderByAsc(KnowledgeChunk::getChunkNo);
        Page<KnowledgeChunk> page = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        Page<KnowledgeChunk> knowledgeChunkPage = knowledgeChunkMapper.selectPage(page, lambdaQueryWrapper);
        List<KnowledgeChunkVO> knowledgeChunkList = knowledgeChunkPage.getRecords().stream()
                .map(this::toChunkVO)
                .toList();
        return PageResult.of(knowledgeChunkList, knowledgeChunkPage.getCurrent(),
                knowledgeChunkPage.getSize(), knowledgeChunkPage.getTotal());
    }

    @Override
    public KnowledgeChunkVO getChunk(Long id) {
        // 查询单个有效知识切片详情。
        KnowledgeChunk knowledgeChunk = knowledgeChunkMapper.selectById(id);
        if (knowledgeChunk == null || Integer.valueOf(1).equals(knowledgeChunk.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识切片不存在");
        }
        return toChunkVO(knowledgeChunk);
    }

    private String writeArticleMetadata(KnowledgeArticle article) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", article.getTitle());
        metadata.put("category", article.getCategory());
        try {
            JsonNode tags = StringUtils.hasText(article.getTags())
                    ? objectMapper.readTree(article.getTags())
                    : objectMapper.createArrayNode();
            metadata.put("tags", tags);
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文章标签数据格式不正确");
        }
    }

    private String writeDocumentMetadata(KnowledgeDocument document) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("documentName", document.getDocumentName());
        metadata.put("fileId", document.getFileId());
        metadata.put("contentType", document.getContentType());
        metadata.put("parserType", document.getParserType());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "知识文件元数据序列化失败");
        }
    }

    private void validateChunkSource(String sourceType, Long sourceId) {
        if ("ARTICLE".equals(sourceType)) {
            KnowledgeArticle article = knowledgeArticleMapper.selectById(sourceId);
            if (article == null || Integer.valueOf(1).equals(article.getDeleted())) {
                throw new BusinessException(ResultCode.NOT_FOUND, "知识文章不存在");
            }
            if (!"PUBLISHED".equals(article.getStatus())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "只有已发布文章可以新增切片");
            }
            return;
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(sourceId);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在");
        }
        if (!"PARSED".equals(document.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有已解析文件可以新增切片");
        }
    }

    private String normalizeMetadata(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return "{}";
        }
        try {
            JsonNode metadataNode = objectMapper.readTree(metadata);
            if (metadataNode == null || !metadataNode.isObject()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "切片元数据必须是JSON对象");
            }
            return objectMapper.writeValueAsString(metadataNode);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "切片元数据不是合法JSON");
        }
    }

    private KnowledgeChunkVO toChunkVO(KnowledgeChunk knowledgeChunk) {
        KnowledgeChunkVO vo = new KnowledgeChunkVO();
        BeanUtils.copyProperties(knowledgeChunk, vo);
        return vo;
    }

    private String sha256(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
