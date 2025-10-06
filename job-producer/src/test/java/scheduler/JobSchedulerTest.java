package scheduler;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

import com.fampay.scheduler.commons.lock.client.CustomDistributedLock;
import com.fampay.scheduler.models.entity.ProducerConfig;
import com.fampay.scheduler.producer.JobProducer;
import com.fampay.scheduler.producer.JobProducerConfig;
import com.fampay.scheduler.producer.JobScheduler;
import com.fampay.scheduler.producer.utils.TimeUtils;
import com.fampay.scheduler.repository.ProducerConfigDao;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobSchedulerTest {

    @Mock
    private JobProducer jobProducer;

    @Mock
    private CustomDistributedLock customDistributedLock;

    @Mock
    private ProducerConfigDao producerConfigDao;

    @Mock
    private JobProducerConfig jobProducerConfig;

    @Mock
    private Lock lock;

    @InjectMocks
    private JobScheduler jobScheduler;

    @Captor
    private ArgumentCaptor<Long> timestampCaptor;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProduceJobsWhenLockAcquired() {
        // Arrange
        when(customDistributedLock.acquireLockWithWait(anyString(), anyLong(), eq(true)))
                .thenReturn(Optional.of(lock));

        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.setConfigId("config-1");
        producerConfig.setLastProducedTimestamp(1000L);

        when(producerConfigDao.findProducerConfById(anyString())).thenReturn(producerConfig);
        when(jobProducerConfig.getConfigId()).thenReturn("config-1");
        when(jobProducerConfig.getPageSize()).thenReturn(10);

        mockStaticDateTimeUtils(2000L);

        // Act
        jobScheduler.scheduleJobFetching();

        // Assert
        verify(jobProducer).produceJobs(eq(1000L), eq(60000L), eq(10));
        verify(producerConfigDao).updateLastProducedTimestamp(eq("config-1"), anyLong());
        verify(lock).unlock();
    }

    @Test
    void shouldLogAndSkipWhenLockNotAcquired() {
        when(customDistributedLock.acquireLockWithWait(anyString(), anyLong(), eq(true)))
                .thenReturn(Optional.empty());

        jobScheduler.scheduleJobFetching();

        verifyNoInteractions(jobProducer);
        verifyNoInteractions(producerConfigDao);
    }

    @Test
    void shouldUseCurrentTimeWhenProducerConfigIsNull() {
        when(customDistributedLock.acquireLockWithWait(anyString(), anyLong(), eq(true)))
                .thenReturn(Optional.of(lock));

        when(producerConfigDao.findProducerConfById(anyString())).thenReturn(null);
        when(jobProducerConfig.getConfigId()).thenReturn("config-1");
        when(jobProducerConfig.getPageSize()).thenReturn(10);

        mockStaticDateTimeUtils(5000L);

        Assertions.assertThrows(Exception.class,()->jobScheduler.scheduleJobFetching());

        verify(jobProducer).produceJobs(eq(5000L), eq(60000L), eq(10));
        verify(lock).unlock();
    }

    @Test
    void shouldUseCurrentTimeWhenLastProducedTimestampIsNull() {
        when(customDistributedLock.acquireLockWithWait(anyString(), anyLong(), eq(true)))
                .thenReturn(Optional.of(lock));

        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.setConfigId("config-1");
        producerConfig.setLastProducedTimestamp(null);

        when(producerConfigDao.findProducerConfById(anyString())).thenReturn(producerConfig);
        when(jobProducerConfig.getConfigId()).thenReturn("config-1");
        when(jobProducerConfig.getPageSize()).thenReturn(10);

        mockStaticDateTimeUtils(7000L);

        jobScheduler.scheduleJobFetching();

        verify(jobProducer).produceJobs(eq(7000L), eq(60000L), eq(10));
        verify(lock).unlock();
    }

    @Test
    void shouldUnlockLockWhenExceptionThrown() {
        when(customDistributedLock.acquireLockWithWait(anyString(), anyLong(), eq(true)))
                .thenReturn(Optional.of(lock));

        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.setConfigId("config-1");
        producerConfig.setLastProducedTimestamp(1000L);

        when(producerConfigDao.findProducerConfById(anyString())).thenReturn(producerConfig);
        when(jobProducerConfig.getConfigId()).thenReturn("config-1");
        when(jobProducerConfig.getPageSize()).thenReturn(10);

        doThrow(new RuntimeException("Simulated Failure")).when(jobProducer)
                .produceJobs(anyLong(), anyLong(), anyInt());

        mockStaticDateTimeUtils(9000L);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jobScheduler.scheduleJobFetching());
        assertEquals("Simulated Failure", thrown.getMessage());
        verify(lock).unlock();
    }

    // --- Helper for mocking static time utility ---
    private void mockStaticDateTimeUtils(long fixedTimeMillis) {
        mockStatic(DateTimeUtils.class);
        mockStatic(TimeUtils.class);

        when(DateTimeUtils.currentTimeMillis()).thenReturn(fixedTimeMillis);
        when(TimeUtils.getEndOfNextMinute(anyLong())).thenReturn(60000L);
    }

    // --- Cleanup static mocks after each test ---
    @AfterEach
    void teardown() {
        clearAllCaches();
    }
}
