/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

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
    mockReport(SCAN_ID, "/AbstractAuditTest/report.zip");

    final Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy the first time
    HttpResponse response = restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", SCAN_ID).body(stage).post();
    assertResponseStatus(200, response);

    // Re-evaluate
    response = restRequest(app.getPublicId(), SCAN_ID).path("reevaluatePolicy").post();
    assertResponseStatus(200, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 2).get(1), null, app.getId(),
        app.getPublicId(), app.getName(), stage.getStageTypeId(), SCAN_ID, true);
  }

  @Test
  public void testReevaluatePolicy_Unauthorized() throws Exception {
    // Attempt to re-evaluate with a user that doesn't have permissions
    HttpResponse response = restRequest(app.getPublicId(), SCAN_ID)
        .auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword()).path("reevaluatePolicy").post();
    assertResponseStatus(403, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null, unauthorizedUser.getUsername());
  }

  @Test
  public void testReevaluatePolicy_NonExistentScanId() throws Exception {
    String scanId = "unknown-scan-id";
    // Attempt to re-evaluate a scanId that doesn't exist
    HttpResponse response = restRequest(app.getPublicId(), scanId).path("reevaluatePolicy").post();
    assertResponseStatus(400, response);

    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, scanId, null);
  }

  @Test
  public void testReevaluatePolicy_BadApplicationId() throws Exception {
    String appId = "unknown-app-public-id";
    HttpResponse response = restRequest(appId, SCAN_ID).path("reevaluatePolicy").post();
    assertResponseStatus(404, response);

    assertEvaluationAuditLog("not-found", null, appId, null, null, null, null);
  }
}
