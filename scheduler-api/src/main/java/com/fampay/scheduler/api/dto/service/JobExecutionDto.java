package com.fampay.scheduler.api.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobExecutionDto {
    private String executionId;
    private long scheduledRunAt;
    private String jobId;
    private String status;
    private Long startTime;
    private Long endTime;
    private long createdAt;
    private long updatedAt;
}