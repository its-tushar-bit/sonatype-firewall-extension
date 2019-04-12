/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiReportDataResourceV2Test
    extends AbstractResourceTest
{
  @Test
  public void testGetData_Redirect() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .parameter("app id", "scan/id").get();

    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location"))
        .isEqualTo(getRestBaseUrl() + "api/v2/applications/app%20id/reports/scan%2Fid/raw");
  }

  @Test
  public void testGetDataUrl() {
    assertThat(ApiReportDataResourceV2.getDataUrl("app id", "scan/id"))
        .isEqualTo("api/v2/applications/app%20id/reports/scan%2Fid/raw");
  }

  @Test
  public void testGetRawData() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report");

    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(ApiReportDataResourceV2.RAW_DATA_PATH).parameter(appPublicId, scanId).get();

    assertResponseStatus(200, response);
    ApiReportRawDataDTOV2 dto = response.getBody(ApiReportRawDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report");

    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(ApiReportDataResourceV2.POLICY_DATA_PATH).parameter(appPublicId, scanId).get();

    assertResponseStatus(200, response);
    ApiReportPolicyDataDTOV2 dto = response.getBody(ApiReportPolicyDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
    assertThat(dto.counts.get("totalComponentCount")).isEqualTo(2);
    // counts should not have null props
    assertThat(dto.counts).doesNotContainKey("grandfatheredPolicyViolationCount");
  }

  @Test
  public void testGetPolicyViolations_noCounts() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report-no-counts");

    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(ApiReportDataResourceV2.POLICY_DATA_PATH).parameter(appPublicId, scanId).get();

    assertResponseStatus(200, response);
    ApiReportPolicyDataDTOV2 dto = response.getBody(ApiReportPolicyDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
    // should not have counts prop if there are no counts
    assertThat(response.getBodyText()).doesNotContain("counts");
  }

  private void createReport(String appPublicId, String scanId, String resource) throws IOException {
    Application app = tempEntity.newApplicationWithParent(appPublicId);
    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(app.getId(), scanId);
    FileUtils.copyFile(zipResourceDir("/ApiReportDataResourceV2Test/" + resource), reportFile);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId);
  }
}
