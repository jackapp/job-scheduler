package com.fampay.scheduler.api.adapter;

import com.fampay.scheduler.api.dto.service.JobExecutionHistory;
import com.fampay.scheduler.api.dto.response.JobResponse;
import com.fampay.scheduler.api.dto.response.CreateJobResponse;
import com.fampay.scheduler.api.dto.response.JobExecutionHistoryResponse;
import com.fampay.scheduler.api.dto.response.JobExecutionResultResponse;
import com.fampay.scheduler.models.dto.JobGuarantee;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.api.dto.service.ApiConfigDto;
import com.fampay.scheduler.api.dto.service.JobExecutionDto;
import com.fampay.scheduler.api.dto.service.JobExecutionResult;
import com.fampay.scheduler.api.dto.request.JobRequest;
import com.fampay.scheduler.api.dto.service.JobRequestDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ControllerAdapter {

    public static List<JobExecutionEntity> getJobExecutionEntitiesFromDtos(List<JobExecutionDto> dtos) {
        List<JobExecutionEntity> jobExecutionEntities = new ArrayList<>();
        for (JobExecutionDto jobExecutionDto : dtos) {
            JobExecutionEntity jobExecutionEntity = getJobExecutionEntityFromDto(jobExecutionDto);
            if (jobExecutionEntity!=null) {
                jobExecutionEntities.add(jobExecutionEntity);
            }
        }
        return jobExecutionEntities;
    }

    public static JobExecutionEntity getJobExecutionEntityFromDto(JobExecutionDto jobExecutionDto)  {
        if (jobExecutionDto!=null) {
            return JobExecutionEntity.builder().executionId(jobExecutionDto.getExecutionId())
                    .jobId(jobExecutionDto.getJobId()).createdAt(jobExecutionDto.getCreatedAt()).scheduledRunAt(jobExecutionDto.getScheduledRunAt())
                    .updatedAt(jobExecutionDto.getUpdatedAt()).endTime(jobExecutionDto.getEndTime()).status(jobExecutionDto.getStatus())
                    .startTime(jobExecutionDto.getStartTime()).build();
        }
        return null;
    }

    public static JobRequestDto createJobRequestDto(JobRequest jobRequest) {
        return JobRequestDto.builder().type(JobGuarantee.valueOf(jobRequest.getType()))
                .api(ApiConfigDto.builder().httpMethod(HttpMethod.valueOf(jobRequest.getApi().getHttpMethod())).url(jobRequest.getApi().getUrl())
                        .payload(jobRequest.getApi().getPayload())
                        .readTimeoutMs(jobRequest.getApi().getReadTimeoutMs()).build()).correlationId(jobRequest.getCorrelationId()).schedule(
                        jobRequest.getSchedule()).build();
    }

    public static JobResponse getJobResponseFromDto(CreateJobResponse createJobResponse) {
        if (createJobResponse == null) return null;
        return JobResponse.builder().jobId(createJobResponse.getJobId()).nextSchedule(createJobResponse.getNextSchedule()).build();
    }

    public static JobExecutionHistoryResponse getJobExecutionHistoryResponseFromDto(JobExecutionHistory jobExecutionHistory) {
        if (jobExecutionHistory==null) return JobExecutionHistoryResponse.builder().executionResults(new ArrayList<>()).build();
        JobExecutionHistoryResponse jobExecutionHistoryResponse = JobExecutionHistoryResponse.builder().jobId(jobExecutionHistory.getJobId())
                .executionResults(new ArrayList<>()).build();

        for (JobExecutionResult result : jobExecutionHistory.getJobExecutionResponseList()) {
            jobExecutionHistoryResponse.getExecutionResults().add(JobExecutionResultResponse
                    .builder().executionId(result.getExecutionId())
                    .executionStatus(result.getExecutionStatus())
                    .completed(result.isCompleted())
                    .response(result.getResponse())
                    .status(result.getStatus())
                    .scheduledAt(result.getScheduledAt()).timeTaken(result.getTimeTaken()).build());
        }
        return jobExecutionHistoryResponse;
    }


}
