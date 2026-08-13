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
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.report.ReportPurger;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.94.0
 */
@IqPostgresTest
class ApiReportResourceV2Test
{
  private IqTestContext ctx;

  @Test
  void testGetReportHistoryForApplication() throws Exception {
    // setup
    Application app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newPolicy(app);
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    final String scanId3 = "ScanId3";
    ctx.createScanFile(app.getId(), scanId1);
    ctx.createScanFile(app.getId(), scanId2);
    ctx.createScanFile(app.getId(), scanId3);
    ctx.mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");

    // Eval policy
    HttpResponse response = evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_RELEASE)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);

    // when fetching reports
    response =
        ctx.restRequest()
            .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
            .parameter(app.getId())
            .get();

    // then assert application with the 3 correct reports are returned
    ctx.assertResponseStatus(200, response);
    ApiReportHistoryDTO evaluations = response.getBody(ApiReportHistoryDTO.class);
    assertThat(evaluations.applicationId).isEqualTo(app.getId());
    assertThat(evaluations.reports).hasSize(3);
    assertPolicyEvaluationResults(evaluations.reports.get(0));
    assertPolicyEvaluationResults(evaluations.reports.get(1));
    assertPolicyEvaluationResults(evaluations.reports.get(2));
  }

  @Test
  void testGetReportHistoryForApplication_ExcludesPurgedReports() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String scanId1 = "scan1";
    String scanId2 = "scan2";
    String scanId3 = "scan3";
    ctx.createScanFile(app.getId(), scanId1);
    ctx.createScanFile(app.getId(), scanId2);
    ctx.createScanFile(app.getId(), scanId3);
    ctx.mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");
    HttpResponse response = evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);

    response = ctx.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
        .parameter(app.getId())
        .get();

    // No reports are purged so they should all be included
    ctx.assertResponseStatus(200, response);
    ApiReportHistoryDTO result = response.getBody(ApiReportHistoryDTO.class);
    assertThat(result.reports).extracting(r -> r.scanId).containsExactly("scan3", "scan2", "scan1");

    // Purge the last report
    DataRetentionPolicy dataRetentionPolicy = new DataRetentionPolicy();
    dataRetentionPolicy.setOwnerId(app.getId());
    dataRetentionPolicy.setContextId(BuildStageType.ID);
    dataRetentionPolicy.setMaxCount(2);
    dataRetentionPolicy.setPurgingEnabled(true);
    ctx.lookup(DataRetentionPolicyDAO.class).insert(dataRetentionPolicy);
    ctx.lookup(ReportPurger.class).execute((JobExecutionContext) null);

    // Only the 2 most recent reports should be included
    response = ctx.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
        .parameter(app.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    result = response.getBody(ApiReportHistoryDTO.class);
    assertThat(result.reports).extracting(r -> r.scanId).containsExactly("scan3", "scan2");
  }

  @Test
  void testGetReportHistoryForApplication_Limit() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    String scanId1 = "scan1";
    String scanId2 = "scan2";
    String scanId3 = "scan3";
    ctx.createScanFile(app.getId(), scanId1);
    ctx.createScanFile(app.getId(), scanId2);
    ctx.createScanFile(app.getId(), scanId3);
    ctx.mockReport(scanId1, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId2, "/" + getClass().getSimpleName() + "/report");
    ctx.mockReport(scanId3, "/" + getClass().getSimpleName() + "/report");
    HttpResponse response = evalRequest(app.getPublicId(), scanId1, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId2, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);
    response = evalRequest(app.getPublicId(), scanId3, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);

    response = ctx.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiReportResourceV2.PATH, "{applicationId}/history")
        .parameter(app.getId())
        .query("limit", 2)
        .get();

    ctx.assertResponseStatus(200, response);
    ApiReportHistoryDTO result = response.getBody(ApiReportHistoryDTO.class);
    assertThat(result.reports).extracting(r -> r.scanId).containsExactly("scan3", "scan2");
  }

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return ctx.restRequest()
        .path(PolicyEvaluateResource.RESOURCE_PATH)
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
