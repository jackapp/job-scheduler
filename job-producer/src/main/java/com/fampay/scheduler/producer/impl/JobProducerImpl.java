package com.fampay.scheduler.producer.impl;

import com.fampay.scheduler.commons.helper.utils.CronUtilsHelper;
import com.fampay.scheduler.producer.JobProducer;
import com.fampay.scheduler.producer.adapter.MessageAdapter;
import com.fampay.scheduler.producer.queue.JobQueue;
import com.fampay.scheduler.repository.JobDao;
import com.fampay.scheduler.repository.JobExecutionDao;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.models.entity.PagedJobs;
import com.fampay.scheduler.producer.utils.JobExecutionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProducerImpl implements JobProducer {

    private static final String QUEUE_NAME = "local-job-queue";

    private final JobDao jobDao;
    private final JobExecutionDao jobExecutionDao;
    private final JobQueue jobQueue;

    @Override
    public void produceJobs(long startTime, long endTime,Integer pageSize) {
        pageSize = pageSize!=null?pageSize:60;
        Long nextTimeOffset=null;
        String nextJobId=null;
        PagedJobs pagedJobs;
        do {
            pagedJobs = jobDao.getJobsPaginated(startTime,endTime,pageSize,nextTimeOffset,nextJobId);
            nextTimeOffset = pagedJobs.getNextScheduledTimestamp();
            nextJobId = pagedJobs.getNextJobId();
            for (JobEntity jobEntity : pagedJobs.getJobEntities()) {
                try {
                    Long timestampToStartScheduling = determineTimeToSchedule(jobEntity.getNextScheduledTime(),startTime,jobEntity.getSchedule());
                    if (timestampToStartScheduling>endTime) {
                        log.info("Repair the jobs to correct their next schedule as we skip the previous schedules {}",jobEntity.getId());
                        jobDao.updateNextRunForJob(jobEntity.getId(),timestampToStartScheduling);
                    } else if (timestampToStartScheduling==-1) {
                        log.info("Ignoring next run for the job :{} as nextScheduledTimestamp is -1",jobEntity.getId());
                    } else {
                        List<JobExecutionEntity> jobExecutionEntities = createJobExecutions(jobEntity,timestampToStartScheduling,endTime);
                        for (JobExecutionEntity jobExecutionEntity : jobExecutionEntities) {
                            boolean success = jobQueue.enqueueJobExecution(MessageAdapter.fromJobProduceData(jobExecutionEntity,jobEntity),QUEUE_NAME);
                            if (!success) {
                                //Send this message to DLQ in future releases.
                                log.error("Message failed to be produced to queue for jobid:{} and execution :{}",jobExecutionEntity.getJobId(),jobExecutionEntity.getExecutionId());
                            }
                        }
                        Long nextTimestamp = CronUtilsHelper.getNextRunInGMTFromStartTime(jobEntity.getSchedule(),endTime);
                        jobDao.updateNextRunForJob(jobEntity.getId(),nextTimestamp);
                    }
                } catch (Exception e) {
                    log.error("Unable to produce job with id :{}",jobEntity.getId(),e);
                }
            }
        } while (!pagedJobs.getJobEntities().isEmpty());
    }

    /**
     * Note that this is an idempotent method, we are only creating the schedule if it's not created already.
     * @param jobEntity Entity for which executions need to be created
     * @param startTimestamp Timestamp from which executions need to be created
     * @param endTimestamp Timestamp till executions need to be created
     * @return
     */
    private List<JobExecutionEntity> createJobExecutions(JobEntity jobEntity,Long startTimestamp, Long endTimestamp) {
        List<JobExecutionEntity> jobExecutionEntities = JobExecutionUtil.generate(jobEntity,startTimestamp,endTimestamp);
        jobExecutionDao.createMultipleJobExecutions(jobExecutionEntities);
        return jobExecutionEntities;
    }

    private Long determineTimeToSchedule(Long nextTimeToRun, Long producerStartTime,String jobSchedule) {
        return CronUtilsHelper.determineTimeToSchedule(nextTimeToRun,producerStartTime,jobSchedule);
    }
}
