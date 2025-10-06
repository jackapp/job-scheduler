package com.fampay.scheduler.producer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joda.time.DateTimeUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtils {

    public static long getEndOfNextMinute(long currentStartTimestamp) {
        ZonedDateTime zdt = Instant.ofEpochMilli(currentStartTimestamp)
                .atZone(ZoneOffset.UTC);
        ZonedDateTime nextMinute = zdt.plusMinutes(1)
                .withSecond(59)
                .withNano(999_999_999);

        return nextMinute.toInstant().toEpochMilli();
    }

    public static Integer calculateDelay(long scheduledTimestamp) {
        long currTime = DateTimeUtils.currentTimeMillis();
        long diffMillis = scheduledTimestamp - currTime;

        if (diffMillis <= 0) {
            return 0;
        }

        long delaySeconds = diffMillis / 1000;

        if (delaySeconds > 900) {
            return 900;
        }

        return (int) delaySeconds;
    }
}
