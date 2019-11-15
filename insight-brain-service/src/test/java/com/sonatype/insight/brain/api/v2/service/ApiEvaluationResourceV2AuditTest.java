/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiEvaluationResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer.RestHandler;

import org.junit.Before;
import org.junit.Test;

public class ApiEvaluationResourceV2AuditTest
    extends AbstractAuditTest
{
  private static final String SCAN_ID = "scanId";

  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testPromoteScan() throws Exception {
    assertResponseStatus(200, promoteScan(true, true, null, app.getId(), SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog(null, app.getId(), app.getPublicId(), app.getName(), Stage.ID_OPERATE, RestHandler.SCAN_ID,
        false);
  }

  @Test
  public void testPromoteScan_NullPromoteScanRequest() throws Exception {
    assertResponseStatus(400,
        restRequest().path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.PROMOTE_SCAN_PATH)
            .parameter(app.getId()).post());
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testPromoteScan_BadApplicationId() throws Exception {
    assertResponseStatus(404, promoteScan(false, false, null, "badAppId", SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog("not-found", "badAppId", null, null, null, null, null);
  }

  @Test
  public void testPromoteScan_BadTargetStageId() throws Exception {
    assertResponseStatus(400, promoteScan(false, false, null, app.getId(), SCAN_ID, "badTargetStageId"));
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testPromoteScan_NoReport() throws Exception {
    assertResponseStatus(200, promoteScan(true, false, null, app.getId(), SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog("not-found", app.getId(), app.getPublicId(), app.getName(), Stage.ID_OPERATE,
        RestHandler.SCAN_ID, null);
  }

  @Test
  public void testPromoteScan_Unauthorized() throws Exception {
    assertResponseStatus(403, promoteScan(false, false, unauthorizedUser(), app.getId(), SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    hdsRespondWith(new ComponentEvaluationDataList()).atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
        .replace("{purpose: evaluation|integration}", ApiComponentEvaluationServiceV2.PURPOSE_EVALUATION));
    int componentCount = 3;

    ApiComponentEvaluationTicketDTOV2 result = evaluateComponents(createEvaluateRequest(componentCount)).post()
        .getBody(ApiComponentEvaluationTicketDTOV2.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "resultId", result.resultId);
  }

  @Test
  public void testEvaluateComponents_ErrorDuringAsyncComponentEvaluationTask() throws Exception {
    hdsRespondWith("Service Unavailable").andStatus(503).atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
        .replace("{purpose: evaluation|integration}", ApiComponentEvaluationServiceV2.PURPOSE_EVALUATION));

    evaluateComponents(createEvaluateRequest(1)).post();

    assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "bad-gateway");
  }

  @Test
  public void testEvaluateComponents_Unauthorized() throws Exception {
    evaluateComponents(null).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetComponentEvaluation() throws Exception {
    String resultId = "resultId";
    ApiComponentEvaluationResultDTOV2 evaluationResultDTO = new ApiComponentEvaluationResultDTOV2();
    evaluationResultDTO.results.add(new ApiComponentDetailsDTOV2());
    File componentDetailsFile = new File(
        getCLMServer().getWorkDir() + "/componentDetails/" + app.getId() + "/componentDetails-" + resultId + ".json");
    JsonUtils.write(componentDetailsFile, evaluationResultDTO);

    getComponentEvaluation(resultId).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_EVALUATION_AD_HOC, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "resultId", resultId);
  }

  @Test
  public void testGetComponentEvaluation_Unauthorized() throws Exception {
    getComponentEvaluation("resultId").with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_EVALUATION_AD_HOC, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private HttpResponse promoteScan(boolean createScanFile,
                                   boolean createReport,
                                   Consumer<HttpRequest> user,
                                   String applicationId,
                                   String scanId,
                                   String stageId) throws Exception
  {
    if (createScanFile) {
      createScanFile(app.getId(), SCAN_ID);
    }
    if (createReport) {
      mockReport(RestHandler.SCAN_ID, "/AbstractAuditTest/report");
    }
    return restRequest().with(user)
        .path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.PROMOTE_SCAN_PATH)
        .parameter(applicationId).body(ApiPromoteScanRequestDTOV2.fromScan(scanId, stageId)).post();
  }

  private HttpRequest evaluateComponents(ApiComponentEvaluationRequestDTOV2 request) {
    return restRequest().path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, app.getId()).body(request);
  }

  private HttpRequest getComponentEvaluation(String resultId) {
    return restRequest().path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, app.getId(), "results", resultId);
  }

  private ApiComponentEvaluationRequestDTOV2 createEvaluateRequest(int componentCount) {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    for (int c = 0; c < componentCount; c++) {
      request.components.add(new ComponentEvaluationV2Helper()
          .createComponent(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), "h"));
    }
    return request;
  }
}
