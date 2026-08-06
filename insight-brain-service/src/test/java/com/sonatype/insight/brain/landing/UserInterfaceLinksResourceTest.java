/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.COMPONENT_SCAN_REPORT_PATH;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.INTEGRATIONS_PRIORITIES_PATH;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.POLICY_VIOLATION_REPORT_PATH;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.PRIORITIES_PATH;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.PRIORITIES_PATH_LEGACY;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksResource.ASSET_INDEX_PATH;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class UserInterfaceLinksResourceTest
    extends AbstractResourceTest
{
  private static final String ASSET_INDEX_PATH_NO_SLASH = ASSET_INDEX_PATH.substring(1);

  private void assertRedirect(HttpResponse response, String expected) {
    assertResponseStatus(307, response);
    String expectedLocation = getRestBaseUrl().replaceFirst("/+$", "") + "/" + expected.replaceFirst("^/+", "");
    assertThat(response.getHeader("Location")).isEqualTo(expectedLocation);
  }

  private HttpResponse get(String path, Object... params) throws Exception {
    return restRequest().path(UserInterfaceLinksHelper.RESOURCE_PATH, path).parameter(params).anon().get();
  }

  @Test
  public void testLinkToDeveloperHome() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.DEVELOPER_HOME_PATH);
    assertRedirect(response, ASSET_INDEX_PATH_NO_SLASH + "#/developer/dashboard");
  }

  @Test
  public void testLinkToFirewallHome() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.FIREWALL_HOME_PATH);
    assertRedirect(response, ASSET_INDEX_PATH_NO_SLASH + "#/firewall/dashboard");
  }

  @Test
  public void testLinkToLifecycleHome() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.LIFECYCLE_HOME_PATH);
    assertRedirect(response, ASSET_INDEX_PATH_NO_SLASH + "#/dashboard/violations");
  }

  @Test
  public void testLinkToSbomManagerHome() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.SBOM_MANAGER_HOME_PATH);
    assertRedirect(response, ASSET_INDEX_PATH_NO_SLASH + "#/sbomManager/dashboard");
  }

  @Test
  public void testLinkToManagement_App() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.MANAGEMENT_PATH, "application", "test id");
    assertRedirect(response, "assets/index.html#/management/view/application/test%20id");
  }

  @Test
  public void testLinkToManagement_App_sbomManager() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.SBOM_MANAGEMENT_PATH, "application", "test id");
    assertRedirect(response, "assets/index.html#/sbomManager/management/view/application/test%20id");
  }

  @Test
  public void testLinkToManagement_Org() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.MANAGEMENT_PATH, "organization", "test id");
    assertRedirect(response, "assets/index.html#/management/view/organization/test%20id");
  }

  @Test
  public void testLinkToManagement_Org_sbomManager() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.SBOM_MANAGEMENT_PATH, "organization", "test id");
    assertRedirect(response, "assets/index.html#/sbomManager/management/view/organization/test%20id");
  }

  @Test
  public void testLinkToSourceControlManagement_ForwardsGithubAppIdIntoHashRoute() throws Exception {
    HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH + "/" + UserInterfaceLinksHelper.SOURCE_CONTROL_MANAGEMENT_PATH)
        .parameter("organization", "test id")
        .query("githubAppId=github-app-123")
        .anon()
        .get();

    assertRedirect(response,
        "assets/index.html#/management/edit/organization/test%20id/source-control?githubAppId=github-app-123");
  }

  @Test
  public void testLinkToManagementEdit_Category() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.ITEM_MANAGEMENT_EDIT_PATH, "organization", "test id", "category", "category id");
    assertRedirect(response, "assets/index.html#/management/edit/organization/test%20id/category/category%20id");
    response =
        get(UserInterfaceLinksHelper.ITEM_MANAGEMENT_EDIT_PATH, "application", "test id", "category", "category id");
    assertRedirect(response, "assets/index.html#/management/edit/application/test%20id/category/category%20id");
  }

  @Test
  public void testLinkToManagementEdit_Label() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.ITEM_MANAGEMENT_EDIT_PATH, "organization", "test id", "label", "label id");
    assertRedirect(response, "assets/index.html#/management/edit/organization/test%20id/label/label%20id");
    response = get(UserInterfaceLinksHelper.ITEM_MANAGEMENT_EDIT_PATH, "application", "test id", "label", "label id");
    assertRedirect(response, "assets/index.html#/management/edit/application/test%20id/label/label%20id");
  }

  @Test
  public void testLinkToManagementEdit_Policy() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.ITEM_MANAGEMENT_EDIT_PATH, "organization", "test id", "policy", "policy id");
    assertRedirect(response, "assets/index.html#/management/edit/organization/test%20id/policy/policy%20id");
    response = get(UserInterfaceLinksHelper.ITEM_MANAGEMENT_EDIT_PATH, "application", "test id", "policy", "policy id");
    assertRedirect(response, "assets/index.html#/management/edit/application/test%20id/policy/policy%20id");
  }

  @Test
  public void testLinkToReport() throws Exception {
    assertThat(UserInterfaceLinksHelper.getReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/app%20id/report/scan%20id");
    HttpResponse response = get(UserInterfaceLinksHelper.REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/applicationReport/app%20id/scan%20id/policy");
  }

  @Test
  public void testLinkComponentScanReport() throws Exception {
    assertThat(UserInterfaceLinksHelper.getReportUrl("my-app-id", "my-scan-id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/my-app-id/report/my-scan-id");
    HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH + "/" + COMPONENT_SCAN_REPORT_PATH)
        .parameter("my-app-id", "my-scan-id", "my-component-scan-hash")
        .query("utm_source=github")
        .anon()
        .get();
    assertRedirect(response,
        "assets/index.html?utm_source=github#/applicationReport/my-app-id" +
            "/my-scan-id/componentDetails/my-component-scan-hash/overview");
  }

  @Test
  public void testLinkComponentScanReportWithTabQueryParam() throws Exception {
    assertThat(UserInterfaceLinksHelper.getReportUrl("my-app-id", "my-scan-id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/my-app-id/report/my-scan-id");
    HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH + "/" + COMPONENT_SCAN_REPORT_PATH)
        .parameter("my-app-id", "my-scan-id", "my-component-scan-hash")
        .query("utm_source=github&tab=violations")
        .anon()
        .get();
    assertRedirect(response,
        "assets/index.html?utm_source=github&tab=violations#/applicationReport/my-app-id" +
            "/my-scan-id/componentDetails/my-component-scan-hash/violations");
  }

  @Test
  public void testLinkToPolicyViolationReport() throws Exception {
    assertThat(UserInterfaceLinksHelper.getPolicyViolationReportPath("my-pv-id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/policyViolationReport/my-pv-id");
    HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH + "/" + POLICY_VIOLATION_REPORT_PATH)
        .parameter("my-pv-id")
        .query("utm_source=github")
        .anon()
        .get();
    assertRedirect(response,
        "assets/index.html?utm_source=github#/violation/my-pv-id?type=violation&sidebarReference=filter");
  }

  @Test
  public void testLinkToSbom() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.LATEST_VERSION_SBOM_REPORT_PATH, "appId", "scanId");
    assertRedirect(response, "api/v2/cycloneDx/1.7/appId/reports/scanId");
  }

  @Test
  public void testLinkToPolicyViolationDetails() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.POLICY_VIOLATION_DETAILS_PATH, "violationId");
    assertRedirect(response, "assets/index.html#/violation/violationId");
  }

  @Test
  public void testLinkToAddWaiver() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.ADD_WAIVER_PATH, "violationId");
    assertRedirect(response, "assets/index.html#/addWaiver/violationId");
  }

  @Test
  public void testLinkToAddWaiverWithComments() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.ADD_WAIVER_PATH + "?comments=some%20comments", "violationId");
    assertRedirect(response, "assets/index.html#/addWaiver/violationId?comments=some%20comments");
  }

  @Test
  public void testLinkToAddWaiverWithReasonId() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.ADD_WAIVER_PATH +
        "?reasonId=9b704ef5bc064fc29d7fe08a251ee9a6", "violationId");
    assertRedirect(response, "assets/index.html#/addWaiver/violationId?reasonId=9b704ef5bc064fc29d7fe08a251ee9a6");
  }

  @Test
  public void testLinkToAddWaiverWithCommentsAndReasonId() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.ADD_WAIVER_PATH +
        "?comments=some%20comments&reasonId=9b704ef5bc064fc29d7fe08a251ee9a6", "violationId");
    assertRedirect(response, "assets/index.html#/addWaiver/violationId?comments=some%20comments" +
        "&reasonId=9b704ef5bc064fc29d7fe08a251ee9a6");
  }

  @Test
  public void testLinkToSpdx() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.LATEST_VERSION_SPDX_REPORT_PATH, "appId", "scanId");
    assertRedirect(response, "api/v2/spdx/appId/reports/scanId");
  }

  @Test
  @ManualIqServerInit
  public void testLinkToReport_WithSourceQuery_Anonymous() throws Exception {
    testLinkToReport_WithSourceQuery(true /* anonymous */);
  }

  @Test
  @ManualIqServerInit
  public void testLinkToReport_WithSourceQuery_Anonymous_IntegratedEnterpriseReportingEnabled() throws Exception {
    testLinkToReport_WithSourceQuery(true /* anonymous */);
  }

  @Test
  @ManualIqServerInit
  public void testLinkToReport_WithSourceQuery_UserIsLoggedIn() throws Exception {
    testLinkToReport_WithSourceQuery(false /* anonymous */);
  }

  private void testLinkToReport_WithSourceQuery(boolean anonymous) throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = Collections.synchronizedMap(new LinkedHashMap<>());
    startIqTestServer(config -> getHdsServer()
        .respondWith((HttpResponseProcessor) (request, response) -> responses.put(
            new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus()))
        .andStatus(204)
        .atUri(TelemetrySender.RESOURCE_PATH));

    Application application = tempEntity.newApplicationWithParent();
    String appPublicId = application.getPublicId();

    assertThat(UserInterfaceLinksHelper.getReportUrl(appPublicId, "scan id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/" + appPublicId + "/report/scan%20id");
    HttpRequest request = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH, UserInterfaceLinksHelper.REPORT_PATH)
        .parameter(appPublicId, "scan id")
        .query("source=Foo");

    if (anonymous) {
      request.anon();
    }
    else {
      // Create an HTTP session and use it on the test request (to have a user logged in when UserInterfaceLinksResource
      // is called).
      HttpCookie sessionCookie = restRequest().path(UserSessionResource.RESOURCE_PATH).post().getSessionCookie();
      request.cookie(sessionCookie);
    }

    HttpResponse redirect = request.get();
    assertRedirect(redirect, "assets/index.html?source=Foo#/applicationReport/" + appPublicId + "/scan%20id/policy");

    Map<TelemetryPurpose, List<TelemetryItem>> telemetryItemsByPurpose = getTelemetryItemsByPurpose(responses);

    List<TelemetryItem> telemetryItems = telemetryItemsByPurpose.get(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    assertThat(telemetryItems.size()).isEqualTo(1);
    TelemetryData telemetryData = telemetryItems.get(0).getTelemetryData().get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    assertThat(telemetryData.getAttributes().get("source")).isEqualTo("Foo".toLowerCase(Locale.ENGLISH));
    assertThat(telemetryData.getAttributes().get("application_id"))
        .isEqualTo(HdsClientAnalytics.obfuscate(application.getId()));
    assertThat(telemetryData.getAttributes().get("real_application_id")).isEqualTo(application.getId());

    assertThat(telemetryData.getAttributes().get("scan_id")).isEqualTo(HdsClientAnalytics.obfuscate("scan id"));
    assertThat(telemetryData.getAttributes().get("is_logged_in")).isEqualTo(!anonymous);
  }

  @Test
  @ManualIqServerInit
  public void testLinkToReport_WithoutSourceQuery() throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = Collections.synchronizedMap(new LinkedHashMap<>());
    startIqTestServer(config -> getHdsServer()
        .respondWith((HttpResponseProcessor) (request, response) -> responses.put(
            new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus()))
        .andStatus(204)
        .atUri(TelemetrySender.RESOURCE_PATH));

    assertThat(UserInterfaceLinksHelper.getReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/app%20id/report/scan%20id");
    HttpResponse response = get(UserInterfaceLinksHelper.REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/applicationReport/app%20id/scan%20id/policy");

    // Wait briefly for any telemetry to be sent, then check that no SOURCE_CONTROL_REPORT_LINK telemetry was sent
    Thread.sleep(500);

    // Check that no SOURCE_CONTROL_REPORT_LINK telemetry was sent when there's no source query parameter
    // Note: other background telemetry may still be sent, so we filter by purpose
    Map<TelemetryPurpose, List<TelemetryItem>> telemetryItemsByPurpose = getTelemetryItemsByPurpose(responses);
    assertThat(telemetryItemsByPurpose.get(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK)).isNull();
  }

  @Test
  public void testLinkToEmbeddableReport() throws Exception {
    assertThat(UserInterfaceLinksHelper.getEmbeddableReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/app%20id/report/scan%20id/embeddable");
    HttpResponse response = get(UserInterfaceLinksHelper.EMBEDDABLE_REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/applicationReport/app%20id/scan%20id/policy?embeddable");
  }

  @Test
  public void testLinkToPdf() throws Exception {
    assertThat(UserInterfaceLinksHelper.getPdfUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/application/app%20id/report/scan%20id/pdf");
    HttpResponse response = get(UserInterfaceLinksHelper.PDF_PATH, "app id", "scan id");
    assertRedirect(response, "rest/report/app%20id/scan%20id/printReport");
  }

  @Test
  public void testLinkToLatestAppReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app-id");
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan id");
    HttpResponse response = get(UserInterfaceLinksHelper.LATEST_REPORT_PATH, "app-id", Stage.ID_BUILD);
    assertRedirect(response, "assets/index.html#/applicationReport/app-id/scan%20id/policy");
  }

  @Test
  public void testLinkToRepositoryReport() throws Exception {
    String url = UserInterfaceLinksHelper.getRepositoryReportUrl("repo id");
    assertThat(url).isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/repository/repo%20id/result");
    HttpResponse response = get(UserInterfaceLinksHelper.REPO_RESULT_PATH, "repo id");
    assertRedirect(response, "assets/index.html#/firewall/repository/repo%20id/result");
  }

  @Test
  public void testLinkToRepositoryReport_dockerProxyRepository() throws Exception {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");

    String url = UserInterfaceLinksHelper.getRepositoryReportUrl(repository.getId());
    assertThat(url).isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/repository/" + repository.getId() + "/result");
    HttpResponse response = get(UserInterfaceLinksHelper.REPO_RESULT_PATH, repository.getId());
    assertRedirect(response,
        "assets/index.html#/firewall/container/repository/" + repository.getId() + "/results");
  }

  @Test
  public void testLinkToVulnerabililtyDetails() throws Exception {
    String url = UserInterfaceLinksHelper.getVulnerabilityDetailsUrl("CVE-8765-4321");
    assertThat(url).isEqualTo(UserInterfaceLinksHelper.RESOURCE_PATH + "/vln/CVE-8765-4321");
    HttpResponse response = get(UserInterfaceLinksHelper.VULNERABILITY_DETAILS_PATH, "CVE-8765-4321");
    assertRedirect(response, "assets/index.html#/vulnerabilities/CVE-8765-4321");
  }

  @Test
  public void testLinkToQuarantinedComponentReport() throws Exception {
    String url = UserInterfaceLinksHelper.getQuarantinedComponentReportPath("token");
    assertThat(url).isEqualTo(
        UserInterfaceLinksHelper.RESOURCE_PATH + "/firewall/repositories/quarantinedComponent/token");
    HttpResponse response = get(UserInterfaceLinksHelper.QUARANTINED_COMPONENT_REPORT_PATH, "token");
    assertRedirect(response, "assets/index.html#/firewall/repositories/quarantinedComponent/token");
  }

  @Test
  public void testLinkToSbomManagerBomPage() throws Exception {
    String url = UserInterfaceLinksHelper.getSBOMBillOfMaterialPath("app-id", "app-version");
    assertThat(url).isEqualTo(
        UserInterfaceLinksHelper.RESOURCE_PATH + "/sbomManager/management/view/application/app-id/bom/app-version");
    HttpResponse response = get(UserInterfaceLinksHelper.SBOM_BOM_VIEW_PATH, "app-id", "app-version");
    assertRedirect(response,
        "assets/index.html#/sbomManager/management/view/application/app-id/bom/app-version/overview");
  }

  @Test
  public void testLinkToIntegrationsPrioritiesReport() throws Exception {
    final Application application = tempEntity.newApplicationWithParent();
    final String appPublicId = application.getPublicId();
    final HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH)
        .path(INTEGRATIONS_PRIORITIES_PATH)
        .parameter(appPublicId, "scan-id", "cli")
        .anon()
        .get();
    assertRedirect(response,
        "assets/index.html#/developer/integrations/" + appPublicId + "/scan-id/cli");
  }

  @Test
  public void testLinkToPrioritiesReport() throws Exception {
    final Application application = tempEntity.newApplicationWithParent();
    final String appPublicId = application.getPublicId();
    final HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH)
        .path(PRIORITIES_PATH)
        .parameter(appPublicId, "scan-id")
        .anon()
        .get();
    assertRedirect(response,
        "assets/index.html#/developer/priorities/" + appPublicId + "/scan-id");
  }

  @Test
  public void testLegacyLinkToPrioritiesReport() throws Exception {
    final Application application = tempEntity.newApplicationWithParent();
    final String appPublicId = application.getPublicId();
    final HttpResponse response = restRequest()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH)
        .path(PRIORITIES_PATH_LEGACY)
        .parameter(appPublicId, "scan-id")
        .anon()
        .get();
    assertRedirect(response,
        "assets/index.html#/developer/priorities/" + appPublicId + "/scan-id");
  }

  @Test
  public void linkToEnterpriseReportingDashboard() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.ENTERPRISE_REPORTING_DASHBOARD_PATH, "success-metrics");
    assertRedirect(response, "assets/index.html#/enterpriseReportingDashboard/success-metrics");
  }

  @Test
  public void testLinkToReviewWaiverRequest() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.REVIEW_WAIVER_REQUEST_PATH, "application", "owner-id", "waiver-request-id");
    assertRedirect(response, "assets/index.html#/requestWaiverReview/application/owner-id/waiver-request-id");
  }

  @Test
  public void testLinkToMalwareDefenseContainerEvaluationReport() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.FIREWALL_CONTAINER_IMAGE_EVALUATION_REPORT_PATH,
        "container-public-id", "scan-id");
    assertRedirect(response, "assets/index.html#/firewall/containerReport/container-public-id/scan-id/policy");
  }

  @Test
  public void testLinkToMalwareDefenseContainerReportPolicy() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.MALWARE_DEFENSE_CONTAINER_IMAGE_EVALUATION_REPORT_PATH,
        "container-public-id", "scan-id");
    assertRedirect(response, "assets/index.html#/firewall/containerReport/container-public-id/scan-id/policy");
  }

  @Test
  public void testLinkToMalwareDefenseRepositoryResults_redirectsToFirewall() throws Exception {
    HttpResponse response = get(UserInterfaceLinksHelper.MALWARE_DEFENSE_REPOSITORY_RESULTS_PATH, "repo id");
    assertRedirect(response, "assets/index.html#/firewall/repository/repo%20id/result");
  }

  // The HTML-shaped HRC handlers intentionally 404 until the frontend Lifecycle Report SPA states land.

  @Test
  public void testLinkToHostedRepositoryComponentPdf_redirectsToHrcRestEndpoint() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.HRC_PDF_PATH, "hrc-id", "scan-id");
    assertRedirect(response, "rest/report/hostedRepositoryComponent/hrc-id/scan-id/printReport");
  }

  @Test
  public void testLinkToHostedRepositoryComponentLatestReport_noScan_returns404() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.HRC_LATEST_REPORT_PATH, "hrc-no-scan", "build");
    assertResponseStatus(404, response);
  }

  @Test
  public void testLinkToHostedRepositoryComponentReport_returns404UntilFrontendStatesLand() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.HRC_REPORT_PATH, "hrc-id", "scan-id");
    assertResponseStatus(404, response);
  }

  @Test
  public void testLinkToHostedRepositoryComponentEmbeddableReport_returns404UntilFrontendStatesLand() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.HRC_EMBEDDABLE_REPORT_PATH, "hrc-id", "scan-id");
    assertResponseStatus(404, response);
  }

  @Test
  public void testLinkToHostedRepositoryComponentPrioritiesReport_returns404UntilFrontendStatesLand() throws Exception {
    HttpResponse response =
        get(UserInterfaceLinksHelper.HRC_PRIORITIES_PATH, "hrc-id", "scan-id");
    assertResponseStatus(404, response);
  }

  private Map<TelemetryPurpose, List<TelemetryItem>> getTelemetryItemsByPurpose(
      final Map<ByteArrayDataSource, Integer> responses) throws MessagingException, IOException
  {
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(responses).isNotEmpty());
    return getTelemetryItems(responses).stream()
        .collect(groupingBy(telemetryItem -> telemetryItem.getTelemetryPurposes().get(0)));
  }
}
