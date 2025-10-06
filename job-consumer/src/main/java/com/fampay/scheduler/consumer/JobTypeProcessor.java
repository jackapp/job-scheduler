package com.fampay.scheduler.consumer;

import com.fampay.scheduler.models.dto.JobGuarantee;
import com.fampay.scheduler.models.queue.JobMessagePayload;

public interface JobTypeProcessor {
    void processJobExecution(JobMessagePayload jobMessagePayload,String messageId,String queueName);
    JobGuarantee getTypeProcessor();
}
