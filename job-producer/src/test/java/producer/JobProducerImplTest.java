package producer;

import com.fampay.scheduler.models.dto.JobExecutionStatus;
import com.fampay.scheduler.models.entity.ApiConfigEntity;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.models.entity.PagedJobs;
import com.fampay.scheduler.producer.impl.JobProducerImpl;
import com.fampay.scheduler.producer.queue.JobQueue;
import com.fampay.scheduler.repository.JobDao;
import com.fampay.scheduler.repository.JobExecutionDao;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

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

    private static final long START_TIME = DateTimeUtils.currentTimeMillis();
    private static final long END_TIME = START_TIME+60000L;
    private static final String JOB_ID = "job-123";
    private static final String CRON_SCHEDULE = "*/1 * * * * ? *";

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void testProduceJobs_Success_EnqueuesJobExecutionsAndUpdatesNextRun() {
        // Arrange
        JobEntity jobEntity = createJobEntity(JOB_ID, START_TIME + 10, CRON_SCHEDULE);
        PagedJobs pagedJobs = PagedJobs.builder()
                .jobEntities(Collections.singletonList(jobEntity))
                .nextScheduledTimestamp(null)
                .nextJobId(null)
                .last(true)
                .build();
        PagedJobs emptyPage = PagedJobs.builder()
                .jobEntities(Collections.emptyList())
                .last(true)
                .build();

        when(jobDao.getJobsPaginated(eq(START_TIME), eq(END_TIME), eq(60), isNull(), isNull()))
                .thenReturn(pagedJobs);

        JobExecutionEntity execution = createJobExecutionEntity(JOB_ID, "exec-1");
        Mockito.doNothing().when(jobExecutionDao).createMultipleJobExecutions(anyList());

        when(jobQueue.enqueueJobExecution(any(), eq("local-job-queue")))
                .thenReturn(true);

        // Act
        jobProducer.produceJobs(START_TIME, END_TIME, 60);

        // Assert
        verify(jobDao, atLeastOnce()).getJobsPaginated(eq(START_TIME), eq(END_TIME), eq(60), isNull(), isNull());
        verify(jobQueue, atLeastOnce()).enqueueJobExecution(any(), eq("local-job-queue"));
        verify(jobDao, times(1)).updateNextRunForJob(eq(JOB_ID), anyLong());
    }

    @Test
    void testProduceJobs_WhenEnqueueFails_LogsErrorButContinuesProcessing() {
        // Arrange
        JobEntity jobEntity = createJobEntity(JOB_ID, START_TIME + 100, CRON_SCHEDULE);
        PagedJobs pagedJobs = PagedJobs.builder()
                .jobEntities(Collections.singletonList(jobEntity))
                .nextScheduledTimestamp(null)
                .nextJobId(null)
                .last(true)
                .build();
        PagedJobs emptyPage = PagedJobs.builder()
                .jobEntities(Collections.emptyList())
                .last(true)
                .build();

        when(jobDao.getJobsPaginated(anyLong(), anyLong(), anyInt(), any(), any()))
                .thenReturn(pagedJobs)
                .thenReturn(emptyPage);

        when(jobQueue.enqueueJobExecution(any(), anyString()))
                .thenReturn(false); // Simulate enqueue failure

        // Act
        jobProducer.produceJobs(START_TIME, END_TIME, null);

        // Assert
        verify(jobQueue, atLeastOnce()).enqueueJobExecution(any(), eq("local-job-queue"));
        verify(jobExecutionDao, times(1)).createMultipleJobExecutions(anyList());
        // Verify the method continues and updates next run even after failure
        verify(jobDao, times(1)).updateNextRunForJob(eq(JOB_ID), anyLong());
    }

    @Test
    void testProduceJobs_WithMultiplePages_ProcessesAllPages() {
        // Arrange
        JobEntity job1 = createJobEntity("job-1", START_TIME + 100, CRON_SCHEDULE);
        JobEntity job2 = createJobEntity("job-2", START_TIME + 200, CRON_SCHEDULE);

        PagedJobs firstPage = PagedJobs.builder()
                .jobEntities(Collections.singletonList(job1))
                .nextScheduledTimestamp(START_TIME + 100)
                .nextJobId("job-1")
                .last(false)
                .build();
        PagedJobs secondPage = PagedJobs.builder()
                .jobEntities(Collections.singletonList(job2))
                .nextScheduledTimestamp(START_TIME + 200)
                .nextJobId("job-2")
                .last(false)
                .build();
        PagedJobs emptyPage = PagedJobs.builder()
                .jobEntities(Collections.emptyList())
                .last(true)
                .build();

        when(jobDao.getJobsPaginated(eq(START_TIME), eq(END_TIME), eq(60), isNull(), isNull()))
                .thenReturn(firstPage);
        when(jobDao.getJobsPaginated(eq(START_TIME), eq(END_TIME), eq(60), eq(START_TIME + 100), eq("job-1")))
                .thenReturn(secondPage);
        when(jobDao.getJobsPaginated(eq(START_TIME), eq(END_TIME), eq(60), eq(START_TIME + 200), eq("job-2")))
                .thenReturn(emptyPage);

        // Act
        jobProducer.produceJobs(START_TIME, END_TIME, 60);

        // Assert
        verify(jobDao, times(3)).getJobsPaginated(anyLong(), anyLong(), anyInt(), any(), any());
        verify(jobExecutionDao, times(2)).createMultipleJobExecutions(anyList());
        verify(jobDao, times(1)).updateNextRunForJob(eq("job-1"), anyLong());
        verify(jobDao, times(1)).updateNextRunForJob(eq("job-2"), anyLong());
    }

    // Helper methods
    private JobEntity createJobEntity(String id, Long nextScheduledTime, String schedule) {
        JobEntity entity = new JobEntity();
        entity.setId(id);
        entity.setApiConfig(ApiConfigEntity.builder().url("").httpMethod("POST").payload(Map.of("message","hi")).build());
        entity.setNextScheduledTime(nextScheduledTime);
        entity.setSchedule(schedule);
        return entity;
    }

    private JobExecutionEntity createJobExecutionEntity(String jobId, String executionId) {
        JobExecutionEntity entity = new JobExecutionEntity();
        entity.setJobId(jobId);
        entity.setStatus(JobExecutionStatus.SCHEDULED.name());
        entity.setExecutionId(executionId);
        return entity;
    }
}