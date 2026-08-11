/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.components.SbomComponentsResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;

/**
 * IQ Server on PostgreSQL — {@code SbomComponentsResourceAuditTest} converted to the reused-server
 * {@link IqPostgresTest} pattern. No base class; wiring is via the injected {@link IqTestContext}.
 */
@IqPostgresTest
class IqPostgresSbomComponentsResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private Application app;

  private InsightWork work;

  @BeforeEach
  void before() throws Exception {
    logOutput.before();
    logOutput.clear();
    app = ctx.tempEntity().newApplicationWithParent();
    work = ctx.lookup(InsightWork.class);
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

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SbomComponentsResource.RESOURCE_BASE_PATH);
  }

  @Test
  void testGetComponentsDetails() throws Exception {
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ctx.tempEntity()
            .newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
                thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        ctx.tempEntity().newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity vulnerability =
        ctx.tempEntity().newThirdPartyCoordinateSecurity(component, "cve", "d1", "l1", 9, "d1", "f1");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(vulnerability, "cve", "resolved",
            "code_not_reachable", "response", "details");
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsResourceTest", ctx.tempFolder()), reportFile);
    HttpResponse response = restRequest().path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion(), component.getHash())
        .get();

    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS, null);
    assertComponentDetailsCustomData(auditDTO, component.getHash());
  }

  @Test
  void testGetComponentsDetails_NotFound() throws Exception {
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);
    HttpResponse response = restRequest().path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .parameter(app.getId(), "any", "any")
        .get();

    ctx.assertResponseStatus(404, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS, "not-found");
    assertComponentDetailsCustomData(auditDTO, "any");
  }

  @Test
  void testGetComponentsDetails_unlicensed() throws Exception {
    HttpResponse response = restRequest().path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .parameter(app.getId(), "any", "any")
        .get();

    ctx.assertResponseStatus(402, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS, "unlicensed");
    assertCustomData(auditDTO, "applicationId", app.getId());
  }

  private void assertComponentDetailsCustomData(AuditDTO auditDTO, String componentHash) {
    assertCustomData(auditDTO, "applicationId", app.getId());
    assertCustomData(auditDTO, "componentHash", componentHash);
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
