/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.LocalDate;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BillingWindowUtilTest
{
  @Test
  public void windowStart_whenTodayIsAfterSubscriptionDay() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 4, 20), 15);
    assertThat(result).isEqualTo(LocalDate.of(2026, 4, 15));
  }

  @Test
  public void windowStart_whenTodayIsBeforeSubscriptionDay() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 4, 10), 15);
    assertThat(result).isEqualTo(LocalDate.of(2026, 3, 15));
  }

  @Test
  public void windowStart_whenTodayIsExactlySubscriptionDay() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 4, 15), 15);
    assertThat(result).isEqualTo(LocalDate.of(2026, 4, 15));
  }

  @Test
  public void windowStart_day31InFebruary() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 3, 5), 31);
    assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  public void windowStart_day31_todayBeforeClampedDay() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 2, 15), 31);
    assertThat(result).isEqualTo(LocalDate.of(2026, 1, 31));
  }

  @Test
  public void windowStart_day1() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 4, 20), 1);
    assertThat(result).isEqualTo(LocalDate.of(2026, 4, 1));
  }

  @Test
  public void resetDate_normalMonth() {
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(LocalDate.of(2026, 4, 15), 15);
    assertThat(resetDate).isEqualTo(LocalDate.of(2026, 5, 15));
  }

  @Test
  public void resetDate_clampedForFebruary() {
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(LocalDate.of(2026, 1, 31), 31);
    assertThat(resetDate).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  public void resetDate_clampedFor30DayMonth() {
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(LocalDate.of(2026, 3, 31), 31);
    assertThat(resetDate).isEqualTo(LocalDate.of(2026, 4, 30));
  }

  @Test
  public void resetDate_clampedForLeapFebruary() {
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(LocalDate.of(2028, 1, 31), 31);
    assertThat(resetDate).isEqualTo(LocalDate.of(2028, 2, 29));
  }

  // --- BDD: billing-window.feature - Year boundary and leap year ---

  @Test
  public void windowStart_yearBoundary_decemberToJanuary() {
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2027, 1, 5), 15);
    assertThat(result).isEqualTo(LocalDate.of(2026, 12, 15));
  }

  @Test
  public void windowStart_leapYear_february29() {
    // 2028 is a leap year
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2028, 3, 5), 29);
    assertThat(result).isEqualTo(LocalDate.of(2028, 2, 29));
  }

  @Test
  public void windowStart_nonLeapYear_february29ClampedTo28() {
    // 2026 is not a leap year, day 29 should clamp to 28
    LocalDate result = BillingWindowUtil.calculateWindowStart(LocalDate.of(2026, 3, 5), 29);
    assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  public void resetDate_yearBoundary_december31ToJanuary() {
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(LocalDate.of(2026, 12, 15), 15);
    assertThat(resetDate).isEqualTo(LocalDate.of(2027, 1, 15));
  }

  @Test
  public void previousWindowStart_simpleCase() {
    LocalDate result = BillingWindowUtil.calculatePreviousWindowStart(LocalDate.of(2026, 5, 15), 15);
    assertThat(result).isEqualTo(LocalDate.of(2026, 4, 15));
  }

  @Test
  public void previousWindowStart_clampsToShorterPreviousMonth() {
    LocalDate result = BillingWindowUtil.calculatePreviousWindowStart(LocalDate.of(2026, 3, 31), 31);
    assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  public void previousWindowStart_yearBoundary() {
    LocalDate result = BillingWindowUtil.calculatePreviousWindowStart(LocalDate.of(2027, 1, 15), 15);
    assertThat(result).isEqualTo(LocalDate.of(2026, 12, 15));
  }
}
