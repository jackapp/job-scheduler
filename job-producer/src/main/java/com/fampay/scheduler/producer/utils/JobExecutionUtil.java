package com.fampay.scheduler.producer.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import com.fampay.scheduler.commons.helper.utils.CronUtilsHelper;
import com.fampay.scheduler.models.dto.JobExecutionStatus;
import com.fampay.scheduler.models.entity.JobEntity;
import com.fampay.scheduler.models.entity.JobExecutionEntity;
import com.google.common.hash.Hashing;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JobExecutionUtil {

    private static final HexFormat HEX = HexFormat.of();

    public static List<JobExecutionEntity> generate(JobEntity jobEntity, long startTimestamp, long endTimestamp) {
        byte []jobHashId = hashJobId(jobEntity.getId());
        List<Long> scheduledTimestamps = CronUtilsHelper.getTimestampsBetween(jobEntity.getSchedule(),startTimestamp,endTimestamp);
        List<JobExecutionEntity> jobExecutionEntities = new ArrayList<>();
        for (Long timestamp : scheduledTimestamps) {
            String executionId = generate(jobHashId,timestamp);
            JobExecutionEntity jobExecutionEntity = JobExecutionEntity.builder().jobId(jobEntity.getId()).executionId(executionId)
                    .scheduledRunAt(timestamp).status(JobExecutionStatus.SCHEDULED.name()).build();
            jobExecutionEntities.add(jobExecutionEntity);
        }
        return jobExecutionEntities;
    }

    public static String generate(byte[] jobHash,long timestampMillis) {
        int seconds = (int) (timestampMillis / 1000);
        byte[] timeBytes = ByteBuffer.allocate(4).putInt(seconds).array();

        byte[] executionId = new byte[12];
        System.arraycopy(timeBytes, 0, executionId, 0, 4);
        System.arraycopy(jobHash, 0, executionId, 4, 8);

        return HEX.formatHex(executionId);

    }

    private static byte[] hashJobId(String jobId) {
        long hash = Hashing.murmur3_128()
                .hashString(jobId, StandardCharsets.UTF_8)
                .asLong();
        return ByteBuffer.allocate(8).putLong(hash).array();
    }

}
