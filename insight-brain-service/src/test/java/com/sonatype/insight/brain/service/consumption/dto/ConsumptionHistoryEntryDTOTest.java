/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link ConsumptionHistoryEntryDTO}'s CSV serialization. */
public class ConsumptionHistoryEntryDTOTest
{
  @Test
  public void getCsvHeader_matchesExpectedColumns() {
    assertThat(ConsumptionHistoryEntryDTO.getCsvHeader())
        .isEqualTo("Billing Period,Total Consumed,Monthly Limit,% Used,Remaining");
  }

  @Test
  public void toCsvLine_withLimit_formatsAllColumns() {
    ConsumptionHistoryEntryDTO dto = entry("2026-04-01", 2500L, 10_000L);

    assertThat(dto.toCsvLine()).isEqualTo("April 2026,2500,10000,25%,7500");
  }

  @Test
  public void toCsvLine_withLimit_roundsPercentToNearestInteger() {
    ConsumptionHistoryEntryDTO dto = entry("2026-01-01", 333L, 1000L);

    assertThat(dto.toCsvLine()).isEqualTo("January 2026,333,1000,33%,667");
  }

  @Test
  public void toCsvLine_exceedingLimit_clampsRemainingToZeroAndShowsPercentAboveHundred() {
    ConsumptionHistoryEntryDTO dto = entry("2026-02-01", 12_500L, 10_000L);

    assertThat(dto.toCsvLine()).isEqualTo("February 2026,12500,10000,125%,0");
  }

  @Test
  public void toCsvLine_zeroConsumption_producesZeroPercentAndFullRemaining() {
    ConsumptionHistoryEntryDTO dto = entry("2026-03-01", 0L, 10_000L);

    assertThat(dto.toCsvLine()).isEqualTo("March 2026,0,10000,0%,10000");
  }

  @Test
  public void toCsvLine_zeroLimit_avoidsDivisionByZeroAndClampsRemainingToZero() {
    ConsumptionHistoryEntryDTO dto = entry("2026-05-01", 500L, 0L);

    assertThat(dto.toCsvLine()).isEqualTo("May 2026,500,0,0%,0");
  }

  @Test
  public void toCsvLine_nullLimit_leavesLimitPercentAndRemainingBlank() {
    ConsumptionHistoryEntryDTO dto = entry("2026-06-01", 1234L, null);

    assertThat(dto.toCsvLine()).isEqualTo("June 2026,1234,,,");
  }

  @Test
  public void toCsvLine_malformedMonth_fallsBackToRawValue() {
    ConsumptionHistoryEntryDTO dto = entry("not-a-month", 100L, 1000L);

    assertThat(dto.toCsvLine()).isEqualTo("not-a-month,100,1000,10%,900");
  }

  @Test
  public void toCsvLine_nullMonth_producesEmptyMonthCell() {
    ConsumptionHistoryEntryDTO dto = entry(null, 100L, 1000L);

    assertThat(dto.toCsvLine()).isEqualTo(",100,1000,10%,900");
  }

  @Test
  public void toCsvLine_largeNumbers_serializeWithoutOverflowOrGrouping() {
    ConsumptionHistoryEntryDTO dto = entry("2026-07-01", 9_000_000_000L, 10_000_000_000L);

    assertThat(dto.toCsvLine()).isEqualTo("July 2026,9000000000,10000000000,90%,1000000000");
  }

  private static ConsumptionHistoryEntryDTO entry(String month, long consumed, Long limit) {
    ConsumptionHistoryEntryDTO dto = new ConsumptionHistoryEntryDTO();
    dto.setMonth(month);
    dto.setConsumed(consumed);
    dto.setLimit(limit);
    return dto;
  }
}
