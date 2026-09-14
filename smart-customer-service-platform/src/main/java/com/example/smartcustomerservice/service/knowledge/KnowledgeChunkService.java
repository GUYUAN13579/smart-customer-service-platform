package com.example.smartcustomerservice.service.knowledge;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeChunkCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeChunkQueryRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeChunkRebuildVO;
import com.example.smartcustomerservice.domain.vo.KnowledgeChunkVO;

public interface KnowledgeChunkService {

    KnowledgeChunkVO createChunk(KnowledgeChunkCreateRequest request);

    KnowledgeChunkRebuildVO rebuildArticleChunks(Long articleId);

    KnowledgeChunkRebuildVO rebuildDocumentChunks(Long documentId);

    PageResult<KnowledgeChunkVO> pageChunks(KnowledgeChunkQueryRequest request);

    KnowledgeChunkVO getChunk(Long id);
}
