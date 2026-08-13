/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.mock.hds.HdsMockServer.RestServlet;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code ScanResourceAuditTest}. Kept in the original package/simple name
 * because {@link #uploadRequest} resolves fixture files via {@code getClass().getSimpleName()}.
 */
@IqPostgresTest
public class ScanResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application app;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private static final String RESOURCE_PATH = "/AbstractAuditTest/report";

  private static final String FILENAME = "AbstractAuditTest-report";

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    app = ctx.tempEntity().newApplicationWithParent("ScanResourceAuditTest_AppId");
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
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ScanResource.RESOURCE_PATH);
  }

  private HttpRequest uploadRequest(String appPublicId, String stageId, String resource) {
    return restRequest().query("stageId", stageId)
        .parameter(appPublicId)
        .part("file", resource, getClass().getResource("/" + getClass().getSimpleName() + "/" + resource))
        .part("filename", resource);
  }

  @Test
  public void testUploadBinary() throws Exception {
    ctx.mockReport(RestServlet.SCAN_ID, RESOURCE_PATH);
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "file").post();
    ctx.assertResponseStatus(200, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), null, app.getId(),
        app.getPublicId(), app.getName(), Stage.ID_BUILD, RestServlet.SCAN_ID, false);
  }

  @Test
  public void testUploadBinary_ReportNotFound() throws Exception {
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "file").post();
    ctx.assertResponseStatus(200, response);

    assertEvaluationAuditLog("not-found", app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD,
        RestServlet.SCAN_ID, null);
  }

  @Test
  public void testUploadBinary_Unauthorized() throws Exception {
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, FILENAME).with(unauthorizedUser())
        .post();
    ctx.assertResponseStatus(403, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testUploadBinary_BadApplicationId() throws Exception {
    String appId = "unknown-app-public-id";
    HttpResponse response = uploadRequest(appId, Stage.ID_BUILD, FILENAME).post();
    ctx.assertResponseStatus(404, response);

    assertEvaluationAuditLog("not-found", null, appId, null, null, null, null);
  }

  @Test
  public void testUploadBinary_BadStageId() throws Exception {
    String stageId = "invalid-stage-id";
    HttpResponse response = uploadRequest(app.getPublicId(), stageId, FILENAME).post();
    ctx.assertResponseStatus(400, response);

    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
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
