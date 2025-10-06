package com.fampay.scheduler.api.service;


import com.fampay.scheduler.api.dto.response.CreateJobResponse;
import com.fampay.scheduler.api.dto.service.JobExecutionHistory;
import com.fampay.scheduler.api.dto.service.JobRequestDto;

public interface JobService {
    CreateJobResponse createJob(JobRequestDto jobRequest);
    JobExecutionHistory getJobExecutionsForJobId(String jobId,Integer limit);

}
