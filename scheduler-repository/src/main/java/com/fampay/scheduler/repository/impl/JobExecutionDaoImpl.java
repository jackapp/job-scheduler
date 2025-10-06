package com.fampay.scheduler.repository.impl;

import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.commons.mongo.BulkOperation;
import com.fampay.scheduler.commons.mongo.helper.IMongoDbHelper;
import com.fampay.scheduler.commons.mongo.helper.impl.BulkOperationImpl;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.repository.JobExecutionDao;
import com.fampay.scheduler.repository.dto.UpdateJobExecutionDto;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.RequiredArgsConstructor;
import org.bson.conversions.Bson;
import org.joda.time.DateTimeUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JobExecutionDaoImpl implements JobExecutionDao {

    private static final String COLLECTION_NAME = "job_executions";
    private final IMongoDbHelper mongoDbHelper;

    @Override
    public void createMultipleJobExecutions(List<JobExecutionEntity> jobExecutionEntities) {
        long createdAt = DateTimeUtils.currentTimeMillis();
        List<BulkOperation<Map<String,Object>>> bulkOperations = new ArrayList<>();

        for (JobExecutionEntity jobExecutionEntity : jobExecutionEntities) {
            jobExecutionEntity.setCreatedAt(createdAt);
            jobExecutionEntity.setUpdatedAt(createdAt);

            bulkOperations.add(new BulkOperationImpl(
                    BulkOperation.Type.UPSERT,
                    Map.of("_id", jobExecutionEntity.getExecutionId()),
                    Map.of("$setOnInsert", CommonSerializationUtil.convertObjectToMap(jobExecutionEntity)),
                    true));
        }
        mongoDbHelper.executeBulkWrite(COLLECTION_NAME,bulkOperations);
    }


    @Override
    public void updateJobExecutionStatus(String executionId, UpdateJobExecutionDto updateJobExecutionDto) {
        updateJobExecutionDto.setUpdatedAt(DateTimeUtils.currentTimeMillis());
        mongoDbHelper.updateById(COLLECTION_NAME,executionId,updateJobExecutionDto);
    }

    @Override
    public Optional<JobExecutionEntity> findByExecutionId(String executionId) {
         return mongoDbHelper.findOptionalById(COLLECTION_NAME,executionId,JobExecutionEntity.class);
    }

    @Override
    public List<JobExecutionEntity> findCompletedJobExecutionsByJobId(String jobId, int limit) {
        List<JobExecutionEntity> result = new ArrayList<>();
        Bson f1 = Filters.eq("jobId",jobId);
        Bson f2 = Filters.lt("scheduledRunAt",DateTimeUtils.currentTimeMillis());
        Bson f3 = Filters.ne("startTime",null);
        Bson f4 = Filters.ne("endTime",null);
        Bson sort = Sorts.descending("scheduledRunAt");
        Bson finalFilter = Filters.and(Filters.and(Filters.and(f1,f2),f3),f4);
        mongoDbHelper.getCollection(COLLECTION_NAME).find(finalFilter).sort(sort).limit(limit).forEach(document ->
                result.add(CommonSerializationUtil.readObject(CommonSerializationUtil.writeString(document),JobExecutionEntity.class)));
        return result;
    }
}
