/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.User;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsumptionResourceHistoryBySourceTest
    extends AbstractConsumptionResourceTest
{
  private static final String HISTORY_BY_SOURCE_PATH = "history/by-source";

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ConsumptionResource.RESOURCE_PATH);
  }

  @Test
  public void getHistoryBySource_asAdmin_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser).path(HISTORY_BY_SOURCE_PATH).get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  public void getHistoryBySource_asUsageViewer_returns200() throws Exception {
    User usageViewerUser = createUsageViewerUser();

    HttpResponse response = restRequest().auth(usageViewerUser).path(HISTORY_BY_SOURCE_PATH).get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  public void getHistoryBySource_asUnauthorized_returns403() throws Exception {
    User regularUser = tempEntity.newUser();

    HttpResponse response = restRequest().auth(regularUser).path(HISTORY_BY_SOURCE_PATH).get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void getHistoryBySource_unauthenticated_returns401() throws Exception {
    HttpResponse response = restRequest().anon().path(HISTORY_BY_SOURCE_PATH).get();

    assertResponseStatus(HttpStatus.SC_UNAUTHORIZED, response);
  }

  // BDD: Valid startDate+endDate range returns 200
  @Test
  public void getHistoryBySource_withValidDateRange_returns200() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(HISTORY_BY_SOURCE_PATH)
        .query("startDate", "2026-01-01")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  // BDD: Invalid date format returns 400
  @Test
  public void getHistoryBySource_withInvalidDateFormat_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(HISTORY_BY_SOURCE_PATH)
        .query("startDate", "not-a-date")
        .query("endDate", "2026-06-30")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  // BDD: Only startDate provided returns 400
  @Test
  public void getHistoryBySource_withOnlyStartDate_returns400() throws Exception {
    User adminUser = createSystemAdminUser();

    HttpResponse response = restRequest().auth(adminUser)
        .path(HISTORY_BY_SOURCE_PATH)
        .query("startDate", "2026-01-01")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }
}
