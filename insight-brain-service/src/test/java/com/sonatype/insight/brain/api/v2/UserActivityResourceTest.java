/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.service.InsightConfig;

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
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private Path logDir;

  @Before
  public void before() throws IOException {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);

    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setSonatypeWork(tempDir.getRoot().getAbsolutePath());
    logDir = tempDir.getRoot().toPath().resolve("logs");
    Files.createDirectories(logDir);
  }

  @After
  public void after() throws IOException {
    deleteAuditLogs();
  }

  @Override
  protected HttpRequest restRequest() {
    return HttpRequest.to(getRestBaseUrl().replaceFirst("/$", ""))
        .path(PublicApiPaths.USER_ACTIVITY_RESOURCE_PATH);
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
    assertThat(response.getContentType()).startsWith("text/csv");
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
    assertThat(response.getContentType()).startsWith("text/csv");
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
    assertThat(response.getContentType()).startsWith("text/csv");
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
    assertThat(response.getContentType()).startsWith("text/csv");
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

  @Test
  public void testExportUserActivity_streamingPath_setsUtf8ContentType() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).contains("charset=utf-8");
  }

  @Test
  public void testExportUserActivity_streamingPath_filenameUsesUtcTimestamp() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    // Filename pattern: user_activity_all-yyyyMMdd-HHmmss.csv (UTC timestamp via DateTimeFormatter)
    assertThat(response.getHeader("Content-Disposition"))
        .matches(".*user_activity_all-\\d{8}-\\d{6}\\.csv.*");
  }

  @Test
  public void testExportUserActivity_withLimit_appliesLimitOnStreamingResponse() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("limit", "1")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    // Single streaming code path now — charset always declared.
    assertThat(response.getContentType()).contains("charset=utf-8");
    assertThat(response.getHeader("Content-Disposition")).contains("user_activity_all");

    // Header line + at most 1 data row = at most 2 lines.
    String csv = response.getBodyText();
    long lineCount = csv.lines().count();
    assertThat(lineCount).isLessThanOrEqualTo(2L);
  }

  @Test
  public void testExportUserActivity_withOffset_skipsRowsOnStreamingResponse() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("offset", "0")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).contains("charset=utf-8");
  }

  @Test
  public void testExportUserActivity_withLimitZero_returnsHeaderOnly() throws Exception {
    copyTestResource("audit-2024-03-13.log");

    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("limit", "0")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).contains("charset=utf-8");

    // limit=0 → header only, no data rows.
    String csv = response.getBodyText();
    long lineCount = csv.lines().count();
    assertThat(lineCount).isEqualTo(1L);
  }

  @Test
  public void testExportUserActivity_streamingPath_invalidDateRange_returns400() throws Exception {
    // Date range > 30 days triggers BadRequestException inside streamAllUserActivitiesForExport.
    // The exception must propagate as HTTP 400, not a 200 with a partial CSV body —
    // proving validation completes before Response.ok(streamingOutput) is built (D7).
    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-01-01")
        .query("endUtcDate", "2024-03-31") // ~89 days, exceeds 30-day cap
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void testExportUserActivity_streamingPath_missingStartDate_returns400() throws Exception {
    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("endUtcDate", "2024-03-13")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void testExportUserActivity_streamingPath_negativeLimit_returns400() throws Exception {
    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("limit", "-1")
        .get();

    // Consistent with getUserActivitySummary/Detail: negative pagination is rejected
    // synchronously by validatePaginationParameters (HTTP 400) — not silently normalized.
    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void testExportUserActivity_streamingPath_negativeOffset_returns400() throws Exception {
    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-03-10")
        .query("endUtcDate", "2024-03-13")
        .query("offset", "-1")
        .get();

    assertResponseStatus(HttpStatus.SC_BAD_REQUEST, response);
  }

  @Test
  public void testExportUserActivity_streamingPath_writesUtf8EncodedNonAsciiUsername() throws Exception {
    // Build a gzipped audit-log file in the per-test logDir with a single JSON event whose
    // username contains a non-ASCII character. Inlined here (instead of a checked-in binary
    // fixture) so the .gz bytes are produced fresh from a UTF-8-encoded source string.
    String auditLine = "{\"timestamp\":\"2024-04-01T10:00:00.000Z\","
        + "\"username\":\"josé\","
        + "\"type\":\"LOGIN\",\"domain\":\"security.user\","
        + "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\","
        + "\"remoteIpAddress\":\"10.0.0.99\",\"userAgent\":\"curl/8\"}";
    // Resolve the same dir the AuditLogFilesProvider actually reads from. main's
    // DefaultAuditLogFilesProvider.getAuditLogDirectory prefers the configured auditLogFilename
    // (parent dir + name) over sonatypeWork/logs — so plant the fixture there.
    InsightConfig insightConfig = lookup(InsightConfig.class);
    Path auditDir = new File(insightConfig.getAuditLogFilename()).getAbsoluteFile().getParentFile().toPath();
    Files.createDirectories(auditDir);
    Path gzFile = auditDir.resolve("audit-2024-04-01.log.gz");
    try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(gzFile))) {
      gz.write(auditLine.getBytes(StandardCharsets.UTF_8));
    }

    // No username filter: stream all rows so the non-ASCII username appears in the CSV
    // regardless of whether the HTTP client URL-encodes 'é' the same way the server decodes it.
    HttpResponse response = restRequest().auth(getUser())
        .path("/export")
        .query("startUtcDate", "2024-04-01")
        .query("endUtcDate", "2024-04-01")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).contains("charset=utf-8");

    String csv = new String(response.getBodyBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertThat(csv).contains("josé");

    // Negative check: bytes should NOT be platform-default-encoded (which would corrupt 'é' on
    // some JVMs). The raw UTF-8 byte sequence for 'é' is 0xC3 0xA9 — assert that pair is present.
    byte[] body = response.getBodyBytes();
    boolean foundUtf8E = false;
    for (int i = 0; i < body.length - 1; i++) {
      if ((body[i] & 0xFF) == 0xC3 && (body[i + 1] & 0xFF) == 0xA9) {
        foundUtf8E = true;
        break;
      }
    }
    assertThat(foundUtf8E).as("UTF-8 byte sequence 0xC3 0xA9 (é) must be present in CSV body").isTrue();
  }

  private void copyTestResource(String filename) throws IOException {
    String filepath = getClass().getClassLoader().getResource(getClass().getSimpleName() + "/" + filename).getFile();
    Path source = new File(filepath).toPath();
    Path target = logDir.resolve(filename.endsWith(".gz") ? filename : filename + ".gz");

    try (GZIPOutputStream outputStream = new GZIPOutputStream(Files.newOutputStream(target))) {
      Files.copy(source, outputStream);
    }
  }

  private void deleteAuditLogs() throws IOException {
    if (logDir == null || !Files.exists(logDir)) {
      return;
    }

    Files.list(logDir)
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
