package com.fampay.scheduler.api.adapter;

import com.fampay.scheduler.api.dto.service.JobRequestDto;
import com.fampay.scheduler.commons.helper.utils.CronUtilsHelper;
import com.fampay.scheduler.models.dto.JobExecutionStatus;
import com.fampay.scheduler.models.entity.ApiConfigEntity;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.api.dto.service.JobExecutionHistory;
import com.fampay.scheduler.api.dto.service.JobExecutionResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joda.time.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DaoAdapter {

    public static JobEntity createJobEntityFromDto(JobRequestDto jobRequest) {
        return JobEntity.builder().apiConfig(ApiConfigEntity.builder()
                        .url(jobRequest.getApi().getUrl()).payload(jobRequest.getApi().getPayload()).httpMethod(
                                jobRequest.getApi().getHttpMethod().name())
                        .readTimeoutMs(jobRequest.getApi().getReadTimeoutMs()).build()).type(jobRequest.getType().name()).schedule(jobRequest.getSchedule())
                .nextScheduledTime(CronUtilsHelper.getScheduledTimeFrom1Min(jobRequest.getSchedule()))
                .createdAt(DateTimeUtils.currentTimeMillis())
                .updatedAt(DateTimeUtils.currentTimeMillis()).build();
    }

    public static JobExecutionHistory convertToExecutionHistory(List<JobExecutionEntity> jobExecutionEntities) {
        if (!(jobExecutionEntities==null || jobExecutionEntities.isEmpty())) {
            JobExecutionHistory jobExecutionHistory = JobExecutionHistory.builder().jobId(jobExecutionEntities.get(0).getJobId())
                    .jobExecutionResponseList(new ArrayList<>()).build();
            for (JobExecutionEntity jobExecutionEntity : jobExecutionEntities) {
                jobExecutionHistory.getJobExecutionResponseList().add(from(jobExecutionEntity));
            }
            return jobExecutionHistory;
        }
        return null;
    }

    private static JobExecutionResult from(JobExecutionEntity jobExecutionEntity) {
        JobExecutionResult jobExecutionResult = JobExecutionResult.builder()
                .executionId(jobExecutionEntity.getExecutionId())
                .completed(JobExecutionStatus.isTerminalState(JobExecutionStatus.valueOf(jobExecutionEntity.getStatus())))
                .scheduledAt(jobExecutionEntity.getScheduledRunAt())
                .executionStatus(jobExecutionEntity.getStatus())
                .build();
        if (jobExecutionEntity.getExecutionResponse()!=null) {
            jobExecutionResult.setStatus(jobExecutionEntity.getExecutionResponse().getStatus());
            jobExecutionResult.setResponse(jobExecutionEntity.getExecutionResponse().getResponse());
        }
        if (jobExecutionEntity.getStartTime()!=null && jobExecutionEntity.getEndTime()!=null) {
            jobExecutionResult.setTimeTaken(jobExecutionEntity.getEndTime()-jobExecutionEntity.getStartTime());
        }
        return jobExecutionResult;
    }
}
