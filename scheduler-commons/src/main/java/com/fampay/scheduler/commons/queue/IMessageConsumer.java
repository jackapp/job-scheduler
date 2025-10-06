package com.fampay.scheduler.commons.queue;


import com.amazonaws.services.sqs.model.Message;
import com.fampay.scheduler.commons.exception.InternalLibraryException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public interface IMessageConsumer {

    /**
     * Start consumer
     */
    void start();

    long getApproxMessages(String queueName) throws InternalLibraryException;

    long getTotalApproxMessages(String queueName) throws InternalLibraryException;

    /**
     * Stop consumer
     */
    void stop();
    /**
     * Consume messages
     */
    <T> List<MessageValue> receiveMessage(String queueName, Class<T> cls) throws InternalLibraryException;

    <T> List<MessageValue<T>> receiveGenericMessage(String queueName, Class<T> cls) throws InternalLibraryException;

    boolean deleteMessage(String queueName, String identifier) throws InternalLibraryException;

    boolean purgeQueue(String queueName) throws InternalLibraryException;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class MessageValue<T> {
        private Message rawMessage;
        private T message;
        private String receiptHandle;
    }
}

