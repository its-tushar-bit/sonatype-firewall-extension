/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;

public enum TimePeriod
{
  WEEK,
  MONTH;

  private DurationFieldType getDurationFieldType() {
    if (this == WEEK) {
      return DurationFieldType.weeks();
    }
    else {
      return DurationFieldType.months();
    }
  }

  public DateTimeFieldType getDateTimeFieldType() {
    if (this == WEEK) {
      return DateTimeFieldType.dayOfWeek();
    }
    else {
      return DateTimeFieldType.dayOfMonth();
    }
  }

  public Period getPeriod(int value) {
    return Period.ZERO.withField(getDurationFieldType(), value);
  }
}
