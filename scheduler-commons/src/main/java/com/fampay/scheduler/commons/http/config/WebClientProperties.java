package com.fampay.scheduler.commons.http.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "webclient")
@Data
public class WebClientProperties {
    /**
     * Maximum number of total connections.
     */
    private int maxConnections = 1000;

    /**
     * Maximum number of pending connection acquisitions.
     */
    private int pendingAcquireMaxCount = 10000;

    /**
     * Maximum time to wait for acquiring a connection (in seconds).
     */
    private int pendingAcquireTimeoutSeconds = 45;

    /**
     * Maximum idle time of a connection (in seconds).
     */
    private int maxIdleTimeSeconds = 100;

    /**
     * Maximum lifetime of a connection (in seconds).
     */
    private int maxLifeTimeSeconds = 100;

    /**
     * Time interval for background eviction of idle connections (in seconds).
     */
    private int evictInBackgroundSeconds = 120;

    /**
     * TCP connect timeout (in milliseconds).
     */
    private int connectTimeoutMillis = 2000;

    /**
     * HTTP client response timeout (in seconds).
     */
    private int responseTimeoutSeconds = 120;

    /**
     * Whether to enable TCP keep-alive.
     */
    private boolean tcpKeepAlive = true;

    /**
     * Whether to enable TCP_NODELAY (disable Nagle’s algorithm).
     */
    private boolean tcpNoDelay = true;

}