/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.sbom.ingestion.SbomDetectionResultDTO;
import com.sonatype.insight.brain.sbom.ingestion.SbomImportResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockServer.RestServlet;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IQ Server on PostgreSQL — audit assertions for SBOM import, converted from
 * {@link com.sonatype.insight.brain.sbom.ingestion.SbomImportResourceAuditTest}.
 */
@IqPostgresTest
class IqPostgresSbomImportResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private Application app;

  private PolicyEvaluationHelper policyEvaluationHelper;

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();

    app = ctx.tempEntity().newApplicationWithParent();
    policyEvaluationHelper = ctx.lookup(PolicyEvaluationHelper.class);
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
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  @Test
  void testImportDetectedSbom() throws Exception {
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);
    ctx.mockReport(RestServlet.SCAN_ID, "/AbstractAuditTest/report");
    URL resource = getClass().getResource("/SbomImportResourceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse responseDetect = ctx.restRequest()
        .parameter(app.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.RESOURCE_PATH, SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = responseDetect.getBody(SbomDetectionResultDTO.class);

    HttpResponse responseCommit = ctx.restRequest()
        .path(SbomImportResource.RESOURCE_PATH, SbomImportResource.COMMIT_PATH)
        .parameter(app.getId(), actual.getSbomSummary().applicationVersion)
        .post();
    ctx.assertResponseStatus(202, responseCommit);
    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CREATE_SBOM_VERSION, 1, null);
    assertCustomData(auditDTOs.get(0), "applicationId", app.getId());
    assertCustomData(auditDTOs.get(0), "sbomVersion", "a140fd3c3ded4bb0a640dc31e2904dc9");
    assertCustomData(auditDTOs.get(0), "status", "UPLOADED");
    assertCustomData(auditDTOs.get(0), "operation", "CREATE");
    assertCustomData(auditDTOs.get(0), "stageId", "compliance");

    ApiThirdPartyScanTicketDTO responseCommitBody = responseCommit.getBody(ApiThirdPartyScanTicketDTO.class);
    policyEvaluationHelper.awaitEvaluationFinished(app.getId(), getStatusId(responseCommitBody.statusUrl));
  }

  private String getStatusId(String statusUrl) {
    return statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
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
