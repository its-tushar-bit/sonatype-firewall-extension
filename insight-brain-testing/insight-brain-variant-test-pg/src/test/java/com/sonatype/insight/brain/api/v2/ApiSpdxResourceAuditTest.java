/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IQ Server on PostgreSQL — {@link ApiSpdxResource} audit-logging assertions, ported to the reused-server
 * variant pattern. No base class: audit-log capture/assertion helpers ({@code AuditTestSupport} in the legacy
 * {@code AbstractAuditTest}) are implemented directly since {@link IqTestContext} does not expose them. This
 * class lives in the original {@code com.sonatype.insight.brain.api.v2} package because it references the
 * package-private {@link ApiSpdxResource#GET_BY_STAGE_PATH} / {@link ApiSpdxResource#GET_BY_REPORT_PATH}.
 */
@IqPostgresTest
class ApiSpdxResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  private String scanId;

  private Application app;

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
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public com.sonatype.insight.brain.dataaccess.policy.PolicyDAO getPolicyDAO() {
    return ctx.lookup(com.sonatype.insight.brain.dataaccess.policy.PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.SPDX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    ctx.createReportFile(appId, scanId, "/ApiSpdxServiceTest/report");
  }

  @Test
  void testGetLatestForStage() throws Exception {
    getHttpRequestLatestForStage().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "scanId", scanId);
  }

  @Test
  void testGetLatestForStage_Unauthorized() throws Exception {
    getHttpRequestLatestForStage().with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetByScanId() throws Exception {
    getHttpRequestByScanId().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "scanId", scanId);
  }

  @Test
  void testGetByScanId_Unauthorized() throws Exception {
    getHttpRequestByScanId().with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private HttpRequest getHttpRequestLatestForStage() throws Exception {
    HttpRequest request = getHttpRequest(ApiSpdxResource.GET_BY_STAGE_PATH);
    request.parameter(app.getId(), Stage.ID_BUILD);
    return request;
  }

  private HttpRequest getHttpRequestByScanId() throws Exception {
    HttpRequest request = getHttpRequest(ApiSpdxResource.GET_BY_REPORT_PATH);
    request.parameter(app.getId(), scanId);
    return request;
  }

  private HttpRequest getHttpRequest(String path) throws IOException {
    createReportFile(app.getId(), scanId);
    return restRequest().path(path);
  }

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
