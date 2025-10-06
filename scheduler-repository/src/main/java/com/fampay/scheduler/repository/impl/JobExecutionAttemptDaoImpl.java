package com.fampay.scheduler.repository.impl;

import com.fampay.scheduler.commons.mongo.helper.IMongoDbHelper;
import com.fampay.scheduler.models.entity.JobExecutionAttemptEntity;
import com.fampay.scheduler.repository.JobExecutionAttemptDao;
import com.fampay.scheduler.repository.dto.UpdateJobExecutionAttemptDto;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTimeUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobExecutionAttemptDaoImpl implements JobExecutionAttemptDao {

    private static final String COLLECTION_NAME = "job_execution_attempts";
    private static final String EXECUTION_ID_FIELD = "executionId";
    private final IMongoDbHelper mongoDbHelper;

    @Override
    public void createJobExecutionAttempt(JobExecutionAttemptEntity jobExecutionAttemptEntity) {
        jobExecutionAttemptEntity.setCreatedAt(DateTimeUtils.currentTimeMillis());
        jobExecutionAttemptEntity.setUpdatedAt(DateTimeUtils.currentTimeMillis());
        mongoDbHelper.save(COLLECTION_NAME,jobExecutionAttemptEntity.getRunId(),jobExecutionAttemptEntity);
    }

    @Override
    public void updateJobExecutionAttempt(String runId,UpdateJobExecutionAttemptDto updateJobExecutionAttemptDto) {
        updateJobExecutionAttemptDto.setUpdatedAt(DateTimeUtils.currentTimeMillis());
        mongoDbHelper.updateById(COLLECTION_NAME,runId,updateJobExecutionAttemptDto);
    }

    @Override
    public List<JobExecutionAttemptEntity> findByJobExecutionId(String executionId) {
        var searchParams = Map.<String, Object>of(EXECUTION_ID_FIELD, executionId);
        List<JobExecutionAttemptEntity> res =  mongoDbHelper.findAll(COLLECTION_NAME, searchParams, JobExecutionAttemptEntity.class);
        if (!res.isEmpty()) {
            return res;
        }
        return new ArrayList<>();
    }
}
