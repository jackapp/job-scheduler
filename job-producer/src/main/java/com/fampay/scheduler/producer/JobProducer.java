package com.fampay.scheduler.producer;

public interface JobProducer {
    void produceJobs(long startTime, long endTime,Integer pageSize);
}