/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.mock.hds.HdsMockServer;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.ReportResource.BROWSE_PATH;
import static com.sonatype.insight.brain.report.ReportResource.PRINT_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ReportResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application app;

  private static final String SCAN_ID = "ReportResourceAuditTest_ScanId";

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    app = ctx.tempEntity().newApplicationWithParent("ReportResourceAuditTest_AppId");
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
    return ctx.restRequest();
  }

  private HttpRequest restRequest(String appId, String scanId) {
    return ctx.restRequest().path(ReportResource.RESOURCE_PATH).parameter(appId, scanId);
  }

  @Test
  void testReevaluatePolicy() throws Exception {
    ctx.mockReport(SCAN_ID, "/AbstractAuditTest/report");
    // Mock the HDS report for the new scan
    ctx.mockReport(HdsMockServer.RestServlet.SCAN_ID, "/AbstractAuditTest/report");
    ctx.createScanFile(app.getId(), SCAN_ID);

    final Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy the first time
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", SCAN_ID)
        .body(stage)
        .post();
    ctx.assertResponseStatus(200, response);

    // Re-evaluate
    response = restRequest(app.getPublicId(), SCAN_ID).path("{scanId}/reevaluatePolicy").post();
    ctx.assertResponseStatus(200, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 2).get(1), null, app.getId(),
        app.getPublicId(), app.getName(), stage.getStageTypeId(), SCAN_ID, true);
  }

  @Test
  void testReevaluatePolicy_Unauthorized() throws Exception {
    // Attempt to re-evaluate with a user that doesn't have permissions
    HttpResponse response = restRequest(app.getPublicId(), SCAN_ID)
        .with(unauthorizedUser())
        .path("{scanId}/reevaluatePolicy")
        .post();
    ctx.assertResponseStatus(403, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  void testReevaluatePolicy_NonExistentScanId() throws Exception {
    String scanId = "unknown-scan-id";
    // Attempt to re-evaluate a scanId that doesn't exist
    HttpResponse response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy").post();
    ctx.assertResponseStatus(400, response);

    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, scanId, null);
  }

  @Test
  void testReevaluatePolicy_BadApplicationId() throws Exception {
    String appId = "unknown-app-public-id";
    HttpResponse response = restRequest(appId, SCAN_ID)
        .path("{scanId}/reevaluatePolicy")
        .post();
    ctx.assertResponseStatus(404, response);

    assertEvaluationAuditLog("not-found", null, appId, null, null, null, null);
  }

  @Test
  void testBrowseReport_Json() throws Exception {
    createReportFile(app.getId(), SCAN_ID);

    restRequest(app.getPublicId(), SCAN_ID).path(BROWSE_PATH, DATA_JSON.getName()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  void testBrowseReport_Html_NoAudit() throws Exception {
    assertNoAuditSubpath("index.html");
  }

  @Test
  void testBrowseReport_Js_NoAudit() throws Exception {
    assertNoAuditSubpath("insight.js");
  }

  @Test
  void testBrowseReport_Unauthorized() throws Exception {
    restRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).path(BROWSE_PATH, DATA_JSON.getName()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testDownloadBundle() throws Exception {
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");
    ctx.createScanFile(app.getId(), SCAN_ID);

    restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", SCAN_ID)
        .body(new Stage(Stage.ID_BUILD))
        .post();
    restRequest(app.getPublicId(), SCAN_ID).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  void testDownloadBundle_Unauthorized() throws Exception {
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");

    restRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testPrintReport() throws Exception {
    createReportFile(app.getId(), SCAN_ID);
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);

    restRequest(app.getPublicId(), SCAN_ID).path(PRINT_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  void testPrintReport_Unauthorized() throws Exception {
    restRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).path(PRINT_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private void assertNoAuditSubpath(final String subpath) throws Exception {
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");

    restRequest(app.getPublicId(), SCAN_ID).path(BROWSE_PATH, subpath).get();

    assertThat(awaitLogEntries(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT, 0)).isEmpty();
  }

  private void createReportFile(String appId, String scanId) throws Exception {
    ctx.createReportFile(appId, scanId, "/ReportResourceTest/sample-report");
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
