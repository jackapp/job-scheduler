package com.fampay.scheduler.producer.queue;


import com.fampay.scheduler.models.queue.JobMessagePayload;

public interface JobQueue {
    boolean enqueueJobExecution(JobMessagePayload jobMessagePayload, String queueName);
}
