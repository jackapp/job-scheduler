package com.fampay.scheduler.repository;

import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.repository.dto.UpdateJobExecutionDto;
import java.util.List;
import java.util.Optional;

public interface JobExecutionDao {
    void createMultipleJobExecutions(List<JobExecutionEntity> jobExecutionEntities);
    void updateJobExecutionStatus(String executionId,UpdateJobExecutionDto updateJobExecutionDto);
    Optional<JobExecutionEntity> findByExecutionId(String executionId);
    List<JobExecutionEntity> findCompletedJobExecutionsByJobId(String jobId, int limit);
}
