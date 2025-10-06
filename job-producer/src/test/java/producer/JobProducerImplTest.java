package producer;

import com.fampay.scheduler.commons.helper.utils.CronUtilsHelper;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.models.entity.PagedJobs;
import com.fampay.scheduler.models.queue.JobMessagePayload;
import com.fampay.scheduler.producer.adapter.MessageAdapter;
import com.fampay.scheduler.producer.impl.JobProducerImpl;
import com.fampay.scheduler.producer.queue.JobQueue;
import com.fampay.scheduler.producer.utils.JobExecutionUtil;
import com.fampay.scheduler.repository.JobDao;
import com.fampay.scheduler.repository.JobExecutionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobProducerImplTest {

    @Mock
    private JobDao jobDao;

    @Mock
    private JobExecutionDao jobExecutionDao;

    @Mock
    private JobQueue jobQueue;

    @InjectMocks
    private JobProducerImpl jobProducer;

    private static final long START_TIME = 1000L;
    private static final long END_TIME = 2000L;
    private static final int PAGE_SIZE = 5;

    private JobMessagePayload mockPayload;

    @BeforeEach
    void setUp() {
        mockPayload = mock(JobMessagePayload.class);
    }


    @Test
    void produceJobs_shouldProcessSinglePageOfJobs_whenAllJobsAreValid() {
        // Arrange
        JobEntity job1 = createJobEntity("job1", 1500L, "0 0 * * *");
        JobEntity job2 = createJobEntity("job2", 1600L, "0 0 * * *");

        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job1, job2), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs);

        List<JobExecutionEntity> executions = Arrays.asList(createJobExecutionEntity("exec1", "job1"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(any(JobEntity.class), anyLong(), anyLong()))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime(anyString(), anyLong()))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), any(JobEntity.class)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(any(JobMessagePayload.class))).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobDao, times(2)).getJobsPaginated(anyLong(), anyLong(), anyInt(), any(), any());
            verify(jobExecutionDao, times(2)).createMultipleJobExecutions(anyList());
            verify(jobQueue, times(2)).enqueueJobExecution(any(JobMessagePayload.class));
            verify(jobDao, times(2)).updateNextRunForJob(anyString(), eq(3000L));
        }
    }

    @Test
    void produceJobs_shouldHandleMultiplePages_whenJobsSpanMultiplePages() {
        // Arrange
        JobEntity job1 = createJobEntity("job1", 1500L, "0 0 * * *");
        JobEntity job2 = createJobEntity("job2", 1600L, "0 0 * * *");
        JobEntity job3 = createJobEntity("job3", 1700L, "0 0 * * *");

        PagedJobs page1 = createPagedJobs(Arrays.asList(job1, job2), 1600L, "job2");
        PagedJobs page2 = createPagedJobs(Arrays.asList(job3), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, 2, null, null))
                .thenReturn(page1);
        when(jobDao.getJobsPaginated(START_TIME, END_TIME, 2, 1600L, "job2"))
                .thenReturn(page2, emptyPage);

        List<JobExecutionEntity> executions = Arrays.asList(createJobExecutionEntity("exec1", "job1"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(any(JobEntity.class), anyLong(), anyLong()))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime(anyString(), anyLong()))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), any(JobEntity.class)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(any(JobMessagePayload.class))).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, 2);

            // Assert
            verify(jobDao, times(2)).getJobsPaginated(anyLong(), anyLong(), anyInt(), any(), any());
            verify(jobExecutionDao, times(3)).createMultipleJobExecutions(anyList());
        }
    }

    // ==================== Job Repair Tests ====================

    @Test
    void produceJobs_shouldRepairJob_whenDeterminedTimestampExceedsEndTime() {
        // Arrange
        JobEntity job = createJobEntity("job1", 500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs, emptyPage);

        try (MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class)) {
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", START_TIME))
                    .thenReturn(2500L); // Greater than END_TIME

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobDao).updateNextRunForJob("job1", 2500L);
            verify(jobExecutionDao, never()).createMultipleJobExecutions(anyList());
            verify(jobQueue, never()).enqueueJobExecution(any(JobMessagePayload.class));
        }
    }

    @Test
    void produceJobs_shouldNotRepairJob_whenNextScheduledTimeIsAfterStartTime() {
        // Arrange
        JobEntity job = createJobEntity("job1", 1500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs, emptyPage);

        List<JobExecutionEntity> executions = Arrays.asList(createJobExecutionEntity("exec1", "job1"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(eq(job), eq(1500L), eq(END_TIME)))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", END_TIME))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), eq(job)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(mockPayload)).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobDao).updateNextRunForJob("job1", 3000L);
            verify(jobExecutionDao).createMultipleJobExecutions(executions);
        }
    }

    // ==================== Job Schedule Completion Tests ====================

    @Test
    void produceJobs_shouldSkipJob_whenDeterminedTimestampIsMinusOne() {
        // Arrange
        JobEntity job = createJobEntity("job1", 500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        try (MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class)) {
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", START_TIME))
                    .thenReturn(-1L);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobDao, never()).updateNextRunForJob(anyString(), anyLong());
            verify(jobExecutionDao, never()).createMultipleJobExecutions(anyList());
            verify(jobQueue, never()).enqueueJobExecution(any(JobMessagePayload.class));
        }
    }

    // ==================== Queue Failure Tests ====================

    @Test
    void produceJobs_shouldLogError_whenEnqueueFails() {
        // Arrange
        JobEntity job = createJobEntity("job1", 1500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        JobExecutionEntity execution = createJobExecutionEntity("exec1", "job1");
        List<JobExecutionEntity> executions = Arrays.asList(execution);

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(any(JobEntity.class), anyLong(), anyLong()))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime(anyString(), anyLong()))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(execution, job))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(mockPayload)).thenReturn(false);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobQueue).enqueueJobExecution(mockPayload);
            verify(jobDao).updateNextRunForJob("job1", 3000L);
            // Error should be logged (verify through logging framework if needed)
        }
    }

    @Test
    void produceJobs_shouldContinueProcessing_whenSomeEnqueueOperationsFail() {
        // Arrange
        JobEntity job = createJobEntity("job1", 1500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        JobExecutionEntity exec1 = createJobExecutionEntity("exec1", "job1");
        JobExecutionEntity exec2 = createJobExecutionEntity("exec2", "job1");
        List<JobExecutionEntity> executions = Arrays.asList(exec1, exec2);

        JobMessagePayload payload1 = mock(JobMessagePayload.class);
        JobMessagePayload payload2 = mock(JobMessagePayload.class);

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(any(JobEntity.class), anyLong(), anyLong()))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime(anyString(), anyLong()))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(exec1, job))
                    .thenReturn(payload1);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(exec2, job))
                    .thenReturn(payload2);

            when(jobQueue.enqueueJobExecution(payload1)).thenReturn(true);
            when(jobQueue.enqueueJobExecution(payload2)).thenReturn(false);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobQueue, times(2)).enqueueJobExecution(any(JobMessagePayload.class));
            verify(jobDao).updateNextRunForJob("job1", 3000L);
        }
    }

    // ==================== Multiple Executions Tests ====================

    @Test
    void produceJobs_shouldEnqueueMultipleExecutions_whenJobGeneratesMultipleExecutions() {
        // Arrange
        JobEntity job = createJobEntity("job1", 1500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        JobExecutionEntity exec1 = createJobExecutionEntity("exec1", "job1");
        JobExecutionEntity exec2 = createJobExecutionEntity("exec2", "job1");
        JobExecutionEntity exec3 = createJobExecutionEntity("exec3", "job1");
        List<JobExecutionEntity> executions = Arrays.asList(exec1, exec2, exec3);

        JobMessagePayload payload1 = mock(JobMessagePayload.class);
        JobMessagePayload payload2 = mock(JobMessagePayload.class);
        JobMessagePayload payload3 = mock(JobMessagePayload.class);

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(job, 1500L, END_TIME))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", END_TIME))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(exec1, job))
                    .thenReturn(payload1);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(exec2, job))
                    .thenReturn(payload2);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(exec3, job))
                    .thenReturn(payload3);

            when(jobQueue.enqueueJobExecution(any(JobMessagePayload.class))).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobQueue, times(3)).enqueueJobExecution(any(JobMessagePayload.class));
            verify(jobExecutionDao).createMultipleJobExecutions(executions);
            verify(jobDao).updateNextRunForJob("job1", 3000L);
        }
    }

    // ==================== Edge Cases ====================

    @Test
    void produceJobs_shouldHandleEmptyFirstPage_whenNoJobsExist() {
        // Arrange
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(emptyPage);

        // Act
        jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

        // Assert
        verify(jobDao, times(1)).getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null);
        verify(jobExecutionDao, never()).createMultipleJobExecutions(anyList());
        verify(jobQueue, never()).enqueueJobExecution(any(JobMessagePayload.class));
    }

    @Test
    void produceJobs_shouldHandleJobWithEmptyExecutionList_whenNoExecutionsGenerated() {
        // Arrange
        JobEntity job = createJobEntity("job1", 1500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        List<JobExecutionEntity> emptyExecutions = Collections.emptyList();

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(job, 1500L, END_TIME))
                    .thenReturn(emptyExecutions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", END_TIME))
                    .thenReturn(3000L);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobExecutionDao).createMultipleJobExecutions(emptyExecutions);
            verify(jobQueue, never()).enqueueJobExecution(any(JobMessagePayload.class));
            verify(jobDao).updateNextRunForJob("job1", 3000L);
        }
    }

    @Test
    void produceJobs_shouldHandleNextScheduledTimeEqualToStartTime_withoutRepair() {
        // Arrange
        JobEntity job = createJobEntity("job1", START_TIME, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        List<JobExecutionEntity> executions = Arrays.asList(createJobExecutionEntity("exec1", "job1"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(job, START_TIME, END_TIME))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", END_TIME))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), eq(job)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(mockPayload)).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobExecutionDao).createMultipleJobExecutions(executions);
            verify(jobQueue).enqueueJobExecution(mockPayload);
            verify(jobDao).updateNextRunForJob("job1", 3000L);
        }
    }

    @Test
    void produceJobs_shouldHandleDeterminedTimestampEqualToEndTime_withoutRepair() {
        // Arrange
        JobEntity job = createJobEntity("job1", 500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        List<JobExecutionEntity> executions = Arrays.asList(createJobExecutionEntity("exec1", "job1"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", START_TIME))
                    .thenReturn(END_TIME);
            executionUtil.when(() -> JobExecutionUtil.generate(job, END_TIME, END_TIME))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", END_TIME))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), eq(job)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(mockPayload)).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobExecutionDao).createMultipleJobExecutions(executions);
            verify(jobQueue).enqueueJobExecution(mockPayload);
            verify(jobDao).updateNextRunForJob("job1", 3000L);
        }
    }

    @Test
    void produceJobs_shouldProcessMixedScenarios_withRepairCompletedAndValidJobs() {
        // Arrange
        JobEntity repairJob = createJobEntity("repair-job", 500L, "0 0 * * *");
        JobEntity completedJob = createJobEntity("completed-job", 600L, "0 0 * * *");
        JobEntity validJob = createJobEntity("valid-job", 1500L, "0 0 * * *");

        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(repairJob, completedJob, validJob), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        List<JobExecutionEntity> validExecutions = Arrays.asList(createJobExecutionEntity("exec1", "valid-job"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            // Repair job - timestamp > endTime
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", START_TIME))
                    .thenReturn(2500L, -1L, 1500L);

            // Valid job processing
            executionUtil.when(() -> JobExecutionUtil.generate(validJob, 1500L, END_TIME))
                    .thenReturn(validExecutions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 * * *", END_TIME))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), eq(validJob)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(mockPayload)).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobDao).updateNextRunForJob("repair-job", 2500L);
            verify(jobDao).updateNextRunForJob("valid-job", 3000L);
            verify(jobExecutionDao, times(1)).createMultipleJobExecutions(anyList());
            verify(jobQueue, times(1)).enqueueJobExecution(mockPayload);
        }
    }

    @Test
    void produceJobs_shouldHandleNullNextOffsetAndJobId_onFirstPage() {
        // Arrange
        JobEntity job = createJobEntity("job1", 1500L, "0 0 * * *");
        PagedJobs pagedJobs = createPagedJobs(Arrays.asList(job), null, null);
        PagedJobs emptyPage = createPagedJobs(Collections.emptyList(), null, null);

        when(jobDao.getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        List<JobExecutionEntity> executions = Arrays.asList(createJobExecutionEntity("exec1", "job1"));

        try (MockedStatic<JobExecutionUtil> executionUtil = mockStatic(JobExecutionUtil.class);
             MockedStatic<CronUtilsHelper> cronHelper = mockStatic(CronUtilsHelper.class);
             MockedStatic<MessageAdapter> messageAdapter = mockStatic(MessageAdapter.class)) {

            executionUtil.when(() -> JobExecutionUtil.generate(any(JobEntity.class), anyLong(), anyLong()))
                    .thenReturn(executions);
            cronHelper.when(() -> CronUtilsHelper.getNextRunInGMTFromStartTime(anyString(), anyLong()))
                    .thenReturn(3000L);
            messageAdapter.when(() -> MessageAdapter.fromJobProduceData(any(JobExecutionEntity.class), any(JobEntity.class)))
                    .thenReturn(mockPayload);

            when(jobQueue.enqueueJobExecution(mockPayload)).thenReturn(true);

            // Act
            jobProducer.produceJobs(START_TIME, END_TIME, PAGE_SIZE);

            // Assert
            verify(jobDao).getJobsPaginated(START_TIME, END_TIME, PAGE_SIZE, null, null);
        }
    }

    // ==================== Helper Methods ====================

    private JobEntity createJobEntity(String id, Long nextScheduledTime, String schedule) {
        JobEntity job = new JobEntity();
        job.setId(id);
        job.setNextScheduledTime(nextScheduledTime);
        job.setSchedule(schedule);
        return job;
    }

    private JobExecutionEntity createJobExecutionEntity(String executionId, String jobId) {
        JobExecutionEntity execution = new JobExecutionEntity();
        execution.setExecutionId(executionId);
        execution.setJobId(jobId);
        return execution;
    }

    private PagedJobs createPagedJobs(List<JobEntity> jobs, Long nextTimestamp, String nextJobId) {
        PagedJobs pagedJobs = new PagedJobs();
        pagedJobs.setJobEntities(jobs);
        pagedJobs.setNextScheduledTimestamp(nextTimestamp);
        pagedJobs.setNextJobId(nextJobId);
        return pagedJobs;
    }
}