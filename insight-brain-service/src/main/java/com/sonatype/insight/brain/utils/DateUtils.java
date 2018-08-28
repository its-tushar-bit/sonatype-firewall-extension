/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

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
}
