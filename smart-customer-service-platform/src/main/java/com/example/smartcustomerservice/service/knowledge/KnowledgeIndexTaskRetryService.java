package com.example.smartcustomerservice.service.knowledge;

public interface KnowledgeIndexTaskRetryService {

    boolean scheduleAutomaticRetry(Long taskId);
}
