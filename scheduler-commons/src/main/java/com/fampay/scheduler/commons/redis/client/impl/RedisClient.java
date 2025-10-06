package com.fampay.scheduler.commons.redis.client.impl;

import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.commons.redis.client.CustomRedisClient;
import com.fampay.scheduler.commons.redis.config.RedisConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.NameMapper;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.client.codec.Codec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.ReplicatedServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.PREFIX_MISSING;
import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.REDIS_NOT_STARTED;


@Slf4j
@Component
public class RedisClient implements CustomRedisClient, DisposableBean {

    private final RedisConfiguration redisConfig;
    private RedissonClient redissonClient;
    private RLocalCachedMap<String, Object> nearCache;

    @Autowired
    public RedisClient(RedisConfiguration redisConfig) throws InternalLibraryException {
        this.redisConfig = redisConfig;
        startIfApplicable();
    }

    private void startIfApplicable() {
        if (!redisConfig.isEnabled()) {
            log.debug("RedisClient not enabled");
            return;
        }
        if(StringUtils.isBlank(redisConfig.getPrefix())) {
            throw InternalLibraryException.childBuilder().message(PREFIX_MISSING)
                    .displayMessage("Prefix mandatory in config").build();
        }
        Config config = new Config();
        if(redisConfig.getRedisMode() == RedisConfiguration.RedisMode.LOCAL) {
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(redisConfig.getUrl());
            if (!StringUtils.isBlank(redisConfig.getUsername()) && !StringUtils.isBlank(redisConfig.getPassword())) {
                singleServerConfig.setUsername(redisConfig.getUsername());
                singleServerConfig.setPassword(redisConfig.getPassword());
            }
            singleServerConfig.setNameMapper(getNameMapper());
        } else if (redisConfig.getRedisMode() == RedisConfiguration.RedisMode.CLUSTER) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            Arrays.stream(redisConfig.getUrl().split(","))
                    .forEach(clusterServersConfig::addNodeAddress);
            if (!StringUtils.isBlank(redisConfig.getUsername()) && !StringUtils.isBlank(redisConfig.getPassword())) {
                clusterServersConfig.setUsername(redisConfig.getUsername());
                clusterServersConfig.setPassword(redisConfig.getPassword());
            }
            clusterServersConfig.setNameMapper(getNameMapper());
        } else {
            ReplicatedServersConfig replicatedServersConfig = config.useReplicatedServers();
            Arrays.stream(redisConfig.getUrl().split(","))
                    .forEach(replicatedServersConfig::addNodeAddress);
            if (redisConfig.getPingTime() <= 0) {
                log.info("RedisClient - using ReplicatedServers without PingConnectionInterval");
            } else {
                log.info("RedisClient - using ReplicatedServers with PingConnectionInterval = {}", redisConfig.getPingTime());
                replicatedServersConfig.setPingConnectionInterval(redisConfig.getPingTime());
            }
            if(redisConfig.getTimeout() <= 0) {
                log.info("RedisClient - using default connection timeout for ReplicatedServers");
            } else {
                log.info("RedisClient - using ReplicatedServers with connection timeout = {}", redisConfig.getTimeout());
                replicatedServersConfig.setConnectTimeout(redisConfig.getTimeout());
            }

            if (!StringUtils.isBlank(redisConfig.getUsername()) && !StringUtils.isBlank(redisConfig.getPassword())) {
                replicatedServersConfig.setUsername(redisConfig.getUsername());
                replicatedServersConfig.setPassword(redisConfig.getPassword());
            }
            replicatedServersConfig.setNameMapper(getNameMapper());
        }

        // Setup custom codec if it is defined
        Codec codec = redisConfig.getCodec();
        if (codec != null) {
            config.setCodec(codec);
        }

        if (redisConfig.getThreads() > 0) {
            config.setThreads(redisConfig.getThreads());
        }

        if (redisConfig.getNettyThreads() > 0) {
            config.setNettyThreads(redisConfig.getNettyThreads());
        }

        if (redisConfig.getWatchdogTimeoutInMillis() > 0) {
            config.setLockWatchdogTimeout(redisConfig.getWatchdogTimeoutInMillis());
        }
        redissonClient = Redisson.create(config);

        setupNearCacheIfApplicable(redisConfig.getNearCacheConfig());
    }

    private void setupNearCacheIfApplicable(RedisConfiguration.NearCacheConfig nearCacheConfig) {
        if(nearCacheConfig.isEnabled()) {
            LocalCachedMapOptions<String, Object> options =  LocalCachedMapOptions.defaults();
            options.syncStrategy(nearCacheConfig.getSyncStrategy());
            options.cacheProvider(nearCacheConfig.getCacheProvider());
            options.timeToLive(nearCacheConfig.getTimeToLive(), nearCacheConfig.getTtlTimeUnit());
            options.maxIdle(nearCacheConfig.getMaxIdleTime(), nearCacheConfig.getMaxIdleTimeUnit());
            options.evictionPolicy(nearCacheConfig.getEvictionPolicy());
            nearCache = redissonClient.getLocalCachedMap("local-cache", options);
        }
    }

    @Override
    public <T> T get(String key, Class<T> cls) throws InternalLibraryException {
        ensureClientIsRunning();
        String value = (String) redissonClient.getBucket(key).get();
        if(StringUtils.isEmpty(value) || cls == String.class) {
            return (T) value;
        } else {
            return CommonSerializationUtil.readObject(value, cls);
        }
    }

    @Override
    public <T> T get(String key, TypeReference<T> cls) throws InternalLibraryException {
        ensureClientIsRunning();
        String value = (String) redissonClient.getBucket(key).get();
        if(StringUtils.isEmpty(value)) {
            return null;
        } else {
            return CommonSerializationUtil.readObject(value, cls);
        }
    }

    @Override
    public void set(String key, Object value) throws InternalLibraryException {
        ensureClientIsRunning();
        redissonClient.getBucket(key).set(CommonSerializationUtil.writeString(value));

    }

    @Override
    public void setWithTtl(String key, Object value, int ttl, TimeUnit timeUnit) throws InternalLibraryException {
        ensureClientIsRunning();
        redissonClient.getBucket(key).set(CommonSerializationUtil.writeString(value), ttl, timeUnit);
    }

    @Override
    public boolean setIfNotPresent(String key, Object value) throws InternalLibraryException {
        ensureClientIsRunning();
        return redissonClient.getBucket(key).trySet(CommonSerializationUtil.writeString(value));
    }

    @Override
    public boolean setIfNotPresentWithTtl(String key, Object value, int ttl, TimeUnit timeUnit) throws InternalLibraryException {
        ensureClientIsRunning();
        return redissonClient.getBucket(key).trySet(CommonSerializationUtil.writeString(value), ttl, timeUnit);
    }

    @Override
    public boolean deleteKey(String key) throws InternalLibraryException {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public void stop() {
        if(Objects.nonNull(redissonClient)) {
            log.info("Shutting down Redis");
            redissonClient.shutdown();
            redissonClient = null;
        }
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    @Override
    public void publish(String topic, String message) {
        log.info("Publishing message: {} to topic: {}", message, topic);
        RTopic redissonTopic =  redissonClient.getTopic(topic);
        long clientsReceivedMessage = redissonTopic.publish(message);
        log.info("Message published. Received by clients(count): {}", clientsReceivedMessage);
    }

    @Override
    public void subscribe(String topic, MessageListener<String> listener) {
        log.info("Subscribing to redis pub/sub topic: {}", topic);
        RTopic redissonTopic =  redissonClient.getTopic(topic);
        redissonTopic.addListener(String.class, listener);
    }

    @Override
    public boolean isHealthy() {
        boolean healthy;
        if(redisConfig.getRedisMode() == RedisConfiguration.RedisMode.LOCAL) {
            healthy = redissonClient.getRedisNodes(RedisNodes.SINGLE).pingAll();
        } else if (redisConfig.getRedisMode() == RedisConfiguration.RedisMode.CLUSTER) {
            healthy = redissonClient.getRedisNodes(RedisNodes.CLUSTER).pingAll();
        } else {
            healthy = redissonClient.getRedisNodes(RedisNodes.MASTER_SLAVE).pingAll();
        }
        return healthy;
    }

    @Override
    public boolean isEnabled() {
        return redisConfig.isEnabled();
    }

    private void ensureClientIsRunning() throws InternalLibraryException {
        if (Objects.isNull(redissonClient)) {
            throw InternalLibraryException.childBuilder().message(REDIS_NOT_STARTED)
                    .displayMessage("Redis client is not started").build();
        }
    }

    private NameMapper getNameMapper(){
        String DELIMITER = ":";
        return new NameMapper() {
            @Override
            public String map(String name) {
                return redisConfig.getPrefix() + DELIMITER + name;
            }
            @Override
            public String unmap(String name) {
                return name.split(DELIMITER)[1];
            }
        };
    }

    @Override
    public void destroy() {
        stop();
    }
}
