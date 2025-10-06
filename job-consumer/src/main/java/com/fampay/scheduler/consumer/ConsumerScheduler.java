package com.fampay.scheduler.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
@RequiredArgsConstructor
public class ConsumerScheduler {

    private final JobConsumer jobConsumer;
    private static final String QUEUE_NAME = "local-job-queue";

    @Scheduled(fixedRate = 100L)
    public void consume(){
        jobConsumer.pollMessagesFromQueue(QUEUE_NAME);
    }
}
