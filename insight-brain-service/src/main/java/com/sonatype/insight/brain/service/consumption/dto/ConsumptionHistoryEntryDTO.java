/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sonatype.insight.brain.utils.CsvWritable;

/**
 * DTO for a single month's consumption history entry.
 *
 * @since 1.204
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumptionHistoryEntryDTO
    implements CsvWritable
{
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

  private String month;

  private String windowEnd;

  private long consumed;

  private Long limit;

  private Double percentUsed;

  private Long remaining;

  public ConsumptionHistoryEntryDTO() {
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public String getWindowEnd() {
    return windowEnd;
  }

  /**
   * Must be the exclusive upper bound of the billing window (i.e. the next window's start).
   * CSV display converts this to an inclusive end via {@code minusDays(1)}.
   */
  public void setWindowEnd(String windowEnd) {
    this.windowEnd = windowEnd;
  }

  public long getConsumed() {
    return consumed;
  }

  public void setConsumed(long consumed) {
    this.consumed = consumed;
  }

  public Long getLimit() {
    return limit;
  }

  public void setLimit(Long limit) {
    this.limit = limit;
  }

  public Double getPercentUsed() {
    return percentUsed;
  }

  public void setPercentUsed(Double percentUsed) {
    this.percentUsed = percentUsed;
  }

  public Long getRemaining() {
    return remaining;
  }

  public void setRemaining(Long remaining) {
    this.remaining = remaining;
  }

  public static String getCsvHeader() {
    return "Billing Period,Total Consumed,Monthly Limit,% Used,Remaining";
  }

  @Override
  public String toCsvLine() {
    String periodLabel = CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(
        CsvWritable.escapeDoubleQuotes(formatPeriod(month, windowEnd)));

    if (limit == null) {
      return CsvWritable.joiner.join(periodLabel, consumed, "", "", "");
    }
    int roundedPercent = limit > 0 ? (int) Math.round(consumed * 100.0 / limit) : 0;
    return CsvWritable.joiner.join(periodLabel, consumed, limit, roundedPercent + "%", Math.max(0L, limit - consumed));
  }

  /**
   * Render the period label. When {@code windowEnd} is provided we emit the full billing range
   * (e.g. {@code "Apr 20, 2026 – May 20, 2026"}) so customers see exactly which dates a row covers
   * — important when {@code subscriptionDay != 1}. Falls back to legacy month-year formatting.
   */
  private static String formatPeriod(String monthStr, String windowEndStr) {
    if (monthStr == null) {
      return "";
    }
    LocalDate start = parseSafely(monthStr);
    if (start == null) {
      return monthStr;
    }
    if (windowEndStr != null) {
      LocalDate end = parseSafely(windowEndStr);
      if (end != null) {
        // CSV-display window-end is the inclusive last day, i.e. the day before the next reset.
        LocalDate inclusiveEnd = end.minusDays(1);
        return start.format(DAY_FORMATTER) + " - " + inclusiveEnd.format(DAY_FORMATTER);
      }
    }
    return start.format(MONTH_FORMATTER);
  }

  private static LocalDate parseSafely(String s) {
    try {
      return LocalDate.parse(s);
    }
    catch (Exception e) {
      return null;
    }
  }
}
