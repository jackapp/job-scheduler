package com.fampay.scheduler.commons.queue.config;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.regions.Regions.AP_SOUTH_1;

@Configuration
@ConfigurationProperties(prefix = "sqs-config")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqsConfiguration {
    private String accessKey = "";
    private String secretAccessKey = "";
    private Regions region = AP_SOUTH_1;
    private Map<String, MessageQueue> queues = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageQueue {
        private String topicQueueUrl;
        private String topicName;
        private QueueType queueType = QueueType.STANDARD;
        private ConsumerSetting consumerSetting = new ConsumerSetting();
    }

    public enum QueueType {
        STANDARD, FIFO
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumerSetting {
        Integer maxNumberOfMessages = 1;
        Integer waitTimeInSeconds;
    }
}