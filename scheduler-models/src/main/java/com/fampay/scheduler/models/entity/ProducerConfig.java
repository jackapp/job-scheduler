package com.fampay.scheduler.models.entity;

import lombok.Data;

@Data
public class ProducerConfig {
    private String configId;
    private Long lastProducedTimestamp;
}
