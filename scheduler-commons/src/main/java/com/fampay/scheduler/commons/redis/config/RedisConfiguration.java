package com.fampay.scheduler.commons.redis.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.redisson.api.LocalCachedMapOptions.CacheProvider;
import org.redisson.api.LocalCachedMapOptions.EvictionPolicy;
import org.redisson.api.LocalCachedMapOptions.SyncStrategy;
import org.redisson.client.codec.Codec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "redis-config")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisConfiguration {
    private String prefix;
    private boolean enabled = false;
    private RedisMode redisMode = RedisMode.MASTER_SLAVE;
    private String url;
    private String username;
    private String password;
    private int threads = 8;
    private int nettyThreads = 8;
    private int timeout = -1;
    private int pingTime = -1;
    private int watchdogTimeoutInMillis = 30000;
    private NearCacheConfig nearCacheConfig = new NearCacheConfig();

    /**
     * <pre>
     * Examples:
     * org.redisson.codec.JsonJacksonCodec (Default)
     *
     * org.redisson.client.codec.StringCodec
     *
     */
    private String codecClass;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NearCacheConfig {
        private boolean enabled = false;
        private Long cacheSize = 0L;
        private Long timeToLive = 0L;
        private TimeUnit ttlTimeUnit = TimeUnit.MILLISECONDS;
        private Long maxIdleTime = 0L;
        private TimeUnit maxIdleTimeUnit = TimeUnit.MILLISECONDS;
        private CacheProvider cacheProvider = CacheProvider.REDISSON;
        private SyncStrategy syncStrategy = SyncStrategy.INVALIDATE;
        private EvictionPolicy evictionPolicy = EvictionPolicy.LFU;
    }

    public enum RedisMode {
        LOCAL,
        MASTER_SLAVE,
        CLUSTER
    }

    public Codec getCodec() {
        if (codecClass != null) {
            try {
                return (Codec) Class.forName(codecClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
