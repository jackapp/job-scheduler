package com.fampay.scheduler.commons.queue;

import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;

import java.util.UUID;

public interface IMessageProducer {

    boolean sendMessageWithDelay(String queueName,String message,Integer delayInMs);
    /**
     * Send a message
     */
    boolean send(String queueName, String key, String deduplicationId, String message) throws InternalLibraryException;

    /**
     * Send a message
     */
    default boolean send(String queueName, String key, Object message) throws InternalLibraryException {
        return send(queueName, key, UUID.randomUUID().toString(), CommonSerializationUtil.writeString(message));
    }

    /**
     * Send a message
     */
    default boolean send(String queueName, Object message) throws InternalLibraryException {
        return send(queueName, UUID.randomUUID().toString(), UUID.randomUUID().toString(), CommonSerializationUtil.writeString(message));
    }

    /**
     * Start producer
     */
    void start();

    /**
     * Stop producer
     */
    void stop();

}