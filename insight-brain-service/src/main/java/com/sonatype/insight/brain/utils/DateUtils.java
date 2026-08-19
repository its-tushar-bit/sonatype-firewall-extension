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

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DateUtils
{
  public static String getDayOfMonthSuffix(final int n) {
    if (n < 1 || n > 31) {
      throw new IllegalArgumentException("Illegal day of month: " + n);
    }
    if (n == 11 || n == 12 || n == 13) {
      return "th";
    }
    switch (n % 10) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
    }
  }

  /**
   * Given a start datetime (in the future or the past) and an offset interval (together forming a recurring set of
   * dates) this method calculates the closest future date in the repeating interval from the given 'from' time.
   *
   * For example, if the interval start date is 6/21 at 1200, the interval is 4 hours and the from time is 0700 on 6/21
   * this method would calculate the closest future date as 0800 on 6/21. And, if the 'from' time was 1300 this method
   * would calculate a closest future date of 1600 on 6/21.
   *
   * @param fromDateTime the datetime to which the next closest interval time should be found
   * @param intervalStartTime the datetime that is the focal point for the interval
   * @param intervalHours number of hours offset from the intervalStartTime, forwards and backwards, to form a
   *          repeating series of datetime values
   * @return closest future datetime value in the interval datetime series
   */
  public static LocalDateTime getClosestFutureDateTime(
      LocalDateTime fromDateTime,
      LocalDateTime intervalStartTime,
      int intervalHours)
  {
    LocalDateTime now = LocalDateTime.now();
    if (null == fromDateTime) {
      fromDateTime = now;
    }
    if (null == intervalStartTime) {
      return fromDateTime;
    }
    LocalDateTime closestFutureIntervalDateTime = intervalStartTime;
    // make sure we're in the past to start
    while (closestFutureIntervalDateTime.isAfter(fromDateTime)) {
      closestFutureIntervalDateTime = closestFutureIntervalDateTime.minusHours(24);
    }
    // advance to the first interval time that is in the future from now
    while (closestFutureIntervalDateTime.isBefore(fromDateTime)) {
      closestFutureIntervalDateTime = closestFutureIntervalDateTime.plusHours(intervalHours);
    }
    return closestFutureIntervalDateTime;
  }

  /**
   * Converts a string in hours and minutes to a LocalTime object. The supported formats for the string are
   * 'HH:mm' and 'HHmm'
   *
   * @return the string converted to LocalTime or the current time if the given string is blank
   */
  public static LocalTime getLocalTimeForHoursAndMinutes(String timeInHoursAndMinutes) {
    if (isBlank(timeInHoursAndMinutes)) {
      return LocalTime.now();
    }
    DateTimeFormatter timeFormatter =
        DateTimeFormatter.ofPattern(timeInHoursAndMinutes.contains(":") ? "HH:mm" : "HHmm");
    return LocalTime.parse(timeInHoursAndMinutes, timeFormatter);
  }

  /**
   * returns the maximum date between two dates; null values are treated as earlier dates
   *
   * @return greater of two dates if both are non-null; the non-null date if only one is null; null if both are null
   */
  public static Date max(Date date1, Date date2) {
    if (null == date1) {
      return date2;
    }
    else {
      if (null == date2) {
        return date1;
      }
      return date1.before(date2) ? date2 : date1;
    }
  }
}
