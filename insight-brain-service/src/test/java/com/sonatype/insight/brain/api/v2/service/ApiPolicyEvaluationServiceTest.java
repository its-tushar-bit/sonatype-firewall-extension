/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationPolicyEvaluationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyEvaluationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiPolicyEvaluationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyEvaluationService apiPolicyEvaluationService;

  @Inject
  private PolicyEvaluateService policyEvaluateService;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testGetApplicationEvaluations() throws IOException, URISyntaxException {
    //setup application, scan reports and evaluations
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(app);
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    createReportFile(app.getId(), scanId1, zipReportDir("/ApiEvaluationResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId2, zipReportDir("/ApiEvaluationResourceV2Test/report", tempDir), insightWork);
    createReportFile(app.getId(), scanId3, zipReportDir("/ApiEvaluationResourceV2Test/report", tempDir), insightWork);

    // Eval policy
    evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD));
    evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE));
    evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD));

    //When fetching all evals for application
    ApiApplicationPolicyEvaluationsDTO evaluations = apiPolicyEvaluationService.getAllPolicyEvaluations(app.getId());

    //Verify 3 evals with correct results are retrieved
    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.policyEvaluations).hasSize(3);
    assertPolicyEvaluationResults(evaluations.policyEvaluations.get(0));
    assertPolicyEvaluationResults(evaluations.policyEvaluations.get(1));
    assertPolicyEvaluationResults(evaluations.policyEvaluations.get(2));
  }

  @Test
  public void testGetApplicationEvaluations_NoReport() throws IOException, URISyntaxException {
    //setup application and evaluation
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(app);
    tempEntity.newPolicyEvaluation(app.getId(), "build", "scanId");

    //When fetching all evals for application
    ApiApplicationPolicyEvaluationsDTO evaluations = apiPolicyEvaluationService.getAllPolicyEvaluations(app.getId());

    //Verify 1 eval with correct results are retrieved
    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.policyEvaluations).hasSize(1);
    assertFalse(evaluations.policyEvaluations.get(0).isReportAvailable);
  }

  private void evalRequest(String appId, String scanId, Stage stage) throws IOException {
    policyEvaluateService.evaluate(appId, scanId, stage);
  }

  private void assertPolicyEvaluationResults(ApiPolicyEvaluationDTO apiPolicyEvaluationDTO) {
    assertTrue(apiPolicyEvaluationDTO.isReportAvailable);
    assertThat(apiPolicyEvaluationDTO.policyEvaluationResult.getAffectedComponentCount())
        .isEqualTo(7);
    assertThat(apiPolicyEvaluationDTO.policyEvaluationResult.getCriticalComponentCount())
        .isEqualTo(0);
    assertThat(apiPolicyEvaluationDTO.policyEvaluationResult.getModerateComponentCount())
        .isEqualTo(0);
    assertThat(apiPolicyEvaluationDTO.policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
  }
}
