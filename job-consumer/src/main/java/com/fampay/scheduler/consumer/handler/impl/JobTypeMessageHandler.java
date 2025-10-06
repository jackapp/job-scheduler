package com.fampay.scheduler.consumer.handler.impl;

import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.consumer.JobTypeProcessor;
import com.fampay.scheduler.consumer.JobTypeProcessorFactory;
import com.fampay.scheduler.consumer.handler.MessageHandler;
import com.fampay.scheduler.models.queue.JobMessagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobTypeMessageHandler implements MessageHandler<String> {

    private final JobTypeProcessorFactory jobTypeProcessorFactory;

    @Override
    public void handleMessage(String message, String messageId,String queueName) {
        JobMessagePayload jobMessagePayload = CommonSerializationUtil.readObject(message, JobMessagePayload.class);
        JobTypeProcessor jobTypeProcessor = jobTypeProcessorFactory.getProcessor(jobMessagePayload.getType());
        jobTypeProcessor.processJobExecution(jobMessagePayload, messageId,queueName);
    }
}
