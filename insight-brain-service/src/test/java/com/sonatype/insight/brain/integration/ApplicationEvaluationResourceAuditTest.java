/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.function.Consumer;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Before;
import org.junit.Test;

@Category(SlowTest.class)
public class ApplicationEvaluationResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testEvaluate() throws Exception {
    String scanId = mockReport("/AbstractAuditTest/report");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    mockScanReceipt(receipt);
    assertResponseStatus(200, evaluate(null, app.getPublicId(), Stage.ID_BUILD));
    assertEvaluationAuditLog(null, app.getId(), app.getPublicId(), app.getName(), Stage.ID_BUILD, scanId, false);
  }

  @Test
  public void testEvaluate_BadApplicationPublicId() throws Exception {
    assertResponseStatus(404, evaluate(null, "badApplicationPublicId", Stage.ID_BUILD));
    assertEvaluationAuditLog("not-found", null, "badApplicationPublicId", null, null, null, null);
  }

  @Test
  public void testEvaluate_BadStageId() throws Exception {
    assertResponseStatus(400, evaluate(null, app.getPublicId(), "badStageId"));
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testEvaluate_ErrorDuringAsyncEvaluationTask() throws Exception {
    hdsRespondWith("Invalid license").andStatus(402).atUri("rest/application/analysis");
    assertResponseStatus(200, evaluate(null, app.getPublicId(), Stage.ID_BUILD));
    assertAuditLog(AuditEvent.EVALUATE_APPLICATION, "unlicensed");
  }

  @Test
  public void testEvaluate_Unauthorized() throws Exception {
    assertResponseStatus(403, evaluate(unauthorizedUser(), app.getPublicId(), Stage.ID_BUILD));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  private HttpResponse evaluate(Consumer<HttpRequest> user, String applicationPublicId, String stageId)
      throws Exception
  {
    return restRequest().with(user)
        .path(ApplicationEvaluationResource.RESOURCE_PATH, ApplicationEvaluationResource.EVALUATE_PATH)
        .query("scanType", ClientScanType.SONATYPE).parameter(applicationPublicId, IntegrationType.CLI, stageId).post();
  }
}
