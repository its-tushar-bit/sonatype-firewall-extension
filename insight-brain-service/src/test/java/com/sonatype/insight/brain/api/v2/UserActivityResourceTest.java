/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityDetailDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivitySummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityFilterOptionsDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserActivityResourceTest
    extends AbstractResourceTest
{
  private static final String LOG_DIR = "./log";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
  }

  @After
  public void after() throws IOException {
    deleteAuditLogs();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_ACTIVITY_RESOURCE_PATH);
  }

  @Test
  public void testGetUserActivitySummary() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    ApiUserActivitySummaryDTO result = objectMapper.readValue(response.getBodyText(), ApiUserActivitySummaryDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.users).isNotNull();
    assertThat(result.totalUsers).isNotNull();
    assertThat(result.dateRange).isNotNull();
    assertThat(result.dateRange.startDate).isEqualTo("2024-03-10");
    assertThat(result.dateRange.endDate).isEqualTo("2024-03-13");
    assertThat(result.pagination).isNotNull();
  }

  @Test
  public void testGetUserActivitySummary_withUsernameFilter() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("username", "john.doe")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    ApiUserActivitySummaryDTO result = objectMapper.readValue(response.getBodyText(), ApiUserActivitySummaryDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.users).isNotNull();
  }

  @Test
  public void testGetUserActivityDetail() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/john.doe")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    ApiUserActivityDetailDTO result = objectMapper.readValue(response.getBodyText(), ApiUserActivityDetailDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.username).isEqualTo("john.doe");
    assertThat(result.activities).isNotNull();
    assertThat(result.pagination).isNotNull();
  }

  @Test
  public void testGetUserActivitySummary_InvalidDateRange() throws Exception {
    HttpResponse response = restRequest().auth(getUser())
        .query("startUtcDate", "invalid-date")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void testGetUserActivityDetail_InvalidPath() throws Exception {
    HttpResponse response = restRequest().auth(getUser())
        .path("/invalid/path")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    // This should return 404 since the path doesn't match any resource endpoint
    assertResponseStatus(HttpStatus.SC_NOT_FOUND, response);
  }

  @Test
  public void testGetFilterOptions() throws Exception {
    HttpResponse response = restRequest().auth(getUser())
        .path("/filterOptions")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    ApiUserActivityFilterOptionsDTO result = objectMapper.readValue(response.getBodyText(),
        ApiUserActivityFilterOptionsDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.domains).isNotNull().isNotEmpty();
    assertThat(result.activityTypes).isNotNull().isNotEmpty();
    assertThat(result.errorTypes).isNotNull().hasSize(13);
    assertThat(result.errorTypes).contains("Success", "bad-request", "bad-authentication",
        "bad-session", "unauthenticated", "unlicensed", "unauthorized", "not-found",
        "bad-gateway", "service-unavailable", "gateway-timeout", "server-error", "client-error");
  }

  @Test
  public void testGetFilterOptions_Unauthorized() throws Exception {
    HttpResponse response = restRequest().auth(getUserWithoutAuditLogAccess())
        .path("/filterOptions")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void testGetUserActivitySummary_Unauthorized() throws Exception {
    HttpResponse response = restRequest().auth(getUserWithoutAuditLogAccess())
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void testExportUserActivitySummary() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo("text/csv");
    assertThat(response.getHeader("Content-Disposition")).contains("attachment");
    assertThat(response.getHeader("Content-Disposition")).contains("user_activity");
    assertThat(response.getHeader("Content-Disposition")).contains(".csv");

    String csvContent = response.getBodyText();
    assertThat(csvContent).isNotNull();
    assertThat(csvContent).contains("Username");
    assertThat(csvContent).contains("Timestamp");
  }

  @Test
  public void testExportUserActivitySummary_withFilters() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("username", "john.doe")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo("text/csv");
  }

  @Test
  public void testExportUserActivityDetail_viaUnifiedEndpoint() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("username", "john.doe")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo("text/csv");
    assertThat(response.getHeader("Content-Disposition")).contains("attachment");
    assertThat(response.getHeader("Content-Disposition")).contains("user_activity_detail");
    assertThat(response.getHeader("Content-Disposition")).contains(".csv");

    String csvContent = response.getBodyText();
    assertThat(csvContent).isNotNull();
    assertThat(csvContent).contains("Username");
    assertThat(csvContent).contains("Timestamp");
  }

  @Test
  public void testExportUserActivityDetail_withFilters() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("username", "john.doe")
        .query("activityTypes", "LOGIN")
        .query("domains", "AUTHENTICATION")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo("text/csv");
  }

  @Test
  public void testExportUserActivitySummary_Unauthorized() throws Exception {
    HttpResponse response = restRequest().auth(getUserWithoutAuditLogAccess())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  @Test
  public void testExportUserActivityDetail_Unauthorized() throws Exception {
    HttpResponse response = restRequest().auth(getUserWithoutAuditLogAccess())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("username", "john.doe")
        .get();

    assertResponseStatus(HttpStatus.SC_FORBIDDEN, response);
  }

  private void copyTestResource(String filename) throws IOException {
    String filepath = getClass().getClassLoader().getResource(getClass().getSimpleName() + "/" + filename).getFile();
    Files.copy(new File(filepath).toPath(), Paths.get(LOG_DIR, filename));
  }

  private void deleteAuditLogs() throws IOException {
    Files.list(Paths.get(LOG_DIR))
        .filter(file -> file.getFileName().toString().startsWith("audit") &&
            (file.getFileName().toString().endsWith(".log") || file.getFileName().toString().endsWith(".gz")))
        .forEach(file -> {
          try {
            Files.delete(file);
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  private User getUser() {
    return createUserWithPermissions(Permission.ACCESS_AUDIT_LOG);
  }

  private User getUserWithoutAuditLogAccess() {
    return createUserWithPermissions(Permission.READ);
  }
}
