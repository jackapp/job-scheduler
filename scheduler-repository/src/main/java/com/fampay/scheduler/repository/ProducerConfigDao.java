package com.fampay.scheduler.repository;


import com.fampay.scheduler.models.entity.ProducerConfig;

public interface ProducerConfigDao {
    ProducerConfig findProducerConfById(String configId);
    void updateLastProducedTimestamp(String configId,long lastProducedTimestamp);
}
