/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ClientScanType;

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

    HttpRequest request = restRequest().path("{scanId}/auditLog/{path}")
        .parameter(app.getPublicId(), "scanId", "security.json");
    testAuthzGet(request);
  }

  @Test
  public void testBrowseReport() throws Exception {
    String scanId = "scanId";
    createReportFile(app.getId(), scanId);

    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("{scanId}/browseReport/{path}")
        .parameter(app.getPublicId(), scanId, "data.json");
    testAuthzGet(request);
  }

  @Test
  public void testGetSbomPolicyViolationReport() throws Exception {
    final String scanId = "ReportResourceTest_ScanId";
    String sbomVersion = "sbomVersion";
    grantReadPermission(app.getId());
    createReportFile(app.getId(), scanId);

    tempEntity.newSbomEvaluation(app, sbomVersion, "spec1",
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"),
        "hash1", scanId, true, "ACTIVE");
    setFeatures(LicensedFeature.SBOM_MANAGER);
    HttpRequest request = restRequest().path("sbom/{sbomVersion}/sbomPolicyViolationReport")
        .parameter(app.getPublicId(), sbomVersion);
    testAuthzGet(request);
  }

  @Test
  public void testDownloadBundle() throws Exception {
    String scanId = "scanId";
    mockReport(scanId, "/ReportResourceTest/report");
    ScanPolicyEvaluator scanPolicyEvaluator = getCLMServer().getInstance(ScanPolicyEvaluator.class);
    scanPolicyEvaluator.evaluate(app, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI, ClientScanType.SONATYPE);

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
    HttpRequest request = restRequest().path("{scanId}/reevaluatePolicy").parameter(app.getPublicId(), "scanId");
    testAuthzPost(request);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/ReportResourceTest/sample-report");
  }
}
