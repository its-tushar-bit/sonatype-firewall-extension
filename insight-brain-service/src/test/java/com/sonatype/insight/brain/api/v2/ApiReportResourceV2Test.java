/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportResultsDTO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.report.ReportPurger;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.94.0
 */
@Category(SlowTest.class)
public class ApiReportResourceV2Test
    extends AbstractResourceTest
{
  @Test
  public void testGetReportHistoryForApplication() throws Exception {
    // setup
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

    // when fetching reports
    response =
        restRequest()
            .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
            .parameter(app.getId())
            .get();

    // then assert application with the 3 correct reports are returned
    assertResponseStatus(200, response);
    ApiReportHistoryDTO evaluations = response.getBody(ApiReportHistoryDTO.class);
    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.reports).hasSize(3);
    assertPolicyEvaluationResults(evaluations.reports.get(0));
    assertPolicyEvaluationResults(evaluations.reports.get(1));
    assertPolicyEvaluationResults(evaluations.reports.get(2));
  }

  @Test
  public void testGetReportHistoryForApplication_ExcludesPurgedReports() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String scanId1 = "scan1";
    String scanId2 = "scan2";
    String scanId3 = "scan3";
    createScanFile(app.getId(), scanId1);
    createScanFile(app.getId(), scanId2);
    createScanFile(app.getId(), scanId3);
    mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");
    HttpResponse response = evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    response = restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
        .parameter(app.getId())
        .get();

    // No reports are purged so they should all be included
    assertResponseStatus(200, response);
    ApiReportHistoryDTO result = response.getBody(ApiReportHistoryDTO.class);
    assertThat(result.reports).extracting(r -> r.scanId).containsExactly("scan3", "scan2", "scan1");

    // Purge the last report
    DataRetentionPolicy dataRetentionPolicy = new DataRetentionPolicy();
    dataRetentionPolicy.setOwnerId(app.getId());
    dataRetentionPolicy.setContextId(BuildStageType.ID);
    dataRetentionPolicy.setMaxCount(2);
    dataRetentionPolicy.setPurgingEnabled(true);
    lookup(DataRetentionPolicyDAO.class).insert(dataRetentionPolicy);
    lookup(ReportPurger.class).execute(null);

    // Only the 2 most recent reports should be included
    response = restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
        .parameter(app.getId())
        .get();
    assertResponseStatus(200, response);
    result = response.getBody(ApiReportHistoryDTO.class);
    assertThat(result.reports).extracting(r -> r.scanId).containsExactly("scan3", "scan2");
  }

  @Test
  public void testGetReportHistoryForApplication_Limit() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String scanId1 = "scan1";
    String scanId2 = "scan2";
    String scanId3 = "scan3";
    createScanFile(app.getId(), scanId1);
    createScanFile(app.getId(), scanId2);
    createScanFile(app.getId(), scanId3);
    mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");
    HttpResponse response = evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    response = restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
        .parameter(app.getId())
        .query("limit", 2)
        .get();

    assertResponseStatus(200, response);
    ApiReportHistoryDTO result = response.getBody(ApiReportHistoryDTO.class);
    assertThat(result.reports).extracting(r -> r.scanId).containsExactly("scan3", "scan2");
  }

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return super.restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(appId)
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
