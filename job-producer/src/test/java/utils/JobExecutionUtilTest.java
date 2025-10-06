package utils;

import com.fampay.scheduler.models.dto.JobExecutionStatus;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.fampay.scheduler.producer.utils.JobExecutionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.bson.types.ObjectId;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobExecutionUtilTest {

    @Test
    @DisplayName("generate() should create job executions for valid cron schedule")
    void testGenerateWithValidCronSchedule() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 0 12 * * ?") // Quartz: Daily at 12:00 PM
                .build();
        long startTimestamp = 1704067200000L; // 2024-01-01 00:00:00 UTC
        long endTimestamp = 1704326400000L;   // 2024-01-04 00:00:00 UTC

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        result.forEach(entity -> {
            assertEquals(jobEntity.getId(), entity.getJobId());
            assertNotNull(entity.getExecutionId());
            assertEquals(JobExecutionStatus.SCHEDULED.name(), entity.getStatus());
            assertNotNull(entity.getScheduledRunAt());
            assertTrue(entity.getScheduledRunAt() >= startTimestamp);
            assertTrue(entity.getScheduledRunAt() <= endTimestamp);
        });
    }

    @Test
    @DisplayName("generate() should return empty list when no scheduled times in range")
    void testGenerateWithNoScheduledTimes() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 0 12 1 1 ?") // Only runs on Jan 1st at noon
                .build();
        long startTimestamp = 1704326400000L; // 2024-01-04
        long endTimestamp = 1704412800000L;   // 2024-01-05

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("generate() should handle multiple executions in time range")
    void testGenerateWithMultipleExecutions() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 */5 * * * ?")
                .build();
        long startTimestamp = 1704067200000L;
        long endTimestamp = startTimestamp + (3600 * 1000);

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 10);
    }

    @Test
    @DisplayName("generate() with byte array should create consistent execution IDs")
    void testGenerateWithByteArrayConsistency() {
        // Arrange
        byte[] jobHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        long timestamp = 1704067200000L;

        // Act
        String executionId1 = JobExecutionUtil.generate(jobHash, timestamp);
        String executionId2 = JobExecutionUtil.generate(jobHash, timestamp);

        // Assert
        assertNotNull(executionId1);
        assertNotNull(executionId2);
        assertEquals(executionId1, executionId2);
        assertEquals(24, executionId1.length()); // 12 bytes = 24 hex chars
    }

    @Test
    @DisplayName("generate() with byte array should create different IDs for different timestamps")
    void testGenerateWithDifferentTimestamps() {
        // Arrange
        byte[] jobHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        long timestamp1 = 1704067200000L;
        long timestamp2 = 1704067260000L; // 60 seconds later

        // Act
        String executionId1 = JobExecutionUtil.generate(jobHash, timestamp1);
        String executionId2 = JobExecutionUtil.generate(jobHash, timestamp2);

        // Assert
        assertNotNull(executionId1);
        assertNotNull(executionId2);
        assertNotEquals(executionId1, executionId2);
    }

    @Test
    @DisplayName("generate() with byte array should create different IDs for different job hashes")
    void testGenerateWithDifferentJobHashes() {
        // Arrange
        byte[] jobHash1 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        byte[] jobHash2 = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        long timestamp = 1704067200000L;

        // Act
        String executionId1 = JobExecutionUtil.generate(jobHash1, timestamp);
        String executionId2 = JobExecutionUtil.generate(jobHash2, timestamp);

        // Assert
        assertNotNull(executionId1);
        assertNotNull(executionId2);
        assertNotEquals(executionId1, executionId2);
    }

    @Test
    @DisplayName("generate() should properly encode timestamp in execution ID")
    void testExecutionIdContainsTimestamp() {
        // Arrange
        byte[] jobHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        long timestamp = 1704067200000L;
        int expectedSeconds = (int) (timestamp / 1000);

        // Act
        String executionId = JobExecutionUtil.generate(jobHash, timestamp);

        // Assert
        assertNotNull(executionId);
        // First 8 hex chars (4 bytes) should represent the timestamp in seconds
        String timeHex = executionId.substring(0, 8);
        byte[] timeBytes = HexFormat.of().parseHex(timeHex);
        int actualSeconds = ByteBuffer.wrap(timeBytes).getInt();
        assertEquals(expectedSeconds, actualSeconds);
    }

    @Test
    @DisplayName("generate() should handle edge case timestamps")
    void testGenerateWithEdgeCaseTimestamps() {
        // Arrange
        byte[] jobHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

        // Act & Assert - minimum positive timestamp
        String id1 = JobExecutionUtil.generate(jobHash, 1000L);
        assertNotNull(id1);
        assertEquals(24, id1.length());

        // Act & Assert - large timestamp
        String id2 = JobExecutionUtil.generate(jobHash, Long.MAX_VALUE / 1000);
        assertNotNull(id2);
        assertEquals(24, id2.length());
    }

    @Test
    @DisplayName("generate() should create unique execution IDs for same job at different times")
    void testUniqueExecutionIdsAcrossTime() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 0 * * * ?") // Every hour
                .build();
        long startTimestamp = 1704067200000L;
        long endTimestamp = startTimestamp + (10 * 3600 * 1000); // 10 hours

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        // Assert
        long uniqueIds = result.stream()
                .map(JobExecutionEntity::getExecutionId)
                .distinct()
                .count();
        assertEquals(result.size(), uniqueIds);
    }

    @Test
    @DisplayName("generate() should handle same start and end timestamp")
    void testGenerateWithSameStartEndTimestamp() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 0 12 * * ?")
                .build();
        long timestamp = 1704067200000L;

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, timestamp, timestamp);

        // Assert
        assertNotNull(result);
        // Should be empty or contain at most one execution depending on CronUtilsHelper behavior
        assertTrue(result.size() <= 1);
    }

    @Test
    @DisplayName("generate() should handle complex cron expression")
    void testGenerateWithComplexCronExpression() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 31 10,11,12,13,14,15 ? * MON-FRI") // Quartz: At 31 minutes past hours 10-15, weekdays only
                .build();
        long startTimestamp = 1704067200000L; // Start of 2024
        long endTimestamp = startTimestamp + (30L * 24 * 3600 * 1000); // 30 days later


        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        assertNotNull(result);
        result.forEach(entity -> {
            assertNotNull(entity.getExecutionId());
            assertEquals(24, entity.getExecutionId().length());
        });
    }

    @Test
    @DisplayName("generate() should handle cron expression with day-of-week range")
    void testGenerateWithDayOfWeekRange() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 31 10 ? * MON-FRI")
                .build();
        long startTimestamp = 1704067200000L;
        long endTimestamp = startTimestamp + (30L * 24 * 3600 * 1000);

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        // Assert
        assertNotNull(result);
        result.forEach(entity -> {
            assertNotNull(entity.getExecutionId());
            assertEquals(24, entity.getExecutionId().length());
        });
    }

    @Test
    @DisplayName("generate() should maintain job ID in all execution entities")
    void testJobIdConsistencyInExecutions() {
        String expectedJobId = new ObjectId().toHexString();
        JobEntity jobEntity = JobEntity.builder()
                .id(expectedJobId)
                .schedule("0 0 12 * * ?")
                .build();
        long startTimestamp = 1704067200000L;
        long endTimestamp = startTimestamp + (7L * 24 * 3600 * 1000);

        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        assertFalse(result.isEmpty());
        result.forEach(entity -> assertEquals(expectedJobId, entity.getJobId()));
    }

    @Test
    @DisplayName("Execution IDs should be valid hex strings")
    void testExecutionIdsAreValidHex() {
        // Arrange
        JobEntity jobEntity = JobEntity.builder()
                .id(new ObjectId().toHexString())
                .schedule("0 0 * * * ?")
                .build();
        long startTimestamp = 1704067200000L;
        long endTimestamp = startTimestamp + (3600 * 1000);

        // Act
        List<JobExecutionEntity> result = JobExecutionUtil.generate(jobEntity, startTimestamp, endTimestamp);

        // Assert
        assertFalse(result.isEmpty());
        result.forEach(entity -> {
            String executionId = entity.getExecutionId();
            assertTrue(executionId.matches("^[0-9a-f]{24}$"),
                    "Execution ID should be 24 hex characters: " + executionId);
        });
    }
}