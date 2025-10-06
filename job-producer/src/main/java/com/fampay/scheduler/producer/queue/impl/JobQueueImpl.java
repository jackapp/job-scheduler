package com.fampay.scheduler.producer.queue.impl;

import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.commons.queue.IMessageProducer;
import com.fampay.scheduler.models.queue.JobMessagePayload;
import com.fampay.scheduler.producer.queue.JobQueue;
import com.fampay.scheduler.producer.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTimeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobQueueImpl implements JobQueue {

    private final IMessageProducer iMessageProducer;

    @Override
    public boolean enqueueJobExecution(JobMessagePayload jobMessagePayload,String queueName) {
        try {
            String message = CommonSerializationUtil.writeString(jobMessagePayload);
            return iMessageProducer.sendMessageWithDelay(queueName,message,calculateDelay(jobMessagePayload.getScheduledRunAt()));
        } catch (Exception e) {
            log.error("Couldnt produce message to queue for payload :{}",jobMessagePayload.getExecutionId(),e);
            return false;
        }
    }

    /**
     * Calculates the delay in seconds for an SQS message based on a scheduled timestamp.
     * Valid delay is clamped between 0 and 900 seconds as per SQS constraints.
     *
     * @param scheduledTimestamp the future timestamp in milliseconds (UTC)
     *                           when the message should be visible
     * @return delay in seconds (0 if already due or past)
     */
    private Integer calculateDelay(long scheduledTimestamp) {
        return TimeUtils.calculateDelay(scheduledTimestamp);
    }
}
