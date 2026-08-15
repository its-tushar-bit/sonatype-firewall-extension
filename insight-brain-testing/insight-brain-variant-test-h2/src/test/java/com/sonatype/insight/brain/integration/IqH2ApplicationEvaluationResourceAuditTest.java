/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.function.Consumer;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.mock.hds.HdsMockResponse;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the {@code com.sonatype.insight.brain.integration} package (matching {@code ApplicationEvaluationResource})
 * because {@link #evaluate} uses that resource's package-private {@code RESOURCE_PATH}/{@code EVALUATE_PATH}
 * constants.
 */
@IqH2Test
class IqH2ApplicationEvaluationResourceAuditTest
    implements AuditTestSupport
{
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

  private HdsMockResponse mockScanReceipt(ScanReceipt scanReceipt) {
    return ctx.hdsRespondWith(scanReceipt).atUri(ScanUploader.HDS_PATH);
  }

  private HttpResponse evaluate(
      Consumer<HttpRequest> user,
      String applicationPublicId,
      String stageId) throws Exception
  {
    return ctx.restRequest()
        .with(user)
        .path(ApplicationEvaluationResource.RESOURCE_PATH, ApplicationEvaluationResource.EVALUATE_PATH)
        .query("scanType", ClientScanType.SONATYPE)
        .parameter(applicationPublicId, IntegrationType.CLI, stageId)
        .post();
  }

  @Test
  void testEvaluate() throws Exception {
    String scanId = mockReport("/AbstractAuditTest/report");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    mockScanReceipt(receipt);
    ctx.assertResponseStatus(200, evaluate(null, app.getPublicId(), Stage.ID_BUILD));
    assertEvaluationAuditLog(null, app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD, scanId, false);
  }

  @Test
  void testEvaluate_BadApplicationPublicId() throws Exception {
    ctx.assertResponseStatus(404, evaluate(null, "badApplicationPublicId", Stage.ID_BUILD));
    assertEvaluationAuditLog("not-found", null, "badApplicationPublicId", null, null, null, null);
  }

  @Test
  void testEvaluate_BadStageId() throws Exception {
    ctx.assertResponseStatus(400, evaluate(null, app.getPublicId(), "badStageId"));
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  void testEvaluate_ErrorDuringAsyncEvaluationTask() throws Exception {
    ctx.hdsRespondWith("Invalid license").andStatus(402).atUri("rest/application/analysis");
    ctx.assertResponseStatus(200, evaluate(null, app.getPublicId(), Stage.ID_BUILD));
    assertAuditLog(AuditEvent.EVALUATE_APPLICATION, "unlicensed");
  }

  @Test
  void testEvaluate_Unauthorized() throws Exception {
    ctx.assertResponseStatus(403, evaluate(unauthorizedUser(), app.getPublicId(), Stage.ID_BUILD));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
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
