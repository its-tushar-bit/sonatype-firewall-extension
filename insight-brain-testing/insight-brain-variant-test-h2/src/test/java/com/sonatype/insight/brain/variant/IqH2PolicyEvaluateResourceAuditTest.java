/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.function.Consumer;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2PolicyEvaluateResourceAuditTest
    implements AuditTestSupport
{
  private static final String SCAN_ID = "scanId";

  private IqTestContext ctx;

  private Application app;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    app = ctx.tempEntity().newApplicationWithParent();
    unauthorizedUser = ctx.tempEntity().newUser();
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

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private String mockReport(String resourceName) {
    String scanId = TemporaryEntity.uuid();
    ctx.mockReport(scanId, resourceName);
    return scanId;
  }

  @Test
  void testEvaluate() throws Exception {
    String scanId = mockReport("/AbstractAuditTest/report");
    ctx.createScanFile(app.getId(), scanId);
    ctx.assertResponseStatus(200, evaluate(null, app.getPublicId(), scanId, Stage.ID_BUILD));
    assertEvaluationAuditLog(null, app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD, scanId, false);
  }

  @Test
  void testEvaluate_Reevaluation() throws Exception {
    String scanId = mockReport("/AbstractAuditTest/report");
    ctx.createScanFile(app.getId(), scanId);
    ctx.assertResponseStatus(200, evaluate(null, app.getPublicId(), scanId, Stage.ID_BUILD));
    ctx.assertResponseStatus(200, evaluate(null, app.getPublicId(), scanId, Stage.ID_BUILD));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 2).get(1), null, app.getId(),
        app.getPublicId(), app.getName(), Stage.ID_BUILD, scanId, true);
  }

  @Test
  void testEvaluate_BadApplicationPublicId() throws Exception {
    ctx.assertResponseStatus(404, evaluate(null, "badApplicationPublicId", SCAN_ID, Stage.ID_BUILD));
    assertEvaluationAuditLog("not-found", null, "badApplicationPublicId", null, null, SCAN_ID, null);
  }

  @Test
  void testEvaluate_BadStageId() throws Exception {
    ctx.createScanFile(app.getId(), SCAN_ID);
    ctx.assertResponseStatus(400, evaluate(null, app.getPublicId(), SCAN_ID, "badStageId"));
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, SCAN_ID, null);
  }

  @Test
  void testEvaluate_BadScanId() throws Exception {
    ctx.createScanFile(app.getId(), SCAN_ID);
    ctx.assertResponseStatus(404, evaluate(null, app.getPublicId(), SCAN_ID, Stage.ID_BUILD));
    assertEvaluationAuditLog("not-found", app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD, SCAN_ID, null);
  }

  @Test
  void testEvaluate_Unauthorized() throws Exception {
    ctx.assertResponseStatus(403, evaluate(unauthorizedUser(), app.getPublicId(), SCAN_ID, Stage.ID_BUILD));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, SCAN_ID, null);
  }

  private HttpResponse evaluate(
      Consumer<HttpRequest> user,
      String applicationPublicId,
      String scanId,
      String stageId) throws Exception
  {
    return ctx.restRequest()
        .with(user)
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(applicationPublicId)
        .body(new Stage(stageId))
        .post();
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
