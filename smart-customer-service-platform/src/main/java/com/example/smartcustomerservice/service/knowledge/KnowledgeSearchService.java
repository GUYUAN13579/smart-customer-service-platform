package com.example.smartcustomerservice.service.knowledge;

import com.example.smartcustomerservice.domain.dto.KnowledgeSearchRequest;
import com.example.smartcustomerservice.domain.vo.KnowledgeSearchVO;

import java.util.List;

public interface KnowledgeSearchService {

    List<KnowledgeSearchVO> search(KnowledgeSearchRequest request);
}
