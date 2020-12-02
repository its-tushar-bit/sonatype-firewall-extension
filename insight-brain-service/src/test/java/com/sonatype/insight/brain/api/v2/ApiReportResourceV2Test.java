/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportResultsDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.94.0
 */
public class ApiReportResourceV2Test
    extends AbstractResourceTest
{
  @Test
  public void testGetReportHistoryForApplication() throws Exception {
    //setup
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicy(app);
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    createScanFile(app.getId(), scanId1);
    createScanFile(app.getId(), scanId2);
    createScanFile(app.getId(), scanId3);
    mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");

    // Eval policy
    HttpResponse response = evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE)).post();
    assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    //when fetching reports
    response =
        restRequest()
            .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, DefaultApiReportResourceV2.PATH, "{applicationId}/history")
            .parameter(app.getId()).get();

    //then assert application with the 3 correct reports are returned
    assertResponseStatus(200, response);
    ApiReportHistoryDTO evaluations = response.getBody(ApiReportHistoryDTO.class);
    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.reports).hasSize(3);
    assertPolicyEvaluationResults(evaluations.reports.get(0));
    assertPolicyEvaluationResults(evaluations.reports.get(1));
    assertPolicyEvaluationResults(evaluations.reports.get(2));
  }

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return super.restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).query("scanId", scanId).parameter(appId)
        .body(stage);
  }

  private void assertPolicyEvaluationResults(ApiReportResultsDTO reportResultsDTO) {
    assertThat(reportResultsDTO.policyEvaluationResult.getAffectedComponentCount())
        .isEqualTo(7);
    assertThat(reportResultsDTO.policyEvaluationResult.getCriticalComponentCount())
        .isEqualTo(0);
    assertThat(reportResultsDTO.policyEvaluationResult.getModerateComponentCount())
        .isEqualTo(0);
    assertThat(reportResultsDTO.policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
  }
}
