package com.example.smartcustomerservice.service.impl.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.smartcustomerservice.config.properties.KnowledgeIndexProperties;
import com.example.smartcustomerservice.domain.entity.KnowledgeChunk;
import com.example.smartcustomerservice.domain.vo.KnowledgeSearchVO;
import com.example.smartcustomerservice.service.knowledge.KnowledgeEsIndexService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeEsIndexServiceImpl implements KnowledgeEsIndexService {

    private final ElasticsearchClient elasticsearchClient;
    private final KnowledgeIndexProperties knowledgeIndexProperties;

    public KnowledgeEsIndexServiceImpl(ElasticsearchClient elasticsearchClient,
                                       KnowledgeIndexProperties knowledgeIndexProperties) {
        this.elasticsearchClient = elasticsearchClient;
        this.knowledgeIndexProperties = knowledgeIndexProperties;
    }

    @Override
    public void ensureIndex() {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(request -> request.index(knowledgeIndexProperties.getIndexName()))
                    .value();
            if (exists) {
                return;
            }

            elasticsearchClient.indices().create(request -> request
                    .index(knowledgeIndexProperties.getIndexName())
                    .mappings(mapping -> mapping
                            .properties("chunkId", property -> property.long_(value -> value))
                            .properties("sourceType", property -> property.keyword(value -> value))
                            .properties("sourceId", property -> property.long_(value -> value))
                            .properties("chunkNo", property -> property.integer(value -> value))
                            .properties("content", property -> property.text(value -> value))
                            .properties("contentHash", property -> property.keyword(value -> value))
                            .properties("metadata", property -> property.keyword(value -> value.index(false)))
                            .properties("embeddingModel", property -> property.keyword(value -> value))
                            .properties("createdAt", property -> property.date(value -> value))
                            .properties("embedding", property -> property.denseVector(value -> value
                                    .dims(knowledgeIndexProperties.getVectorDimensions())
                                    .index(true)
                                    .similarity("cosine")))
                    ));
        } catch (IOException e) {
            throw new IllegalStateException("创建或检查知识库 Elasticsearch 索引失败", e);
        }
    }

    @Override
    public String indexChunk(KnowledgeChunk chunk, float[] embedding, String embeddingModel) {
        if (embedding == null || embedding.length != knowledgeIndexProperties.getVectorDimensions()) {
            throw new IllegalArgumentException("Embedding 向量维度与 Elasticsearch 索引配置不一致");
        }
        try {
            String documentId = "knowledge-chunk-" + chunk.getId();
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("chunkId", chunk.getId());
            document.put("sourceType", chunk.getSourceType());
            document.put("sourceId", chunk.getSourceId());
            document.put("chunkNo", chunk.getChunkNo());
            document.put("content", chunk.getContent());
            document.put("contentHash", chunk.getContentHash());
            document.put("metadata", chunk.getMetadata());
            document.put("embeddingModel", embeddingModel);
            // Elasticsearch 的 JSON 映射器默认不支持直接序列化 LocalDateTime，改为 ISO-8601 文本。
            document.put("createdAt", chunk.getCreatedAt() == null ? null : chunk.getCreatedAt().toString());
            document.put("embedding", toFloatList(embedding));

            elasticsearchClient.index(request -> request
                    .index(knowledgeIndexProperties.getIndexName())
                    .id(documentId)
                    .document(document));
            return documentId;
        } catch (IOException e) {
            throw new IllegalStateException("写入知识切片 Elasticsearch 索引失败", e);
        }
    }

    @Override
    public List<KnowledgeSearchVO> search(float[] queryEmbedding, String sourceType, Integer topK) {
        if (queryEmbedding == null || queryEmbedding.length != knowledgeIndexProperties.getVectorDimensions()) {
            throw new IllegalArgumentException("查询 Embedding 向量维度与 Elasticsearch 索引配置不一致");
        }
        int resultSize = topK == null ? 5 : topK;
        if (resultSize < 1) {
            throw new IllegalArgumentException("检索结果数量必须大于0");
        }

        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(request -> request.index(knowledgeIndexProperties.getIndexName()))
                    .value();
            if (!exists) {
                return List.of();
            }

            int numCandidates = Math.max(resultSize * 10, 50);
            SearchResponse<Map> response = elasticsearchClient.search(request -> {
                request.index(knowledgeIndexProperties.getIndexName())
                        .size(resultSize)
                        .knn(knn -> {
                            knn.field("embedding")
                                    .queryVector(toFloatList(queryEmbedding))
                                    .k((long) resultSize)
                                    .numCandidates((long) numCandidates);
                            if (StringUtils.hasText(sourceType)) {
                                knn.filter(filter -> filter.term(term -> term
                                        .field("sourceType")
                                        .value(sourceType)));
                            }
                            return knn;
                        });
                return request;
            }, Map.class);

            return response.hits().hits().stream()
                    .map(this::toSearchVO)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("查询知识库 Elasticsearch 索引失败", e);
        }
    }

    private KnowledgeSearchVO toSearchVO(Hit<Map> hit) {
        Map source = hit.source();
        if (source == null) {
            return null;
        }
        Long chunkId = toLong(source.get("chunkId"));
        Long sourceId = toLong(source.get("sourceId"));
        Integer chunkNo = toInteger(source.get("chunkNo"));
        String sourceType = toStringValue(source.get("sourceType"));
        String content = toStringValue(source.get("content"));
        if (chunkId == null || sourceId == null || !StringUtils.hasText(sourceType)
                || !StringUtils.hasText(content)) {
            return null;
        }

        KnowledgeSearchVO vo = new KnowledgeSearchVO();
        vo.setChunkId(chunkId);
        vo.setSourceType(sourceType);
        vo.setSourceId(sourceId);
        vo.setChunkNo(chunkNo);
        vo.setContent(content);
        vo.setScore(hit.score());
        vo.setMetadata(toStringValue(source.get("metadata")));
        return vo;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Long.valueOf(text);
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Integer.valueOf(text);
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<Float> toFloatList(float[] embedding) {
        List<Float> values = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            values.add(value);
        }
        return values;
    }
}
