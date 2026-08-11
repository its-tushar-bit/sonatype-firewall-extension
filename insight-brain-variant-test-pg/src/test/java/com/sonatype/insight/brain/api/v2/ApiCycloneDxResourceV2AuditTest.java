/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.cyclonedx.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Package-scoped: touches {@link ApiCycloneDxResourceV2}'s package-private {@code GET_BY_STAGE_PATH}/
 * {@code GET_BY_REPORT_PATH} constants, and resolves report fixtures via {@code getClass().getSimpleName()} —
 * so the class keeps the original simple name and package (see convert-resource-test-to-variant skill, Step 3).
 */
@IqPostgresTest
class ApiCycloneDxResourceV2AuditTest
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private String scanId;

  private Application app;

  private User unauthorizedUser;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    scanId = com.sonatype.insight.brain.dataaccess.TemporaryEntity.uuid();
    app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/" + getClass().getSimpleName() + "/report");
  }

  private void createReportFile(String applicationId, String scanId, String sourceReportDir) throws IOException {
    var insightWork = ctx.insightWork();
    ReportHelper.saveMockReport(insightWork, ctx.tempFolder(), sourceReportDir, applicationId, scanId);
  }

  @Test
  void testGetLatest() throws Exception {
    getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  void testGetLatest_With_Version_1_1() throws Exception {
    getHttpRequestLatest("1.1/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_11,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  void testGetLatest_With_Version_1_5() throws Exception {
    getHttpRequestLatest("1.5/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_15,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  void testGetLatest_Unauthorized() throws Exception {
    getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).with(this::asUnauthorizedUser).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetByReportId() throws Exception {
    getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  void testGetByReportId_With_Version_1_1() throws Exception {
    getHttpRequestByReportId("1.1/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_11,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  void testGetByReportId_With_Version_1_5() throws Exception {
    getHttpRequestByReportId("1.5/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_15,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  void testGetByReportId_Unauthorized() throws Exception {
    getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).with(this::asUnauthorizedUser).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private void asUnauthorizedUser(HttpRequest httpRequest) {
    httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest getHttpRequestLatest(String path) throws Exception {
    return getHttpRequestLatest(path, null, null);
  }

  private HttpRequest getHttpRequestLatest(String path, Version version, String mediaType) throws Exception {
    HttpRequest request = getHttpRequest(path, mediaType);
    if (version != null) {
      request.parameter(app.getId(), Stage.ID_BUILD, version.getVersionString());
    }
    else {
      request.parameter(app.getId(), Stage.ID_BUILD);
    }
    return request;
  }

  private HttpRequest getHttpRequestByReportId(String path) throws Exception {
    return getHttpRequestByReportId(path, null, null);
  }

  private HttpRequest getHttpRequestByReportId(String path, Version version, String mediaType) throws Exception {
    HttpRequest request = getHttpRequest(path, mediaType);
    if (version != null) {
      request.parameter(app.getId(), scanId, version.getVersionString());
    }
    else {
      request.parameter(app.getId(), scanId);
    }
    return request;
  }

  private HttpRequest getHttpRequest(final String path, final String mediaType) throws IOException {
    createReportFile(app.getId(), scanId);

    HttpRequest request = restRequest().path(path);
    if (mediaType != null) {
      request.header("Accept", mediaType);
    }
    return request;
  }

  // --- inlined AuditTestSupport helpers (kept base-class-free; see skill Step 2 translation table) ---

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLog(auditEvent, error, null);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error, String username) {
    java.util.List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, 1);
    AuditDTO auditDTO = auditDTOs.get(0);
    assertStandardData(auditDTO, auditEvent, error, username);
    return auditDTO;
  }

  private java.util.List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, java.util.concurrent.TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private java.util.List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(this::parseAuditLog)
        .filter(dto -> auditEvent.getType().equals(dto.type))
        .collect(toList());
  }

  private AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().readValue(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error, String username) {
    boolean systemEvent = com.sonatype.insight.brain.security.MDCUsernameScope.SYSTEM.equals(username);
    if (username == null) {
      username = "unauthorized".equals(error) ? unauthorizedUser.getUsername() : User.ADMIN_USERNAME;
    }
    assertThat(auditDTO.domain).isEqualTo(auditEvent.getDomain());
    assertThat(auditDTO.type).isEqualTo(auditEvent.getType());
    assertThat(auditDTO.error).isEqualTo(error);
    assertThat(auditDTO.timestamp).matches("2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+0-9Z.:]+");
    assertThat(auditDTO.requestMethod).isNull();
    assertThat(auditDTO.requestUri).isNull();
    assertThat(auditDTO.forwarded).isNull();
    if (systemEvent) {
      assertThat(auditDTO.remoteIpAddress).isNull();
      assertThat(auditDTO.userAgent).isNull();
    }
    else {
      assertThat(auditDTO.remoteIpAddress).isNotEmpty();
      assertThat(auditDTO.userAgent).isNotEmpty();
    }
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

  private void assertApplicationData(AuditDTO auditDTO, Application application) {
    assertCustomData(auditDTO, "applicationId", application.getId());
    assertCustomData(auditDTO, "applicationPublicId", application.getPublicId());
    assertCustomData(auditDTO, "applicationName", application.getName());
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
