package com.fampay.scheduler.api.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobExecutionResult {
     private String executionId;
     private String executionStatus;
     private Long scheduledAt;
     private boolean completed;
     private Long timeTaken;
     private String status;
     private String response;
}
