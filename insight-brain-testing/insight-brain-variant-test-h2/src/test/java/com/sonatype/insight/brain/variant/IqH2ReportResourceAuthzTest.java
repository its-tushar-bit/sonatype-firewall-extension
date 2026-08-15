/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.scan.model.ClientScanType;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.mock.hds.HdsMockServer.RestServlet.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; reproduces the {@code AbstractResourceAuthzTest}
 * fixture (org/app/repo + authorized/unauthorized users) and its {@code testAuthzGet}/{@code testAuthcGet}/
 * {@code testAuthzPost} helpers that the legacy {@code ReportResourceAuthzTest} inherited from its base class.
 */
@IqH2Test
class IqH2ReportResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private Repository repo;

  private RepositoryManager repositoryManager;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    repositoryManager = ctx.tempEntity().newRepositoryManager();
    repo = ctx.tempEntity().newRepository(repositoryManager, "test");
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(ReportResource.RESOURCE_PATH);
  }

  private void assertStatus(HttpResponse response, Integer status) {
    if (status == null) {
      assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(400);
    }
    else {
      assertThat(response.getStatusCode()).isEqualTo(status);
    }
  }

  private HttpResponse testAuthzGet(HttpRequest request) throws Exception {
    return testAuthzGet(request, null);
  }

  private HttpResponse testAuthzGet(HttpRequest request, Integer expectedSuccessStatus) throws Exception {
    HttpResponse response = request.auth(unauthorized).get();
    assertStatus(response, 403);

    response = request.auth(authorized).get();
    assertStatus(response, expectedSuccessStatus);
    return response;
  }

  private HttpResponse testAuthzPost(HttpRequest request) throws Exception {
    HttpResponse response = request.auth(unauthorized).post();
    assertStatus(response, 403);

    response = request.auth(authorized).post();
    assertStatus(response, null);
    return response;
  }

  @Test
  void testAuditLog() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("{scanId}/auditLog/{path}")
        .parameter(app.getPublicId(), "scanId", "security.json");
    testAuthzGet(request);
  }

  @Test
  void testBrowseReport() throws Exception {
    String scanId = "scanId";
    createReportFile(app.getId(), scanId);

    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("{scanId}/browseReport/{path}")
        .parameter(app.getPublicId(), scanId, "data.json");
    testAuthzGet(request);
  }

  @Test
  void testDownloadBundle() throws Exception {
    String scanId = "scanId";
    ctx.mockReport(scanId, "/ReportResourceTest/report");
    ScanPolicyEvaluator scanPolicyEvaluator = ctx.lookup(ScanPolicyEvaluator.class);
    scanPolicyEvaluator.evaluate(app, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    HttpRequest request = restRequest().path(ReportResource.DOWNLOAD_BUNDLE_PATH)
        .parameter(app.getPublicId(), scanId);
    testAuthzGet(request);
  }

  @Test
  void testReevaluatePolicy_application() throws Exception {
    String scanId = "scanId";
    ctx.createScanFile(app.getId(), scanId);
    ctx.mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");
    ctx.tempEntity().newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    HttpRequest request = restRequest().path("{scanId}/reevaluatePolicy").parameter(app.getPublicId(), "scanId");
    testAuthzPost(request);
  }

  @Test
  void testReevaluatePolicy_containerImageForFirewall() throws Exception {
    Organization organization = ctx.tempEntity().newOrganizationWithRepositoryManager("test-org-for-firewall");
    Application application = ctx.tempEntity().newApplicationWithParent(organization);

    String scanId = "scanId";
    ctx.createScanFile(application.getId(), scanId);
    ctx.mockReport(scanId, "/ReportResourceTest/report");
    // Mock the HDS report for the new scan
    ctx.mockReport(SCAN_ID, "/ReportResourceTest/report");
    ctx.tempEntity().newPolicyEvaluation(application.getId(), Stage.ID_PROXY, scanId);
    createReportFile(application.getId(), scanId);
    grantPermission(application.getId(), Permission.EVALUATE_COMPONENT);
    HttpRequest request = restRequest().path("{scanId}/reevaluatePolicy").parameter(application.getPublicId(), scanId);
    testAuthzPost(request);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    ctx.createReportFile(appId, scanId, "/ReportResourceTest/sample-report");
  }
}
