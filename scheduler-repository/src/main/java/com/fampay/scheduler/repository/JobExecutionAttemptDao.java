package com.fampay.scheduler.repository;

import com.fampay.scheduler.models.entity.JobExecutionAttemptEntity;
import com.fampay.scheduler.repository.dto.UpdateJobExecutionAttemptDto;

import java.util.List;

public interface JobExecutionAttemptDao {
    void createJobExecutionAttempt(JobExecutionAttemptEntity jobExecutionAttemptEntity);
    void updateJobExecutionAttempt(String runId,UpdateJobExecutionAttemptDto updateJobExecutionAttemptDto);
    List<JobExecutionAttemptEntity> findByJobExecutionId(String executionId);
}
