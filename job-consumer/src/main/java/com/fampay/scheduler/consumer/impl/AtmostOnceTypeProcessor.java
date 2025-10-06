package com.fampay.scheduler.consumer.impl;

import com.fampay.scheduler.commons.http.client.AsyncHttpClient;
import com.fampay.scheduler.commons.http.dto.ApiRequest;
import com.fampay.scheduler.commons.lock.annotation.ExecuteInLock;
import com.fampay.scheduler.commons.queue.IMessageConsumer;
import com.fampay.scheduler.consumer.JobTypeProcessor;
import com.fampay.scheduler.models.dto.JobExecutionStatus;
import com.fampay.scheduler.models.dto.JobGuarantee;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.models.entity.JobExecutionResponse;
import com.fampay.scheduler.models.queue.JobMessagePayload;
import com.fampay.scheduler.repository.JobExecutionDao;
import com.fampay.scheduler.repository.dto.UpdateJobExecutionDto;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Qualifier("atmost_once_processor")
public class AtmostOnceTypeProcessor implements JobTypeProcessor {

    private final IMessageConsumer messageConsumer;
    private final JobExecutionDao jobExecutionDao;
    private final AsyncHttpClient asyncHttpClient;

    @ExecuteInLock(waitingTime = 100L, prefix = "JOB_EXECUTION", lockIdKeys = {"jobMessagePayload.executionId"})
    @Override
    public void processJobExecution(JobMessagePayload jobMessagePayload, String messageId, String queueName) {
        Optional<JobExecutionEntity> jobExecutionEntityOptional = jobExecutionDao.findByExecutionId(jobMessagePayload.getExecutionId());
        if (jobExecutionEntityOptional.isPresent()) {
            JobExecutionEntity jobExecutionEntity = jobExecutionEntityOptional.get();
            if (!JobExecutionStatus.hasStartedBefore(JobExecutionStatus.from(jobExecutionEntity.getStatus()))) {
                try {
                    jobExecutionDao.updateJobExecutionStatus(jobExecutionEntity.getExecutionId(), UpdateJobExecutionDto.builder()
                            .status(JobExecutionStatus.STARTED.name()).startTime(DateTimeUtils.currentTimeMillis()).build());
                    asyncHttpClient.callApi(ApiRequest.builder().httpMethod(HttpMethod.valueOf(jobMessagePayload.getApiConfig().getHttpMethod()))
                            .url(jobMessagePayload.getApiConfig().getUrl()).payload(jobMessagePayload.getApiConfig().getPayload()).readTimeout(jobMessagePayload.getApiConfig().getReadTimeoutMs())
                            .build()).subscribe(apiResponse -> {
                        handleResponse(jobMessagePayload, apiResponse.getHttpStatus() + "", apiResponse.getResponse(), messageId, queueName);
                    });
                } catch (Exception e) {
                    jobExecutionDao.updateJobExecutionStatus(jobMessagePayload.getExecutionId(), UpdateJobExecutionDto.builder().endTime(DateTimeUtils.currentTimeMillis())
                            .status(JobExecutionStatus.FAILED.name()).executionResponse(
                                    JobExecutionResponse.builder()
                                            .response(e.getMessage()).build()).build());
                }
            }
        }
        messageConsumer.deleteMessage(queueName, messageId);
    }

    @Override
    public JobGuarantee getTypeProcessor() {
        return JobGuarantee.ATMOST_ONE;
    }

    private void handleResponse(JobMessagePayload jobMessagePayload, String statusCode, String body, String messageId, String queueName) {
        jobExecutionDao.updateJobExecutionStatus(jobMessagePayload.getExecutionId(), UpdateJobExecutionDto.builder().endTime(DateTimeUtils.currentTimeMillis())
                .status(JobExecutionStatus.FINISHED.name()).executionResponse(
                        JobExecutionResponse.builder()
                                .status(statusCode)
                                .response(body).build()).build());
        messageConsumer.deleteMessage(queueName, messageId);

    }
}
