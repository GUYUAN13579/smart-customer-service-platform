package com.example.smartcustomerservice.service.knowledge;

import com.example.smartcustomerservice.domain.entity.KnowledgeChunk;
import com.example.smartcustomerservice.domain.vo.KnowledgeSearchVO;

import java.util.List;

public interface KnowledgeEsIndexService {

    void ensureIndex();

    String indexChunk(KnowledgeChunk chunk, float[] embedding, String embeddingModel);

    List<KnowledgeSearchVO> search(float[] queryEmbedding, String sourceType, Integer topK);
}
