package com.fampay.scheduler.producer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "job-producer-config")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobProducerConfig {
    private String configId;
    private Integer pageSize=60;
}
