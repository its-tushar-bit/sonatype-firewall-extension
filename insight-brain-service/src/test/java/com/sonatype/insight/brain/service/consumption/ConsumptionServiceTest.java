/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionEventDAO;
import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionLimitConfigDAO;
import com.sonatype.insight.brain.model.Organization;
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
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionTopAppsResponseDTO;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConsumptionService}.
 *
 * @since 1.204
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsumptionServiceTest
{
  private static final String TEST_ORG_ID = "test-org-id";

  @Mock
  private ConsumptionEventDAO eventDAO;

  @Mock
  private ConsumptionLimitConfigDAO limitConfigDAO;

  @InjectMocks
  private ConsumptionService service;

  @Before
  public void setUp() {
    ConsumptionContext.set(TEST_ORG_ID, "STANDARD", "TEST");
    when(eventDAO.activityBreakdownByRange(any(Instant.class), any(Instant.class))).thenReturn(new HashMap<>());
    when(eventDAO.historyByWindows(anyList(), anyList(), anyList())).thenReturn(Collections.emptyList());
  }

  @After
  public void tearDown() {
    ConsumptionContext.clear();
  }

  @Test
  public void getCurrentMonthSummary_withLimit() {
    when(eventDAO.sumByTimestampRange(any(), any())).thenReturn(5000L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(10000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    assertThat(result.getConsumed()).isEqualTo(5000L);
    assertThat(result.getLimit()).isEqualTo(10000L);
    assertThat(result.getWarningThresholdPct()).isEqualTo(80);
    assertThat(result.getPercentUsed()).isEqualTo(50.0);
    assertThat(result.getRemaining()).isEqualTo(5000L);
    assertThat(result.getResetDate()).isNotNull();
    assertThat(result.getBillingWindowStart()).isNotNull();
    assertThat(result.getTier()).isEqualTo("ENTERPRISE");
    assertThat(result.getActivityBreakdown()).isNotNull();
  }

  @Test
  public void getCurrentMonthSummary_withoutLimit() {
    when(eventDAO.sumByTimestampRange(any(), any())).thenReturn(3000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.empty());

    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "PROFESSIONAL");

    assertThat(result.getConsumed()).isEqualTo(3000L);
    assertThat(result.getLimit()).isNull();
    assertThat(result.getWarningThresholdPct()).isNull();
    assertThat(result.getPercentUsed()).isNull();
    assertThat(result.getRemaining()).isNull();
  }

  @Test
  public void getCurrentMonthSummary_customThreshold_flowsThroughToDto() {
    when(eventDAO.sumByTimestampRange(any(), any())).thenReturn(6000L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(10000L);
    config.setWarningThresholdPct(60);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    assertThat(result.getWarningThresholdPct()).isEqualTo(60);
  }

  @Test
  public void getCurrentMonthSummary_noLimitWithConfig_thresholdNotStamped() {
    when(eventDAO.sumByTimestampRange(any(), any())).thenReturn(3000L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setWarningThresholdPct(90);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "PROFESSIONAL");

    assertThat(result.getLimit()).isNull();
    assertThat(result.getWarningThresholdPct()).isNull();
  }

  @Test
  public void getMonthlyHistory_returnsCorrectEntries() {
    int subscriptionDayOfMonth = 15;
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate currentWindowStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDayOfMonth);
    LocalDate prevMonth = BillingWindowUtil.calculatePreviousWindowStart(currentWindowStart, subscriptionDayOfMonth);
    LocalDate prevPrevMonth = BillingWindowUtil.calculatePreviousWindowStart(prevMonth, subscriptionDayOfMonth);
    LocalDate prevPrevPrevMonth = BillingWindowUtil.calculatePreviousWindowStart(prevPrevMonth, subscriptionDayOfMonth);

    List<ConsumptionMonthlyTotal> history = Arrays.asList(
        new ConsumptionMonthlyTotal(prevMonth, 8000L),
        new ConsumptionMonthlyTotal(prevPrevMonth, 7500L),
        new ConsumptionMonthlyTotal(prevPrevPrevMonth, 6000L));
    when(eventDAO.historyByWindows(anyList(), anyList(), anyList())).thenReturn(history);

    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(10000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    List<ConsumptionHistoryEntryDTO> result = service.getMonthlyHistory(subscriptionDayOfMonth);

    assertThat(result).hasSize(12);
    assertThat(result.get(0).getConsumed()).isEqualTo(0L);
    assertThat(result.stream().filter(e -> e.getConsumed() == 8000L).findFirst()).isPresent();
    assertThat(result.stream().filter(e -> e.getConsumed() == 7500L).findFirst()).isPresent();
    assertThat(result.stream().filter(e -> e.getConsumed() == 6000L).findFirst()).isPresent();
  }

  @Test
  public void mapToDisplayBuckets_mergesCorrectly() {
    // Given - raw breakdown with counts for each activity type
    Map<String, Long> rawBreakdown = new HashMap<>();
    // These should be merged into "App Scan + Re-evaluate"
    rawBreakdown.put("APP_SCAN", 1000L);
    rawBreakdown.put("RE_EVALUATE", 500L);
    // These should be merged into "Version Recommendations"
    rawBreakdown.put("DEVELOPER_PRIORITIES", 200L);
    rawBreakdown.put("VERSION_RECOMMENDATION", 300L);
    // These should stay separate
    rawBreakdown.put("CONTINUOUS_MONITORING", 150L);
    rawBreakdown.put("COMPONENT_DETAILS", 75L);
    rawBreakdown.put("REACHABILITY", 50L);
    rawBreakdown.put("API", 25L);

    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(2300L); // Total of all
    when(eventDAO.activityBreakdownByRange(any(Instant.class), any(Instant.class))).thenReturn(rawBreakdown);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.empty());

    // When
    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    // Then
    Map<String, Long> buckets = result.getActivityBreakdown();

    // APP_SCAN + RE_EVALUATE merged
    assertThat(buckets.get("App Scan + Re-evaluate")).isEqualTo(1500L);

    // DEVELOPER_PRIORITIES + VERSION_RECOMMENDATION merged
    assertThat(buckets.get("Version Recommendations")).isEqualTo(500L);

    // Others stay separate
    assertThat(buckets.get("Continuous Monitoring")).isEqualTo(150L);
    assertThat(buckets.get("Component Details")).isEqualTo(75L);
    assertThat(buckets.get("Reachability Analysis")).isEqualTo(50L);
    assertThat(buckets.get("APIs")).isEqualTo(25L);
  }

  @Test
  public void getCurrentMonthSummary_percentUsed_roundsToOneDecimal() {
    // Given - 3333 / 10000 = 33.33% -> should be 33.3%
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(3333L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(10000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    // When
    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    // Then
    assertThat(result.getPercentUsed()).isEqualTo(33.3);
  }

  @Test
  public void getMonthlyHistory_withEmptyHistory_returns12ZeroedEntries() {
    // Given - DAO returns no data, service pads to 12 windows
    when(eventDAO.historyByWindows(anyList(), anyList(), anyList())).thenReturn(Collections.emptyList());
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.empty());

    // When
    List<ConsumptionHistoryEntryDTO> result = service.getMonthlyHistory(15);

    // Then - result has exactly 12 zeroed entries (padded)
    assertThat(result).hasSize(12);
    assertThat(result).allMatch(entry -> entry.getConsumed() == 0L);
  }

  @Test
  public void getMonthlyHistory_withSubscriptionDay31_clampsShortMonths() {
    when(eventDAO.historyByWindows(anyList(), anyList(), anyList())).thenReturn(Collections.emptyList());
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.empty());

    List<ConsumptionHistoryEntryDTO> result = service.getMonthlyHistory(31);

    assertThat(result).hasSize(12);
    assertThat(result).extracting(ConsumptionHistoryEntryDTO::getMonth).isSorted();
    assertThat(result).allMatch(entry -> entry.getMonth() != null);
    assertThat(result).noneMatch(entry -> {
      LocalDate d = LocalDate.parse(entry.getMonth());
      return d.getDayOfMonth() > d.lengthOfMonth();
    });
  }

  @Test
  public void getMonthlyHistory_passesMonthlyWindowBoundariesToDao() {
    // Pin first/last window boundaries so a regression in computeRecentWindows
    // (wrong N, off-by-one, duplicates) fails the test instead of being absorbed
    // by anyList() matchers in other tests.
    when(eventDAO.historyByWindows(anyList(), anyList(), anyList())).thenReturn(Collections.emptyList());
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.empty());

    int subscriptionDay = 1;
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate currentBillingStart = BillingWindowUtil.calculateWindowStart(today, subscriptionDay);
    LocalDate currentResetDate = BillingWindowUtil.calculateResetDate(currentBillingStart, subscriptionDay);
    LocalDate oldestBillingStart = currentBillingStart;
    for (int i = 0; i < 11; i++) {
      oldestBillingStart = BillingWindowUtil.calculatePreviousWindowStart(oldestBillingStart, subscriptionDay);
    }

    service.getMonthlyHistory(subscriptionDay);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Instant>> startsCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Instant>> endsCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<LocalDate>> labelsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventDAO).historyByWindows(startsCaptor.capture(), endsCaptor.capture(), labelsCaptor.capture());

    List<Instant> starts = startsCaptor.getValue();
    List<Instant> ends = endsCaptor.getValue();
    List<LocalDate> labels = labelsCaptor.getValue();

    assertThat(starts).hasSize(12);
    assertThat(ends).hasSize(12);
    assertThat(labels).hasSize(12);

    // Oldest window (index 0) is 11 months before current.
    assertThat(labels.get(0)).isEqualTo(oldestBillingStart);
    assertThat(starts.get(0)).isEqualTo(oldestBillingStart.atStartOfDay(ZoneOffset.UTC).toInstant());

    // Most recent window (index 11) starts at currentBillingStart and ends at resetDate.
    assertThat(labels.get(11)).isEqualTo(currentBillingStart);
    assertThat(starts.get(11)).isEqualTo(currentBillingStart.atStartOfDay(ZoneOffset.UTC).toInstant());
    assertThat(ends.get(11)).isEqualTo(currentResetDate.atStartOfDay(ZoneOffset.UTC).toInstant());

    // Labels are strictly ascending (no duplicates).
    assertThat(labels).isSorted();
  }

  // --- BDD: activity-bucket-classification.feature ---

  @Test
  public void mapToDisplayBuckets_allActivityTypes_producesExactly6Buckets() {
    // Given - one event per activity type
    Map<String, Long> rawBreakdown = new HashMap<>();
    rawBreakdown.put("APP_SCAN", 100L);
    rawBreakdown.put("RE_EVALUATE", 100L);
    rawBreakdown.put("CONTINUOUS_MONITORING", 100L);
    rawBreakdown.put("COMPONENT_DETAILS", 100L);
    rawBreakdown.put("VERSION_RECOMMENDATION", 100L);
    rawBreakdown.put("REACHABILITY", 100L);
    rawBreakdown.put("API", 100L);
    rawBreakdown.put("DEVELOPER_PRIORITIES", 100L);

    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(800L);
    when(eventDAO.activityBreakdownByRange(any(Instant.class), any(Instant.class))).thenReturn(rawBreakdown);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.empty());

    // When
    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    // Then - 8 enum values collapse to exactly 6 display buckets
    Map<String, Long> buckets = result.getActivityBreakdown();
    assertThat(buckets).hasSize(6);
    assertThat(buckets).containsKeys(
        "App Scan + Re-evaluate",
        "Continuous Monitoring",
        "Component Details",
        "Version Recommendations",
        "Reachability Analysis",
        "APIs");
  }

  // --- BDD: limit-enforcement.feature ---

  @Test
  public void getCurrentMonthSummary_overLimit_returnsPercentAbove100() {
    // Given - 15000 consumed / 10000 limit = 150%
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(15000L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(10000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    // When
    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    // Then
    assertThat(result.getPercentUsed()).isEqualTo(150.0);
    assertThat(result.getRemaining()).isEqualTo(-5000L);
  }

  @Test
  public void getCurrentMonthSummary_at80PercentThreshold() {
    // Given - 8000 consumed / 10000 limit = 80%
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(8000L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(10000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    // When
    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    // Then
    assertThat(result.getPercentUsed()).isEqualTo(80.0);
    assertThat(result.getRemaining()).isEqualTo(2000L);
  }

  @Test
  public void getCurrentMonthSummary_overLimit_negativeRemaining() {
    // Given - 52000 consumed / 50000 limit
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(52000L);
    ConsumptionLimitConfig config = new ConsumptionLimitConfig("test-org");
    config.setMonthlyLimit(50000L);
    when(limitConfigDAO.getConfig(eq(TEST_ORG_ID))).thenReturn(Optional.of(config));

    // When
    ConsumptionSummaryDTO result = service.getCurrentMonthSummary(15, "ENTERPRISE");

    // Then
    assertThat(result.getRemaining()).isEqualTo(-2000L);
  }

  // --- I4: topApps capping ---

  @Test
  public void getAllConsumingApps_capsResultsAtMaxLimit() {
    List<ConsumptionAppTotal> apps = Arrays.asList(
        new ConsumptionAppTotal("app-1", "pub-1", "App 1", 100),
        new ConsumptionAppTotal("app-2", "pub-2", "App 2", 90),
        new ConsumptionAppTotal("app-3", "pub-3", "App 3", 80),
        new ConsumptionAppTotal("app-4", "pub-4", "App 4", 70),
        new ConsumptionAppTotal("app-5", "pub-5", "App 5", 60),
        new ConsumptionAppTotal("app-6", "pub-6", "App 6", 50),
        new ConsumptionAppTotal("app-7", "pub-7", "App 7", 40),
        new ConsumptionAppTotal("app-8", "pub-8", "App 8", 30),
        new ConsumptionAppTotal("app-9", "pub-9", "App 9", 20),
        new ConsumptionAppTotal("app-10", "pub-10", "App 10", 10));
    when(eventDAO.topAppsByRange(any(Instant.class), any(Instant.class), anyInt())).thenReturn(apps);
    when(eventDAO.countDistinctAppsByRange(any(Instant.class), any(Instant.class))).thenReturn(47);
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(900L);

    ConsumptionTopAppsResponseDTO result = service.getAllConsumingApps(15);

    assertThat(result.getApps()).hasSize(10);
    assertThat(result.getTotalApps()).isEqualTo(47);
    assertThat(result.getTotalConsumed()).isEqualTo(900L);
    assertThat(result.getApps().get(0).getAppId()).isEqualTo("app-1");
    assertThat(result.getApps().get(0).getPublicId()).isEqualTo("pub-1");
    assertThat(result.getApps().get(0).getName()).isEqualTo("App 1");
  }

  @Test
  public void getAllConsumingApps_propagatesNullPublicIdAndName_whenAppIsDeleted() {
    when(eventDAO.topAppsByRange(any(Instant.class), any(Instant.class), anyInt())).thenReturn(Arrays.asList(
        new ConsumptionAppTotal("orphan-internal", null, null, 42)));
    when(eventDAO.countDistinctAppsByRange(any(Instant.class), any(Instant.class))).thenReturn(1);
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(42L);

    ConsumptionTopAppsResponseDTO result = service.getAllConsumingApps(1);

    assertThat(result.getApps()).hasSize(1);
    assertThat(result.getApps().get(0).getAppId()).isEqualTo("orphan-internal");
    assertThat(result.getApps().get(0).getPublicId()).isNull();
    assertThat(result.getApps().get(0).getName()).isNull();
    assertThat(result.getApps().get(0).getConsumed()).isEqualTo(42L);
  }

  @Test
  public void getAllConsumingApps_passesMaxLimitToDao() {
    when(eventDAO.topAppsByRange(any(Instant.class), any(Instant.class), anyInt())).thenReturn(Collections.emptyList());
    when(eventDAO.countDistinctAppsByRange(any(Instant.class), any(Instant.class))).thenReturn(0);
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(0L);

    service.getAllConsumingApps(15);

    verify(eventDAO).topAppsByRange(any(Instant.class), any(Instant.class), eq(10));
  }

  @Test
  public void getAllConsumingApps_totalAppsReportsDistinctCount_notCappedListSize() {
    List<ConsumptionAppTotal> top10 = Arrays.asList(
        new ConsumptionAppTotal("app-1", "pub-1", "App 1", 100),
        new ConsumptionAppTotal("app-2", "pub-2", "App 2", 90),
        new ConsumptionAppTotal("app-3", "pub-3", "App 3", 80),
        new ConsumptionAppTotal("app-4", "pub-4", "App 4", 70),
        new ConsumptionAppTotal("app-5", "pub-5", "App 5", 60),
        new ConsumptionAppTotal("app-6", "pub-6", "App 6", 50),
        new ConsumptionAppTotal("app-7", "pub-7", "App 7", 40),
        new ConsumptionAppTotal("app-8", "pub-8", "App 8", 30),
        new ConsumptionAppTotal("app-9", "pub-9", "App 9", 20),
        new ConsumptionAppTotal("app-10", "pub-10", "App 10", 10));
    when(eventDAO.topAppsByRange(any(Instant.class), any(Instant.class), anyInt())).thenReturn(top10);
    when(eventDAO.countDistinctAppsByRange(any(Instant.class), any(Instant.class))).thenReturn(123);
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(5000L);

    ConsumptionTopAppsResponseDTO result = service.getAllConsumingApps(15);

    assertThat(result.getApps()).hasSize(10);
    assertThat(result.getTotalApps()).isEqualTo(123);
    assertThat(result.getTotalConsumed()).isEqualTo(5000L);
  }

  @Test
  public void getCurrentMonthSummary_passesOrgIdFromContext_toLimitConfigDAO() {
    ConsumptionContext.clear();
    ConsumptionContext.set("explicit-tenant-A", "ENTERPRISE", "API");
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(0L);
    when(limitConfigDAO.getConfig(eq("explicit-tenant-A"))).thenReturn(Optional.empty());

    service.getCurrentMonthSummary(15, "ENTERPRISE");

    verify(limitConfigDAO).getConfig(eq("explicit-tenant-A"));
  }

  @Test
  public void getCurrentMonthSummary_fallsBackToRootOrg_whenNoContext() {
    ConsumptionContext.clear();
    when(eventDAO.sumByTimestampRange(any(Instant.class), any(Instant.class))).thenReturn(0L);
    when(limitConfigDAO.getConfig(eq(Organization.ROOT_ORGANIZATION_ID))).thenReturn(Optional.empty());

    service.getCurrentMonthSummary(15, "ENTERPRISE");

    verify(limitConfigDAO).getConfig(eq(Organization.ROOT_ORGANIZATION_ID));
  }

  // --- I3: getHistoryWithBreakdown coverage ---

  @Test
  public void getHistoryWithBreakdown_monthly_groupsRowsByMonth() {
    LocalDate label = LocalDate.of(2026, 1, 1);
    List<ConsumptionMonthlyBreakdown> rows = Arrays.asList(
        new ConsumptionMonthlyBreakdown(label, "APP_SCAN", 100L),
        new ConsumptionMonthlyBreakdown(label, "COMPONENT_DETAILS", 50L));
    when(eventDAO.historyWithBreakdownByWindows(anyList(), anyList(), anyList())).thenReturn(rows);

    List<ConsumptionHistoryBreakdownDTO> result = service.getHistoryWithBreakdown(Aggregation.MONTHLY, 1);

    assertThat(result).extracting(ConsumptionHistoryBreakdownDTO::getMonth).contains(label.toString());
  }

  @Test
  public void getHistoryWithBreakdown_emptyResult_returnsZeroPaddedWindows() {
    when(eventDAO.historyWithBreakdownByWindows(anyList(), anyList(), anyList()))
        .thenReturn(Collections.emptyList());

    List<ConsumptionHistoryBreakdownDTO> result = service.getHistoryWithBreakdown(Aggregation.MONTHLY, 1);

    assertThat(result).hasSize(12);
  }

  @Test
  public void getHistoryWithBreakdown_weekly_callsWeeklyDao() {
    when(eventDAO.weeklyHistoryWithBreakdownByWindows(anyList(), anyList(), anyList()))
        .thenReturn(Collections.emptyList());

    service.getHistoryWithBreakdown(Aggregation.WEEKLY, 1);

    verify(eventDAO).weeklyHistoryWithBreakdownByWindows(anyList(), anyList(), anyList());
  }

  @Test
  public void getHistoryWithBreakdown_daily_callsDailyDao() {
    when(eventDAO.dailyHistoryWithBreakdownByWindow(any(Instant.class), any(Instant.class)))
        .thenReturn(Collections.emptyList());

    service.getHistoryWithBreakdown(Aggregation.DAILY, 1);

    verify(eventDAO).dailyHistoryWithBreakdownByWindow(any(Instant.class), any(Instant.class));
  }

  // --- I3: getMonthlyHistoryBySource coverage ---

  @Test
  public void getMonthlyHistoryBySource_groupsRowsBySource() {
    LocalDate label = LocalDate.of(2026, 1, 1);
    List<ConsumptionMonthlyBreakdown> rows = Arrays.asList(
        new ConsumptionMonthlyBreakdown(label, "API", 30L),
        new ConsumptionMonthlyBreakdown(label, "UI", 20L));
    when(eventDAO.historyBySourceByWindows(anyList(), anyList(), anyList())).thenReturn(rows);

    List<ConsumptionHistoryBreakdownDTO> result = service.getMonthlyHistoryBySource(1);

    assertThat(result).extracting(ConsumptionHistoryBreakdownDTO::getMonth).contains(label.toString());
  }

  @Test
  public void getMonthlyHistoryBySource_emptyResult_returnsZeroPaddedWindows() {
    when(eventDAO.historyBySourceByWindows(anyList(), anyList(), anyList()))
        .thenReturn(Collections.emptyList());

    List<ConsumptionHistoryBreakdownDTO> result = service.getMonthlyHistoryBySource(1);

    assertThat(result).hasSize(12);
  }

  // --- I3: getMonthlyHistoryByStage coverage ---

  @Test
  public void getMonthlyHistoryByStage_callsDAOWith12WindowsAndPivotsRows() {
    LocalDate label = LocalDate.of(2026, 1, 1);
    List<ConsumptionMonthlyBreakdown> rows = Arrays.asList(
        new ConsumptionMonthlyBreakdown(label, "build", 100L),
        new ConsumptionMonthlyBreakdown(label, "release", 25L));
    when(eventDAO.historyByStageByWindows(anyList(), anyList(), anyList())).thenReturn(rows);

    List<ConsumptionHistoryBreakdownDTO> result = service.getMonthlyHistoryByStage(1);

    assertThat(result).extracting(ConsumptionHistoryBreakdownDTO::getMonth).contains(label.toString());
    // Find the entry corresponding to our label and verify its breakdown contains both stages.
    ConsumptionHistoryBreakdownDTO matched = result.stream()
        .filter(dto -> label.toString().equals(dto.getMonth()))
        .findFirst()
        .orElseThrow();
    assertThat(matched.getBreakdown()).containsEntry("build", 100L);
    assertThat(matched.getBreakdown()).containsEntry("release", 25L);

    // Verify the DAO was actually called with 12 windows — matches the test name.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Instant>> startsCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Instant>> endsCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<LocalDate>> labelsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventDAO).historyByStageByWindows(startsCaptor.capture(), endsCaptor.capture(), labelsCaptor.capture());
    assertThat(startsCaptor.getValue()).hasSize(12);
    assertThat(endsCaptor.getValue()).hasSize(12);
    assertThat(labelsCaptor.getValue()).hasSize(12);
  }

  @Test
  public void getMonthlyHistoryByStage_emptyResult_returnsZeroPaddedWindows() {
    when(eventDAO.historyByStageByWindows(anyList(), anyList(), anyList()))
        .thenReturn(Collections.emptyList());

    List<ConsumptionHistoryBreakdownDTO> result = service.getMonthlyHistoryByStage(1);

    assertThat(result).hasSize(12);
  }

  // --- I3: getDailyHistory coverage ---

  @Test
  public void getDailyHistory_returnsDailyAverageAndPeak() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = BillingWindowUtil.calculateWindowStart(today, 1);
    List<ConsumptionDailyTotal> rows = Arrays.asList(
        new ConsumptionDailyTotal(windowStart, 10L),
        new ConsumptionDailyTotal(windowStart.plusDays(1), 30L),
        new ConsumptionDailyTotal(windowStart.plusDays(2), 5L));
    when(eventDAO.dailyHistoryByWindow(any(Instant.class), any(Instant.class))).thenReturn(rows);

    ConsumptionDailyHistoryDTO result = service.getDailyHistory(1);

    assertThat(result).isNotNull();
    assertThat(result.getDailyHistory()).isNotEmpty();
    assertThat(result.getDailyAverage()).isGreaterThan(0.0);
    assertThat(result.getPeakDay()).isNotNull();
    assertThat(result.getPeakDay().getCount()).isEqualTo(30L);
    assertThat(result.getPeakDay().getDate()).isEqualTo(windowStart.plusDays(1).toString());
  }

  @Test
  public void getDailyHistory_noRows_returnsZeroEntriesAndNullPeakDate() {
    when(eventDAO.dailyHistoryByWindow(any(Instant.class), any(Instant.class)))
        .thenReturn(Collections.emptyList());

    ConsumptionDailyHistoryDTO result = service.getDailyHistory(1);

    assertThat(result).isNotNull();
    assertThat(result.getDailyHistory()).allMatch(e -> e.getComponents() == 0L);
    assertThat(result.getPeakDay()).isNotNull();
    assertThat(result.getPeakDay().getCount()).isEqualTo(0L);
    assertThat(result.getPeakDay().getDate()).isNull();
  }

  @Test
  public void getDailyHistory_multipleDaysTiedForPeak_firstSeenWins() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = BillingWindowUtil.calculateWindowStart(today, 1);
    LocalDate dayA = windowStart;
    LocalDate dayB = windowStart.plusDays(1);
    List<ConsumptionDailyTotal> rows = Arrays.asList(
        new ConsumptionDailyTotal(dayA, 50L),
        new ConsumptionDailyTotal(dayB, 50L),
        new ConsumptionDailyTotal(windowStart.plusDays(2), 10L));
    when(eventDAO.dailyHistoryByWindow(any(Instant.class), any(Instant.class))).thenReturn(rows);

    ConsumptionDailyHistoryDTO result = service.getDailyHistory(1);

    assertThat(result.getPeakDay().getCount()).isEqualTo(50L);
    assertThat(result.getPeakDay().getDate()).isEqualTo(dayA.toString());
  }

}
