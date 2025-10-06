package com.fampay.scheduler.commons.queue.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.queue.IMessageProducer;
import com.fampay.scheduler.commons.queue.config.SqsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.CONFIG_MISSING;
import static com.fampay.scheduler.commons.queue.config.SqsConfiguration.QueueType.FIFO;

@Slf4j
@Component
@Qualifier("sqs")
public class SqsMessageProducer implements IMessageProducer, DisposableBean {

    private AmazonSQS sqsClient;
    private final SqsConfiguration config;

    @Autowired
    public SqsMessageProducer(SqsConfiguration config) {
        this.config = config;
        start();
    }

    @Override
    public void start() {
        if(Objects.isNull(config.getQueues()) || config.getQueues().size() == 0) {
            log.error("No queue configured for SQS. So skipping SQS startup.");
            return;
        }

        if(StringUtils.isNotBlank(config.getAccessKey()) || StringUtils.isNotBlank(config.getSecretAccessKey())) {
            AWSCredentials credentials = new BasicAWSCredentials(config.getAccessKey(), config.getSecretAccessKey());
            sqsClient = AmazonSQSClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(config.getRegion())
                    .build();
        } else {
            sqsClient = AmazonSQSClientBuilder.standard()
                    .withRegion(config.getRegion())
                    .build();
        }
    }

    @Override
    public boolean sendMessageWithDelay(String queueName,String message, Integer delayInSeconds) {
        if(!config.getQueues().containsKey(queueName)) {
            throw InternalLibraryException.childBuilder().message(CONFIG_MISSING)
                    .displayMessage("Queue Config missing").build();
        }
        boolean messageSent = true;
        SqsConfiguration.MessageQueue queueConfig = config.getQueues().get(queueName);
        SendMessageRequest publishRequest = new SendMessageRequest(queueConfig.getTopicQueueUrl(), message);
        publishRequest.setDelaySeconds(delayInSeconds);

        try {
            SendMessageResult publishResult = sqsClient.sendMessage(publishRequest);
            log.info("Message result received: {}", publishRequest);
            if(publishResult.getSdkHttpMetadata().getHttpStatusCode() >= 400) {
                log.error("Failed to send message");
                messageSent = false;
            }
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            messageSent = false;
        }
        return messageSent;
    }

    @Override
    public boolean send(String queueName, String key, String deduplicationId, String message) throws InternalLibraryException {

        if(!config.getQueues().containsKey(queueName)) {
            throw InternalLibraryException.childBuilder().message(CONFIG_MISSING)
                    .displayMessage("Queue Config missing").build();
        }
        boolean messageSent = true;
        SqsConfiguration.MessageQueue queueConfig = config.getQueues().get(queueName);
        SendMessageRequest publishRequest = new SendMessageRequest(queueConfig.getTopicQueueUrl(), message);


        if(Objects.equals(queueConfig.getQueueType(), FIFO)) {
            publishRequest.setMessageGroupId(key);
            publishRequest.setMessageDeduplicationId(deduplicationId);
        }
        try {
            SendMessageResult publishResult = sqsClient.sendMessage(publishRequest);
            log.info("Message result received: {}", publishRequest);
            if(publishResult.getSdkHttpMetadata().getHttpStatusCode() >= 400) {
                log.error("Failed to send message");
                messageSent = false;
            }
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            messageSent = false;
        }

        return messageSent;
    }

    @Override
    public void stop() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
