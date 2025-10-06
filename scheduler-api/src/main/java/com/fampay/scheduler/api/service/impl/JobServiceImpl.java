package com.fampay.scheduler.api.service.impl;

import com.fampay.scheduler.commons.exception.HttpExceptions;
import com.fampay.scheduler.commons.helper.utils.CronUtilsHelper;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.repository.JobDao;
import com.fampay.scheduler.repository.JobExecutionDao;
import com.fampay.scheduler.api.adapter.DaoAdapter;
import com.fampay.scheduler.api.dto.response.CreateJobResponse;
import com.fampay.scheduler.api.dto.service.JobExecutionHistory;
import com.fampay.scheduler.api.dto.service.JobRequestDto;
import com.fampay.scheduler.api.service.JobService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class JobServiceImpl implements JobService {

    private final JobDao jobDao;
    private final JobExecutionDao jobExecutionDao;

    @Override
    public CreateJobResponse createJob(JobRequestDto jobRequest) {
        if (CronUtilsHelper.isValidCron(jobRequest.getSchedule())) {
            JobEntity jobEntity = DaoAdapter.createJobEntityFromDto(jobRequest);
            jobDao.createJob(jobEntity);
            return CreateJobResponse.builder().jobId(jobEntity.getId()).nextSchedule(jobEntity.getNextScheduledTime()).build();
        } else {
            throw HttpExceptions.ClientErrorException.childBuilder().statusCode(HttpStatus.SC_BAD_REQUEST).rawResponse("Invalid cron expression").build();
        }
    }

    @Override
    public JobExecutionHistory getJobExecutionsForJobId(String jobId,Integer limit) {
        if (limit==null || limit > 20) {
            limit=20;
        }
        return DaoAdapter.convertToExecutionHistory(jobExecutionDao.findCompletedJobExecutionsByJobId(jobId,limit));
    }
}
