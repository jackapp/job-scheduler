package com.fampay.scheduler.consumer.handler;

public interface MessageHandler<T> {
    void handleMessage(T message,String messageId,String queueName);
}
