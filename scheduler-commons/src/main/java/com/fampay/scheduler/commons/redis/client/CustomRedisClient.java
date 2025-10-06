package com.fampay.scheduler.commons.redis.client;

import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import java.util.concurrent.TimeUnit;

public interface CustomRedisClient {
    /**
     * Fetch value for key from Redis. If key is not present, return null.
     * @param key String
     * @param cls Class
     * @param <T> Template
     * @return Template
     */
    <T> T get(String key, Class<T> cls) throws InternalLibraryException;

    /**
     * Fetch value for key from Redis. If key is not present, return null.
     * @param key String
     * @param cls Class
     * @param <T> Template
     * @return Template
     */
    <T> T get(String key, TypeReference<T> cls) throws InternalLibraryException;

    /**
     * Store (key, value) in redis
     * @param key String
     * @param value Object
     */
    void set(String key, Object value) throws InternalLibraryException;

    /**
     * Store (key, value) in redis with given TTL
     * @param key String
     * @param value Object
     * @param ttl int
     * @param timeUnit TimeUnit
     */
    void setWithTtl(String key, Object value, int ttl, TimeUnit timeUnit) throws InternalLibraryException;

    /**
     * Store (key, value) in redis
     * @param key String
     * @param value Object
     */
    boolean setIfNotPresent(String key, Object value) throws InternalLibraryException;

    /**
     * Store (key, value) in redis with given TTL
     * @param key String
     * @param value Object
     * @param ttl int
     * @param timeUnit TimeUnit
     */
    boolean setIfNotPresentWithTtl(String key, Object value, int ttl, TimeUnit timeUnit) throws InternalLibraryException;

    /**
     * Delete (key) from redis
     * @param key String
     */
    boolean deleteKey(String key)  throws InternalLibraryException;

    void stop();

    default RedissonClient getRedissonClient() throws InternalLibraryException {
        throw InternalLibraryException.childBuilder().message("Not implemented").build();
    }

    void publish(String topic, String message);

    void subscribe(String topic, MessageListener<String> listener);

    boolean isHealthy();

    boolean isEnabled();
}
