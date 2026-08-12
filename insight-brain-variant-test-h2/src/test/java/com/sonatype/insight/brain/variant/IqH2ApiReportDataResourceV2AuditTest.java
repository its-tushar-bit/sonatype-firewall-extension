/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.function.Consumer;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2.SCAN_PATH;

/**
 * Converted from the legacy {@code ApiReportDataResourceV2AuditTest}.
 */
@IqH2Test
class IqH2ApiReportDataResourceV2AuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private Application app;

  private static final String SCAN_ID = "ApiReportResourceAuditTest_ScanId";

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    app = ctx.tempEntity().newApplicationWithParent("ApiReportResourceAuditTest_AppId");
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

  private HttpRequest reportDataRequest(String appId, String scanId, String reportTypePath) {
    return ctx.restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(reportTypePath)
        .parameter(appId, scanId);
  }

  @Test
  void testGetRawData() throws Exception {
    ctx.createScanFile(app.getId(), SCAN_ID);
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");
    ctx.restRequest()
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", SCAN_ID)
        .body(new Stage(Stage.ID_BUILD))
        .post();

    reportDataRequest(app.getPublicId(), SCAN_ID, ApiReportDataResourceV2.RAW_DATA_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  void testGetRawData_Unauthorized() throws Exception {
    reportDataRequest(app.getPublicId(), SCAN_ID, ApiReportDataResourceV2.RAW_DATA_PATH)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void getDependencyTree() throws Exception {
    ctx.createScanFile(app.getId(), SCAN_ID);
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");

    ctx.restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
        .parameter(app.getPublicId(), SCAN_ID)
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "not-found");
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  void getDependencyTree_Unauthorized() throws Exception {
    ctx.restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
        .parameter(app.getPublicId(), SCAN_ID)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  void testGetPolicyViolations() throws Exception {
    ctx.createScanFile(app.getId(), SCAN_ID);
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");
    ctx.restRequest()
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .parameter(app.getPublicId())
        .query("scanId", SCAN_ID)
        .body(new Stage(Stage.ID_BUILD))
        .post();

    reportDataRequest(app.getPublicId(), SCAN_ID, ApiReportDataResourceV2.POLICY_DATA_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  void testGetPolicyViolations_Unauthorized() throws Exception {
    reportDataRequest(app.getPublicId(), SCAN_ID, ApiReportDataResourceV2.POLICY_DATA_PATH)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
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
