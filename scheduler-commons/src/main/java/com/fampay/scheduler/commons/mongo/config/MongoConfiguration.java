package com.fampay.scheduler.commons.mongo.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mongo-config")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MongoConfiguration {
    private String name;
    private String address;
    private Integer connectTimeoutMS = 10000;
    private Integer readTimeoutMS = 10000;
    private Integer writeTimeoutMS = 1000;
    private Integer maxPoolSize = 100;
    private Integer minPoolSize = 10;
    private Integer maxConnectionIdleTimeMS = 10000;
    private boolean journalEnabled = true;
    private String writeConcernLevel = "majority";
    private String readConcernLevel = "majority";
    private String readPreference = "primary";
    private String database;
    private String username;
    private String password;
    private boolean srvMode = false;
}