/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.LocalDate;
import java.util.Optional;

import com.sonatype.insight.brain.service.consumption.dto.ConsumptionDateRange;

import jakarta.ws.rs.WebApplicationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DateRangeValidatorTest
{
  private final DateRangeValidator validator = new DateRangeValidator();

  @Test
  public void absentParamsReturnEmpty() {
    assertThat(validator.validate(Optional.empty(), Optional.empty(), 366)).isEmpty();
  }

  @Test
  public void onlyStartPresentThrows400() {
    assertThatThrownBy(() -> validator.validate(Optional.of("2026-06-01"), Optional.empty(), 366))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @Test
  public void onlyEndPresentThrows400() {
    assertThatThrownBy(() -> validator.validate(Optional.empty(), Optional.of("2026-06-30"), 366))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @Test
  public void invalidIsoFormatThrows400() {
    assertThatThrownBy(() -> validator.validate(Optional.of("06/01/2026"), Optional.of("2026-06-30"), 366))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @Test
  public void startAfterEndThrows400() {
    assertThatThrownBy(() -> validator.validate(Optional.of("2026-06-30"), Optional.of("2026-06-01"), 366))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @Test
  public void rangeExceedingCapThrows400() {
    assertThatThrownBy(() -> validator.validate(Optional.of("2026-01-01"), Optional.of("2026-06-30"), 90))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @Test
  public void validRangeReturnsParsed() {
    Optional<ConsumptionDateRange> r = validator.validate(Optional.of("2026-06-01"), Optional.of("2026-06-30"), 366);
    assertThat(r).isPresent();
    assertThat(r.get().getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    assertThat(r.get().getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
  }

  @Test
  public void sameDayRangeIsValid() {
    Optional<ConsumptionDateRange> r = validator.validate(Optional.of("2026-06-01"), Optional.of("2026-06-01"), 366);
    assertThat(r).isPresent();
    assertThat(r.get().getStartDate()).isEqualTo(r.get().getEndDate());
  }

  @Test
  public void exactlyAtCapIsValid() {
    // Inclusive-day-count semantics: cap=90 allows [start, end] spanning at most 90
    // calendar days inclusive. 2026-04-02 to 2026-06-30 is 90 inclusive days
    // (April 29 + May 31 + June 30 = 90), so this is the boundary that passes.
    Optional<ConsumptionDateRange> r = validator.validate(Optional.of("2026-04-02"), Optional.of("2026-06-30"), 90);
    assertThat(r).isPresent();
  }

  @Test
  public void oneDayPastCapThrows400() {
    // Boundary guard: cap=90 must reject 91 inclusive days. 2026-04-01 to 2026-06-30
    // is 91 inclusive days, so it must trip the cap.
    assertThatThrownBy(() -> validator.validate(Optional.of("2026-04-01"), Optional.of("2026-06-30"), 90))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @Test
  public void dailyHistoryCapAt92AllowsExactly92InclusiveDays() {
    // Regression guard for the inclusive-day-count fix. The user-facing
    // /daily-history endpoint advertises "up to 92 days"; the validator must
    // accept exactly 92 inclusive days, not 93.
    Optional<ConsumptionDateRange> r = validator.validate(Optional.of("2026-04-01"), Optional.of("2026-07-01"), 92);
    assertThat(r).isPresent();
    // And 93 inclusive days (Apr 1 → Jul 2) must trip the cap.
    assertThatThrownBy(() -> validator.validate(Optional.of("2026-04-01"), Optional.of("2026-07-02"), 92))
        .isInstanceOf(WebApplicationException.class);
  }
}
