/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.DefaultApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportResultsDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ScanHelper;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ApiReportServiceV2Test
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  @Inject
  private PolicyEvaluateService policyEvaluateService;

  @Inject
  private InsightWork insightWork;

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
    //setup application scan reports and evaluations
    tempEntity.newPolicy(appOne);
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ScanHelper.createDummyScanFile(insightWork, appOne.getId(), scanId1);
    ScanHelper.createDummyScanFile(insightWork, appOne.getId(), scanId2);
    ScanHelper.createDummyScanFile(insightWork, appOne.getId(), scanId3);
    createReportFile(appOne.getId(), scanId1, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(appOne.getId(), scanId2, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);
    createReportFile(appOne.getId(), scanId3, zipReportDir("/ApiReportResourceV2Test/report", tempDir), insightWork);

    // Eval policy
    evalRequest(appOne.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    evalRequest(appOne.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE));
    evalRequest(appOne.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));

    //When fetching all reports for application
    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForApplication(appOne.getId());

    //Verify 3 reports with correct results are retrieved
    assertThat(reports.applicationId).isEqualTo(appOne.getId());
    assertThat(reports.reports).hasSize(3);
    assertPolicyEvaluationResults(reports.reports.get(0));
    assertPolicyEvaluationResults(reports.reports.get(1));
    assertPolicyEvaluationResults(reports.reports.get(2));
  }

  @Test
  public void testGetReportHistoryForApplication_NoReport() {
    //setup evaluation
    tempEntity.newPolicy(appOne);
    tempEntity.newPolicyEvaluation(appOne.getId(), "build", "scanId");

    //When fetching all reports for application
    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForApplication(appOne.getId());

    //Verify no reports are retrieved
    assertThat(reports.applicationId).isEqualTo(appOne.getId());
    assertThat(reports.reports).isEmpty();
  }

  private void evalRequest(String appId, String scanId, Stage stage) throws IOException {
    policyEvaluateService.evaluate(appId, scanId, stage);
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
            .isEqualTo(UserInterfaceLinksResource.getLatestReportUrl(app.getPublicId(), expectedStageId));
        assertThat(report.reportPdfUrl)
            .isEqualTo(UserInterfaceLinksResource.getPdfUrl(app.getPublicId(), expectedScanId));
        assertThat(report.reportHtmlUrl)
            .isEqualTo(UserInterfaceLinksResource.getReportUrl(app.getPublicId(), expectedScanId));
        assertThat(report.embeddableReportHtmlUrl)
            .isEqualTo(UserInterfaceLinksResource.getEmbeddableReportUrl(app.getPublicId(), expectedScanId));
        assertThat(report.reportDataUrl)
            .isEqualTo(DefaultApiReportDataResourceV2.getDataUrl(app.getPublicId(), expectedScanId));
        return;
      }
    }
    fail("Did not find appId:" + app.getPublicId() + " stage:" + expectedStageId + " scanId:" + expectedScanId);
  }
}
