package com.fampay.scheduler.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionResultResponse {
    private String executionId;
    private String executionStatus;
    private Long scheduledAt;
    private boolean completed;
    private Long timeTaken;
    private String status;
    private String response;
}
