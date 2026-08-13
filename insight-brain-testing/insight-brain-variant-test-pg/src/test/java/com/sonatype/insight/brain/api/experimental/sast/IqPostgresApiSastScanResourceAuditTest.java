/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.List;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.experimental.sast.SastTestUtil.buildTestSastScanRequestDTO;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL — audit-log assertions for {@code ApiSastScanResource}, converted from
 * the legacy {@code ApiSastScanResourceAuditTest} ({@code AbstractAuditTest}) to the shared,
 * reused-server test context. No base class: audit-log capture and assertion helpers that lived on
 * {@code AbstractAuditTest}/{@code AuditTestSupport} are inlined below.
 */
@IqPostgresTest
class IqPostgresApiSastScanResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @Test
  void testCreateSastScan_Unauthorized() throws Exception {
    final Application app = ctx.tempEntity().newApplicationWithParent();
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .with(unauthorizedUser())
        .parameter(app.getPublicId())
        .body(buildTestSastScanRequestDTO())
        .post();
    ctx.assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.CREATE_SAST_SCAN, "unauthorized");
  }

  @Test
  void testCreateSastScan_Authorized() throws Exception {
    final Application app = ctx.tempEntity().newApplicationWithParent();
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(app.getPublicId())
        .body(buildTestSastScanRequestDTO())
        .post();
    ctx.assertResponseStatus(200, response);
    final SastScanResponseDTO result = response.getBody(SastScanResponseDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_SAST_SCAN, null);
    assertCustomData(auditDTO, "sastScanId", result.sastScanId);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, 1);
    AuditDTO auditDTO = auditDTOs.get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    Awaitility.await("Expect audit event " + auditEvent)
        .atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(IqPostgresApiSastScanResourceAuditTest::parseAuditLog)
        .filter(dto -> auditEvent.getType().equals(dto.type))
        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
  }

  private static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JSON.readValue(auditLogEntry, AuditDTO.class);
    }
    catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    String username = "unauthorized".equals(error) ? unauthorizedUser.getUsername() : User.ADMIN_USERNAME;
    assertThat(auditDTO.domain).isEqualTo(auditEvent.getDomain());
    assertThat(auditDTO.type).isEqualTo(auditEvent.getType());
    assertThat(auditDTO.error).isEqualTo(error);
    assertThat(auditDTO.timestamp).matches("2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+0-9Z.:]+");
    assertThat(auditDTO.requestMethod).isNull();
    assertThat(auditDTO.requestUri).isNull();
    assertThat(auditDTO.forwarded).isNull();
    assertThat(auditDTO.remoteIpAddress).isNotEmpty();
    assertThat(auditDTO.userAgent).isNotEmpty();
    assertThat(auditDTO.username).isEqualTo(username);
  }

  private void assertCustomData(AuditDTO auditDTO, String key, Object value) {
    if (value == null) {
      assertThat(auditDTO.data).doesNotContainKey(key);
    }
    else {
      assertThat(auditDTO.data).containsEntry(key, value);
    }
  }
}
