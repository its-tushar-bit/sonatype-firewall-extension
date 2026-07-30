/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionAppTotal;
import com.sonatype.insight.brain.model.consumption.ConsumptionDailyTotal;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.model.consumption.ConsumptionMonthlyBreakdown;
import com.sonatype.insight.brain.model.consumption.ConsumptionMonthlyTotal;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link ConsumptionEventDAO} using real H2 database.
 *
 * @since 1.204
 */
public class ConsumptionEventDAOTest
    extends AbstractDataTest
{
  private ConsumptionEventDAO dao;

  @Before
  public void setup() {
    initialize();
    dao = daoFactory.createConsumptionEventDAO();
  }

  @Test
  public void sumByMonth_noData_returnsZero() {
    long result = dao.sumByMonth(LocalDate.of(2026, 5, 1));

    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void batchInsert_and_sumByMonth_roundTrip() {
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);
    List<ConsumptionEvent> events = Arrays.asList(
        buildEvent("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth),
        buildEvent("org-1", "app-2", ActivityType.RE_EVALUATE, 50, billingMonth));

    tempEntity.insertConsumptionEvents(events);

    long total = dao.sumByMonth(billingMonth);
    assertThat(total).isEqualTo(150L);
  }

  @Test
  public void activityBreakdownByMonth_groupsByActivityType() {
    LocalDate billingMonth = LocalDate.of(2026, 3, 1);
    List<ConsumptionEvent> events = Arrays.asList(
        buildEvent("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth),
        buildEvent("org-1", "app-1", ActivityType.APP_SCAN, 200, billingMonth),
        buildEvent("org-1", "app-2", ActivityType.CONTINUOUS_MONITORING, 50, billingMonth));

    tempEntity.insertConsumptionEvents(events);

    Map<String, Long> breakdown = dao.activityBreakdownByMonth(billingMonth);
    assertThat(breakdown.get("APP_SCAN")).isEqualTo(300L);
    assertThat(breakdown.get("CONTINUOUS_MONITORING")).isEqualTo(50L);
  }

  @Test
  public void topAppsByMonth_respectsLimit() {
    LocalDate billingMonth = LocalDate.of(2026, 2, 1);
    List<ConsumptionEvent> events = Arrays.asList(
        buildEvent("org-1", "app-a", ActivityType.APP_SCAN, 500, billingMonth),
        buildEvent("org-1", "app-b", ActivityType.APP_SCAN, 300, billingMonth),
        buildEvent("org-1", "app-c", ActivityType.APP_SCAN, 100, billingMonth));

    tempEntity.insertConsumptionEvents(events);

    List<ConsumptionAppTotal> topApps = dao.topAppsByMonth(billingMonth, 2);
    assertThat(topApps).hasSize(2);
    assertThat(topApps.get(0).getAppId()).isEqualTo("app-a");
    assertThat(topApps.get(0).getComponentCount()).isEqualTo(500L);
    assertThat(topApps.get(1).getAppId()).isEqualTo("app-b");
  }

  @Test
  public void topAppsByMonth_joinsApplicationPublicIdAndName_whenAppExists() {
    com.sonatype.insight.brain.model.Application app = tempEntity.newApplicationWithParent();
    LocalDate billingMonth = LocalDate.of(2026, 6, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEvent("org-1", app.getId(), ActivityType.APP_SCAN, 123, billingMonth)));

    List<ConsumptionAppTotal> topApps = dao.topAppsByMonth(billingMonth, 10);

    assertThat(topApps).hasSize(1);
    assertThat(topApps.get(0).getAppId()).isEqualTo(app.getId());
    assertThat(topApps.get(0).getPublicId()).isEqualTo(app.getPublicId());
    assertThat(topApps.get(0).getName()).isEqualTo(app.getName());
    assertThat(topApps.get(0).getComponentCount()).isEqualTo(123L);
  }

  @Test
  public void topAppsByMonth_returnsNullPublicIdAndName_forOrphanEvent() {
    LocalDate billingMonth = LocalDate.of(2026, 7, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEvent("org-1", "orphan-internal-id", ActivityType.APP_SCAN, 42, billingMonth)));

    List<ConsumptionAppTotal> topApps = dao.topAppsByMonth(billingMonth, 10);

    assertThat(topApps).hasSize(1);
    assertThat(topApps.get(0).getAppId()).isEqualTo("orphan-internal-id");
    assertThat(topApps.get(0).getPublicId()).isNull();
    assertThat(topApps.get(0).getName()).isNull();
    assertThat(topApps.get(0).getComponentCount()).isEqualTo(42L);
  }

  @Test
  public void countDistinctAppsByMonth_countsUniqueAppIdsInWindow() {
    LocalDate billingMonth = LocalDate.of(2026, 8, 1);
    LocalDate otherMonth = LocalDate.of(2026, 9, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEvent("org-1", "app-a", ActivityType.APP_SCAN, 10, billingMonth),
        buildEvent("org-1", "app-a", ActivityType.RE_EVALUATE, 5, billingMonth),
        buildEvent("org-1", "app-b", ActivityType.APP_SCAN, 7, billingMonth),
        buildEvent("org-1", "app-c", ActivityType.APP_SCAN, 3, billingMonth),
        buildEvent("org-1", "app-d", ActivityType.APP_SCAN, 99, otherMonth)));

    assertThat(dao.countDistinctAppsByMonth(billingMonth)).isEqualTo(3);
    assertThat(dao.countDistinctAppsByMonth(otherMonth)).isEqualTo(1);
  }

  @Test
  public void countDistinctAppsByMonth_returnsZero_whenNoEvents() {
    assertThat(dao.countDistinctAppsByMonth(LocalDate.of(2026, 10, 1))).isZero();
  }

  @Test
  public void countDistinctAppsByMonth_excludesNullAppIdEvents() {
    LocalDate billingMonth = LocalDate.of(2026, 11, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEvent("org-1", null, ActivityType.API, 1, billingMonth),
        buildEvent("org-1", "app-x", ActivityType.APP_SCAN, 1, billingMonth)));

    assertThat(dao.countDistinctAppsByMonth(billingMonth)).isEqualTo(1);
  }

  @Test
  public void monthlyHistory_returnsDescendingOrder() {
    LocalDate month1 = LocalDate.of(2026, 1, 1);
    LocalDate month2 = LocalDate.of(2026, 2, 1);
    List<ConsumptionEvent> events = Arrays.asList(
        buildEvent("org-1", "app-1", ActivityType.APP_SCAN, 100, month1),
        buildEvent("org-1", "app-1", ActivityType.APP_SCAN, 200, month2));

    tempEntity.insertConsumptionEvents(events);

    List<ConsumptionMonthlyTotal> history = dao.monthlyHistory(12);
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getBillingMonth()).isEqualTo(month2);
    assertThat(history.get(0).getTotalConsumed()).isEqualTo(200L);
    assertThat(history.get(1).getBillingMonth()).isEqualTo(month1);
  }

  @Test
  public void dailyHistory_returnsRecentDays() {
    Instant now = Instant.now();
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    List<ConsumptionEvent> events = Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 10, billingMonth, now),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 20, billingMonth, now));

    tempEntity.insertConsumptionEvents(events);

    List<ConsumptionDailyTotal> daily = dao.dailyHistory(30, now);
    assertThat(daily).isNotEmpty();
    assertThat(daily.get(0).getComponentCount()).isEqualTo(30L);
  }

  @Test
  public void batchInsert_emptyList_doesNothing() {
    tempEntity.insertConsumptionEvents(Collections.emptyList());

    long total = dao.sumByMonth(LocalDate.of(2026, 5, 1));
    assertThat(total).isEqualTo(0L);
  }

  @Test
  public void recordEvent_contributesToAggregates() {
    LocalDate billingMonth = LocalDate.of(2026, 7, 1);
    Instant timestamp = Instant.parse("2026-07-15T10:30:00Z");
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId("org-record-1");
    event.setAppId("app-record-1");
    event.setScanId("scan-record-1");
    event.setUserId("user-record-1");
    event.setTier("ENTERPRISE");
    event.setSource("API");
    event.setActivityType(ActivityType.APP_SCAN);
    event.setComponentCount(42);
    event.setBillingMonth(billingMonth);
    event.setEventTimestamp(timestamp);

    dao.recordEvent(event);

    assertThat(dao.sumByMonth(billingMonth)).isEqualTo(42L);
    Map<String, Long> breakdown = dao.activityBreakdownByMonth(billingMonth);
    assertThat(breakdown).containsEntry("APP_SCAN", 42L);
  }

  @Test
  public void recordEvent_generatesIdWhenAbsent() {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId("org-record-3");
    event.setTier("ENTERPRISE");
    event.setSource("UI");
    event.setActivityType(ActivityType.APP_SCAN);
    event.setComponentCount(1);
    event.setBillingMonth(LocalDate.of(2026, 9, 1));
    event.setEventTimestamp(Instant.now());
    assertThat(event.getId()).isNull();

    dao.recordEvent(event);

    assertThat(event.getId()).isNotNull().isNotEmpty();
  }

  @Test
  public void recordEvent_propagatesNonUniqueIntegrityViolation() {
    // The H2 fallback narrows the absorbed SQLState to "23505" (unique violation) so
    // non-unique integrity failures — column-overflow, FK, NOT NULL, CHECK — propagate
    // rather than being silently swallowed. Without this narrowness a future widening
    // of the catch back to the broader C23 class would silently drop billing rows.
    // Trigger NOT NULL on org_id by setting it to null at the model level (the setter
    // does not validate, only the DB does).
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId(null); // violates NOT NULL constraint on org_id
    event.setTier("ENTERPRISE");
    event.setSource("API");
    event.setActivityType(ActivityType.APP_SCAN);
    event.setComponentCount(1);
    event.setBillingMonth(LocalDate.of(2026, 12, 1));
    event.setEventTimestamp(Instant.parse("2026-12-10T10:00:00Z"));

    assertThatThrownBy(() -> dao.recordEvent(event)).isInstanceOf(Exception.class);
  }

  @Test
  public void recordEvent_duplicateIdempotencyKey_isIgnored() {
    LocalDate billingMonth = LocalDate.of(2026, 12, 1);
    String sharedKey = "test-dedup-key-abc";

    ConsumptionEvent first = new ConsumptionEvent();
    first.setOrgId("org-dedup-1");
    first.setTier("ENTERPRISE");
    first.setSource("API");
    first.setActivityType(ActivityType.APP_SCAN);
    first.setComponentCount(42);
    first.setBillingMonth(billingMonth);
    first.setEventTimestamp(Instant.parse("2026-12-10T10:00:00Z"));
    first.setIdempotencyKey(sharedKey);

    ConsumptionEvent second = new ConsumptionEvent();
    second.setOrgId("org-dedup-1");
    second.setTier("ENTERPRISE");
    second.setSource("API");
    second.setActivityType(ActivityType.APP_SCAN);
    second.setComponentCount(99);
    second.setBillingMonth(billingMonth);
    second.setEventTimestamp(Instant.parse("2026-12-10T10:00:01Z"));
    second.setIdempotencyKey(sharedKey);

    dao.recordEvent(first);
    dao.recordEvent(second); // same idempotency_key — must be a no-op

    assertThat(dao.sumByMonth(billingMonth)).isEqualTo(42L);
  }

  private ConsumptionEvent buildEvent(
      String orgId,
      String appId,
      ActivityType activityType,
      int componentCount,
      LocalDate billingMonth)
  {
    return buildEventWithTimestamp(orgId, appId, activityType, componentCount, billingMonth, Instant.now());
  }

  private ConsumptionEvent buildEventWithTimestamp(
      String orgId,
      String appId,
      ActivityType activityType,
      int componentCount,
      LocalDate billingMonth,
      Instant timestamp)
  {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId(orgId);
    event.setAppId(appId);
    event.setEventTimestamp(timestamp);
    event.setComponentCount(componentCount);
    event.setActivityType(activityType);
    event.setSource("UI");
    event.setTier("ENTERPRISE");
    event.setBillingMonth(billingMonth);
    return event;
  }

  // --- New range/window-based methods (10 new tests) ---

  @Test
  public void sumByTimestampRange_noData_returnsZero() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    long result = dao.sumByTimestampRange(start, end);
    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void sumByTimestampRange_withData_returnsCorrectSum() {
    Instant start = Instant.parse("2026-05-10T00:00:00Z");
    Instant end = Instant.parse("2026-05-20T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-05-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-2", ActivityType.RE_EVALUATE, 50, billingMonth,
            Instant.parse("2026-05-16T12:00:00Z"))));

    long total = dao.sumByTimestampRange(start, end);
    assertThat(total).isEqualTo(150L);
  }

  @Test
  public void activityBreakdownByRange_groupsByActivityType() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-05-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 200, billingMonth,
            Instant.parse("2026-05-16T12:00:00Z")),
        buildEventWithTimestamp("org-1", "app-2", ActivityType.CONTINUOUS_MONITORING, 50, billingMonth,
            Instant.parse("2026-05-17T14:00:00Z"))));

    Map<String, Long> breakdown = dao.activityBreakdownByRange(start, end);
    assertThat(breakdown.get("APP_SCAN")).isEqualTo(300L);
    assertThat(breakdown.get("CONTINUOUS_MONITORING")).isEqualTo(50L);
  }

  @Test
  public void topAppsByRange_respectsLimit() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-a", ActivityType.APP_SCAN, 500, billingMonth,
            Instant.parse("2026-05-10T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-b", ActivityType.APP_SCAN, 300, billingMonth,
            Instant.parse("2026-05-11T12:00:00Z")),
        buildEventWithTimestamp("org-1", "app-c", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-05-12T14:00:00Z"))));

    List<ConsumptionAppTotal> topApps = dao.topAppsByRange(start, end, 2);
    assertThat(topApps).hasSize(2);
    assertThat(topApps.get(0).getAppId()).isEqualTo("app-a");
    assertThat(topApps.get(0).getComponentCount()).isEqualTo(500L);
    assertThat(topApps.get(1).getAppId()).isEqualTo("app-b");
  }

  @Test
  public void countDistinctAppsByRange_countsUniqueAppIds() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-a", ActivityType.APP_SCAN, 10, billingMonth,
            Instant.parse("2026-05-10T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-a", ActivityType.RE_EVALUATE, 5, billingMonth,
            Instant.parse("2026-05-11T12:00:00Z")),
        buildEventWithTimestamp("org-1", "app-b", ActivityType.APP_SCAN, 7, billingMonth,
            Instant.parse("2026-05-12T14:00:00Z")),
        buildEventWithTimestamp("org-1", "app-c", ActivityType.APP_SCAN, 3, billingMonth,
            Instant.parse("2026-05-13T16:00:00Z"))));

    assertThat(dao.countDistinctAppsByRange(start, end)).isEqualTo(3);
  }

  @Test
  public void historyByWindows_bucketsByWindow() {
    LocalDate label1 = LocalDate.of(2026, 3, 1);
    LocalDate label2 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = label2.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant start2 = end1;
    Instant end2 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

    LocalDate billingMonth = LocalDate.of(2026, 4, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-03-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 200, billingMonth,
            Instant.parse("2026-04-15T12:00:00Z"))));

    List<ConsumptionMonthlyTotal> history = dao.historyByWindows(
        Arrays.asList(start1, start2),
        Arrays.asList(end1, end2),
        Arrays.asList(label1, label2));

    assertThat(history).hasSize(2);
    assertThat(history.get(0).getBillingMonth()).isEqualTo(label1);
    assertThat(history.get(0).getTotalConsumed()).isEqualTo(100L);
    assertThat(history.get(1).getBillingMonth()).isEqualTo(label2);
    assertThat(history.get(1).getTotalConsumed()).isEqualTo(200L);
  }

  @Test
  public void historyWithBreakdownByWindows_groupsByWindowAndActivity() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

    LocalDate billingMonth = LocalDate.of(2026, 4, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-04-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-2", ActivityType.APP_SCAN, 50, billingMonth,
            Instant.parse("2026-04-16T12:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.CONTINUOUS_MONITORING, 30, billingMonth,
            Instant.parse("2026-04-17T14:00:00Z"))));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyWithBreakdownByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(2);
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getBillingMonth()).isEqualTo(label1);
      assertThat(b.getGroupKey()).isEqualTo("APP_SCAN");
      assertThat(b.getComponentCount()).isEqualTo(150L);
    });
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getBillingMonth()).isEqualTo(label1);
      assertThat(b.getGroupKey()).isEqualTo("CONTINUOUS_MONITORING");
      assertThat(b.getComponentCount()).isEqualTo(30L);
    });
  }

  @Test
  public void historyBySourceByWindows_groupsByWindowAndSource() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

    LocalDate billingMonth = LocalDate.of(2026, 4, 1);
    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e1.setSource("UI");
    ConsumptionEvent e2 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 40, billingMonth,
        Instant.parse("2026-04-16T12:00:00Z"));
    e2.setSource("CLI");
    ConsumptionEvent e3 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 10, billingMonth,
        Instant.parse("2026-04-17T14:00:00Z"));
    e3.setSource("API");
    tempEntity.insertConsumptionEvents(Arrays.asList(e1, e2, e3));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyBySourceByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(3);
    assertThat(breakdown).anySatisfy(b -> assertThat(b.getGroupKey()).isEqualTo("UI"));
    assertThat(breakdown).anySatisfy(b -> assertThat(b.getGroupKey()).isEqualTo("CLI"));
    assertThat(breakdown).anySatisfy(b -> assertThat(b.getGroupKey()).isEqualTo("API"));
  }

  @Test
  public void dailyHistoryByWindow_returnsAscendingDays() {
    Instant start = Instant.parse("2026-05-10T00:00:00Z");
    Instant end = Instant.parse("2026-05-20T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 10, billingMonth,
            Instant.parse("2026-05-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 20, billingMonth,
            Instant.parse("2026-05-15T12:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 30, billingMonth,
            Instant.parse("2026-05-16T14:00:00Z"))));

    List<ConsumptionDailyTotal> daily = dao.dailyHistoryByWindow(start, end);
    assertThat(daily).isNotEmpty();
    assertThat(daily.stream().map(ConsumptionDailyTotal::getDay).toList()).isSorted();
    assertThat(daily.stream().filter(d -> d.getDay().equals(LocalDate.of(2026, 5, 15))).findFirst())
        .isPresent()
        .hasValueSatisfying(d -> assertThat(d.getComponentCount()).isEqualTo(30L));
    assertThat(daily.stream().filter(d -> d.getDay().equals(LocalDate.of(2026, 5, 16))).findFirst())
        .isPresent()
        .hasValueSatisfying(d -> assertThat(d.getComponentCount()).isEqualTo(30L));
  }

  @Test
  public void dailyHistoryWithBreakdownByWindow_groupsByDayAndActivity() {
    Instant start = Instant.parse("2026-05-10T00:00:00Z");
    Instant end = Instant.parse("2026-05-20T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 10, billingMonth,
            Instant.parse("2026-05-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.CONTINUOUS_MONITORING, 5, billingMonth,
            Instant.parse("2026-05-15T12:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 20, billingMonth,
            Instant.parse("2026-05-16T14:00:00Z"))));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.dailyHistoryWithBreakdownByWindow(start, end);
    assertThat(breakdown).isNotEmpty();
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getBillingMonth()).isEqualTo(LocalDate.of(2026, 5, 15));
      assertThat(b.getGroupKey()).isEqualTo("APP_SCAN");
    });
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getBillingMonth()).isEqualTo(LocalDate.of(2026, 5, 15));
      assertThat(b.getGroupKey()).isEqualTo("CONTINUOUS_MONITORING");
    });
  }

  @Test
  public void weeklyHistoryWithBreakdownByWindows_bucketsByWeek() {
    LocalDate week1Start = LocalDate.of(2026, 4, 20);
    LocalDate week2Start = LocalDate.of(2026, 4, 27);
    Instant start1 = week1Start.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = week2Start.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant start2 = end1;
    Instant end2 = LocalDate.of(2026, 5, 4).atStartOfDay(ZoneOffset.UTC).toInstant();

    LocalDate billingMonth = LocalDate.of(2026, 4, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-04-22T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 200, billingMonth,
            Instant.parse("2026-04-29T12:00:00Z"))));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.weeklyHistoryWithBreakdownByWindows(
        Arrays.asList(start1, start2),
        Arrays.asList(end1, end2),
        Arrays.asList(week1Start, week2Start));

    assertThat(breakdown).hasSize(2);
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getBillingMonth()).isEqualTo(week1Start);
      assertThat(b.getComponentCount()).isEqualTo(100L);
    });
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getBillingMonth()).isEqualTo(week2Start);
      assertThat(b.getComponentCount()).isEqualTo(200L);
    });
  }

  @Test
  public void historyByStageByWindows_groupsByWindowAndStage() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    String appId = tempEntity.newApplicationWithParent().getId();
    String buildScanId = "scan-build-1";
    String releaseScanId = "scan-release-1";
    tempEntity.insertPolicyEvaluation(appId, buildScanId, "build");
    tempEntity.insertPolicyEvaluation(appId, releaseScanId, "release");

    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 100, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e1.setScanId(buildScanId);
    ConsumptionEvent e2 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 40, billingMonth,
        Instant.parse("2026-04-16T12:00:00Z"));
    e2.setScanId(buildScanId);
    ConsumptionEvent e3 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 10, billingMonth,
        Instant.parse("2026-04-17T14:00:00Z"));
    e3.setScanId(releaseScanId);
    tempEntity.insertConsumptionEvents(Arrays.asList(e1, e2, e3));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(2);
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("build");
      assertThat(b.getComponentCount()).isEqualTo(140L);
    });
    assertThat(breakdown).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("release");
      assertThat(b.getComponentCount()).isEqualTo(10L);
    });
  }

  @Test
  public void historyByStageByWindows_eventsWithoutScanId_bucketToUnknown() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 50, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e1.setScanId(null);
    tempEntity.insertConsumptionEvents(Collections.singletonList(e1));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getGroupKey()).isEqualTo(ConsumptionEventDAO.STAGE_UNKNOWN);
    assertThat(breakdown.get(0).getComponentCount()).isEqualTo(50L);
  }

  @Test
  public void historyByStageByWindows_eventsWithMissingPolicyEvaluation_bucketToUnknown() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 25, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e1.setScanId("scan-orphan");
    tempEntity.insertConsumptionEvents(Collections.singletonList(e1));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getGroupKey()).isEqualTo(ConsumptionEventDAO.STAGE_UNKNOWN);
    assertThat(breakdown.get(0).getComponentCount()).isEqualTo(25L);
  }

  @Test
  public void historyByStageByWindows_emptyWindowsReturnsEmpty() {
    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());

    assertThat(breakdown).isEmpty();
  }

  @Test
  public void historyByStageByWindows_filtersByTimeRange() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    String appId = tempEntity.newApplicationWithParent().getId();
    String scanId = "scan-build-2";
    tempEntity.insertPolicyEvaluation(appId, scanId, "build");

    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 7, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e1.setScanId(scanId);
    ConsumptionEvent e2 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 99,
        LocalDate.of(2026, 3, 1),
        Instant.parse("2026-03-15T10:00:00Z"));
    e2.setScanId(scanId);
    tempEntity.insertConsumptionEvents(Arrays.asList(e1, e2));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getComponentCount()).isEqualTo(7L);
  }

  @Test
  public void historyByStageByWindows_reevaluationRow_doesNotDoubleCount() {
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    String appId = tempEntity.newApplicationWithParent().getId();
    String scanId = "scan-with-reeval";

    // Primary (reevaluation=false) policy_evaluation row
    tempEntity.insertPolicyEvaluation(appId, scanId, "build");
    // Reevaluation row sharing the same applicationId+scan_id
    tempEntity.insertPolicyReEvaluation(appId, scanId, "build");

    ConsumptionEvent e = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 50, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e.setScanId(scanId);
    tempEntity.insertConsumptionEvents(Collections.singletonList(e));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getGroupKey()).isEqualTo("build");
    assertThat(breakdown.get(0).getComponentCount()).isEqualTo(50L); // not 100L
  }

  @Test
  public void historyByStageByWindows_forMonitoringRow_bucketsToActualStage() {
    // PolicyMonitor.evaluate generates a fresh scan_id and writes a single
    // policy_evaluation row with reevaluation=false, for_monitoring=true. The
    // consumption event records that same fresh scan_id with
    // activity_type=CONTINUOUS_MONITORING. The JOIN must match this row so the
    // event buckets under its actual stage rather than falling into "Unknown".
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    String appId = tempEntity.newApplicationWithParent().getId();
    String monitoringScanId = "scan-monitoring-only";

    // Only a for_monitoring=true row exists for this scan_id (PolicyMonitor never
    // creates a primary first; it generates a fresh scan_id each run).
    tempEntity.insertPolicyEvaluationForMonitoring(appId, monitoringScanId, "build");

    ConsumptionEvent e = buildEventWithTimestamp("org-1", appId,
        ActivityType.CONTINUOUS_MONITORING, 50, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e.setScanId(monitoringScanId);
    tempEntity.insertConsumptionEvents(Collections.singletonList(e));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getGroupKey()).isEqualTo("build"); // not "Unknown"
    assertThat(breakdown.get(0).getComponentCount()).isEqualTo(50L);
  }

  // Note: the JOIN explicitly filters FOR_OBSOLETE_SCAN=false for consistency with
  // the other queries in PolicyEvaluationDAO. PolicyEvaluationDAO.validate today
  // rejects for_obsolete_scan=true paired with reevaluation=false at insert time, so
  // a regression test against this filter is not feasible without bypassing the DAO.

  @Test
  public void historyByStageByWindows_scanIdSharedAcrossApplications_doesNotDoubleCount() {
    // `policy_evaluation.scan_id` is unique only within an application, so a
    // pair of (applicationId, scanId) is the actual identifier (see
    // `PolicyEvaluationDAO.getLastByOwnerIdAndScanId`). Without an
    // applicationId match in the JOIN, a consumption event whose scan_id
    // happens to also exist for a different application would fan out and
    // multiply SUM(component_count).
    LocalDate label1 = LocalDate.of(2026, 4, 1);
    Instant start1 = label1.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end1 = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    LocalDate billingMonth = LocalDate.of(2026, 4, 1);

    String appA = tempEntity.newApplicationWithParent().getId();
    String appB = tempEntity.newApplicationWithParent().getId();
    String sharedScanId = "scan-collision";

    // Same scan_id, two different applications, two different stages
    tempEntity.insertPolicyEvaluation(appA, sharedScanId, "build");
    tempEntity.insertPolicyEvaluation(appB, sharedScanId, "release");

    ConsumptionEvent e = buildEventWithTimestamp("org-1", appA, ActivityType.APP_SCAN, 50, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z"));
    e.setScanId(sharedScanId);
    tempEntity.insertConsumptionEvents(Collections.singletonList(e));

    List<ConsumptionMonthlyBreakdown> breakdown = dao.historyByStageByWindows(
        Collections.singletonList(start1),
        Collections.singletonList(end1),
        Collections.singletonList(label1));

    assertThat(breakdown).hasSize(1);
    assertThat(breakdown.get(0).getGroupKey()).isEqualTo("build"); // appA's stage, not appB's
    assertThat(breakdown.get(0).getComponentCount()).isEqualTo(50L); // not 100L
  }

  // --- WithRange overloads ---

  @Test
  public void historyByWindowsWithRange_sumsTotalForRange() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-07-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-05-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-2", ActivityType.RE_EVALUATE, 50, billingMonth,
            Instant.parse("2026-06-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-3", ActivityType.APP_SCAN, 999, billingMonth,
            Instant.parse("2026-04-15T10:00:00Z")) // outside range
    ));

    List<ConsumptionMonthlyTotal> result = dao.historyByWindowsWithRange(start, end);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTotalConsumed()).isEqualTo(150L);
    assertThat(result.get(0).getBillingMonth()).isEqualTo(LocalDate.of(2026, 5, 1));
  }

  @Test
  public void historyByWindowsWithRange_noData_returnsZero() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");

    List<ConsumptionMonthlyTotal> result = dao.historyByWindowsWithRange(start, end);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTotalConsumed()).isEqualTo(0L);
  }

  @Test
  public void historyWithBreakdownByWindowsWithRange_groupsByActivityType() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    tempEntity.insertConsumptionEvents(Arrays.asList(
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
            Instant.parse("2026-05-10T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 50, billingMonth,
            Instant.parse("2026-05-20T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-2", ActivityType.CONTINUOUS_MONITORING, 30, billingMonth,
            Instant.parse("2026-05-15T10:00:00Z")),
        buildEventWithTimestamp("org-1", "app-3", ActivityType.APP_SCAN, 999, billingMonth,
            Instant.parse("2026-04-15T10:00:00Z")) // outside range
    ));

    List<ConsumptionMonthlyBreakdown> result = dao.historyWithBreakdownByWindowsWithRange(start, end);

    assertThat(result).hasSize(2);
    assertThat(result).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("APP_SCAN");
      assertThat(b.getComponentCount()).isEqualTo(150L);
      assertThat(b.getBillingMonth()).isEqualTo(LocalDate.of(2026, 5, 1));
    });
    assertThat(result).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("CONTINUOUS_MONITORING");
      assertThat(b.getComponentCount()).isEqualTo(30L);
    });
  }

  @Test
  public void historyBySourceByWindowsWithRange_groupsBySource() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);
    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 100, billingMonth,
        Instant.parse("2026-05-10T10:00:00Z"));
    e1.setSource("UI");
    ConsumptionEvent e2 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 40, billingMonth,
        Instant.parse("2026-05-15T10:00:00Z"));
    e2.setSource("CLI");
    ConsumptionEvent e3 = buildEventWithTimestamp("org-1", "app-1", ActivityType.APP_SCAN, 999, billingMonth,
        Instant.parse("2026-04-15T10:00:00Z")); // outside range
    e3.setSource("UI");
    tempEntity.insertConsumptionEvents(Arrays.asList(e1, e2, e3));

    List<ConsumptionMonthlyBreakdown> result = dao.historyBySourceByWindowsWithRange(start, end);

    assertThat(result).hasSize(2);
    assertThat(result).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("UI");
      assertThat(b.getComponentCount()).isEqualTo(100L);
    });
    assertThat(result).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("CLI");
      assertThat(b.getComponentCount()).isEqualTo(40L);
    });
  }

  @Test
  public void historyByStageByWindowsWithRange_groupsByStage() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);

    String appId = tempEntity.newApplicationWithParent().getId();
    String buildScanId = "scan-range-build";
    String releaseScanId = "scan-range-release";
    tempEntity.insertPolicyEvaluation(appId, buildScanId, "build");
    tempEntity.insertPolicyEvaluation(appId, releaseScanId, "release");

    ConsumptionEvent e1 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 70, billingMonth,
        Instant.parse("2026-05-10T10:00:00Z"));
    e1.setScanId(buildScanId);
    ConsumptionEvent e2 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 30, billingMonth,
        Instant.parse("2026-05-20T10:00:00Z"));
    e2.setScanId(releaseScanId);
    ConsumptionEvent e3 = buildEventWithTimestamp("org-1", appId, ActivityType.APP_SCAN, 999, billingMonth,
        Instant.parse("2026-04-10T10:00:00Z")); // outside range
    e3.setScanId(buildScanId);
    tempEntity.insertConsumptionEvents(Arrays.asList(e1, e2, e3));

    List<ConsumptionMonthlyBreakdown> result = dao.historyByStageByWindowsWithRange(start, end);

    assertThat(result).hasSize(2);
    assertThat(result).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("build");
      assertThat(b.getComponentCount()).isEqualTo(70L);
      assertThat(b.getBillingMonth()).isEqualTo(LocalDate.of(2026, 5, 1));
    });
    assertThat(result).anySatisfy(b -> {
      assertThat(b.getGroupKey()).isEqualTo("release");
      assertThat(b.getComponentCount()).isEqualTo(30L);
    });
  }

  @Test
  public void historyByStageByWindowsWithRange_noMatchingScan_bucketsToUnknown() {
    Instant start = Instant.parse("2026-05-01T00:00:00Z");
    Instant end = Instant.parse("2026-06-01T00:00:00Z");
    LocalDate billingMonth = LocalDate.of(2026, 5, 1);

    ConsumptionEvent e = buildEventWithTimestamp("org-1", "app-orphan", ActivityType.APP_SCAN, 25, billingMonth,
        Instant.parse("2026-05-15T10:00:00Z"));
    e.setScanId(null);
    tempEntity.insertConsumptionEvents(Collections.singletonList(e));

    List<ConsumptionMonthlyBreakdown> result = dao.historyByStageByWindowsWithRange(start, end);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getGroupKey()).isEqualTo(ConsumptionEventDAO.STAGE_UNKNOWN);
    assertThat(result.get(0).getComponentCount()).isEqualTo(25L);
  }
}
