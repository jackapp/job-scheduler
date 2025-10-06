package com.fampay.scheduler.repository;


import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.PagedJobs;

import java.util.List;
import java.util.Optional;

public interface JobDao {
    void createJob(JobEntity jobEntity);
    Optional<JobEntity> getJobById(String jobId);
    Optional<JobEntity> getJobByCorrelationId(String correlationId);
    List<JobEntity> getJobsScheduledBetween(long startTimestamp,long endTimestamp);
    List<JobEntity> getJobsScheduledBefore(long endTimestamp);
    void updateNextRunForJob(String id,Long nextTimestamp);
    PagedJobs getJobsPaginated(long startTimestamp, long endTimestamp, int pagesize, Long lastNextScheduledTimestamp, String lastJobId);

}
