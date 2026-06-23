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
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionSummaryDTO;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BDD tests for the consumption summary endpoint response shape.
 * Maps to: api-summary.feature
 */
public class ConsumptionResourceSummaryTest
    extends AbstractConsumptionResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ConsumptionResource.RESOURCE_PATH);
  }

  // BDD: Summary response contains all required fields
  @Test
  public void getSummary_responseContainsAllRequiredFields() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    ConsumptionSummaryDTO summary = response.getBody(ConsumptionSummaryDTO.class);
    assertThat(summary).isNotNull();
    assertThat(summary.getResetDate()).isNotNull();
    assertThat(summary.getBillingWindowStart()).isNotNull();
    assertThat(summary.getActivityBreakdown()).isNotNull();
  }

  // BDD: Summary without limit has null percentUsed and remaining
  @Test
  public void getSummary_withoutLimit_hasNullPercentAndRemaining() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);

    ConsumptionSummaryDTO summary = response.getBody(ConsumptionSummaryDTO.class);
    assertThat(summary).isNotNull();
    assertThat(summary.getLimit()).isNull();
    assertThat(summary.getPercentUsed()).isNull();
    assertThat(summary.getRemaining()).isNull();
  }

  // BDD: Summary includes activity breakdown
  @Test
  public void getSummary_includesActivityBreakdown() throws Exception {
    seedOneEventPerActivityType();

    User adminUser = createSystemAdminUser();
    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);

    ConsumptionSummaryDTO summary = response.getBody(ConsumptionSummaryDTO.class);
    Map<String, Long> breakdown = summary.getActivityBreakdown();
    assertThat(breakdown).isNotNull();
    assertThat(breakdown).containsEntry("App Scan + Re-evaluate", 2L);
    assertThat(breakdown).containsEntry("Continuous Monitoring", 1L);
    assertThat(breakdown).containsEntry("Component Details", 1L);
    assertThat(breakdown).containsEntry("Version Recommendations", 2L);
    assertThat(breakdown).containsEntry("Reachability Analysis", 1L);
    assertThat(breakdown).containsEntry("APIs", 1L);
    assertThat(breakdown.values().stream().mapToLong(Long::longValue).sum())
        .isEqualTo(summary.getConsumed());
  }

  private void seedOneEventPerActivityType() {
    LocalDate currentBillingMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    List<ConsumptionEvent> events = new ArrayList<>();
    for (ActivityType type : ActivityType.values()) {
      if (type == ActivityType.OTHERS) {
        continue;
      }
      events.add(buildEvent(type, currentBillingMonth));
    }
    tempEntity.insertConsumptionEvents(events);
  }

  private ConsumptionEvent buildEvent(ActivityType activityType, LocalDate billingMonth) {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId(Organization.ROOT_ORGANIZATION_ID);
    event.setAppId("test-app-id-" + activityType.name());
    event.setEventTimestamp(Instant.now());
    event.setComponentCount(1);
    event.setActivityType(activityType);
    event.setSource("UI");
    event.setTier("APP_BASED");
    event.setBillingMonth(billingMonth);
    return event;
  }

  // BDD: Usage viewer can also access summary
  @Test
  public void getSummary_asUsageViewer_returns200() throws Exception {
    User usageViewerUser = createUsageViewerUser();

    HttpResponse response = restRequest().auth(usageViewerUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  // BDD: Valid startDate+endDate range returns 200
  @Test
  public void getSummary_withValidDateRange_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .query("startDate", "2026-01-01")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  // BDD: Invalid date format returns 400
  @Test
  public void getSummary_withInvalidDateFormat_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .query("startDate", "not-a-date")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  // BDD: Only startDate provided returns 400
  @Test
  public void getSummary_withOnlyStartDate_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .query("startDate", "2026-01-01")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  // BDD: Range exceeding 366-day cap returns 400. The Summary endpoint passes
  // 366 as the cap to DateRangeValidator (only /daily-history uses 92). This
  // test guards the HTTP boundary — the unit-level DateRangeValidatorTest
  // covers the math, this proves the resource wires the right cap.
  @Test
  public void getSummary_rangeExceeding366DayCap_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    // 2025-01-01 → 2026-06-30 is 546 inclusive days (well past 366).
    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .query("startDate", "2025-01-01")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

}
