/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DateUtilsTest
{
  @Test
  public void testGetDayOfMonthSuffix() {
    assertThat(DateUtils.getDayOfMonthSuffix(1), is("st"));
    assertThat(DateUtils.getDayOfMonthSuffix(2), is("nd"));
    assertThat(DateUtils.getDayOfMonthSuffix(3), is("rd"));
    assertThat(DateUtils.getDayOfMonthSuffix(4), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(5), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(6), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(7), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(8), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(9), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(10), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(11), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(12), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(13), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(14), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(15), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(16), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(17), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(18), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(19), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(20), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(21), is("st"));
    assertThat(DateUtils.getDayOfMonthSuffix(22), is("nd"));
    assertThat(DateUtils.getDayOfMonthSuffix(23), is("rd"));
    assertThat(DateUtils.getDayOfMonthSuffix(24), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(25), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(26), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(27), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(28), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(29), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(30), is("th"));
    assertThat(DateUtils.getDayOfMonthSuffix(31), is("st"));

    for (int notDayOfMonth : new int[]{-1, 0, 32}) {
      try {
        DateUtils.getDayOfMonthSuffix(notDayOfMonth);
        fail();
      }
      catch (IllegalArgumentException e) {
        assertThat(e.getMessage(), is("Illegal day of month: " + notDayOfMonth));
      }
    }
  }
}
