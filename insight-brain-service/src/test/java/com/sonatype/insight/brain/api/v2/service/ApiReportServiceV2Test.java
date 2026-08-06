/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportResultsDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

@Category(SlowTest.class)
public class ApiReportServiceV2Test
    extends AbstractServiceAuthzTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  @Inject
  private PolicyEvaluateService policyEvaluateService;

  @Inject
  LegacyViolationService legacyViolationService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  private Application appOne;

  private Application appThree;

  @Before
  public void setup() {
    appOne = tempEntity.newApplicationWithParent("one");
    tempEntity.newPolicyEvaluation(appOne.getId(), StageTypes.BUILD.getId(), "one-old",
        new Date(System.currentTimeMillis() - 1000));
    tempEntity.newPolicyEvaluation(appOne.getId(), StageTypes.BUILD.getId(), "one-build");
    tempEntity.newPolicyEvaluation(appOne.getId(), StageTypes.RELEASE.getId(), "one-release");
    grantReadPermission(appOne.getId());
    grantEvaluateApplicationPermission(appOne.getId());

    Application app = tempEntity.newApplicationWithParent("two");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.STAGE_RELEASE.getId(), "two");

    appThree = tempEntity.newApplicationWithParent("three");
    tempEntity.newPolicyEvaluation(appThree.getId(), StageTypes.OPERATE.getId(), "three");
    grantReadPermission(appThree.getId());

    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Test
  public void testGetAll() {
    List<ApiApplicationReportDTOV2> reports = apiReportServiceV2.getAll();

    assertThat(reports).hasSize(3);

    assertContainsReport(appOne, StageTypes.BUILD, "one-build", reports);
    assertContainsReport(appOne, StageTypes.RELEASE, "one-release", reports);
    assertContainsReport(appThree, StageTypes.OPERATE, "three", reports);
  }

  @Test
  public void testGetByApplicationId() {
    List<ApiApplicationReportDTOV2> reports = apiReportServiceV2.getByApplicationId(appOne.getId());

    assertThat(reports).hasSize(2);

    assertContainsReport(appOne, StageTypes.BUILD, "one-build", reports);
    assertContainsReport(appOne, StageTypes.RELEASE, "one-release", reports);
  }

  @Test
  public void testGetReportHistoryForApplication() throws IOException, URISyntaxException {
    // setup application scan reports and evaluations
    Application app = tempEntity.newApplicationWithParent("application");
    tempEntity.newPolicy(app);
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId2);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId3);
    createReportFile(app.getId(), scanId1,
        zipReportDir("/ApiReportResourceV2Test/report-with-iq-scanner-version", tempDir), insightWork);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId3, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);

    // Eval policy
    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD), ScanTriggerType.WEB_UI);
    evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE), ScanTriggerType.IDE);
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD), ScanTriggerType.REPOSITORY_MANAGER);

    // When fetching all reports for application
    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(app, null, null);

    // Verify 3 reports with correct results are retrieved
    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(3);
    assertPolicyEvaluationResults(reports.reports.get(0));
    assertPolicyEvaluationResults(reports.reports.get(1));
    assertPolicyEvaluationResults(reports.reports.get(2));

    assertThat(reports.reports.get(0).scanTriggerType).isEqualTo(ScanTriggerType.REPOSITORY_MANAGER.getId());
    assertThat(reports.reports.get(0).scanTriggerTypeDisplayName).isEqualTo(
        ScanTriggerType.REPOSITORY_MANAGER.getDisplayName());
    assertThat(reports.reports.get(0).scanTriggerInternal).isFalse();
    assertThat(reports.reports.get(0).scannerVersion).isEqualTo("2.6-SNAPSHOT");
    assertThat(reports.reports.get(1).scanTriggerType).isEqualTo(ScanTriggerType.IDE.getId());
    assertThat(reports.reports.get(1).scanTriggerTypeDisplayName).isEqualTo(ScanTriggerType.IDE.getDisplayName());
    assertThat(reports.reports.get(1).scanTriggerInternal).isFalse();
    assertThat(reports.reports.get(1).scannerVersion).isEqualTo("2.6-SNAPSHOT");
    assertThat(reports.reports.get(2).scanTriggerType).isEqualTo(ScanTriggerType.WEB_UI.getId());
    assertThat(reports.reports.get(2).scanTriggerTypeDisplayName).isEqualTo(ScanTriggerType.WEB_UI.getDisplayName());
    assertThat(reports.reports.get(2).scanTriggerInternal).isTrue();
    assertThat(reports.reports.get(2).scannerVersion).isEqualTo("1.189.0-01");
  }

  @Test
  public void testGetReportHistoryForApplication_ByStage() throws IOException, URISyntaxException {
    Application app = tempEntity.newApplicationWithParent("application");
    tempEntity.newPolicy(app);
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId2);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId3);
    createReportFile(app.getId(), scanId1, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId3, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);

    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE));
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(app, "release", null);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(1);
    assertThat(reports.reports.get(0).stage).isEqualTo("release");

    reports = apiReportServiceV2.getReportHistoryForOwner(app, "build", null);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(2);
    assertThat(reports.reports.get(0).stage).isEqualTo("build");
    assertThat(reports.reports.get(1).stage).isEqualTo("build");
  }

  @Test
  public void testGetReportHistoryForApplication_ByStage_EmptyResultSet() throws IOException, URISyntaxException {
    Application app = tempEntity.newApplicationWithParent("application");
    tempEntity.newPolicy(app);
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId2);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId3);
    createReportFile(app.getId(), scanId1, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId3, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);

    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE));
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(app, "source", null);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).isEmpty();
  }

  @Test
  public void testGetReportHistoryForApplication_ByLimit() throws IOException, URISyntaxException {
    Application app = tempEntity.newApplicationWithParent("application");
    tempEntity.newPolicy(app);
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId2);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId3);
    createReportFile(app.getId(), scanId1, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId3, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);

    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE));

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(app, null, 2);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(2);
    assertThat(reports.reports.get(0).scanId).isEqualTo("ScanId3");
    assertThat(reports.reports.get(1).scanId).isEqualTo("ScanId2");
  }

  @Test
  public void testGetReportHistoryForApplication_ByStageAndLimit() throws IOException, URISyntaxException {
    Application app = tempEntity.newApplicationWithParent("application");
    tempEntity.newPolicy(app);
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId2);
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId3);
    createReportFile(app.getId(), scanId1, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId3, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);

    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE));

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(app, "build", 1);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(1);
    assertThat(reports.reports.get(0).stage).isEqualTo("build");
    assertThat(reports.reports.get(0).scanId).isEqualTo("ScanId2"); // should return most recent
  }

  @Test
  public void testGetReportHistoryForApplication_InvalidStage() {
    Application app = tempEntity.newApplicationWithParent("application");
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    assertThatThrownBy(() -> apiReportServiceV2.getReportHistoryForOwner(app, "no-such-stage", null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Invalid stage: no-such-stage.");
  }

  @Test
  public void testGetReportHistoryForApplication_InvalidLimit() {
    Application app = tempEntity.newApplicationWithParent("application");
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    assertThatThrownBy(() -> apiReportServiceV2.getReportHistoryForOwner(app, null, 0))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Limit must be positive integer.");
  }

  @Test
  public void testResolveHistoryLimit_DefaultClampAndRejectNonPositive() {
    assertThat(ApiReportServiceV2.resolveHistoryLimit(null)).isEqualTo(100);
    assertThat(ApiReportServiceV2.resolveHistoryLimit(1)).isEqualTo(1);
    assertThat(ApiReportServiceV2.resolveHistoryLimit(100)).isEqualTo(100);
    assertThat(ApiReportServiceV2.resolveHistoryLimit(101)).isEqualTo(100);
    assertThat(ApiReportServiceV2.resolveHistoryLimit(10_000)).isEqualTo(100);
    // Non-positive rejection at the service boundary is covered by
    // testGetReportHistoryForApplication_InvalidLimit; cover -1 here at the helper.
    assertThatThrownBy(() -> ApiReportServiceV2.resolveHistoryLimit(-1))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Limit must be positive integer.");
  }

  @Test
  public void testGetReportHistoryForApplication_LegacyViolationCount() throws IOException, URISyntaxException {
    // setup application scan reports and evaluations
    Application app = tempEntity.newApplicationWithParent("application");
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());
    grantWritePermission();
    Policy policy = tempEntity.newPolicy(app);

    final String scanId1 = "ScanId1";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);
    createReportFile(app.getId(), scanId1, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));

    final String scanId2 = "ScanId2";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId2);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(app, null, null);

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(2);
    assertThat(reports.reports.get(0).policyEvaluationResult.getLegacyViolationCount()).isEqualTo(36);
    assertThat(reports.reports.get(1).policyEvaluationResult.getLegacyViolationCount()).isZero();
  }

  @Test
  public void testGetReportHistoryForApplication_NoReport() {
    // setup evaluation
    Application application = tempEntity.newApplicationWithParent("application");
    grantReadPermission(application.getId());

    // When fetching all reports for application
    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForOwner(application, null, null);

    // Verify no reports are retrieved
    assertThat(reports.applicationId).isEqualTo(application.getId());
    assertThat(reports.reports).isEmpty();
  }

  @Test
  public void testGetReportHistoryWhenTheReportFileWasDeleted_NoExceptionWasThrow() throws URISyntaxException, IOException {
    // setup evaluation
    Application app = tempEntity.newApplicationWithParent("application");
    tempEntity.newPolicy(app);
    grantReadPermission(app.getId());
    grantEvaluateApplicationPermission(app.getId());

    final String scanId1 = "ScanId1";
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId1);

    File reportFile = zipReportDir("/ApiReportResourceV2Test/report", tempDir);
    FileUtils.copyFile(reportFile, insightWork.getReportFile(app.getId(), scanId1));

    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    // removing report file
    FileUtils.delete(insightWork.getReportFile(app.getId(), scanId1));

    // executing when the reports were deleted
    ApiReportHistoryDTO apiReportHistoryDTO =
        apiReportServiceV2.getReportHistoryForOwner(app, null, null);

    // no exceptions were thrown
    assertThat(apiReportHistoryDTO).isNotNull();
  }

  private void evalRequest(String appId, String scanId, Stage stage) throws IOException {
    evalRequest(appId, scanId, stage, ScanTriggerType.CLI);
  }

  private void evalRequest(String appId, String scanId, Stage stage, ScanTriggerType triggerType) throws IOException {
    policyEvaluateService.evaluate(appId, scanId, stage, triggerType);
  }

  private void assertPolicyEvaluationResults(ApiReportResultsDTO apiReportResultsDTO) {
    assertThat(apiReportResultsDTO.policyEvaluationResult.getAffectedComponentCount())
        .isEqualTo(7);
    assertThat(apiReportResultsDTO.policyEvaluationResult.getCriticalComponentCount())
        .isEqualTo(0);
    assertThat(apiReportResultsDTO.policyEvaluationResult.getModerateComponentCount())
        .isEqualTo(0);
    assertThat(apiReportResultsDTO.policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
  }

  private void assertContainsReport(
      Application app,
      StageType expectedStage,
      String expectedScanId,
      List<ApiApplicationReportDTOV2> actual)
  {
    String expectedStageId = expectedStage.getId();
    for (ApiApplicationReportDTOV2 report : actual) {
      if (app.getId().equals(report.applicationId) && expectedStageId.equals(report.stage)) {
        assertThat(report.latestReportHtmlUrl)
            .isEqualTo(UserInterfaceLinksHelper.getLatestReportUrl(app.getPublicId(), expectedStageId));
        assertThat(report.reportPdfUrl)
            .isEqualTo(UserInterfaceLinksHelper.getPdfUrl(app.getPublicId(), expectedScanId));
        assertThat(report.reportHtmlUrl)
            .isEqualTo(UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), expectedScanId));
        assertThat(report.embeddableReportHtmlUrl)
            .isEqualTo(UserInterfaceLinksHelper.getEmbeddableReportUrl(app.getPublicId(), expectedScanId));
        assertThat(report.reportDataUrl)
            .isEqualTo(ApiReportDataResourceV2.getDataUrl(app.getPublicId(), expectedScanId));
        return;
      }
    }
    fail("Did not find appId:" + app.getPublicId() + " stage:" + expectedStageId + " scanId:" + expectedScanId);
  }
}
