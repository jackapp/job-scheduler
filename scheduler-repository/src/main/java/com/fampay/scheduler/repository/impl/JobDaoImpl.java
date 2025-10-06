package com.fampay.scheduler.repository.impl;

import com.fampay.scheduler.commons.mongo.dto.PagedResult;
import com.fampay.scheduler.commons.mongo.helper.IMongoDbHelper;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.PagedJobs;
import com.fampay.scheduler.repository.JobDao;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.joda.time.DateTimeUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;


@RequiredArgsConstructor
@Component
public class JobDaoImpl implements JobDao {


    private static final String COLLECTION_NAME = "jobs";
    private static final String CORRELATION_ID_FIELD = "correlationId";
    private static final String NEXT_SCHEDULED_TIME_FIELD = "nextScheduledTime";
    private static final String ID_FIELD = "id";


    private final IMongoDbHelper mongoDbHelper;

    @Override
    public void createJob(JobEntity jobEntity) {
        if (Objects.isNull(jobEntity.getId())) {
            jobEntity.setId((new ObjectId()).toHexString());
        }
        mongoDbHelper.save(COLLECTION_NAME, jobEntity.getId(), jobEntity);
    }

    @Override
    public Optional<JobEntity> getJobById(String jobId) {
        return mongoDbHelper.findOptionalById(COLLECTION_NAME, jobId, JobEntity.class);
    }

    @Override
    public Optional<JobEntity> getJobByCorrelationId(String correlationId) {
        var searchParams = Map.<String, Object>of(CORRELATION_ID_FIELD, correlationId);
        List<JobEntity> res = mongoDbHelper.findAll(COLLECTION_NAME, searchParams, JobEntity.class);
        if (!res.isEmpty()) {
            return Optional.of(res.getFirst());
        }
        return Optional.empty();
    }

    @Override
    public List<JobEntity> getJobsScheduledBetween(long startTimestamp, long endTimestamp) {
        Bson bsonFilter = Filters.and(gte(NEXT_SCHEDULED_TIME_FIELD, startTimestamp),
                lte(NEXT_SCHEDULED_TIME_FIELD, endTimestamp));
        return mongoDbHelper.findAll(COLLECTION_NAME, bsonFilter, JobEntity.class);
    }

    @Override
    public List<JobEntity> getJobsScheduledBefore(long endTimestamp) {
        Bson bsonFilter = Filters.lte(NEXT_SCHEDULED_TIME_FIELD, endTimestamp);
        return mongoDbHelper.findAll(COLLECTION_NAME, bsonFilter, JobEntity.class);
    }

    @Override
    public void updateNextRunForJob(String id, Long nextTimestamp) {
        mongoDbHelper.updateById(COLLECTION_NAME,id,Map.of(NEXT_SCHEDULED_TIME_FIELD,nextTimestamp,"updatedAt", DateTimeUtils.currentTimeMillis()));
    }

    @Override
    public PagedJobs getJobsPaginated(long startTimestamp, long endTimestamp, int pageSize, Long lastNextScheduledTime, String lastId) {
        var baseFilter = and(
                gte(NEXT_SCHEDULED_TIME_FIELD, startTimestamp),
                lte(NEXT_SCHEDULED_TIME_FIELD, endTimestamp)
        );

        var finalFilter = baseFilter;
        if (lastNextScheduledTime != null && StringUtils.isNotEmpty(lastId)) {
            var cursorFilter = or(
                    gt(NEXT_SCHEDULED_TIME_FIELD, lastNextScheduledTime),
                    and(
                            eq(NEXT_SCHEDULED_TIME_FIELD, lastNextScheduledTime),
                            gt(ID_FIELD, lastId)
                    )
            );
            finalFilter = and(baseFilter, cursorFilter);
        }
        List<String> cursorFields = List.of(NEXT_SCHEDULED_TIME_FIELD, "id");
        var sortOrder = ascending(cursorFields);
        PagedResult<JobEntity> jobEntityPagedResult = mongoDbHelper.getDocumentsPaginated(COLLECTION_NAME, finalFilter, sortOrder, pageSize, cursorFields, JobEntity.class);
        return PagedJobs.builder().jobEntities(jobEntityPagedResult.getResults()).nextJobId((String) jobEntityPagedResult.getCursorMap().get(ID_FIELD))
                .nextScheduledTimestamp((Long) jobEntityPagedResult.getCursorMap().get(NEXT_SCHEDULED_TIME_FIELD)).build();
    }


}
