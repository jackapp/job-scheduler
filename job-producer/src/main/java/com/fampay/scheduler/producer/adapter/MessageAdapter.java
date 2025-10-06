package com.fampay.scheduler.producer.adapter;

import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.models.queue.ApiConfig;
import com.fampay.scheduler.models.queue.JobMessagePayload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageAdapter {

    public static JobMessagePayload fromJobProduceData(JobExecutionEntity jobExecutionEntity, JobEntity jobEntity) {
        return JobMessagePayload.builder().jobId(jobEntity.getId()).type(jobEntity.getType())
                .executionId(jobExecutionEntity.getExecutionId()).scheduledRunAt(jobExecutionEntity.getScheduledRunAt())
                .createdAt(jobExecutionEntity.getCreatedAt()).updatedAt(jobExecutionEntity.getUpdatedAt())
                .apiConfig(ApiConfig.builder().payload(jobEntity.getApiConfig().getPayload()).readTimeoutMs(jobEntity.getApiConfig().getReadTimeoutMs()).url(jobEntity.getApiConfig().getUrl()).httpMethod(jobEntity.getApiConfig().getHttpMethod()).build())
                .build();
    }
}
