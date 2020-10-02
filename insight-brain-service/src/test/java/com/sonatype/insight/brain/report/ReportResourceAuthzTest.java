/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class ReportResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ReportResource.RESOURCE_PATH);
  }

  @Test
  public void testAuditLog() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("auditLog/{path}").parameter(app.getPublicId(), "scanId", "security.json");
    testAuthzGet(request);
  }

  @Test
  public void testBrowseReport() throws Exception {
    String scanId = "scanId";
    createReportFile(app.getId(), scanId);

    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("browseReport/{path}")
        .parameter(app.getPublicId(), scanId, "data.json");
    testAuthzGet(request);
  }

  @Test
  public void testDownloadBundle() throws Exception {
    String scanId = "scanId";
    mockReport(scanId, "/ReportResourceTest/report");
    ScanPolicyEvaluator scanPolicyEvaluator = getCLMServer().getInstance(ScanPolicyEvaluator.class);
    scanPolicyEvaluator.evaluate(app, scanId, new Stage(Stage.ID_BUILD));

    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    HttpRequest request = restRequest().path(ReportResource.DOWNLOAD_BUNDLE_PATH)
        .parameter(app.getPublicId(), scanId);
    testAuthzGet(request);
  }

  @Test
  public void testReevaluatePolicy() throws Exception {
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    HttpRequest request = restRequest().path("reevaluatePolicy").parameter(app.getPublicId(), "scanId");
    testAuthzPost(request);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/ReportResourceTest/sample-report");
  }
}
