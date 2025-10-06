package com.fampay.scheduler.repository.dto;

import com.fampay.scheduler.models.entity.JobExecutionResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateJobExecutionDto {
    private String status;
    private JobExecutionResponse executionResponse;
    private Long startTime;
    private Long endTime;
    private Long updatedAt;
}
