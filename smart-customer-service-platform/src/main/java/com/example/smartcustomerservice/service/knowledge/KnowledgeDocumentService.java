package com.example.smartcustomerservice.service.knowledge;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.KnowledgeDocumentCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeDocumentQueryRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeDocumentVO;

public interface KnowledgeDocumentService {

    KnowledgeDocumentVO createDocument(KnowledgeDocumentCreateRequest request);

    PageResult<KnowledgeDocumentVO> pageDocuments(KnowledgeDocumentQueryRequest request);

    KnowledgeDocumentVO getDocument(Long id);

    KnowledgeDocumentVO parseDocument(Long id);

    KnowledgeDocumentVO retryParseDocument(Long id);

    KnowledgeDocumentVO offlineDocument(Long id);

    Boolean deleteDocument(Long id);
}
