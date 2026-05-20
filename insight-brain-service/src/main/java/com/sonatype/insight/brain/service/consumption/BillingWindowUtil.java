/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.LocalDate;

/**
 * Calculates billing window boundaries based on subscription start date.
 *
 * @since 1.204
 */
public final class BillingWindowUtil
{
  /**
   * Placeholder subscription anchor day used until per-tenant subscription day is wired in
   * (CLM-39593: resolve from license effective date). Use this constant at every site that
   * would otherwise hardcode {@code 1} so the eventual swap is a single-file change.
   */
  public static final int DEFAULT_SUBSCRIPTION_DAY = 1;

  private BillingWindowUtil() {
    // utility class
  }

  /**
   * Calculate the start of the current billing window.
   *
   * @param today the current date
   * @param subscriptionDayOfMonth the day of month the subscription started (1-31)
   * @return the start date of the billing window containing today
   */
  public static LocalDate calculateWindowStart(LocalDate today, int subscriptionDayOfMonth) {
    int clampedDay = Math.min(subscriptionDayOfMonth, today.lengthOfMonth());
    LocalDate windowStartThisMonth = today.withDayOfMonth(clampedDay);

    if (!today.isBefore(windowStartThisMonth)) {
      // Today is on or after the window start day this month
      return windowStartThisMonth;
    }
    else {
      // Today is before the window start day, so the window started last month
      LocalDate previousMonth = today.minusMonths(1);
      int clampedPrev = Math.min(subscriptionDayOfMonth, previousMonth.lengthOfMonth());
      return previousMonth.withDayOfMonth(clampedPrev);
    }
  }

  /**
   * Calculate the reset date (start of next billing window).
   */
  public static LocalDate calculateResetDate(LocalDate windowStart, int subscriptionDayOfMonth) {
    LocalDate nextMonth = windowStart.plusMonths(1);
    int clampedDay = Math.min(subscriptionDayOfMonth, nextMonth.lengthOfMonth());
    return nextMonth.withDayOfMonth(clampedDay);
  }

  /**
   * Calculate the start of the billing window immediately preceding {@code currentStart}.
   * Clamps to the previous month's last valid day when subscriptionDayOfMonth exceeds it.
   */
  public static LocalDate calculatePreviousWindowStart(LocalDate currentStart, int subscriptionDayOfMonth) {
    LocalDate previous = currentStart.minusMonths(1);
    int clampedDay = Math.min(subscriptionDayOfMonth, previous.lengthOfMonth());
    return previous.withDayOfMonth(clampedDay);
  }
}
