/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionEventDAO;
import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionLimitConfigDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.Aggregation;
import com.sonatype.insight.brain.model.consumption.ConsumptionAppTotal;
import com.sonatype.insight.brain.model.consumption.ConsumptionDailyTotal;
import com.sonatype.insight.brain.model.consumption.ConsumptionLimitConfig;
import com.sonatype.insight.brain.model.consumption.ConsumptionMonthlyBreakdown;
import com.sonatype.insight.brain.model.consumption.ConsumptionMonthlyTotal;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionDailyHistoryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionHistoryBreakdownDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionHistoryEntryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionSummaryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionTopAppDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionTopAppsResponseDTO;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for consumption-based pricing summary, history, and top-apps queries.
 *
 * @since 1.204
 */
@Named
@Singleton
public class ConsumptionService
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionService.class);

  private static final int DEFAULT_HISTORY_MONTHS = 12;

  private static final int DEFAULT_HISTORY_WEEKS = 52;

  private static final int MAX_TOP_APPS = 10;

  private final ConsumptionEventDAO eventDAO;

  private final ConsumptionLimitConfigDAO limitConfigDAO;

  @Inject
  public ConsumptionService(ConsumptionEventDAO eventDAO, ConsumptionLimitConfigDAO limitConfigDAO) {
    this.eventDAO = eventDAO;
    this.limitConfigDAO = limitConfigDAO;
  }

  public ConsumptionSummaryDTO getCurrentMonthSummary(int subscriptionDayOfMonth, String tier) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDayOfMonth);
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(windowStart, subscriptionDayOfMonth);

    Instant rangeStart = windowStart.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant rangeEnd = resetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    long consumed = eventDAO.sumByTimestampRange(rangeStart, rangeEnd);
    Map<String, Long> rawBreakdown = eventDAO.activityBreakdownByRange(rangeStart, rangeEnd);
    Map<String, Long> displayBuckets = mapToDisplayBuckets(rawBreakdown);

    Optional<ConsumptionLimitConfig> limitConfig = limitConfigDAO.getConfig(currentOrgId());
    Long limit = limitConfig.map(ConsumptionLimitConfig::getMonthlyLimit).orElse(null);
    Integer warningThresholdPct =
        limit != null ? limitConfig.map(ConsumptionLimitConfig::getWarningThresholdPct).orElse(null) : null;

    ConsumptionSummaryDTO dto = new ConsumptionSummaryDTO();
    dto.setConsumed(consumed);
    dto.setLimit(limit);
    dto.setWarningThresholdPct(warningThresholdPct);
    dto.setPercentUsed(calculatePercentUsed(consumed, limit));
    dto.setRemaining(calculateRemaining(consumed, limit));
    dto.setResetDate(resetDate.toString());
    dto.setBillingWindowStart(windowStart.toString());
    dto.setTier(tier);
    dto.setActivityBreakdown(displayBuckets);
    return dto;
  }

  public List<ConsumptionHistoryEntryDTO> getMonthlyHistory(int subscriptionDayOfMonth) {
    BillingWindowSeries windows = computeRecentWindows(subscriptionDayOfMonth, DEFAULT_HISTORY_MONTHS);
    List<ConsumptionMonthlyTotal> history =
        eventDAO.historyByWindows(windows.starts, windows.ends, windows.labels);
    Optional<ConsumptionLimitConfig> limitConfig = limitConfigDAO.getConfig(currentOrgId());
    Long limit = limitConfig.map(ConsumptionLimitConfig::getMonthlyLimit).orElse(null);

    // Map each window-start label to its window-end so the CSV/DTO can render full ranges.
    Map<LocalDate, LocalDate> endByLabel = new LinkedHashMap<>();
    for (int i = 0; i < windows.labels.size(); i++) {
      endByLabel.put(windows.labels.get(i),
          LocalDate.ofInstant(windows.ends.get(i), ZoneOffset.UTC));
    }

    Map<LocalDate, Long> totalsByPeriod = new LinkedHashMap<>();
    for (LocalDate label : windows.labels) {
      totalsByPeriod.put(label, 0L);
    }
    for (ConsumptionMonthlyTotal total : history) {
      totalsByPeriod.merge(total.getBillingMonth(), total.getTotalConsumed(), Long::sum);
    }

    Long limitForCalc = limit;
    return totalsByPeriod.entrySet().stream().map(entry -> {
      ConsumptionHistoryEntryDTO dto = new ConsumptionHistoryEntryDTO();
      dto.setMonth(entry.getKey().toString());
      LocalDate end = endByLabel.get(entry.getKey());
      if (end != null) {
        dto.setWindowEnd(end.toString());
      }
      dto.setConsumed(entry.getValue());
      dto.setLimit(limitForCalc);
      dto.setPercentUsed(calculatePercentUsed(entry.getValue(), limitForCalc));
      dto.setRemaining(calculateRemaining(entry.getValue(), limitForCalc));
      return dto;
    }).collect(Collectors.toList());
  }

  public List<ConsumptionHistoryBreakdownDTO> getHistoryWithBreakdown(Aggregation agg, int subscriptionDayOfMonth) {
    if (agg == Aggregation.MONTHLY) {
      BillingWindowSeries windows = computeRecentWindows(subscriptionDayOfMonth, DEFAULT_HISTORY_MONTHS);
      List<ConsumptionMonthlyBreakdown> rows =
          eventDAO.historyWithBreakdownByWindows(windows.starts, windows.ends, windows.labels);
      return groupBreakdownRows(rows, true, windows.labels);
    }
    if (agg == Aggregation.WEEKLY) {
      BillingWindowSeries weeks = computeRecentWeeklyWindows(DEFAULT_HISTORY_WEEKS);
      List<ConsumptionMonthlyBreakdown> rows =
          eventDAO.weeklyHistoryWithBreakdownByWindows(weeks.starts, weeks.ends, weeks.labels);
      return groupBreakdownRows(rows, true, weeks.labels);
    }
    if (agg == Aggregation.DAILY) {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      LocalDate windowStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDayOfMonth);
      LocalDate resetDate = BillingWindowUtil.calculateResetDate(windowStart, subscriptionDayOfMonth);
      Instant rangeStart = windowStart.atStartOfDay(ZoneOffset.UTC).toInstant();
      Instant rangeEnd = resetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
      List<ConsumptionMonthlyBreakdown> rows =
          eventDAO.dailyHistoryWithBreakdownByWindow(rangeStart, rangeEnd);
      List<LocalDate> expectedDays = computeDailyPeriods(windowStart, today, resetDate);
      return groupBreakdownRows(rows, true, expectedDays);
    }
    throw new IllegalStateException("Unhandled aggregation type: " + agg);
  }

  public List<ConsumptionHistoryBreakdownDTO> getMonthlyHistoryBySource(int subscriptionDayOfMonth) {
    BillingWindowSeries windows = computeRecentWindows(subscriptionDayOfMonth, DEFAULT_HISTORY_MONTHS);
    List<ConsumptionMonthlyBreakdown> rows =
        eventDAO.historyBySourceByWindows(windows.starts, windows.ends, windows.labels);
    return groupBreakdownRows(rows, false, windows.labels);
  }

  /** Compute the last {@code n} billing windows ending at the current window, ASC (oldest first). */
  private static BillingWindowSeries computeRecentWindows(int subscriptionDayOfMonth, int n) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate currentStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDayOfMonth);
    LocalDate[] starts = new LocalDate[n];
    starts[n - 1] = currentStart;
    for (int i = n - 2; i >= 0; i--) {
      starts[i] = BillingWindowUtil.calculatePreviousWindowStart(starts[i + 1], subscriptionDayOfMonth);
    }
    List<LocalDate> labels = new ArrayList<>(n);
    List<Instant> rangeStarts = new ArrayList<>(n);
    List<Instant> rangeEnds = new ArrayList<>(n);
    for (LocalDate windowStart : starts) {
      LocalDate windowEnd = BillingWindowUtil.calculateResetDate(windowStart, subscriptionDayOfMonth);
      labels.add(windowStart);
      rangeStarts.add(windowStart.atStartOfDay(ZoneOffset.UTC).toInstant());
      rangeEnds.add(windowEnd.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
    return new BillingWindowSeries(rangeStarts, rangeEnds, labels);
  }

  /**
   * Compute the last {@code n} 7-day windows ending at today (inclusive), ASC (oldest first).
   * The most recent window covers {@code [today-6, today+1)} so events from the current
   * partial week are always included.
   */
  private static BillingWindowSeries computeRecentWeeklyWindows(int n) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate latestWeekStart = today.minusDays(6);
    LocalDate oldestWeekStart = latestWeekStart.minusDays(7L * (n - 1));
    List<LocalDate> labels = new ArrayList<>(n);
    List<Instant> starts = new ArrayList<>(n);
    List<Instant> ends = new ArrayList<>(n);
    LocalDate weekStart = oldestWeekStart;
    for (int i = 0; i < n; i++) {
      LocalDate weekEnd = weekStart.plusDays(7);
      labels.add(weekStart);
      starts.add(weekStart.atStartOfDay(ZoneOffset.UTC).toInstant());
      ends.add(weekEnd.atStartOfDay(ZoneOffset.UTC).toInstant());
      weekStart = weekStart.plusDays(7);
    }
    return new BillingWindowSeries(starts, ends, labels);
  }

  private record BillingWindowSeries(List<Instant> starts, List<Instant> ends, List<LocalDate> labels)
  {
  }

  public ConsumptionTopAppsResponseDTO getAllConsumingApps(int subscriptionDayOfMonth) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDayOfMonth);
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(windowStart, subscriptionDayOfMonth);
    Instant rangeStart = windowStart.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant rangeEnd = resetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    List<ConsumptionAppTotal> appTotals = eventDAO.topAppsByRange(rangeStart, rangeEnd, MAX_TOP_APPS);
    List<ConsumptionTopAppDTO> apps = appTotals.stream()
        .map(total -> new ConsumptionTopAppDTO(total.getAppId(), total.getPublicId(), total.getName(),
            total.getComponentCount()))
        .collect(Collectors.toList());
    int totalApps = eventDAO.countDistinctAppsByRange(rangeStart, rangeEnd);
    long totalConsumed = eventDAO.sumByTimestampRange(rangeStart, rangeEnd);
    return new ConsumptionTopAppsResponseDTO(apps, totalApps, totalConsumed);
  }

  public ConsumptionDailyHistoryDTO getDailyHistory(int subscriptionDayOfMonth) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDayOfMonth);
    LocalDate resetDate = BillingWindowUtil.calculateResetDate(windowStart, subscriptionDayOfMonth);
    Instant rangeStart = windowStart.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant rangeEnd = resetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    List<ConsumptionDailyTotal> rows = eventDAO.dailyHistoryByWindow(rangeStart, rangeEnd);

    // The Evaluated Components tile renders a cumulative trend (left=old, right=new) so
    // callers expect ASC order. Iterate ASC for cumulative correctness AND emit ASC.
    // Bound the loop by resetDate (exclusive) so a clock-skewed today never drives a
    // runaway iteration past the current billing window.
    Map<LocalDate, Long> dayTotalsAsc = new LinkedHashMap<>();
    LocalDate cursor = windowStart;
    while (!cursor.isAfter(today) && cursor.isBefore(resetDate)) {
      dayTotalsAsc.put(cursor, 0L);
      cursor = cursor.plusDays(1);
    }
    for (ConsumptionDailyTotal row : rows) {
      dayTotalsAsc.merge(row.getDay(), row.getComponentCount(), Long::sum);
    }

    List<ConsumptionDailyHistoryDTO.DailyEntry> entries = new ArrayList<>();
    long cumulative = 0;
    long peak = 0;
    String peakDate = null;
    long total = 0;
    for (Map.Entry<LocalDate, Long> entry : dayTotalsAsc.entrySet()) {
      long count = entry.getValue();
      cumulative += count;
      total += count;
      entries.add(new ConsumptionDailyHistoryDTO.DailyEntry(
          entry.getKey().toString(), count, cumulative));
      if (count > peak) {
        peak = count;
        peakDate = entry.getKey().toString();
      }
    }

    double dailyAverage = dayTotalsAsc.isEmpty() ? 0.0 : (double) total / dayTotalsAsc.size();
    return new ConsumptionDailyHistoryDTO(
        entries, dailyAverage, new ConsumptionDailyHistoryDTO.PeakDay(peak, peakDate));
  }

  /**
   * Days from windowStart through today inclusive, ASC (oldest first). Used to ensure
   * daily charts render every day of the current billing window even if some days have no events.
   * Bounded by resetDate (exclusive) so a clock-skewed today never drives a runaway iteration
   * past the current billing window.
   */
  private static List<LocalDate> computeDailyPeriods(LocalDate windowStart, LocalDate today, LocalDate resetDate) {
    List<LocalDate> days = new ArrayList<>();
    LocalDate cursor = windowStart;
    while (!cursor.isAfter(today) && cursor.isBefore(resetDate)) {
      days.add(cursor);
      cursor = cursor.plusDays(1);
    }
    return days;
  }

  private Map<String, Long> mapToDisplayBuckets(Map<String, Long> rawBreakdown) {
    Map<String, Long> buckets = new LinkedHashMap<>();
    for (ActivityType type : ActivityType.values()) {
      if (type == ActivityType.OTHERS) {
        continue;
      }
      buckets.putIfAbsent(type.getDisplayBucket(), 0L);
    }
    for (Map.Entry<String, Long> row : rawBreakdown.entrySet()) {
      ActivityType type = resolveActivityType(row.getKey());
      buckets.merge(type.getDisplayBucket(), row.getValue(), Long::sum);
    }
    return buckets;
  }

  private static ActivityType resolveActivityType(String raw) {
    if (raw == null) {
      return ActivityType.OTHERS;
    }
    try {
      return ActivityType.valueOf(raw);
    }
    catch (IllegalArgumentException e) {
      return ActivityType.OTHERS;
    }
  }

  private static String currentOrgId() {
    ConsumptionContext ctx = ConsumptionContext.get();
    if (ctx != null && ctx.getOrgId() != null) {
      return ctx.getOrgId();
    }
    log.warn("ConsumptionContext unavailable; falling back to root organization for limit lookup. "
        + "May indicate a missing context filter or upstream license/tier resolution failure.");
    return Organization.ROOT_ORGANIZATION_ID;
  }

  static Double calculatePercentUsed(long consumed, Long limit) {
    return limit != null && limit > 0 ? Math.floor(consumed * 1000.0 / limit) / 10.0 : null;
  }

  static Long calculateRemaining(long consumed, Long limit) {
    return limit != null ? limit - consumed : null;
  }

  private List<ConsumptionHistoryBreakdownDTO> groupBreakdownRows(
      List<ConsumptionMonthlyBreakdown> rows,
      boolean applyDisplayBuckets)
  {
    return groupBreakdownRows(rows, applyDisplayBuckets, /* expectedPeriods */ null);
  }

  /**
   * Group raw breakdown rows by period, padding any expected period that has no rows
   * with a zero-filled entry. When {@code expectedPeriods} is null, no padding happens
   * (returns only periods that actually have data, in DESC order from the input).
   */
  private List<ConsumptionHistoryBreakdownDTO> groupBreakdownRows(
      List<ConsumptionMonthlyBreakdown> rows,
      boolean applyDisplayBuckets,
      List<LocalDate> expectedPeriods)
  {
    Map<LocalDate, Map<String, Long>> grouped = new LinkedHashMap<>();
    Map<LocalDate, Long> totals = new LinkedHashMap<>();

    if (expectedPeriods != null) {
      // Seed in caller-specified order so output matches expected sequence even when no rows exist
      for (LocalDate period : expectedPeriods) {
        grouped.put(period, new LinkedHashMap<>());
        totals.put(period, 0L);
      }
    }

    for (ConsumptionMonthlyBreakdown row : rows) {
      LocalDate period = row.getBillingMonth();
      grouped.computeIfAbsent(period, k -> new LinkedHashMap<>())
          .merge(row.getGroupKey(), row.getComponentCount(), Long::sum);
      totals.merge(period, row.getComponentCount(), Long::sum);
    }

    return grouped.entrySet().stream().map(entry -> {
      LocalDate period = entry.getKey();
      Map<String, Long> breakdown = applyDisplayBuckets
          ? mapToDisplayBuckets(entry.getValue())
          : entry.getValue();
      return new ConsumptionHistoryBreakdownDTO(period.toString(), totals.get(period), breakdown);
    }).collect(Collectors.toList());
  }
}
