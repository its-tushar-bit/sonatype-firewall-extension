/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DateUtilsTest
{
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
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
        DateUtils.getDayOfMonthSuffix(notDayOfMonth);
      }).withMessage("Illegal day of month: " + notDayOfMonth);
    }
  }
}
