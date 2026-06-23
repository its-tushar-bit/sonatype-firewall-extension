/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.sonatype.insight.brain.service.consumption.dto.ConsumptionDateRange;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Named
@Singleton
public class DateRangeValidator
{
  public Optional<ConsumptionDateRange> validate(
      Optional<String> startDate,
      Optional<String> endDate,
      int maxDays)
  {
    if (startDate.isEmpty() && endDate.isEmpty()) {
      return Optional.empty();
    }
    if (startDate.isEmpty() || endDate.isEmpty()) {
      throw bad("Both startDate and endDate must be provided");
    }
    LocalDate s;
    LocalDate e;
    try {
      s = LocalDate.parse(startDate.get());
      e = LocalDate.parse(endDate.get());
    }
    catch (DateTimeParseException ex) {
      throw bad("startDate and endDate must be ISO-8601 (YYYY-MM-DD)");
    }
    if (s.isAfter(e)) {
      throw bad("startDate must be on or before endDate");
    }
    // Use inclusive-day-count semantics to match the user-facing "N days max" promise.
    // ChronoUnit.DAYS.between counts FULL days between two LocalDates, so
    // [Jun 1, Jun 30] returns 29 (29 full days), but the user thinks of that range
    // as 30 days. Without +1, getDailyHistory(92) silently accepts 93 inclusive days.
    if (ChronoUnit.DAYS.between(s, e) + 1 > maxDays) {
      throw bad("Date range exceeds maximum of " + maxDays + " days");
    }
    return Optional.of(new ConsumptionDateRange(s, e));
  }

  private static WebApplicationException bad(String msg) {
    return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
  }
}
