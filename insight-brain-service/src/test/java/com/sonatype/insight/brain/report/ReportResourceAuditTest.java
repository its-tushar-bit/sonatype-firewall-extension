/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.mock.hds.HdsMockServer;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.ReportResource.BROWSE_PATH;
import static com.sonatype.insight.brain.report.ReportResource.PRINT_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  private static final String SCAN_ID = "ReportResourceAuditTest_ScanId";

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("ReportResourceAuditTest_AppId");
  }

  private HttpRequest restRequest(String appId, String scanId) {
    return restRequest().path(ReportResource.RESOURCE_PATH).parameter(appId, scanId);
  }

  @Test
  public void testReevaluatePolicy() throws Exception {
    mockReport(SCAN_ID, "/AbstractAuditTest/report");
    // Mock the HDS report for the new scan
    mockReport(HdsMockServer.RestServlet.SCAN_ID, "/AbstractAuditTest/report");
    createScanFile(app.getId(), SCAN_ID);

    final Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy the first time
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", SCAN_ID)
        .body(stage)
        .post();
    assertResponseStatus(200, response);

    // Re-evaluate
    response = restRequest(app.getPublicId(), SCAN_ID).path("{scanId}/reevaluatePolicy").post();
    assertResponseStatus(200, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 2).get(1), null, app.getId(),
        app.getPublicId(), app.getName(), stage.getStageTypeId(), SCAN_ID, true);
  }

  @Test
  public void testReevaluatePolicy_Unauthorized() throws Exception {
    // Attempt to re-evaluate with a user that doesn't have permissions
    HttpResponse response = restRequest(app.getPublicId(), SCAN_ID)
        .with(unauthorizedUser())
        .path("{scanId}/reevaluatePolicy")
        .post();
    assertResponseStatus(403, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testReevaluatePolicy_NonExistentScanId() throws Exception {
    String scanId = "unknown-scan-id";
    // Attempt to re-evaluate a scanId that doesn't exist
    HttpResponse response = restRequest(app.getPublicId(), scanId).path("{scanId}/reevaluatePolicy").post();
    assertResponseStatus(400, response);

    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, scanId, null);
  }

  @Test
  public void testReevaluatePolicy_BadApplicationId() throws Exception {
    String appId = "unknown-app-public-id";
    HttpResponse response = restRequest(appId, SCAN_ID)
        .path("{scanId}/reevaluatePolicy")
        .post();
    assertResponseStatus(404, response);

    assertEvaluationAuditLog("not-found", null, appId, null, null, null, null);
  }

  @Test
  public void testBrowseReport_Json() throws Exception {
    createReportFile(app.getId(), SCAN_ID);

    restRequest(app.getPublicId(), SCAN_ID).path(BROWSE_PATH, DATA_JSON.getName()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  public void testBrowseReport_Html_NoAudit() throws Exception {
    assertNoAuditSubpath("index.html");
  }

  @Test
  public void testBrowseReport_Js_NoAudit() throws Exception {
    assertNoAuditSubpath("insight.js");
  }

  @Test
  public void testBrowseReport_Unauthorized() throws Exception {
    restRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).path(BROWSE_PATH, DATA_JSON.getName()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDownloadBundle() throws Exception {
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    createScanFile(app.getId(), SCAN_ID);

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
  public void testDownloadBundle_Unauthorized() throws Exception {
    mockReport(SCAN_ID, "/ReportResourceTest/report");

    restRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).path(ReportResource.DOWNLOAD_BUNDLE_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testPrintReport() throws Exception {
    createReportFile(app.getId(), SCAN_ID);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);

    restRequest(app.getPublicId(), SCAN_ID).path(PRINT_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  public void testPrintReport_Unauthorized() throws Exception {
    restRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).path(PRINT_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private void assertNoAuditSubpath(final String subpath) throws Exception {
    mockReport(SCAN_ID, "/ReportResourceTest/report");

    restRequest(app.getPublicId(), SCAN_ID).path(BROWSE_PATH, subpath).get();

    assertThat(awaitLogEntries(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT, 0)).isEmpty();
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/ReportResourceTest/sample-report");
  }
}
