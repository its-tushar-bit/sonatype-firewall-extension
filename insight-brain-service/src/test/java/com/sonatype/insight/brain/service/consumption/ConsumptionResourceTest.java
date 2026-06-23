/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.List;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionHistoryEntryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionSummaryDTO;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConsumptionResource}.
 */
public class ConsumptionResourceTest
    extends AbstractConsumptionResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ConsumptionResource.RESOURCE_PATH);
  }

  @Test
  public void getSummary_asAdmin_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    ConsumptionSummaryDTO summary = response.getBody(ConsumptionSummaryDTO.class);
    assertThat(summary).isNotNull();
  }

  @Test
  public void getSummary_asUnauthorized_returns403() throws Exception {
    // Create a user without USAGE_VIEWER or SYSTEM_ADMIN role
    User regularUser = tempEntity.newUser();

    HttpResponse response = restRequest().auth(regularUser)
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void getHistory_asUsageViewer_returns200() throws Exception {
    User usageViewerUser = createUsageViewerUser();

    HttpResponse response = restRequest().auth(usageViewerUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    List<ConsumptionHistoryEntryDTO> history = response.getBodyList(ConsumptionHistoryEntryDTO.class);
    assertThat(history).isNotNull();
  }

  @Test
  public void getHistory_asUnauthorized_returns403() throws Exception {
    // Create a user without USAGE_VIEWER or SYSTEM_ADMIN role
    User regularUser = tempEntity.newUser();

    HttpResponse response = restRequest().auth(regularUser)
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void getSummary_unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest().anon()
        .path(ConsumptionResource.SUMMARY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_UNAUTHORIZED, response);
  }

  @Test
  public void getHistory_unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest().anon()
        .path(ConsumptionResource.HISTORY_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_UNAUTHORIZED, response);
  }

  // --- BDD: api-authentication.feature - Export endpoint auth tests ---

  @Test
  public void exportCsv_asAdmin_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  public void exportCsv_asUsageViewer_returns200() throws Exception {
    User usageViewerUser = createUsageViewerUser();

    HttpResponse response = restRequest().auth(usageViewerUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  public void exportCsv_asUnauthorized_returns403() throws Exception {
    User regularUser = tempEntity.newUser();

    HttpResponse response = restRequest().auth(regularUser)
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void exportCsv_unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest().anon()
        .path(ConsumptionResource.EXPORT_PATH)
        .get();

    assertResponseStatus(HttpStatus.SC_UNAUTHORIZED, response);
  }

  // --- I3: Aggregation parameter validation ---

  @Test
  public void getHistoryBreakdown_invalidAggregation_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("aggregation", "invalid_value")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void getHistoryBreakdown_validAggregation_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("aggregation", "daily")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getBodyText()).startsWith("[");
  }

  @Test
  public void getHistoryBreakdown_weeklyAggregation_returns200WithJsonArray() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("aggregation", "weekly")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getBodyText()).startsWith("[");
  }

  @Test
  public void getHistoryBreakdown_monthlyAggregation_returns200WithJsonArray() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("aggregation", "monthly")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getBodyText()).startsWith("[");
  }

  // BDD: history/breakdown with valid date range returns 200
  @Test
  public void getHistoryBreakdown_withValidDateRange_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("startDate", "2026-01-01")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  // BDD: history/breakdown with invalid date format returns 400
  @Test
  public void getHistoryBreakdown_withInvalidDateFormat_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("startDate", "not-a-date")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  // BDD: history/breakdown with only startDate returns 400
  @Test
  public void getHistoryBreakdown_withOnlyStartDate_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path("history/breakdown")
        .query("startDate", "2026-01-01")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void allEndpoints_asAdmin_featureDisabled_return403() throws Exception {
    // Override AbstractConsumptionResourceTest's @Before that enables the feature.
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    User adminUser = createSystemAdminUser();

    List<String> paths = List.of(
        ConsumptionResource.SUMMARY_PATH,
        ConsumptionResource.HISTORY_PATH,
        "history/breakdown",
        "history/by-source",
        "top-apps",
        "daily-history",
        ConsumptionResource.EXPORT_PATH);

    for (String path : paths) {
      HttpResponse response = restRequest().auth(adminUser).path(path).get();
      assertThat(response.getStatusCode())
          .as("path=%s should return 403 when feature disabled", path)
          .isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
  }

}
