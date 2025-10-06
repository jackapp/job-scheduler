package com.fampay.scheduler.commons.helper.utils;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CronUtilsHelper {

    private static final CronParser PARSER =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    /**
     * Validates if a cron expression (with seconds) is valid.
     *
     * @param cronExpression the cron expression
     * @return true if valid, false otherwise
     */
    public static boolean isValidCron(String cronExpression) {
        try {
            PARSER.parse(cronExpression).validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Long determineTimeToSchedule(Long nextTimeToRun, Long startTime,String schedule) {
        if (nextTimeToRun<startTime) {
            return CronUtilsHelper.getNextRunInGMTFromStartTime(schedule,startTime);
        } else {
            return nextTimeToRun;
        }
    }

    /**
     * Gets the next run timestamp in GMT starting from the given startTimestamp (milliseconds).
     * Returns -1 if invalid or no next execution found.
     */
    public static long getNextRunInGMTFromStartTime(String cronExpression, long startTimestampMillis) {
        if (StringUtils.isEmpty(cronExpression)) {
            return -1L;
        }
        try {

            Cron cron = PARSER.parse(cronExpression);
            cron.validate();

            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            ZonedDateTime start = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(startTimestampMillis),
                    ZoneId.of("GMT")
            );

            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(start);
            return nextExecution.map(zdt -> zdt.toInstant().toEpochMilli()).orElse(-1L);
        } catch (Exception e) {
            return -1L;
        }
    }
    /**
     * Generate all execution timestamps (in millis, UTC) that fall between start and end timestamps.
     *
     * @param cronExpression quartz-style cron expression
     * @param startMillis start timestamp (epoch millis)
     * @param endMillis end timestamp (epoch millis)
     * @return list of execution timestamps (epoch millis UTC)
     */
    public static List<Long> getTimestampsBetween(String cronExpression, long startMillis, long endMillis) {
        List<Long> timestamps = new ArrayList<>();
        if (cronExpression == null || cronExpression.isEmpty()) return timestamps;

        Cron cron = PARSER.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime start = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC);
        ZonedDateTime end = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC);

        Optional<ZonedDateTime> next = executionTime.nextExecution(start.minusNanos(1));

        while (next.isPresent() && !next.get().isAfter(end)) {
            ZonedDateTime fireTime = next.get();
            timestamps.add(fireTime.toInstant().toEpochMilli());
            next = executionTime.nextExecution(fireTime);
        }

        return timestamps;
    }

    public static Long getScheduledTimeFrom1Min(String cronExpression) {
        Instant oneMinuteFromNow = Instant.now().plus(Duration.ofMinutes(1));
        long timestamp = oneMinuteFromNow.toEpochMilli();
        return getNextRunInGMTFromStartTime(cronExpression,timestamp);
    }


}
