package com.fampay.scheduler.consumer;


import com.fampay.scheduler.commons.queue.IMessageConsumer;
import com.fampay.scheduler.consumer.handler.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SqsJobConsumer implements JobConsumer {

    private final IMessageConsumer messageConsumer;
    private final MessageHandler messageHandler;

    @Async
    public void pollMessagesFromQueue(String queueName) {
        List<IMessageConsumer.MessageValue> messageValues = messageConsumer.receiveMessage(queueName,String.class);
        for (IMessageConsumer.MessageValue messageValue : messageValues) {
            try {
                log.info("Message consumed from sqs: {}", messageValue.getMessage());
                log.info("Message processed successfully, hence will delete from queue.");
                messageHandler.handleMessage(messageValue.getMessage().toString(),messageValue.getReceiptHandle(),queueName);
                log.info("Message deleted from queue.");
            } catch (Exception e) {
                log.error("Issue with the payload : {}",messageValue,e);
                messageConsumer.deleteMessage(queueName,messageValue.getReceiptHandle());
            }
        }
    }
}
