package com.fampay.scheduler.api.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobExecutionHistory {
    private String jobId;
    private List<JobExecutionResult> jobExecutionResponseList;

}
