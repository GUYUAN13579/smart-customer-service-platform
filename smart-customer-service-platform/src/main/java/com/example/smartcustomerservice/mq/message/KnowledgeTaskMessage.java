package com.example.smartcustomerservice.mq.message;

import java.io.Serial;
import java.io.Serializable;

public class KnowledgeTaskMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;

    public KnowledgeTaskMessage() {
    }

    public KnowledgeTaskMessage(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
