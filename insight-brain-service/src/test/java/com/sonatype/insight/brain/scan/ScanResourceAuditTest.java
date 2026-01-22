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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.mock.hds.HdsMockServer.RestServlet;

import org.junit.Before;
import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ScanResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  private static final String RESOURCE_PATH = "/AbstractAuditTest/report";

  private static final String FILENAME = "AbstractAuditTest-report";

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("ScanResourceAuditTest_AppId");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ScanResource.RESOURCE_PATH);
  }

  private HttpRequest uploadRequest(String appPublicId, String stageId, String resource) {
    return restRequest().query("stageId", stageId).parameter(appPublicId)
        .part("file", resource, getClass().getResource("/" + getClass().getSimpleName() + "/" + resource))
        .part("filename", resource);
  }

  @Test
  public void testUploadBinary() throws Exception {
    mockReport(RestServlet.SCAN_ID, RESOURCE_PATH);
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "file").post();
    assertResponseStatus(200, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), null, app.getId(),
        app.getPublicId(), app.getName(), Stage.ID_BUILD, RestServlet.SCAN_ID, false);
  }

  @Test
  public void testUploadBinary_ReportNotFound() throws Exception {
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, "file").post();
    assertResponseStatus(200, response);

    assertEvaluationAuditLog("not-found", app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD,
        RestServlet.SCAN_ID, null);
  }

  @Test
  public void testUploadBinary_Unauthorized() throws Exception {
    HttpResponse response = uploadRequest(app.getPublicId(), Stage.ID_BUILD, FILENAME).with(unauthorizedUser())
        .post();
    assertResponseStatus(403, response);

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testUploadBinary_BadApplicationId() throws Exception {
    String appId = "unknown-app-public-id";
    HttpResponse response = uploadRequest(appId, Stage.ID_BUILD, FILENAME).post();
    assertResponseStatus(404, response);

    assertEvaluationAuditLog("not-found", null, appId, null, null, null, null);
  }

  @Test
  public void testUploadBinary_BadStageId() throws Exception {
    String stageId = "invalid-stage-id";
    HttpResponse response = uploadRequest(app.getPublicId(), stageId, FILENAME).post();
    assertResponseStatus(400, response);

    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }
}
