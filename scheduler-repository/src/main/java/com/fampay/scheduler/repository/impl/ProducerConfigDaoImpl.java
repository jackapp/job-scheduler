package com.fampay.scheduler.repository.impl;

import com.fampay.scheduler.commons.mongo.helper.IMongoDbHelper;
import com.fampay.scheduler.models.entity.ProducerConfig;
import com.fampay.scheduler.repository.ProducerConfigDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ProducerConfigDaoImpl implements ProducerConfigDao {

    private static final String COLLECTION_NAME = "producer_config";
    private static final String LAST_PRODUCED_TIMESTAMP_FIELD = "lastProducedTimestamp";

    private final IMongoDbHelper mongoDbHelper;

    @Override
    public ProducerConfig findProducerConfById(String configId) {
        return mongoDbHelper.findById(COLLECTION_NAME,configId, ProducerConfig.class);
    }

    @Override
    public void updateLastProducedTimestamp(String configId, long lastProducedTimestamp) {
        mongoDbHelper.updateById(COLLECTION_NAME,configId, Map.of(LAST_PRODUCED_TIMESTAMP_FIELD,lastProducedTimestamp));
    }
}
