package com.fampay.scheduler.commons.queue.impl;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fampay.scheduler.commons.exception.InternalLibraryException;
import com.fampay.scheduler.commons.helper.utils.CommonSerializationUtil;
import com.fampay.scheduler.commons.queue.IMessageConsumer;
import com.fampay.scheduler.commons.queue.config.SqsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.CONFIG_MISSING;
import static com.fampay.scheduler.commons.exception.LibraryErrorMessages.UNKNOWN_ERROR;

@Slf4j
@Component
@Qualifier("sqs")
public class SqsMessageConsumer implements IMessageConsumer, DisposableBean {

    private AmazonSQS sqsClient;
    private final SqsConfiguration config;

    @Autowired
    public SqsMessageConsumer(SqsConfiguration config) {
        this.config = config;
        start();
    }

    @Override
    public void start() {
        if(Objects.isNull(config.getQueues()) || config.getQueues().isEmpty()) {
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
    public <T> List<MessageValue> receiveMessage(String queueName, Class<T> cls) throws InternalLibraryException {
        return receiveGenericMessage(queueName, cls).stream().map(message -> MessageValue.builder()
                .receiptHandle(message.getReceiptHandle())
                .message(message.getMessage())
                .build()).collect(Collectors.toList());
    }

    @Override
    public <T> List<MessageValue<T>> receiveGenericMessage(String queueName, Class<T> cls) throws InternalLibraryException {
        SqsConfiguration.MessageQueue queueConfig = validateAndGetConfig(queueName);

        ReceiveMessageRequest consumerRequest = new ReceiveMessageRequest()
                .withQueueUrl(queueConfig.getTopicQueueUrl())
                .withMessageAttributeNames("All");
        consumerRequest.setMaxNumberOfMessages(queueConfig.getConsumerSetting().getMaxNumberOfMessages());
        if(Objects.nonNull(queueConfig.getConsumerSetting().getWaitTimeInSeconds())) {
            consumerRequest.setWaitTimeSeconds(queueConfig.getConsumerSetting().getWaitTimeInSeconds());
        }

        List<MessageValue<T>> mappedMessages = new ArrayList<>();
        try {
            ReceiveMessageResult result = sqsClient.receiveMessage(consumerRequest);
            List<Message> messages = result.getMessages();

            for(Message message: messages) {
                log.debug("Message received: {}", message.getBody());
                mappedMessages.add(MessageValue.<T>builder()
                        .rawMessage(message)
                        .message(CommonSerializationUtil.readObject(message.getBody(), cls))
                        .receiptHandle(message.getReceiptHandle())
                        .build());
            }
        } catch (Exception ex) {
            log.error("Error in fetching data from queue", ex);
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage(ex.getMessage()).build();
        }
        return mappedMessages;
    }

    @Override
    public boolean deleteMessage(String queueName, String identifier) throws InternalLibraryException {
        SqsConfiguration.MessageQueue queueConfig = validateAndGetConfig(queueName);

        boolean messageDeleted = true;
        try {
            DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest()
                    .withQueueUrl(queueConfig.getTopicQueueUrl()).withReceiptHandle(identifier);
            DeleteMessageResult result = sqsClient.deleteMessage(deleteMessageRequest);
            log.debug("Delete Message Result: {}", result);
            if(result.getSdkHttpMetadata().getHttpStatusCode() >= 400) {
                messageDeleted = false;
            }
        } catch (Exception ex) {
            log.error("Error deleting queue:{}, identifier:{}", queueName, identifier, ex);
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage(ex.getMessage()).build();
        }

        return messageDeleted;
    }

    @Override
    public boolean purgeQueue(String queueName) throws InternalLibraryException {
        SqsConfiguration.MessageQueue queueConfig = validateAndGetConfig(queueName);

        boolean queuePurged = true;
        try {
            PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest().withQueueUrl(queueConfig.getTopicQueueUrl());
            PurgeQueueResult result = sqsClient.purgeQueue(purgeQueueRequest);
            log.debug("Purge Queue Result: {}", result);
            if (result.getSdkHttpMetadata().getHttpStatusCode() >= 400) {
                queuePurged = false;
            }
        } catch (Exception ex) {
            log.error("Error purging queue:{}", queueName, ex);
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage(ex.getMessage()).build();
        }

        return queuePurged;
    }

    @Override
    public long getApproxMessages(String queueName) throws InternalLibraryException {
        SqsConfiguration.MessageQueue queueConfig = validateAndGetConfig(queueName);

        GetQueueAttributesResult result;
        try {
            GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest()
                    .withQueueUrl(queueConfig.getTopicQueueUrl()).withAttributeNames(QueueAttributeName.ApproximateNumberOfMessages);
            result = sqsClient.getQueueAttributes(queueAttributesRequest);
        } catch (Exception ex) {
            log.error("Error getting approximate number of Messages Result: {}", queueName, ex);
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage(ex.getMessage()).build();
        }
        if(result.getSdkHttpMetadata().getHttpStatusCode() != 200) {
            log.error("Error getting approximate number of Messages Result, Http Status Code: {}",
                    result.getSdkHttpMetadata().getHttpStatusCode());
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage("SQS Client response is not success").build();
        }
        log.debug("Approximate number of Messages Result: {}", result);
        long approxMessages = Long.parseLong(result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessages.toString()));
        return approxMessages;
    }

    @Override
    public long getTotalApproxMessages(String queueName) throws InternalLibraryException {
        SqsConfiguration.MessageQueue queueConfig = validateAndGetConfig(queueName);

        GetQueueAttributesResult result;
        try {
            GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest()
                    .withQueueUrl(queueConfig.getTopicQueueUrl()).withAttributeNames(QueueAttributeName.ApproximateNumberOfMessages,
                            QueueAttributeName.ApproximateNumberOfMessagesNotVisible, QueueAttributeName.ApproximateNumberOfMessagesDelayed);
            result = sqsClient.getQueueAttributes(queueAttributesRequest);
        } catch (Exception ex) {
            log.error("Error getting total approximate number of Messages Result: {}", queueName, ex);
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage(ex.getMessage()).build();
        }
        if (result.getSdkHttpMetadata().getHttpStatusCode() != 200) {
            log.error("Error getting total approximate number of Messages Result, Http Status Code: {}",
                    result.getSdkHttpMetadata().getHttpStatusCode());
            throw InternalLibraryException.childBuilder().message(UNKNOWN_ERROR)
                    .displayMessage("SQS Client response is not success").build();
        }
        log.debug("Approx number of messages result: {}", result);
        long approxMessages = Long.parseLong(result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessages.toString()));
        approxMessages += Long.parseLong(result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessagesNotVisible.toString()));
        approxMessages += Long.parseLong(result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessagesDelayed.toString()));
        return approxMessages;
    }

    @Override
    public void stop() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    private SqsConfiguration.MessageQueue validateAndGetConfig(String queueName) throws InternalLibraryException {
        if(!config.getQueues().containsKey(queueName)) {
            throw InternalLibraryException.childBuilder().message(CONFIG_MISSING)
                    .displayMessage("Queue Config missing").build();
        }
        return config.getQueues().get(queueName);
    }
}
