/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.scan.model.ClientScanType;

import java.io.IOException;
import org.junit.Test;

import static com.sonatype.insight.mock.hds.HdsMockServer.RestServlet.SCAN_ID;

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
  public void testDownloadBundle() throws Exception {
    String scanId = "scanId";
    mockReport(scanId, "/ReportResourceTest/report");
    ScanPolicyEvaluator scanPolicyEvaluator = getCLMServer().getInstance(ScanPolicyEvaluator.class);
    scanPolicyEvaluator.evaluate(app, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    HttpRequest request = restRequest().path(ReportResource.DOWNLOAD_BUNDLE_PATH)
        .parameter(app.getPublicId(), scanId);
    testAuthzGet(request);
  }

  @Test
  public void testReevaluatePolicy_application() throws Exception {
    String scanId = "scanId";
    createScanFile(app.getId(), scanId);
    mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    HttpRequest request = restRequest().path("{scanId}/reevaluatePolicy").parameter(app.getPublicId(), "scanId");
    testAuthzPost(request);
  }

  @Test
  public void testReevaluatePolicy_containerImageForFirewall() throws Exception {
    Organization organization = tempEntity.newOrganizationWithRepositoryManager("test-org-for-firewall");
    Application application = tempEntity.newApplicationWithParent(organization);

    String scanId = "scanId";
    createScanFile(application.getId(), scanId);
    mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, scanId);
    createReportFile(application.getId(), scanId);
    grantPermission(application.getId(), Permission.EVALUATE_COMPONENT);
    HttpRequest request = restRequest().path("{scanId}/reevaluatePolicy").parameter(application.getPublicId(), scanId);
    testAuthzPost(request);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/ReportResourceTest/sample-report");
  }
}
