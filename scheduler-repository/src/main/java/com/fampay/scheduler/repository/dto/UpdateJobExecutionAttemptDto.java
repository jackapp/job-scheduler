package com.fampay.scheduler.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateJobExecutionAttemptDto {
    private String response;
    private String status;
    private Long endTime;
    private Long updatedAt;
}
