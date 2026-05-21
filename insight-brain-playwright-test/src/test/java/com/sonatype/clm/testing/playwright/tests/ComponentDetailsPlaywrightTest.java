/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/*
 * Playwright test for the Component Details page.
 */
public class ComponentDetailsPlaywrightTest
    extends AbstractIqUiTest
{

  private record HdsStub(String uri, String resourcePath, String rawResponse)
  {
  }

  private static final String REFERENCE_POLICIES_RESOURCE = "/reference-policies-v3.json";

  private static final String CANNED_REPORT_CLASSPATH_DIR = "/canned-reports/large-report";

  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String PARENT_ORG_NAME = "Parent Organization";

  private static final String ORG_NAME = "Test Organization";

  private static final String APPLICATION_NAME = "ApplicationReportTest";

  private static final String APPLICATION_INTERNAL_NAME = "ApplicationReportTest";

  private static final String EXPECTED_REPORT_TITLE = "ApplicationReportTest Build Report";

  private static final String SECURITY_COMPONENT_HASH = "1e48256a2341047e7d72";

  private static final String LEGAL_COMPONENT_HASH = "fa78f54738ccf77379d1";

  private static final int EXPECTED_TAB_COUNT = 6;

  private static final int EXPECTED_VULNERABILITY_ROW_COUNT = 3;

  private static final List<HdsStub> COMPONENT_HDS_STUBS = List.of(
      new HdsStub("rest/ci/componentDetails",
          "/componentDetails/javancssComponentDetails-29.50.json", null),
      new HdsStub("rest/ci/componentDetails/list",
          "/componentDetails/javancssComponentDetailsList.json", null));

  private static final List<HdsStub> LEGAL_HDS_STUBS = List.of(
      new HdsStub("/rest/license/metadata", "/legal/legalLicenseMetadataHdsResponse.json", null),
      new HdsStub("/rest/legal/comment", "/legal/legalCommentHdsResponse.json", null),
      new HdsStub("/rest/legal/file",
          "/legal/ApplicationAttributionReportTest-legalFileHdsResponse.json", null),
      new HdsStub("/rest/legal/source-link", null, "[]"));

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
    for (HdsStub stub : COMPONENT_HDS_STUBS) {
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
    for (HdsStub stub : LEGAL_HDS_STUBS) {
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
    URL referencePolicyUrl = getClass().getResource(REFERENCE_POLICIES_RESOURCE);
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization parentOrg = tempEntity.newOrganization(PARENT_ORG_NAME);
    org = tempEntity.newOrganization(ORG_NAME, parentOrg);
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication(APPLICATION_NAME, APPLICATION_INTERNAL_NAME, org.getId());
    URL zippedReport = ReportHelper.zipReport(CANNED_REPORT_CLASSPATH_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator reportEvaluator =
        new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
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

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(EXPECTED_REPORT_TITLE);
    reportPage.openFirstComponentFromReport();
    new ComponentDetailsPageAssertions(detailsPage).shouldShowHeaderTitle();
    new ComponentDetailsPageAssertions(detailsPage).shouldHaveComponentTabCount(EXPECTED_TAB_COUNT);

    detailsPage.navigateBackToApplicationReport();
    playwrightWaitUntilUrlContains("/applicationReport/");
  }

  @Test
  @Category(SanityTest.class)
  public void testComponentDetailsTabNavigation() {
    ApplicationReportPage reportPage = new ApplicationReportPage();
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    ComponentDetailsPageAssertions detailsAssertions = new ComponentDetailsPageAssertions(detailsPage);

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    new ApplicationReportPageAssertions(reportPage).shouldShowReportHeaderContaining(EXPECTED_REPORT_TITLE);
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
        ComponentDetailsPage.urlToSecurity(app, SCAN_ID, SECURITY_COMPONENT_HASH));
    detailsAssertions.shouldShowSecurityTabPanel();
    detailsAssertions.shouldShowVulnerabilityTableWithRowCount(EXPECTED_VULNERABILITY_ROW_COUNT);
  }

  @Test
  @Category(SanityTest.class)
  public void testLegalTab_licenseDetectionTile() {
    ComponentDetailsPage detailsPage = new ComponentDetailsPage();

    playwrightRefreshOrOpen(
        ComponentDetailsPage.urlToLegal(app, SCAN_ID, LEGAL_COMPONENT_HASH));
    new ComponentDetailsPageAssertions(detailsPage).shouldShowLegalTabWithLicenseDetections();
  }

}
