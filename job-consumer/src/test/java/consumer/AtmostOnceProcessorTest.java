package consumer;

import com.fampay.scheduler.commons.http.client.AsyncHttpClient;
import com.fampay.scheduler.commons.http.dto.ApiResponse;
import com.fampay.scheduler.commons.queue.IMessageConsumer;
import com.fampay.scheduler.consumer.impl.AtmostOnceTypeProcessor;
import com.fampay.scheduler.models.dto.JobExecutionStatus;
import com.fampay.scheduler.models.dto.JobGuarantee;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.models.queue.ApiConfig;
import com.fampay.scheduler.models.queue.JobMessagePayload;
import com.fampay.scheduler.repository.JobExecutionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AtmostOnceProcessorTest {

    @Mock
    private IMessageConsumer messageConsumer;

    @Mock
    private JobExecutionDao jobExecutionDao;

    @Mock
    private AsyncHttpClient asyncHttpClient;

    @InjectMocks
    private AtmostOnceTypeProcessor processor;

    private JobMessagePayload payload;
    private JobExecutionEntity jobEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ApiConfig apiConfig = ApiConfig.builder()
                .url("http://localhost:3000/mock-endpoint")
                .httpMethod("POST")
                .payload("{\"k\":\"v\"}")
                .readTimeoutMs(5000L)
                .build();

        payload = new JobMessagePayload();
        payload.setExecutionId("exec-1");
        payload.setApiConfig(apiConfig);

        jobEntity = new JobExecutionEntity();
        jobEntity.setExecutionId("exec-1");
        jobEntity.setStatus(JobExecutionStatus.SCHEDULED.name());
    }

    @Test
    void testProcessJobExecution_SuccessfulApiCall() {
        when(jobExecutionDao.findByExecutionId("exec-1")).thenReturn(Optional.of(jobEntity));
        when(asyncHttpClient.callApi(any())).thenReturn(Mono.just(
                ApiResponse.builder().httpStatus(200).response("{\"ok\":true}").build()
        ));

        processor.processJobExecution(payload, "msg-1", "queue-1");

        // Verify status moved to STARTED
        verify(jobExecutionDao).updateJobExecutionStatus(eq("exec-1"), argThat(dto ->
                dto.getStatus().equals(JobExecutionStatus.STARTED.name())
        ));

        // Verify message deleted immediately
        verify(messageConsumer).deleteMessage("queue-1", "msg-1");

        // Verify response eventually leads to FINISHED state
        verify(jobExecutionDao, timeout(1000)).updateJobExecutionStatus(eq("exec-1"), argThat(dto ->
                dto.getStatus().equals(JobExecutionStatus.FINISHED.name()) &&
                        dto.getExecutionResponse().getStatus().equals("200")
        ));
    }

    @Test
    void testProcessJobExecution_AlreadyStarted_ShouldNotCallApi() {
        jobEntity.setStatus(JobExecutionStatus.STARTED.name());
        when(jobExecutionDao.findByExecutionId("exec-1")).thenReturn(Optional.of(jobEntity));

        processor.processJobExecution(payload, "msg-1", "queue-1");

        verify(messageConsumer).deleteMessage("queue-1", "msg-1");
        verifyNoInteractions(asyncHttpClient);
        verify(jobExecutionDao, never()).updateJobExecutionStatus(eq("exec-1"), argThat(dto ->
                JobExecutionStatus.STARTED.name().equals(dto.getStatus())
        ));
    }

    @Test
    void testProcessJobExecution_JobNotFound_ShouldOnlyDeleteMessage() {
        when(jobExecutionDao.findByExecutionId("exec-1")).thenReturn(Optional.empty());

        processor.processJobExecution(payload, "msg-1", "queue-1");

        verify(messageConsumer).deleteMessage("queue-1", "msg-1");
        verifyNoInteractions(asyncHttpClient);
    }

    @Test
    void testProcessJobExecution_ApiCallThrowsException_ShouldMarkFailed() {
        when(jobExecutionDao.findByExecutionId("exec-1")).thenReturn(Optional.of(jobEntity));
        when(asyncHttpClient.callApi(any())).thenThrow(new RuntimeException("HTTP failed"));

        processor.processJobExecution(payload, "msg-1", "queue-1");

        verify(jobExecutionDao).updateJobExecutionStatus(eq("exec-1"), argThat(dto ->
                dto.getStatus().equals(JobExecutionStatus.FAILED.name()) &&
                        dto.getExecutionResponse().getResponse().contains("HTTP failed")
        ));

        verify(messageConsumer).deleteMessage("queue-1", "msg-1");
    }

    @Test
    void testGetTypeProcessor_ReturnsAtmostOnce() {
        assert(processor.getTypeProcessor() == JobGuarantee.ATMOST_ONE);
    }
}
