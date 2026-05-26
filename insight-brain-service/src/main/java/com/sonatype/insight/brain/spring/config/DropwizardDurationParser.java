/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.time.Duration;
import java.util.Locale;

public final class DropwizardDurationParser
{
  private DropwizardDurationParser() {
  }

  public static Duration parse(String duration) {
    if (duration == null || duration.trim().isEmpty()) {
      throw new IllegalArgumentException("Duration must not be blank");
    }

    String trimmed = duration.trim();

    try {
      return Duration.ofSeconds(Long.parseLong(trimmed));
    }
    catch (NumberFormatException ignored) {
      // fall through
    }

    String normalized = trimmed.toLowerCase(Locale.ROOT);
    int unitStart = 0;
    while (unitStart < normalized.length() && Character.isDigit(normalized.charAt(unitStart))) {
      unitStart++;
    }
    if (unitStart == 0 || unitStart >= normalized.length()) {
      throw new IllegalArgumentException("Unsupported duration: " + duration);
    }

    long value;
    try {
      value = Long.parseLong(normalized.substring(0, unitStart));
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("Unsupported duration: " + duration, e);
    }

    String unit = normalized.substring(unitStart).trim();
    try {
      if (unit.equals("d") || unit.equals("day") || unit.equals("days")) {
        return Duration.ofDays(value);
      }
      if (unit.equals("h") || unit.equals("hour") || unit.equals("hours")) {
        return Duration.ofHours(value);
      }
      if (unit.equals("m") || unit.equals("min") || unit.equals("mins") || unit.equals("minute")
          || unit.equals("minutes"))
      {
        return Duration.ofMinutes(value);
      }
      if (unit.equals("s") || unit.equals("second") || unit.equals("seconds")) {
        return Duration.ofSeconds(value);
      }
      if (unit.equals("ms") || unit.equals("millisecond") || unit.equals("milliseconds")) {
        return Duration.ofMillis(value);
      }
      if (unit.equals("us") || unit.equals("microsecond") || unit.equals("microseconds")) {
        return Duration.ofNanos(Math.multiplyExact(value, 1_000L));
      }
      if (unit.equals("ns") || unit.equals("nanosecond") || unit.equals("nanoseconds")) {
        return Duration.ofNanos(value);
      }
    }
    catch (ArithmeticException e) {
      throw new IllegalArgumentException("Duration is too large: " + duration, e);
    }

    throw new IllegalArgumentException("Unsupported duration unit in: " + duration);
  }
}
