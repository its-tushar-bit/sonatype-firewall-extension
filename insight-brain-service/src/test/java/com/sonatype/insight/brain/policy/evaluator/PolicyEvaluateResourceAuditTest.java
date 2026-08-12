/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.function.Consumer;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyEvaluateResourceAuditTest
    extends AbstractAuditTest
{
  private static final String SCAN_ID = "scanId";

  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testEvaluate() throws Exception {
    String scanId = mockReport("/AbstractAuditTest/report");
    createScanFile(app.getId(), scanId);
    assertResponseStatus(200, evaluate(null, app.getPublicId(), scanId, Stage.ID_BUILD));
    assertEvaluationAuditLog(null, app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD, scanId, false);
  }

  @Test
  public void testEvaluate_Reevaluation() throws Exception {
    String scanId = mockReport("/AbstractAuditTest/report");
    createScanFile(app.getId(), scanId);
    assertResponseStatus(200, evaluate(null, app.getPublicId(), scanId, Stage.ID_BUILD));
    assertResponseStatus(200, evaluate(null, app.getPublicId(), scanId, Stage.ID_BUILD));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 2).get(1), null, app.getId(),
        app.getPublicId(), app.getName(), Stage.ID_BUILD, scanId, true);
  }

  @Test
  public void testEvaluate_BadApplicationPublicId() throws Exception {
    assertResponseStatus(404, evaluate(null, "badApplicationPublicId", SCAN_ID, Stage.ID_BUILD));
    assertEvaluationAuditLog("not-found", null, "badApplicationPublicId", null, null, SCAN_ID, null);
  }

  @Test
  public void testEvaluate_BadStageId() throws Exception {
    createScanFile(app.getId(), SCAN_ID);
    assertResponseStatus(400, evaluate(null, app.getPublicId(), SCAN_ID, "badStageId"));
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, SCAN_ID, null);
  }

  @Test
  public void testEvaluate_BadScanId() throws Exception {
    createScanFile(app.getId(), SCAN_ID);
    assertResponseStatus(404, evaluate(null, app.getPublicId(), SCAN_ID, Stage.ID_BUILD));
    assertEvaluationAuditLog("not-found", app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD, SCAN_ID, null);
  }

  @Test
  public void testEvaluate_Unauthorized() throws Exception {
    assertResponseStatus(403, evaluate(unauthorizedUser(), app.getPublicId(), SCAN_ID, Stage.ID_BUILD));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, SCAN_ID, null);
  }

  private HttpResponse evaluate(
      Consumer<HttpRequest> user,
      String applicationPublicId,
      String scanId,
      String stageId) throws Exception
  {
    return restRequest().with(user)
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(applicationPublicId)
        .body(new Stage(stageId))
        .post();
  }
}
