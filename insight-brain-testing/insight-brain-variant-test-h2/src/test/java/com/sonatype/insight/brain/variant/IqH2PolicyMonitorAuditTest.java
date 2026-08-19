/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConfig;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer.RestServlet;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; implements {@link AuditTestSupport} directly
 * (rather than inheriting {@code AbstractAuditTest}) so it can register its own {@link LogOutput} scoped to the
 * {@code audit} logger, plus a second {@link LogOutput} scoped to {@link PolicyMonitor} for the info-level log
 * assertions the legacy {@code PolicyMonitorAuditTest} made via its own {@code @Rule}.
 */
@IqH2Test
class IqH2PolicyMonitorAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private final TestLogOutput policyMonitorLogOutput = new TestLogOutput(PolicyMonitor.class);

  private PolicyMonitor policyMonitor;

  private Application app;

  private Application app2;

  private Stage stage;

  private Stage complianceStage;

  private InsightWork insightWork;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    policyMonitorLogOutput.before();
    policyMonitorLogOutput.clear();

    policyMonitor = ctx.lookup(PolicyMonitor.class);
    app = ctx.tempEntity().newApplicationWithParent("MonitoredApp");
    app2 = ctx.tempEntity().newApplicationWithParent("MonitoredApp2");
    stage = new Stage(ReleaseStageType.ID);
    complianceStage = new Stage(ComplianceStageType.ID);
    ctx.tempEntity().newPolicyMonitoring(app.getId(), stage.getStageTypeId());
    ctx.tempEntity().newPolicyMonitoring(app2.getId(), complianceStage.getStageTypeId());
    insightWork = ctx.lookup(InsightWork.class);
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
    policyMonitorLogOutput.tearDown();
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
  void testRunEvaluation_AppWithMonitoring() throws IOException {
    ctx.createScanFile(app.getId(), RestServlet.SCAN_ID);
    ctx.tempEntity().newPolicyEvaluation(app.getId(), stage.getStageTypeId(), RestServlet.SCAN_ID);
    ctx.createReportFile(app.getId(), RestServlet.SCAN_ID, "/PolicyMonitorTest/report");

    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), null, app.getId(),
        app.getPublicId(), app.getName(), ReleaseStageType.ID, scanId2, false, SYSTEM_USER);
  }

  @Test
  void testRunEvaluation_AppWithMonitoring_WithNoLastPrimaryEvaluation() {
    policyMonitor.run();

    awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 0);
  }

  @Test
  void testRunEvaluation_AppWithMonitoring_WhenNoScanFileFound() {
    ctx.tempEntity().newPolicyEvaluation(app.getId(), stage.getStageTypeId(), RestServlet.SCAN_ID);

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), "server-error", app.getId(),
        app.getPublicId(), app.getName(), null, null, null, SYSTEM_USER);
  }

  @Test
  void testRunEvaluation_SbomManagerComplianceStage_AppWithMonitoring() throws Exception {
    setEvaluationQueueConfig(false);
    ctx.createScanFile(app2.getId(), RestServlet.SCAN_ID);
    ctx.tempEntity().newPolicyEvaluation(app2.getId(), complianceStage.getStageTypeId(), RestServlet.SCAN_ID);
    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);
    File scanZip = createScanFileZip(app2, scanId2, "scan/scan-third-party.xml");
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata = ctx.tempEntity()
        .newThirdPartySbomMetadata(thirdPartyFile.getId(),
            app2.getId(), ACTIVE, "xyz");
    ctx.tempEntity().newThirdPartyScan(scanId2, scanId2, thirdPartyFile, scanZip.getName());

    policyMonitor.run();

    assertEvaluationAuditLog(awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 1).get(0), null, app2.getId(),
        app2.getPublicId(), app2.getName(), ComplianceStageType.ID, scanId2, false, SYSTEM_USER);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_APPLICATION, null, SYSTEM_USER);
    assertCustomData(auditDTO, "sbomVersion", sbomMetadata.getSbomVersion());
    assertThat(policyMonitorLogOutput).atInfoLevel()
        .contains("SBOM Manager Policy Monitoring is enabled for application '" +
            app2.getName() + "' and stage '" + complianceStage.getStageTypeId() + "'");
  }

  @Test
  void testRunEvaluation_SbomManagerComplianceStage_AppWithMonitoring_MissingFilteredScanFile() {
    setEvaluationQueueConfig(false);
    ctx.createScanFile(app2.getId(), RestServlet.SCAN_ID);
    ctx.tempEntity().newPolicyEvaluation(app2.getId(), complianceStage.getStageTypeId(), RestServlet.SCAN_ID);
    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ctx.tempEntity().newThirdPartySbomMetadata(thirdPartyFile.getId(), app2.getId(), ACTIVE, "xyz");
    ctx.tempEntity().newThirdPartyScan(scanId2, scanId2, thirdPartyFile, "scan/deleted.gz");

    policyMonitor.run();

    awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 0);
  }

  @Test
  void testRunEvaluation_SbomManagerComplianceStage_evaluationQueueEnabled() throws Exception {
    setEvaluationQueueConfig(true);
    ctx.createScanFile(app2.getId(), RestServlet.SCAN_ID);
    ctx.tempEntity().newPolicyEvaluation(app2.getId(), complianceStage.getStageTypeId(), RestServlet.SCAN_ID);
    String scanId2 = "PolicyMonitorTest_scanId2";
    mockScanReceiptAndReport(scanId2);
    File scanZip = createScanFileZip(app2, scanId2, "scan/scan-third-party.xml");
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata = ctx.tempEntity()
        .newThirdPartySbomMetadata(thirdPartyFile.getId(),
            app2.getId(), ACTIVE, "xyz");
    ctx.tempEntity().newThirdPartyScan(scanId2, scanId2, thirdPartyFile, scanZip.getName());

    policyMonitor.run();

    awaitLogEntries(AuditEvent.EVALUATE_APPLICATION, 0);
    assertThat(policyMonitorLogOutput).atInfoLevel()
        .doesNotContain("SBOM Manager Policy Monitoring is enabled for application '" +
            app.getName() + "' and stage '" + stage.getStageTypeId() + "'");
  }

  private void mockScanReceiptAndReport(String scanId) {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    ctx.hdsRespondWith(scanReceipt).atUri(ScanUploader.HDS_PATH);
    ctx.mockReport(scanId, "/PolicyMonitorTest/report");
  }

  private File createScanFileZip(Application app, String scanId, final String fileName) throws Exception {
    URL resource = getClass().getResource("/PolicyMonitorTest/" + fileName);
    File scanXml = new File(resource.toURI());

    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    Files.createDirectories(scanFile.getParentFile().toPath());

    try (GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(scanFile.toPath()))) {
      FileUtils.copyFile(scanXml, gzipStream);
    }
    return scanFile;
  }

  private void setEvaluationQueueConfig(final boolean enabled) {
    EvaluationQueueConfig evaluationQueueConfig = EvaluationQueueConfig.builder().enabled(enabled).build();
    ApiConfigurationService apiConfigurationService = ctx.lookup(ApiConfigurationService.class);
    apiConfigurationService.setConfigurationNoAuthz(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG,
        JsonUtils.convertValue(evaluationQueueConfig, Map.class));
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    TestLogOutput(Class<?>... types) {
      super(types);
    }

    void tearDown() {
      after();
    }
  }
}
