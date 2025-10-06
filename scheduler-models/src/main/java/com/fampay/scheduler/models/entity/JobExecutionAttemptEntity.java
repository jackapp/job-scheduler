package com.fampay.scheduler.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobExecutionAttemptEntity {
    private String runId;
    private String executionId;
    private Long startTime;
    private Long endTime;
    private String status;
    private String response;
    private Long createdAt;
    private Long updatedAt;

}
