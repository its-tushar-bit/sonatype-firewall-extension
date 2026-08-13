/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — port of the legacy {@code ApiScanHealthConfigurationResourceAuditTest} to the
 * reused-server variant pattern. No base class: audit-log capture/assertion helpers ({@code AuditTestSupport} in the
 * legacy base chain) are inlined here since {@link IqTestContext} does not expose audit-log support.
 */
@IqPostgresTest
class IqPostgresApiScanHealthConfigurationResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private ScanHealthConfigDAO scanHealthConfigDAO;

  private Organization testOrg;

  private Application testApp;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    scanHealthConfigDAO = ctx.lookup(ScanHealthConfigDAO.class);
    testOrg = ctx.tempEntity().newOrganization();
    testApp = ctx.tempEntity().newApplication(testOrg.getId());
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  @Test
  void testSetConfiguration_Organization() throws Exception {
    ScanHealthConfigDTO dto = new ScanHealthConfigDTO(true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(dto)
        .put();

    ctx.assertResponseStatus(200, response);
    assertAuditLog(AuditEvent.CONFIGURE_SCAN_HEALTH, null);

    // Cleanup
    scanHealthConfigDAO.delete(ORGANIZATION.toString(), testOrg.getId());
  }

  @Test
  void testSetConfiguration_Application() throws Exception {
    ScanHealthConfigDTO dto = new ScanHealthConfigDTO(true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .body(dto)
        .put();

    ctx.assertResponseStatus(200, response);
    assertAuditLog(AuditEvent.CONFIGURE_SCAN_HEALTH, null);

    // Cleanup
    scanHealthConfigDAO.delete(APPLICATION.toString(), testApp.getId());
  }

  @Test
  void testDeleteConfiguration_Organization() throws Exception {
    scanHealthConfigDAO.save(new ScanHealthConfig(
        testOrg.getId(), ORGANIZATION.toString(), "{\"failOnZeroComponents\":true}"));

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_SCAN_HEALTH, null);
  }

  @Test
  void testDeleteConfiguration_Application() throws Exception {
    scanHealthConfigDAO.save(new ScanHealthConfig(
        testApp.getId(), APPLICATION.toString(), "{\"failOnZeroComponents\":true}"));

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_SCAN_HEALTH, null);
  }

  @Test
  void testDeleteConfiguration_NotFound() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    ctx.assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_SCAN_HEALTH, "not-found");
  }

  // --- inlined AuditTestSupport / AbstractAuditTest helpers (not exposed by IqTestContext) --------

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(IqPostgresApiScanHealthConfigurationResourceAuditTest::parseAuditLog)
        .filter(dto -> auditEvent.getType().equals(dto.type))
        .collect(toCollection(ArrayList::new));
  }

  private static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JSON.readValue(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error).get(0);
  }

  private List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error));
    return auditDTOs;
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    String username = User.ADMIN_USERNAME;
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

  /** Exposes {@link LogOutput#after()} (protected, {@code ExternalResource}) for {@code @AfterEach} teardown. */
  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
