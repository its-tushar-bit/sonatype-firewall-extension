/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DateUtilsTest
{
  private static final DateTimeFormatter TEST_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd '@' HH:mm");

  @Test
  public void testGetDayOfMonthSuffix() {
    assertThat(DateUtils.getDayOfMonthSuffix(1)).isEqualTo("st");
    assertThat(DateUtils.getDayOfMonthSuffix(2)).isEqualTo("nd");
    assertThat(DateUtils.getDayOfMonthSuffix(3)).isEqualTo("rd");
    assertThat(DateUtils.getDayOfMonthSuffix(4)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(5)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(6)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(7)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(8)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(9)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(10)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(11)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(12)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(13)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(14)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(15)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(16)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(17)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(18)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(19)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(20)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(21)).isEqualTo("st");
    assertThat(DateUtils.getDayOfMonthSuffix(22)).isEqualTo("nd");
    assertThat(DateUtils.getDayOfMonthSuffix(23)).isEqualTo("rd");
    assertThat(DateUtils.getDayOfMonthSuffix(24)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(25)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(26)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(27)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(28)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(29)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(30)).isEqualTo("th");
    assertThat(DateUtils.getDayOfMonthSuffix(31)).isEqualTo("st");

    for (int notDayOfMonth : new int[]{-1, 0, 32}) {
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> DateUtils.getDayOfMonthSuffix(notDayOfMonth))
          .withMessage("Illegal day of month: " + notDayOfMonth);
    }
  }

  @Test
  public void testGetClosestFutureDateTime_intervalStartInFuture() {
    assertExpectedFutureDateTime("2021-06-08 @ 08:30", "2021-06-08 @ 21:00", 3, "2021-06-08 @ 09:00");
    assertExpectedFutureDateTime("2021-06-08 @ 11:45", "2021-06-08 @ 22:15", 4, "2021-06-08 @ 14:15");
  }

  @Test
  public void testGetClosestFutureDateTime_intervalStartInPast() {
    assertExpectedFutureDateTime("2021-06-08 @ 08:30", "2021-06-08 @ 06:00", 4, "2021-06-08 @ 10:00");
    assertExpectedFutureDateTime("2021-06-08 @ 11:45", "2021-06-08 @ 10:15", 1, "2021-06-08 @ 12:15");
  }

  @Test
  public void testGetLocalTimeForHoursAndMinutes_timeStringBlank() {
    // given:
    LocalTime testStartTime = LocalTime.now();

    // when: no time supplied
    LocalTime localTime = DateUtils.getLocalTimeForHoursAndMinutes(null);

    // then:
    assertThat(localTime).isBetween(testStartTime, LocalTime.now());

    // and when: time is blank
    localTime = DateUtils.getLocalTimeForHoursAndMinutes("");

    // then:
    assertThat(localTime).isBetween(testStartTime, LocalTime.now());
  }

  @Test
  public void testGetLocalTimeForHoursAndMinutes_validTimes() {
    // given: a mapping of time strings to their expected LocalTime values
    Map<String, LocalTime> timeDataAndExpectedValues = ImmutableMap.of(
        "2357", LocalTime.of(23, 57, 0),
        "23:57", LocalTime.of(23, 57, 0),
        "07:29", LocalTime.of(7, 29, 0),
        "0945", LocalTime.of(9, 45, 0));

    // verify each pair
    timeDataAndExpectedValues.forEach((k, v) -> assertThat(DateUtils.getLocalTimeForHoursAndMinutes(k)).isEqualTo(v));
  }

  @Test
  public void testMax() {
    // given two non-null dates
    Date now = new Date();
    Date earlier = new Date(System.currentTimeMillis() - 5_000);
    Date later = new Date(System.currentTimeMillis() + 5_000);
    assertThat(DateUtils.max(now, later)).isEqualTo(later);
    assertThat(DateUtils.max(later, now)).isEqualTo(later);
    assertThat(DateUtils.max(now, earlier)).isEqualTo(now);
    assertThat(DateUtils.max(earlier, now)).isEqualTo(now);
    assertThat(DateUtils.max(earlier, later)).isEqualTo(later);
    assertThat(DateUtils.max(later, earlier)).isEqualTo(later);

    // both dates null
    assertThat(DateUtils.max(null, null)).isNull();

    // only one date null
    assertThat(DateUtils.max(null, now)).isEqualTo(now);
    assertThat(DateUtils.max(now, null)).isEqualTo(now);
  }

  private void assertExpectedFutureDateTime(String from, String intervalStart, int interval, String expected) {
    LocalDateTime fromDateTime = LocalDateTime.parse(from, TEST_DATETIME_FORMAT);
    LocalDateTime intervalStartDateTime = LocalDateTime.parse(intervalStart, TEST_DATETIME_FORMAT);
    LocalDateTime expectedFutureDateTime = LocalDateTime.parse(expected, TEST_DATETIME_FORMAT);

    LocalDateTime actualFutureDateTime =
        DateUtils.getClosestFutureDateTime(fromDateTime, intervalStartDateTime, interval);

    assertThat(actualFutureDateTime).isEqualTo(expectedFutureDateTime);
  }
}
