package utils;

import com.fampay.scheduler.commons.helper.utils.CronUtilsHelper;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CronUtilsHelperTest {

    private static final long NOW = Instant.now().toEpochMilli();

    @Nested
    @DisplayName("Tests for isValidCron()")
    class IsValidCronTests {

        @Test
        void testValidEvery5Seconds() {
            assertTrue(CronUtilsHelper.isValidCron("0/5 * * * * ?"));
        }

        @Test
        void testValidEveryMinute() {
            assertTrue(CronUtilsHelper.isValidCron("0 0/1 * * * ?"));
        }

        @Test
        void testInvalidMissingSeconds() {
            assertFalse(CronUtilsHelper.isValidCron("*/5 * * * *"));
        }

        @Test
        void testInvalidTooManyFields() {
            assertFalse(CronUtilsHelper.isValidCron("0 0 0 0 0 0 0"));
        }

        @Test
        void testInvalidCharacters() {
            assertFalse(CronUtilsHelper.isValidCron("abc def ghi"));
        }

        @Test
        void testNullCron() {
            assertFalse(CronUtilsHelper.isValidCron(null));
        }

        @Test
        void testEmptyCron() {
            assertFalse(CronUtilsHelper.isValidCron(""));
        }

        @Test
        void testInvalidMonth() {
            assertFalse(CronUtilsHelper.isValidCron("0 0 0 ? 13 MON"));
        }

        @Test
        void testValidSpecificDayAndMonth() {
            assertTrue(CronUtilsHelper.isValidCron("0 15 10 ? * MON"));
        }
    }

    @Nested
    @DisplayName("Tests for getNextRunInGMTFromStartTime()")
    class GetNextRunTests {

        @Test
        void testValidCronReturnsNextMinute() {
            long start = NOW;
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("0 0/1 * * * ?", start);
            assertTrue(result > start, "Next run should be in the future");
        }

        @Test
        void testInvalidCronReturnsMinusOne() {
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("abc", NOW);
            assertEquals(-1L, result);
        }

        @Test
        void testNullCronReturnsMinusOne() {
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime(null, NOW);
            assertEquals(-1L, result);
        }

        @Test
        void testEmptyCronReturnsMinusOne() {
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("", NOW);
            assertEquals(-1L, result);
        }

        @Test
        void testValidCronSpecificTime() {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).withMinute(0).withSecond(0).withNano(0);
            long start = now.toInstant().toEpochMilli();
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("0 30 10 * * ?", start);
            assertTrue(result > start);
        }

        @Test
        void testInvalidDateCron() {
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 0 31 2 ?", NOW);
            assertEquals(-1L, result);
        }

        @Test
        void testFarFutureStart() {
            long future = NOW + Duration.ofDays(3650).toMillis(); // 10 years
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 12 * * ?", future);
            assertTrue(result > future);
        }

        @Test
        void testPastStart() {
            long past = NOW - Duration.ofDays(1).toMillis();
            long result = CronUtilsHelper.getNextRunInGMTFromStartTime("0 0 0 * * ?", past);
            assertTrue(result > past);
        }
    }

    @Nested
    @DisplayName("Tests for getTimestampsBetween()")
    class GetTimestampsBetweenTests {

        @Test
        void testValidEveryMinuteFor5Minutes() {
            long start = NOW;
            long end = start + Duration.ofMinutes(5).toMillis();
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0 0/1 * * * ?", start, end);
            assertTrue(timestamps.size() >= 5);
        }

        @Test
        void testValidHourlyFor3Hours() {
            long start = NOW;
            long end = start + Duration.ofHours(3).toMillis();
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0 0 * * * ?", start, end);
            assertTrue(timestamps.size() >= 3);
        }

        @Test
        void testInvalidCronReturnsEmpty() {
            Assertions.assertThrows(IllegalArgumentException.class,()->CronUtilsHelper.getTimestampsBetween("abc", NOW, NOW + 3600000));
        }

        @Test
        void testNullCronReturnsEmpty() {
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween(null, NOW, NOW + 3600000);
            assertTrue(timestamps.isEmpty());
        }

        @Test
        void testEmptyCronReturnsEmpty() {
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("", NOW, NOW + 3600000);
            assertTrue(timestamps.isEmpty());
        }

        @Test
        void testEndBeforeStartReturnsEmpty() {
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0/5 * * * * ?", NOW + 1000, NOW);
            assertTrue(timestamps.isEmpty());
        }

        @Test
        void testSingleTimestampInRange() {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).withMinute(59).withSecond(0);
            long start = now.toInstant().toEpochMilli();
            long end = start + Duration.ofMinutes(2).toMillis();
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0 0 * * * ?", start, end);
            assertTrue(timestamps.size() <= 1);
        }

        @Test
        void testBoundaryInclusiveEnd() {
            long start = NOW;
            long end = start + Duration.ofMinutes(1).toMillis();
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0 0/1 * * * ?", start, end);
            assertFalse(timestamps.isEmpty());
        }

        @Test
        void testWeeklyCronWithin2Weeks() {
            long start = NOW;
            long end = start + Duration.ofDays(14).toMillis();
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0 0 0 ? * SUN", start, end);
            assertTrue(timestamps.size() >= 2);
        }

        @Test
        void testLeapYearCron() {
            ZonedDateTime startZdt = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime endZdt = ZonedDateTime.of(2025, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
            List<Long> timestamps = CronUtilsHelper.getTimestampsBetween("0 0 0 29 2 ?",
                    startZdt.toInstant().toEpochMilli(), endZdt.toInstant().toEpochMilli());
            // Should have 2 or more (2020, 2024)
            assertTrue(timestamps.size() >= 2);
        }
    }

    // ------------------------------------------------------------------------

    @Nested
    @DisplayName("Tests for getScheduledTimeFrom1Min()")
    class GetScheduledTimeFrom1MinTests {

        @Test
        void testValidCronEveryMinute() {
            long result = CronUtilsHelper.getScheduledTimeFrom1Min("0 0/1 * * * ?");
            assertTrue(result > System.currentTimeMillis());
        }

        @Test
        void testInvalidCron() {
            assertEquals(-1L, CronUtilsHelper.getScheduledTimeFrom1Min("abc"));
        }

        @Test
        void testNullCron() {
            assertEquals(-1L, CronUtilsHelper.getScheduledTimeFrom1Min(null));
        }

        @Test
        void testEmptyCron() {
            assertEquals(-1L, CronUtilsHelper.getScheduledTimeFrom1Min(""));
        }

        @Test
        void testNextRunAfterSeveralHours() {
            long result = CronUtilsHelper.getScheduledTimeFrom1Min("0 0 5 * * ?");
            assertTrue(result > System.currentTimeMillis());
        }

        @Test
        void testInvalidDateCron() {
            long result = CronUtilsHelper.getScheduledTimeFrom1Min("0 0 0 31 2 ?");
            assertEquals(-1L, result);
        }
    }
}

