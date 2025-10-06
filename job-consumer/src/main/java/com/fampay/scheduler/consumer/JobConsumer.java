package com.fampay.scheduler.consumer;

public interface JobConsumer {
    void pollMessagesFromQueue(String queueName);
}
