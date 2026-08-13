/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

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
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer.RestServlet;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Converted from the legacy {@code ApiEvaluationResourceV2AuditTest}.
 */
@IqH2Test
public class IqH2ApiEvaluationResourceV2AuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private static final String SCAN_ID = "scanId";

  private Application app;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    app = ctx.tempEntity().newApplicationWithParent();
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

  @Test
  void testPromoteScan() throws Exception {
    ctx.assertResponseStatus(200, promoteScan(true, true, null, app.getId(), SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog(null, app.getId(), app.getPublicId(), app.getName(), Stage.ID_OPERATE, RestServlet.SCAN_ID,
        false);
  }

  @Test
  void testPromoteScan_NullPromoteScanRequest() throws Exception {
    ctx.assertResponseStatus(400,
        ctx.restRequest()
            .path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.PROMOTE_SCAN_PATH)
            .parameter(app.getId())
            .post());
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  void testPromoteScan_BadApplicationId() throws Exception {
    ctx.assertResponseStatus(404, promoteScan(false, false, null, "badAppId", SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog("not-found", "badAppId", null, null, null, null, null);
  }

  @Test
  void testPromoteScan_BadTargetStageId() throws Exception {
    ctx.assertResponseStatus(400, promoteScan(false, false, null, app.getId(), SCAN_ID, "badTargetStageId"));
    assertEvaluationAuditLog("bad-request", app.getId(), app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  void testPromoteScan_NoReport() throws Exception {
    ctx.assertResponseStatus(200, promoteScan(true, false, null, app.getId(), SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog("not-found", app.getId(), app.getPublicId(), app.getName(), Stage.ID_OPERATE,
        RestServlet.SCAN_ID, null);
  }

  @Test
  void testPromoteScan_Unauthorized() throws Exception {
    ctx.assertResponseStatus(403,
        promoteScan(false, false, unauthorizedUser(), app.getId(), SCAN_ID, Stage.ID_OPERATE));
    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "unauthorized", app.getId(),
        app.getPublicId(), app.getName(), null, null, null);
  }

  @Test
  void testEvaluateComponents() throws Exception {
    ctx.hdsRespondWith(new ComponentEvaluationDataList())
        .atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
            .replace("{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_EVALUATION));
    int componentCount = 3;

    ApiComponentEvaluationTicketDTOV2 result = evaluateComponents(createEvaluateRequest(componentCount)).post()
        .getBody(ApiComponentEvaluationTicketDTOV2.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "resultId", result.resultId);
  }

  @Test
  void testEvaluateComponents_ErrorDuringAsyncComponentEvaluationTask() throws Exception {
    ctx.hdsRespondWith("Service Unavailable")
        .andStatus(503)
        .atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
            .replace("{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_EVALUATION));

    evaluateComponents(createEvaluateRequest(1)).post();

    assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "bad-gateway");
  }

  @Test
  void testEvaluateComponents_Unauthorized() throws Exception {
    evaluateComponents(null).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetComponentEvaluation() throws Exception {
    String resultId = "resultId";
    ApiComponentEvaluationResultDTOV2 evaluationResultDTO = new ApiComponentEvaluationResultDTOV2();
    evaluationResultDTO.results.add(new ApiComponentDetailsDTOV2());
    File componentDetailsFile =
        ctx.lookup(InsightWork.class).getComponentDetailsFile(app.getId(), resultId);
    JsonUtils.write(componentDetailsFile, evaluationResultDTO);

    getComponentEvaluation(resultId).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_EVALUATION_AD_HOC, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "resultId", resultId);
  }

  @Test
  void testGetComponentEvaluation_Unauthorized() throws Exception {
    getComponentEvaluation("resultId").with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_EVALUATION_AD_HOC, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testEvaluateSourceControl_Unauthorized() throws Exception {
    ctx.assertResponseStatus(403, evaluateSourceControl(unauthorizedUser(), app.getId(), Stage.ID_DEVELOP));
    assertSourceControlEvaluationAuditLog("unauthorized", app.getId(), app.getPublicId(), app.getName());
  }

  private HttpResponse evaluateSourceControl(
      Consumer<HttpRequest> user,
      String applicationId,
      String stageId) throws Exception
  {
    ctx.tempEntity().newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    PasswordHandler pwHandler = ctx.lookup(PasswordHandler.class);
    ctx.tempEntity()
        .newSourceControl(app.getId(), "http://example.com/my/repo.git", null,
            new String(pwHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, "TestBaseBranchName", null);

    return ctx.restRequest()
        .with(user)
        .path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2,
            ApiEvaluationResourceV2.SOURCE_CONTROL_EVALUATION_PATH)
        .parameter(applicationId)
        .body(new ApiSourceControlEvaluationRequestDTO(stageId, "TestBranchName"))
        .post();
  }

  private HttpResponse promoteScan(
      boolean createScanFile,
      boolean createReport,
      Consumer<HttpRequest> user,
      String applicationId,
      String scanId,
      String stageId) throws Exception
  {
    if (createScanFile) {
      ctx.createScanFile(app.getId(), SCAN_ID);
    }
    if (createReport) {
      ctx.mockReport(RestServlet.SCAN_ID, "/AbstractAuditTest/report");
    }
    return ctx.restRequest()
        .with(user)
        .path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.PROMOTE_SCAN_PATH)
        .parameter(applicationId)
        .body(ApiPromoteScanRequestDTOV2.fromScan(scanId, stageId))
        .post();
  }

  private HttpRequest evaluateComponents(ApiComponentEvaluationRequestDTOV2 request) {
    return ctx.restRequest().path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, app.getId()).body(request);
  }

  private HttpRequest getComponentEvaluation(String resultId) {
    return ctx.restRequest().path(PublicApiPaths.APPLICATION_EVALUATION_PATH_V2, app.getId(), "results", resultId);
  }

  private ApiComponentEvaluationRequestDTOV2 createEvaluateRequest(int componentCount) {
    PolicyDAO policyDAO = ctx.lookup(PolicyDAO.class);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    for (int c = 0; c < componentCount; c++) {
      request.components.add(new ComponentEvaluationV2Helper(policyDAO)
          .createComponent(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), "h"));
    }
    return request;
  }

  private void assertSourceControlEvaluationAuditLog(
      String error,
      String applicationId,
      String applicationPublicId,
      String applicationName)
  {
    AuditDTO auditDTO = awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0);
    assertStandardData(auditDTO, AuditEvent.EVALUATE_APPLICATION, error, null /* username */);
    assertApplicationData(auditDTO, applicationId, applicationPublicId, applicationName);
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
