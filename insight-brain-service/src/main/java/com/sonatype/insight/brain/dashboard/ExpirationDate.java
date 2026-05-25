/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

public enum ExpirationDate
{
  ALL(0),
  EXPIRED(-1),
  IN_24_HOURS(1),
  IN_7_DAYS(7),
  IN_30_DAYS(30),
  IN_90_DAYS(90),
  IN_OVER_90_DAYS(Integer.MAX_VALUE),
  AUTO(-2),
  NEVER(null);

  private final Integer days;

  ExpirationDate(final Integer days) {
    this.days = days;
  }

  public Integer getDays() {
    return days;
  }
}
