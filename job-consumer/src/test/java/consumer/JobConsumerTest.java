package consumer;


import com.fampay.scheduler.commons.http.client.AsyncHttpClient;
import com.fampay.scheduler.commons.queue.IMessageConsumer;
import com.fampay.scheduler.consumer.JobTypeProcessor;
import com.fampay.scheduler.repository.JobExecutionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JobConsumerTest {

//    @Mock
//    private IMessageConsumer messageConsumer;
//
//    @Mock
//    private JobExecutionDao jobExecutionDao;
//
//    @Mock
//    private AsyncHttpClient asyncHttpClient;
//
//    private MockWebServer mockWebServer;
//
//    @InjectMocks
//    private JobTypeProcessor jobExecutionProcessor;
//
//    private MockWebServer mockWebServer;
//
//    @BeforeEach
//    void setUp() {
//        mockWebServer = new MockWebServer();
//    }
}