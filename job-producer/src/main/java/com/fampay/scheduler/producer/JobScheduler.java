package com.fampay.scheduler.producer;

import com.fampay.scheduler.commons.lock.client.CustomDistributedLock;
import com.fampay.scheduler.models.entity.ProducerConfig;
import com.fampay.scheduler.producer.utils.TimeUtils;
import com.fampay.scheduler.repository.ProducerConfigDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTimeUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class JobScheduler {

    private static final String JOB_PRODUCER_LOCK_ID = "job-producer-lock";

    private final JobProducer jobProducer;
    private final CustomDistributedLock customDistributedLock;
    private final ProducerConfigDao producerConfigDao;
    private final JobProducerConfig jobProducerConfig;

    @Scheduled(fixedRate = 10000L)
    public void scheduleJobFetching() {
        log.info("Job Scheduler is running");
        Optional<Lock> lock = customDistributedLock.acquireLockWithWait(JOB_PRODUCER_LOCK_ID, 1000L,true);
        if (lock.isPresent()) {
            ProducerConfig producerConfig = producerConfigDao.findProducerConfById(jobProducerConfig.getConfigId());
            long startTimestamp = getStartTimestamp(producerConfig,jobProducerConfig.getMaxHistoryAllowed());
            long endTimestamp = TimeUtils.getEndOfNextMinute(DateTimeUtils.currentTimeMillis());
            jobProducer.produceJobs(startTimestamp,endTimestamp,jobProducerConfig.getPageSize());
            producerConfigDao.updateLastProducedTimestamp(producerConfig.getConfigId(),DateTimeUtils.currentTimeMillis());
            lock.get().unlock();
        } else {
            log.info("Couldnt acquire lock hence ignoring this schedule");
        }
    }

    private long getStartTimestamp(ProducerConfig producerConfig,Long maxAllowedHistory) {
        Long currentTime = DateTimeUtils.currentTimeMillis();
        if (producerConfig==null || producerConfig.getLastProducedTimestamp()==null) {
            return currentTime;
        }
        // Keep this configurable, if the timestamp is more than
        if (currentTime-producerConfig.getLastProducedTimestamp()<=maxAllowedHistory) {
            return producerConfig.getLastProducedTimestamp();
        } else {
            return currentTime-maxAllowedHistory;
        }
    }

}
