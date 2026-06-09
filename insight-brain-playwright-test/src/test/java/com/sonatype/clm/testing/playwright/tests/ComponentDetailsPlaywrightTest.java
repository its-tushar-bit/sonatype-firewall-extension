/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Route;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/*
 * Playwright test for the Component Details page.
 */
public class ComponentDetailsPlaywrightTest
    extends AbstractIqUiTest
{

  private record HdsStub(String uri, String resourcePath, String rawResponse)
  {
  }

  private record ComponentDetailsData(
      String referencePoliciesResource,
      String cannedReportClasspathDir,
      String scanId,
      String parentOrgName,
      String orgName,
      String applicationName,
      String applicationInternalName,
      String expectedReportTitle,
      String securityComponentHash,
      String legalComponentHash,
      String claimComponentHash,
      String vulnerabilityDetailsFixtureResource,
      int expectedTabCount,
      int expectedVulnerabilityRowCount,
      List<HdsStub> componentHdsStubs,
      List<HdsStub> legalHdsStubs)
  {
  }

  private static final ComponentDetailsData DATA =
      TestDataManager.load("component-details", ComponentDetailsData.class);

  private Organization org;

  private Application app;

  @Before
  public void seedReportAndOpenDashboardAsAdmin() throws IOException {
    stubComponentDetailsEndpoints();
    stubLegalHdsEndpoints();
    createOrgImportPoliciesAndEvaluateCannedReport();
    openDashboardAndLoginAsAdmin();
  }

  private void stubComponentDetailsEndpoints() {
    for (HdsStub stub : DATA.componentHdsStubs()) {
      testCLMServer.getHdsServer()
          .respondWith(getClass().getResource(stub.resourcePath()))
          .atUri(stub.uri());
    }
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  private void stubLegalHdsEndpoints() {
    for (HdsStub stub : DATA.legalHdsStubs()) {
      String uri = stub.uri();
      if (stub.resourcePath() != null) {
        testCLMServer.getHdsServer()
            .respondWith(getClass().getResource(stub.resourcePath()))
            .atUri(uri);
      }
      else {
        testCLMServer.getHdsServer()
            .respondWith(stub.rawResponse())
            .atUri(uri);
      }
    }
  }

  private void createOrgImportPoliciesAndEvaluateCannedReport() throws IOException {
    URL referencePolicyUrl = getClass().getResource(DATA.referencePoliciesResource());
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization parentOrg = tempEntity.newOrganization(DATA.parentOrgName());
    org = tempEntity.newOrganization(DATA.orgName(), parentOrg);
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication(
        DATA.applicationName(), DATA.applicationInternalName(), org.getId());
    URL zippedReport = ReportHelper.zipReport(DATA.cannedReportClasspathDir(), tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator reportEvaluator =
        new TestReportEvaluator(app, DATA.scanId(), zippedReport, baseUrlFromTest, work);
    reportEvaluator.evaluatePolicy();
  }

  private void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentDetailsEnabled() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(DATA.expectedReportTitle());
    reportPage.openFirstComponentFromReport();
    new ComponentDetailsPageAssertions(detailsPage).shouldShowHeaderTitle();
    new ComponentDetailsPageAssertions(detailsPage).shouldHaveComponentTabCount(DATA.expectedTabCount());

    detailsPage.navigateBackToApplicationReport();
    playwrightWaitUntilUrlContains("/applicationReport/");
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentDetailsTabNavigation() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(DATA.expectedReportTitle());
    reportPage.openFirstComponentFromReport();
    detailsAssertions.shouldShowHeaderTitle();

    detailsAssertions.shouldShowOverviewTabContent();

    detailsPage.clickComponentDetailsTab("Policy Violations");
    playwrightWaitUntilUrlContains("/violations");
    detailsAssertions.shouldShowViolationsTabContent();

    detailsPage.clickComponentDetailsTab("Security");
    playwrightWaitUntilUrlContains("/security");
    detailsAssertions.shouldShowSecurityTabContent();

    detailsPage.clickComponentDetailsTab("Legal");
    playwrightWaitUntilUrlContains("/legal");
    detailsAssertions.shouldShowLegalTabContent();

    detailsPage.clickComponentDetailsTab("Labels");
    playwrightWaitUntilUrlContains("/labels");
    detailsAssertions.shouldShowLabelsTabContent();

    detailsPage.clickComponentDetailsTab("Audit Log");
    playwrightWaitUntilUrlContains("/audit");
    detailsAssertions.shouldShowAuditLogTabContent();

    detailsPage.clickComponentDetailsTab("Overview");
    playwrightWaitUntilUrlContains("/overview");
    detailsAssertions.shouldShowOverviewTabContent();
  }

  @Test
  @Category(SanityTest.class)
  public void testSecurityTab_vulnerabilityTableEntries() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToSecurity(app, DATA.scanId(), DATA.securityComponentHash()));
    detailsAssertions.shouldShowSecurityTabPanel();
    detailsAssertions.shouldShowVulnerabilityTableWithRowCount(DATA.expectedVulnerabilityRowCount());
  }

  @Test
  @Category(SanityTest.class)
  public void testLegalTab_licenseDetectionTile() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToLegal(app, DATA.scanId(), DATA.legalComponentHash()));
    new ComponentDetailsPageAssertions(detailsPage).shouldShowLegalTabWithLicenseDetections();
  }

  @Test
  @Category(RegressionTest.class)
  public void testPolicyViolationPopover_embeddedMode() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToViolations(app, DATA.scanId(), DATA.securityComponentHash()));
    detailsAssertions.shouldShowHeaderTitle();
    detailsAssertions.shouldShowViolationsTabContent();
    detailsAssertions.shouldShowPolicyViolationsTableWithRows();
    detailsPage.policyViolationRows().first().click();
    detailsAssertions.shouldShowPopoverInEmbeddedMode();
  }

  @Test
  @Category(RegressionTest.class)
  public void testOverviewTab_componentInformationTile() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToOverview(app, DATA.scanId(), DATA.securityComponentHash()));
    playwrightWaitUntilUrlContains(ComponentDetailsPage.URL_FRAGMENT);
    detailsAssertions.shouldShowOverviewTabContent();
    detailsAssertions.shouldShowComponentInformationTileWithHeader();
  }

  @Test
  @Category(RegressionTest.class)
  public void testViolationsTab_policyViolationsTile() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToViolations(app, DATA.scanId(), DATA.securityComponentHash()));
    detailsAssertions.shouldShowViolationsTabContent();
    detailsAssertions.shouldShowPolicyViolationsTableWithRows();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAuditLogTab_tableHeaders() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToAudit(app, DATA.scanId(), DATA.securityComponentHash()));
    detailsAssertions.shouldShowAuditLogTabContent();
    detailsAssertions.shouldShowAuditLogTableHeaders();
  }

  @Test
  @Category(RegressionTest.class)
  public void testPagination_nextAndPreviousComponent() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(DATA.expectedReportTitle());
    reportPage.openFirstComponentFromReport();
    detailsAssertions.shouldShowHeaderTitle();
    detailsAssertions.shouldShowPaginationFooter();

    detailsPage.paginationNextLink().click();
    detailsAssertions.shouldShowHeaderTitle();

    detailsPage.paginationPrevLink().click();
    detailsAssertions.shouldShowHeaderTitle();
  }

  @Test
  @Category(RegressionTest.class)
  public void testBackNavigation_returnsToApplicationReport() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(DATA.expectedReportTitle());
    reportPage.openFirstComponentFromReport();
    detailsAssertions.shouldShowHeaderTitle();

    detailsPage.navigateBackToApplicationReport();
    playwrightWaitUntilUrlContains("/applicationReport/");
  }

  @Test
  @Category(RegressionTest.class)
  public void testPolicyViolationPopover_addWaiverButtonNavigatesToAddWaiver() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToViolations(app, DATA.scanId(), DATA.securityComponentHash()));
    detailsAssertions.shouldShowViolationsTabContent();
    detailsAssertions.shouldShowPolicyViolationsTableWithRows();
    detailsPage.policyViolationRows().first().click();
    detailsAssertions.shouldShowPopoverInEmbeddedMode();

    assertThat(detailsPage.popoverAddWaiverButton()).isVisible();
    detailsPage.popoverAddWaiverButton().click();
    playwrightWaitUntilUrlContains("/addWaiver/");
  }

  @Test
  @Category(RegressionTest.class)
  public void testPolicyViolationPopover_requestWaiverMenuItemNavigatesToRequestWaiver() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, DATA.scanId()));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(DATA.expectedReportTitle());
    reportPage.openFirstComponentFromReport();
    detailsAssertions.shouldShowHeaderTitle();

    detailsPage.clickComponentDetailsTab("Policy Violations");
    playwrightWaitUntilUrlContains("/violations");
    detailsAssertions.shouldShowViolationsTabContent();
    detailsAssertions.shouldShowPolicyViolationsTableWithRows();

    detailsPage.policyViolationRows().first().click();
    detailsAssertions.shouldShowPopoverInEmbeddedMode();

    assertThat(detailsPage.popoverRequestWaiverDropdownToggle()).isVisible();
    detailsPage.popoverRequestWaiverDropdownToggle().click();
    assertThat(detailsPage.popoverRequestWaiverMenuItem()).isVisible();
    detailsPage.popoverRequestWaiverMenuItem().click();
    playwrightWaitUntilUrlContains("/requestWaiver/");
  }

  @Test
  @Category(RegressionTest.class)
  public void testSecurityTab_vulnerabilityOverrideFormInPopover() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    page.route(Pattern.compile(".*/api/v2/vulnerabilities/.*"), route -> {
      try (InputStream is = getClass().getResourceAsStream(DATA.vulnerabilityDetailsFixtureResource())) {
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(body));
      }
      catch (IOException e) {
        route.abort();
      }
    });

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToSecurity(app, DATA.scanId(), DATA.securityComponentHash()));
    playwrightWaitUntilUrlContains(ComponentDetailsPage.URL_FRAGMENT);
    detailsAssertions.shouldShowSecurityTabPanel();
    detailsAssertions.shouldShowVulnerabilityTableWithRowCount(DATA.expectedVulnerabilityRowCount());

    detailsPage.iqVulnerabilityTableBodyRows().first().click();
    assertThat(detailsPage.vulnerabilityDetailsPopover()).isVisible();
    assertThat(detailsPage.securityVulnerabilityOverrideForm()).isVisible();
    assertThat(detailsPage.vulnerabilityStatusDropdown()).isVisible();
    assertThat(detailsPage.vulnerabilityOverrideSaveButton()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLabelsTab_applyLabelModalOpensForOrgScopedLabel() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToLabels(app, DATA.scanId(), DATA.securityComponentHash()));
    detailsAssertions.shouldShowLabelsTabContent();
    assertThat(detailsPage.availableLabelTags().first()).isVisible();
    detailsPage.availableLabelTags().first().click();
    assertThat(detailsPage.applyLabelModal()).isVisible();
    assertThat(detailsPage.applyLabelModalHeading()).isVisible();
    assertThat(detailsPage.applyLabelScopeDropdown()).isVisible();
    detailsPage.applyLabelModalCancelButton().click();
    assertThat(detailsPage.applyLabelModal()).not().isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLabelsTab_removeLabelModalOpensForAppliedLabel() {
    String labelText = "AutoLabel-" + TemporaryEntity.uuid();
    Label seededLabel = tempEntity.newLabel(app.getId(), labelText);
    tempEntity.newComponentLabel(app.getId(), seededLabel.getId(), DATA.securityComponentHash());

    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToLabels(app, DATA.scanId(), DATA.securityComponentHash()));
    playwrightWaitUntilUrlContains(ComponentDetailsPage.URL_FRAGMENT);
    detailsAssertions.shouldShowLabelsTabContent();

    assertThat(detailsPage.appliedLabelTags().first()).isVisible();
    detailsPage.appliedLabelTags().first().click();
    assertThat(detailsPage.removeLabelModal()).isVisible();
    detailsPage.removeLabelModalCancelButton().click();
    assertThat(detailsPage.removeLabelModal()).not().isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testClaimTab_formStateValidationAndCancel() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToOverview(app, DATA.scanId(), DATA.claimComponentHash()));
    playwrightWaitUntilUrlContains(ComponentDetailsPage.URL_FRAGMENT);
    detailsAssertions.shouldShowOverviewTabContent();

    detailsPage.clickComponentDetailsTab("Claim");
    playwrightWaitUntilUrlContains("/claim");

    assertThat(detailsPage.claimForm()).isVisible();
    assertThat(detailsPage.claimGroupIdField()).isVisible();
    assertThat(detailsPage.claimArtifactIdField()).isVisible();
    assertThat(detailsPage.claimVersionField()).isVisible();
    assertThat(detailsPage.claimExtensionField()).isVisible();

    detailsPage.claimGroupIdField().fill("com.example");
    assertThat(detailsPage.claimCancelButton()).isEnabled();
    detailsPage.claimCancelButton().click();
    assertThat(detailsPage.claimGroupIdField()).hasValue("");
  }

  @Test
  @Category(RegressionTest.class)
  public void testClaimTab_revokeClaimModal() {
    ComponentIdentifier claimCoords = ComponentIdentifier.createMavenCoordinates(
        "com.example.claimed", "test-artifact", "1.0.0");
    tempEntity.newClaimedComponent(DATA.claimComponentHash(), claimCoords);

    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToOverview(app, DATA.scanId(), DATA.claimComponentHash()));
    playwrightWaitUntilUrlContains(ComponentDetailsPage.URL_FRAGMENT);
    detailsAssertions.shouldShowOverviewTabContent();

    detailsPage.clickComponentDetailsTab("Claim");
    playwrightWaitUntilUrlContains("/claim");

    assertThat(detailsPage.claimRevokeButton()).isVisible();
    detailsPage.claimRevokeButton().click();
    assertThat(detailsPage.revokeClaimModal()).isVisible();
    assertThat(detailsPage.revokeClaimModalConfirmButton()).isVisible();
  }

}
