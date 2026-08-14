/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DateConverterTest
{
  @Test
  public void testToLocalDate() {
    Date date = new GregorianCalendar(2023, Calendar.JULY, 4, 23, 55, 49).getTime();
    LocalDate localDate = LocalDate.of(2023, Month.JULY.getValue(), 4);

    assertThat(DateConverter.toLocalDate(date)).isEqualTo(localDate);
  }

  @Test
  public void testToDate() {
    LocalDate localDate = LocalDate.of(2023, Month.JULY.getValue(), 4);
    Date date = new GregorianCalendar(2023, Calendar.JULY, 4).getTime();

    assertThat(DateConverter.toDate(localDate)).isEqualTo(date);
  }
}
