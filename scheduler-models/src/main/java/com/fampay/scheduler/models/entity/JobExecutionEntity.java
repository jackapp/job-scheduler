package com.fampay.scheduler.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobExecutionEntity {
    private String executionId;
    private long scheduledRunAt;
    private String jobId;
    private String status;
    private Long startTime;
    private Long endTime;
    private long createdAt;
    private long updatedAt;
    private JobExecutionResponse executionResponse;
}