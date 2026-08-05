/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.TimeoutError;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPage;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.BasePage;
import com.sonatype.clm.testing.playwright.pages.BaseUrlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.CopyrightOverrideFormPage;
import com.sonatype.clm.testing.playwright.pages.CrowdConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.InnerSourceRepositoryEditorPage;
import com.sonatype.clm.testing.playwright.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.playwright.pages.LegalDashboardPage;
import com.sonatype.clm.testing.playwright.pages.LegalDashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.MailConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.MailConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.NexusOneClassicEmbedPage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePageAssertions;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPage;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.ProxyConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ProxyConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.RolesPage;
import com.sonatype.clm.testing.playwright.pages.RolesPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsPage;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePage;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.pages.UserManagementPage;
import com.sonatype.clm.testing.playwright.pages.UserTokenConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.UserTokenConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.WebhookEditorPage;
import com.sonatype.clm.testing.playwright.pages.WebhookListPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.HdsStubs;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsService;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * AT-EMBED regression coverage for Classic Success Metrics, API, Legal Dashboard, Orgs and
 * Policies, and Enterprise/Operational Reporting pages mounted natively inside the Nexus One shell
 * without duplicate Classic chrome.
 */
public class NexusOneClassicEmbedPlaywrightTest
    extends AbstractIqUiTest
{
  private record EnterpriseReportingHdsStubs(JsonNode currentVersion, JsonNode dashboards)
  {
  }

  private static final String SUCCESS_METRICS_DESCRIPTION_SUBSTRING =
      "Success Metrics is an experimental feature providing high-level statistics on the past performance of Sonatype Lifecycle.";

  // Uses a reserved .invalid TLD so it cannot collide with any real base URL a
  // shared fixture might have left configured — dirtiness compares this
  // literal against baseUrlConfiguration.serverData.baseUrl.
  private static final String DIRTY_GUARD_TEST_BASE_URL = "http://dirty-guard-test.invalid";

  private static final EnterpriseReportingHdsStubs ENTERPRISE_REPORTING_HDS =
      TestDataManager.load("enterprise-reporting-hds-stubs", EnterpriseReportingHdsStubs.class);

  private String originalSuccessMetricsEnabled;

  @Before
  public void enablePreviewUiAndLogin() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    originalSuccessMetricsEnabled =
        lookup(SystemConfigurationPropertyDAO.class).get(SuccessMetricsService.PROPERTY_ENABLED);
    lookup(SystemConfigurationPropertyDAO.class).set(SuccessMetricsService.PROPERTY_ENABLED, "true");
    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();
  }

  @After
  public void resetPreviewUiAndSuccessMetricsConfig() {
    // Dismiss any dirty-guard modal a failed test may have left open, so the
    // next test doesn't hit a blocked transition on refresh.
    new UnsavedChangesModalComponent().continueIfOpen();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    if (originalSuccessMetricsEnabled != null) {
      lookup(SystemConfigurationPropertyDAO.class)
          .set(SuccessMetricsService.PROPERTY_ENABLED, originalSuccessMetricsEnabled);
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedApiPage_rendersSwaggerInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/api"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    ApiDocumentationPageAssertions apiAssertions = new ApiDocumentationPageAssertions(new ApiDocumentationPage());

    assertThat(page).hasURL(Pattern.compile(".*/nexus-one/index\\.html#/api(?:\\?.*)?$"));
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    apiAssertions.shouldShowSwaggerLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLegacyComingSoonApiUrl_redirectsToCleanEmbedPath() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/api"));

    Pattern cleanEmbedUrl = Pattern.compile(".*/nexus-one/index\\.html#/api(?:\\?.*)?$");
    page.waitForURL(cleanEmbedUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    ApiDocumentationPageAssertions apiAssertions = new ApiDocumentationPageAssertions(new ApiDocumentationPage());

    assertThat(page).hasURL(cleanEmbedUrl);
    assertThat(embedPage.classicComponentMount()).isVisible();
    apiAssertions.shouldShowSwaggerLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSuccessMetrics_rendersClassicLandingInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/success-metrics"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SuccessMetricsPage successMetrics = new SuccessMetricsPage();
    SuccessMetricsPageAssertions successMetricsAssertions = new SuccessMetricsPageAssertions(successMetrics);

    assertThat(page).hasURL(Pattern.compile(".*/nexus-one/index\\.html#/success-metrics(?:\\?.*)?$"));
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Success Metrics")).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    successMetricsAssertions.shouldBeLoaded();
    successMetricsAssertions.shouldHaveHeading("Success Metrics");
    successMetricsAssertions.shouldHaveDescriptionContaining(SUCCESS_METRICS_DESCRIPTION_SUBSTRING);
  }

  @Test
  @Category(RegressionTest.class)
  public void testLegacyComingSoonSuccessMetricsUrl_redirectsToCleanEmbedPath() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));

    Pattern cleanEmbedUrl = Pattern.compile(".*/nexus-one/index\\.html#/success-metrics(?:\\?.*)?$");
    page.waitForURL(cleanEmbedUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SuccessMetricsPage successMetrics = new SuccessMetricsPage();
    SuccessMetricsPageAssertions successMetricsAssertions = new SuccessMetricsPageAssertions(successMetrics);

    assertThat(page).hasURL(cleanEmbedUrl);
    assertThat(embedPage.classicComponentMount()).isVisible();
    successMetricsAssertions.shouldBeLoaded();
    successMetricsAssertions.shouldHaveHeading("Success Metrics");
  }

  @Test
  @Category(RegressionTest.class)
  public void testLegacyComingSoonSuccessMetricsReportUrl_redirectsToCleanEmbedPath() {
    String reportName = "pw-sm-embed-report-" + TemporaryEntity.uuid();
    SuccessMetricsReport report = tempEntity.newSuccessMetricsReport("admin", reportName, "{}");

    playwrightRefreshOrOpen(
        NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics/" + report.getId()));

    // BasePage#escapeForJsRegex is used instead of Pattern.quote() because Playwright Java
    // serializes this pattern to a JS RegExp, which does not understand Java's \Q...\E quoting.
    Pattern cleanEmbedUrl = Pattern.compile(
        ".*/nexus-one/index\\.html#/success-metrics/" + BasePage.escapeForJsRegex(report.getId()) + "(?:\\?.*)?$");
    page.waitForURL(cleanEmbedUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SuccessMetricsPage successMetrics = new SuccessMetricsPage();
    SuccessMetricsPageAssertions successMetricsAssertions = new SuccessMetricsPageAssertions(successMetrics);

    assertThat(page).hasURL(cleanEmbedUrl);
    assertThat(embedPage.classicComponentMount()).isVisible();
    successMetricsAssertions.shouldShowIndividualReport(reportName);
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedLegal_rendersNativeLegalListInsideNexusOneShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/legal"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    // /legal is now the Nexus One native LEGAL_VIOLATION triage page (CLM-43207) — it mounts
    // PreviewLegalList in-shell instead of redirecting to the Classic ALP dashboard. The Classic
    // dashboard is still available at /legal/applicationsDashboard for tenants with ALP.
    Pattern legalUrl = Pattern.compile(".*/nexus-one/index\\.html#/legal(?:\\?.*)?$");
    page.waitForURL(legalUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));
    assertThat(page).hasURL(legalUrl);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).hasAttribute("aria-current", "page");
    // Native NOSC page — no Classic mount here.
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
    assertThat(page.getByTestId("preview-legal-page")).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedLegalApplicationsDashboard_rendersClassicAlpDashboardInShell() {
    setFeatures(LicensedFeature.values());

    // Direct-navigate to the Classic ALP dashboard, which remains embedded (via LegalDashboardMount)
    // for tenants with Advanced Legal Pack. The clean /legal path now goes to the native Nexus One
    // Legal list instead — that's covered by the sibling test above.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/legal/applicationsDashboard"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    LegalDashboardPage legalDashboard = new LegalDashboardPage();
    LegalDashboardPageAssertions legalAssertions = new LegalDashboardPageAssertions(legalDashboard);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
    legalAssertions.shouldBeVisible();

    // Tab click resolves in-shell (state lives in the Nexus One bundle's own router, not Classic's).
    legalDashboard.componentsTab().click();

    assertThat(page).hasURL(Pattern.compile(".*/legal/componentsDashboard.*"));
    legalAssertions.shouldShowComponentsTabActive();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    // PortalDrawer targets `.nx-page` which only Classic's own root App.jsx provided — regression
    // guard ensures the Nexus One shell also provides it so the drawer isn't silently null.
    legalDashboard.openFilterDrawer();
    assertThat(legalDashboard.filterDrawer()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLegacyComingSoonLegalUrl_redirectsToNativeLegalList() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/legal"));

    // Legacy /coming-soon/legal now redirects to the native /legal page (CLM-43207); previously it
    // redirected to Classic's /legal/applicationsDashboard, but that ALP dashboard moved off the
    // canonical Legal entry point.
    Pattern legalUrl = Pattern.compile(".*/nexus-one/index\\.html#/legal(?:\\?.*)?$");
    page.waitForURL(legalUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    assertThat(page).hasURL(legalUrl);
    assertThat(embedPage.leftNavLink("Legal")).hasAttribute("aria-current", "page");
    assertThat(page.getByTestId("preview-legal-page")).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedLegalApplicationDetails_filterOpensAsOverlayNotInline() throws Exception {
    setFeatures(LicensedFeature.values());

    Application app = tempEntity.newApplicationWithParent();
    String hash = TemporaryEntity.uuid().replace("-", "").substring(0, 20);
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("org.package", "component1", "1.0");
    tempEntity.newApplicationComponentLicense(
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentIdentifier).getId(),
        "Apache-2.0");
    HdsStubs.legalOverview(testCLMServer.getHdsServer());

    playwrightRefreshOrOpen(
        NexusOneClassicEmbedPage.embedUrl("/legal/application/" + app.getPublicId() + "/stage/" + BuildStageType.ID));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();

    assertThat(embedPage.classicComponentMount()).isVisible();
    legalPage.openFilterSidebar();
    assertThat(legalPage.reviewStatusFilterGroup()).isVisible();

    // Regression guard: LegalApplicationDetailsFilter renders via IqPopover (position: absolute),
    // a different Classic UI pattern than the Legal Dashboard's PortalDrawer-based filter above.
    // IqPopover's own stylesheet (_iqPopover.scss) is only pulled in by Classic's central
    // scss.scss, which the Nexus One bundle never loads — without IqPopover.jsx importing it
    // directly, `position: absolute` never applies and the filter renders inline (position:
    // static) as a normal child of the page instead of an overlay, matching Classic's real
    // computed width/position exactly once the stylesheet loads (verified against Classic itself:
    // both render this popover at ~300px via the same .nx-viewport-sized > .iq-popover CSS rule).
    String position = (String) legalPage.filterSidebar()
        .evaluate("el => getComputedStyle(el.closest('.iq-popover')).position");
    assertEquals("absolute", position);
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedComponentLegalOverview_copyrightModalBlocksLeftNav() throws Exception {
    setFeatures(LicensedFeature.values());

    Application app = tempEntity.newApplicationWithParent();
    String hash = TemporaryEntity.uuid().replace("-", "").substring(0, 20);
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("org.package", "component1", "1.0");
    tempEntity.newApplicationComponentLicense(
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentIdentifier).getId(),
        "Apache-2.0");
    HdsStubs.legalOverview(testCLMServer.getHdsServer());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl(
        ComponentLegalOverviewPage.hashRoute(app.getPublicId(), BuildStageType.ID, hash)));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    CopyrightOverrideFormPage copyrightPage = new CopyrightOverrideFormPage();

    assertThat(embedPage.classicComponentMount()).isVisible();
    copyrightPage.openCopyrightModal();
    assertThat(copyrightPage.modal()).isVisible();

    // No setForce(true): an unforced click times out if the modal backdrop covers the link,
    // which is the signal this regression test needs.
    Locator dashboardLink = embedPage.leftNavLink("Dashboard");
    boolean navigatedWhileBlocked;
    try {
      dashboardLink.click(new Locator.ClickOptions().setTimeout(1500));
      navigatedWhileBlocked = true;
    }
    catch (TimeoutError expected) {
      navigatedWhileBlocked = false;
    }
    assertFalse("LeftNav must not be clickable while the copyright modal is open", navigatedWhileBlocked);
    assertThat(copyrightPage.modal()).isVisible();

    copyrightPage.clickCancel();
    assertThat(copyrightPage.modal()).not().isVisible();
    dashboardLink.click();
    assertThat(page).hasURL(Pattern.compile(".*/dashboard.*"));
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedLegal_orgsAndAppsUnlicensed_hidesNavEntry() {
    // CLM-43207 moved the Legal rail entry off the Advanced Legal Pack gate — the native
    // Nexus One Legal V1 list is available without ALP, on the same Lifecycle gate as
    // Applications / Violations. The rail entry now shows for isLicensed && isOrgsAndAppsEnabled
    // tenants; stripping ORGS_AND_APPS hides it.
    setMissingFeature(LicensedFeature.ORGS_AND_APPS);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/home"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).not().isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedLegal_advancedLegalPackUnlicensed_stillShowsNavEntry() {
    // Regression guard for CLM-43207: the Legal V1 list intentionally does NOT require
    // Advanced Legal Pack. Stripping ADVANCED_LEGAL_PACK must leave the rail entry visible
    // (the Classic ALP dashboard at /legal/applicationsDashboard still gates itself separately).
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/home"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedReporting_enterpriseReportingLicensed_rendersEnterpriseInShell() {
    setFeatures(LicensedFeature.values());
    stubEnterpriseReportingHds();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/reports"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    EnterpriseReportingPageAssertions enterpriseAssertions =
        new EnterpriseReportingPageAssertions(new EnterpriseReportingPage());

    assertThat(page).hasURL(Pattern.compile(".*/nexus-one/index\\.html#/reports(?:\\?.*)?$"));
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Enterprise Reporting")).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    enterpriseAssertions.shouldBeLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedReporting_enterpriseReportingUnlicensed_rendersOperationalInShell() {
    setMissingFeature(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/reports"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OperationalReportingPageAssertions operationalAssertions =
        new OperationalReportingPageAssertions(new OperationalReportingPage());

    assertThat(page).hasURL(Pattern.compile(".*/nexus-one/index\\.html#/reports(?:\\?.*)?$"));
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Operational Reporting")).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    operationalAssertions.shouldBeLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLegacyComingSoonReportsUrl_redirectsToCleanEmbedPath() {
    setFeatures(LicensedFeature.values());
    stubEnterpriseReportingHds();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/reports"));

    Pattern cleanEmbedUrl = Pattern.compile(".*/nexus-one/index\\.html#/reports(?:\\?.*)?$");
    page.waitForURL(cleanEmbedUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    EnterpriseReportingPageAssertions enterpriseAssertions =
        new EnterpriseReportingPageAssertions(new EnterpriseReportingPage());

    assertThat(page).hasURL(cleanEmbedUrl);
    assertThat(embedPage.classicComponentMount()).isVisible();
    enterpriseAssertions.shouldBeLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedOrgsAndPolicies_rendersRootOrgSummaryInsideNexusOneShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/orgs-and-policies"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    OwnerSummaryPageAssertions ownerSummaryAssertions = new OwnerSummaryPageAssertions(ownerSummary);

    // The clean /orgs-and-policies entry redirects straight to the root org's summary rather than
    // mounting a component of its own - see nexus-one/routes.tsx's NATIVE_CLASSIC_EMBED_REDIRECTS.
    assertThat(page).hasURL(Pattern.compile(".*/management/view/organization/.*"));

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Orgs & Policies")).isVisible();
    assertThat(embedPage.leftNavLink("Orgs & Policies")).hasAttribute("aria-current", "page");
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    ownerSummaryAssertions.shouldBeVisible();
    ownerSummaryAssertions.shouldShowPoliciesTile();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLegacyComingSoonOrgsAndPoliciesUrl_redirectsToRootOrgSummary() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/orgs-and-policies"));

    Pattern rootOrgSummaryUrl = Pattern.compile(".*/management/view/organization/.*");
    page.waitForURL(rootOrgSummaryUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OwnerSummaryPageAssertions ownerSummaryAssertions =
        new OwnerSummaryPageAssertions(new OwnerSummaryPage());

    assertThat(page).hasURL(rootOrgSummaryUrl);
    assertThat(embedPage.classicComponentMount()).isVisible();
    ownerSummaryAssertions.shouldBeVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedOrgsAndPolicies_policyEditorNavigationStaysInShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/orgs-and-policies"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage policyEditor = new PolicyEditorPage();

    // Drilling from the org summary (management.view.organization) into the new-policy editor
    // (management.edit.organization.create-policy) crosses a state both bundles register: it must
    // resolve inside the Nexus One shell, never bounce out to the Classic bundle.
    ownerSummary.addPolicyButton().click();

    assertThat(policyEditor.container()).isVisible();
    // Still on the Nexus One bundle (/assets/nexus-one/index.html#...), on a management sub-route,
    // not the Classic bundle (/assets/index.html#...).
    assertThat(page).hasURL(Pattern.compile(".*/assets/nexus-one/index\\.html#/management/edit/organization/.*"));
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
    assertThat(embedPage.leftNavLink("Orgs & Policies")).hasAttribute("aria-current", "page");
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedOrgsAndPolicies_innerSourceEditButtonNavigatesInShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/orgs-and-policies"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    InnerSourceRepositoryEditorPage innerSourceEditor = new InnerSourceRepositoryEditorPage();

    // The InnerSource tile's Edit button stateGo's to repositoryBaseConfigurations.organization, a
    // sibling state tree that nexus-one/routes.tsx must register; without it the click does nothing
    // (CLM-42161).
    ownerSummary.innerSourceRepositoryTile()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit"))
        .click();

    assertThat(innerSourceEditor.container()).isVisible();
    assertThat(page).hasURL(Pattern.compile(".*/management/edit/organization/.*/repositoryBaseConfigurations"));
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
  }

  private void stubEnterpriseReportingHds() {
    testCLMServer.getHdsServer()
        .respondWith(ENTERPRISE_REPORTING_HDS.currentVersion().toString())
        .atUri("rest/enterpriseReporting/currentVersion");
    testCLMServer.getHdsServer()
        .respondWith(ENTERPRISE_REPORTING_HDS.dashboards().toString())
        .atUri("rest/enterpriseReporting/dashboards");
  }

  @Test
  @Category(RegressionTest.class)
  public void testNonEmbeddedComingSoonRoute_stillRendersStub() {
    playwrightRefreshOrOpen(NexusOnePage.url("/coming-soon/system-config"));

    NexusOnePageAssertions assertions = new NexusOnePageAssertions(new NexusOnePage());
    assertions.shouldBeVisible();
    assertions.shouldHaveHeadingText("Coming Soon");
  }

  /**
   * CLM-42186: Success Metrics admin configuration mounts natively at
   * {@code /successMetricsConfiguration} on the Nexus One bundle, rendering
   * the Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedSuccessMetricsConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/successMetricsConfiguration"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SuccessMetricsConfigurationPage configPage = new SuccessMetricsConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(configPage.container()).isVisible();
    assertThat(configPage.pageHeading()).isVisible();
    assertThat(configPage.tileHeading()).isVisible();
    assertThat(configPage.enabledToggleInput()).isChecked();
    assertThat(configPage.updateButton()).isVisible();
  }

  /**
   * CLM-42186 dirty-guard cancel path: toggling the switch dirties the form; a
   * hash navigation triggers the shell dirty-guard; Cancel keeps the user on
   * the config page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSuccessMetricsConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/successMetricsConfiguration"));

    SuccessMetricsConfigurationPage configPage = new SuccessMetricsConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    configPage.container().waitFor();
    configPage.enabledToggle().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.container()).isVisible();
    assertThat(configPage.enabledToggleInput()).not().isChecked();
  }

  /**
   * CLM-42186 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * the target route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSuccessMetricsConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/successMetricsConfiguration"));

    SuccessMetricsConfigurationPage configPage = new SuccessMetricsConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    configPage.container().waitFor();
    configPage.enabledToggle().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42465: Users admin page mounts natively at {@code /users} on the
   * Nexus One bundle, rendering the Classic user list as-is inside the
   * Nexus One shell. This is a list page with no dirty guard.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedUsers_rendersClassicListInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/users"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    UserManagementPage usersPage = new UserManagementPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(usersPage.container()).isVisible();
    assertThat(usersPage.configureUsersHeading()).isVisible();
  }

  /**
   * CLM-42465: a user without CONFIGURE_SYSTEM navigating to {@code /users}
   * is redirected to the Nexus One violations dashboard before the Users page
   * mounts. Covers the {@code redirectTo} guard on the {@code users} route.
   *
   * <p>
   * Log in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One (see the analogous SystemNotice auth test for
   * the same pattern and rationale).
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedUsers_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/users"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    UserManagementPage usersPage = new UserManagementPage();
    assertThat(usersPage.container()).isHidden();
  }

  /**
   * CLM-42465: createUser form mounts at {@code /users/_new_}. Filling any
   * field sets {@code userConfiguration.isDirty}; navigating away triggers
   * the shell dirty-guard modal. Cancel keeps the user on the form.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedCreateUser_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/users/_new_"));

    UserManagementPage userPage = new UserManagementPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    userPage.userForm().waitFor();
    userPage.firstNameInput().fill("dirty");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/dashboard"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(userPage.userForm()).isVisible();
  }

  /**
   * CLM-42465: Continue closes the dirty-guard modal and lets the navigation
   * proceed; the create-user form unmounts and the target page mounts.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedCreateUser_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/users/_new_"));

    UserManagementPage userPage = new UserManagementPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    userPage.userForm().waitFor();
    userPage.firstNameInput().fill("dirty");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/successMetricsConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(userPage.userForm()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42464: Administrators list page mounts natively at
   * {@code /administrators} on the Nexus One bundle, rendering
   * the Classic list as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedAdministrators_rendersClassicListInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    AdministratorsPage adminPage = new AdministratorsPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(adminPage.container()).isVisible();
    assertThat(adminPage.pageTitle()).isVisible();
    assertThat(adminPage.tileHeader()).isVisible();
    assertThat(adminPage.table()).isVisible();
  }

  /**
   * CLM-42464: Administrators edit page mounts natively at
   * {@code /administrators/{roleId}} on the Nexus One bundle, rendering
   * the Classic edit form inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedAdministratorsEdit_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators/" + Role.POLICY_ADMIN_ROLE_ID));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    AdministratorsEditPage editPage = new AdministratorsEditPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(editPage.root()).isVisible();
  }

  /**
   * Seeds a non-admin user this test owns so the search dropdown on the Administrators edit
   * form has a deterministic candidate to select, and returns the exact
   * {@code "<displayName> (<internalName>)"} label {@code formatGroupUsers.js} renders for it.
   */
  private String seedSearchableAdministratorCandidate() {
    String username = TemporaryEntity.uuid();
    tempEntity.newUser(username, "Jane", "Doe", username + "@doe.net");
    return "Jane Doe (" + username + ")";
  }

  /**
   * CLM-42464 dirty-guard cancel path: selecting a user from the search dropdown
   * adds them to addedUsers, setting isDirty=true; a hash navigation triggers
   * the shell dirty-guard; Cancel keeps the user on the edit page with the dirty
   * state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdministratorsEdit_dirtyGuardBlocksNavigationOnCancel() {
    String searchableUserItem = seedSearchableAdministratorCandidate();
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators/" + Role.POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage editPage = new AdministratorsEditPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    editPage.root().waitFor();
    editPage.searchAndAddByText("*", searchableUserItem);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(editPage.root()).isVisible();
  }

  /**
   * CLM-42464 dirty-guard continue path: selecting a user from the search
   * dropdown sets isDirty=true; Continue closes the modal and lets the
   * transition proceed; the edit page unmounts and the user lands on
   * the target route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdministratorsEdit_dirtyGuardAllowsNavigationOnContinue() {
    String searchableUserItem = seedSearchableAdministratorCandidate();
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators/" + Role.POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage editPage = new AdministratorsEditPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    editPage.root().waitFor();
    editPage.searchAndAddByText("*", searchableUserItem);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(editPage.root()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42464 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /administrators is redirected to the Nexus One violations dashboard before
   * the admin page ever mounts. Covers the redirectTo function on the route.
   *
   * <p>
   * Log in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One. Going straight to the Nexus One URL while
   * logged out hits {@code ensureNexusOneShellAccess}, which bounces
   * unauthenticated requests back to Classic before the router — so the
   * route's own {@code redirectTo} would never fire.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdministrators_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    AdministratorsPage adminPage = new AdministratorsPage();
    assertThat(adminPage.container()).isHidden();
  }

  /**
   * Base URL admin configuration mounts natively at {@code /baseUrl} on the
   * Nexus One bundle, rendering the Classic form as-is inside the Nexus One
   * shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedBaseUrlConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/baseUrl"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(configPage.saveButton()).isVisible();
    assertThat(configPage.cancelButton()).isVisible();
    assertThat(configPage.deleteButton()).isVisible();
    assertThat(configPage.baseUrlAttribute()).isVisible();
  }

  /**
   * Base URL dirty-guard cancel path: editing the URL field dirties the form;
   * a hash navigation triggers the shell dirty-guard; Cancel keeps the user on
   * the config page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedBaseUrlConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/baseUrl"));

    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    configPage.saveButton().waitFor();
    configPage.baseUrlAttribute().fill(DIRTY_GUARD_TEST_BASE_URL);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/dashboard"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.saveButton()).isVisible();
  }

  /**
   * Base URL dirty-guard continue path: Continue closes the modal and lets the
   * transition proceed; the config page unmounts and the user lands on
   * another Classic-embedded page ({@code /successMetricsConfiguration})
   * whose classic-component mount asserts the transition actually completed.
   * Not {@code /dashboard} — that's a native NOUX route with no
   * {@link NexusOneClassicEmbedPage#classicComponentMount()}, so the sibling
   * mount assertion never fires there.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedBaseUrlConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/baseUrl"));

    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    configPage.saveButton().waitFor();
    configPage.baseUrlAttribute().fill(DIRTY_GUARD_TEST_BASE_URL);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/successMetricsConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.saveButton()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42206: Classic System Notice Configuration mounts natively at
   * {@code /systemNoticeConfiguration} on the Nexus One bundle, rendering the
   * Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedSystemNoticeConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SystemNoticePage noticePage = new SystemNoticePage();
    SystemNoticePageAssertions noticeAssertions = new SystemNoticePageAssertions(noticePage);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    noticeAssertions.shouldRenderPageLayout();
  }

  /**
   * CLM-42206 dirty-guard cancel path: typing in the notice text field dirties
   * the form; a hash navigation triggers the shell dirty-guard; Cancel keeps
   * the user on the config page.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSystemNoticeConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));

    SystemNoticePage noticePage = new SystemNoticePage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    noticePage.container().waitFor();
    noticePage.noticeText().fill("dirty notice text");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(noticePage.container()).isVisible();
  }

  /**
   * CLM-42206 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * the target route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSystemNoticeConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));

    SystemNoticePage noticePage = new SystemNoticePage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    noticePage.container().waitFor();
    noticePage.noticeText().fill("dirty notice text");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(noticePage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42206 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /systemNoticeConfiguration is redirected to the Nexus One violations
   * dashboard before the admin form ever mounts. Covers the redirectTo
   * function on the route.
   *
   * <p>
   * Log in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One. Going straight to the Nexus One URL while
   * logged out hits {@code ensureNexusOneShellAccess}, which bounces
   * unauthenticated requests back to Classic before the router — so the
   * route's own {@code redirectTo} would never fire.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSystemNoticeConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    SystemNoticePage noticePage = new SystemNoticePage();
    assertThat(noticePage.container()).isHidden();
  }

  /**
   * Mail configuration embed renders inside the Nexus One shell.
   *
   * <p>
   * Verifies that navigating to the NOUX {@code /mailConfig} route renders the
   * Classic {@link MailConfigurationPage} inside the Nexus One embed mount,
   * with the NOUX left nav visible and the Classic global sidebar hidden.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedMailConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/mailConfig"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    MailConfigurationPage mailPage = new MailConfigurationPage();
    MailConfigurationPageAssertions mailAssertions = new MailConfigurationPageAssertions(mailPage);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
    mailAssertions.shouldRenderPageLayout();
  }

  /**
   * Mail configuration dirty guard blocks navigation on cancel.
   *
   * <p>
   * Fills a mail hostname field to trigger the dirty state, then attempts to
   * navigate away. The unsaved changes modal should appear and canceling should
   * return to the mail config page with the unsaved data still visible.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedMailConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/mailConfig"));

    MailConfigurationPage mailPage = new MailConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    mailPage.container().waitFor();
    mailPage.hostnameInput().fill("dirty-test-mail.example.invalid");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(mailPage.container()).isVisible();
  }

  /**
   * Mail configuration dirty guard allows navigation on continue.
   *
   * <p>
   * Fills a mail hostname field to trigger the dirty state, then attempts to
   * navigate away. The unsaved changes modal should appear and continuing should
   * navigate to the target page (system notice configuration), discarding unsaved
   * changes.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedMailConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/mailConfig"));

    MailConfigurationPage mailPage = new MailConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    mailPage.container().waitFor();
    mailPage.hostnameInput().fill("dirty-test-mail.example.invalid");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(mailPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * Mail configuration unauthorized user redirects to violations dashboard.
   *
   * <p>
   * Logs in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One. A non-admin user who deep-links {@code /mailConfig}
   * should be redirected to the violations dashboard by the route's
   * {@code redirectTo} guard (which checks {@code CONFIGURE_SYSTEM} permission).
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedMailConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/mailConfig"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    MailConfigurationPage mailPage = new MailConfigurationPage();
    assertThat(mailPage.container()).isHidden();
  }

  /**
   * CLM-42956: SAML Configuration mounts natively at
   * {@code /saml} on the Nexus One bundle, rendering the
   * Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedSamlConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/saml"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    SamlConfigurationPageAssertions samlAssertions = new SamlConfigurationPageAssertions(samlPage);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    samlAssertions.shouldRenderPageLayout();
  }

  /**
   * CLM-42956 dirty-guard cancel path: typing in the identity provider name
   * field dirties the form; a hash navigation triggers the shell dirty-guard;
   * Cancel keeps the user on the config page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSamlConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/saml"));

    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    samlPage.identityProviderName().waitFor();
    // This value is client-only and requires no server-side cleanup.
    samlPage.identityProviderName().fill("dirty-idp-name");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(samlPage.identityProviderName()).hasValue("dirty-idp-name");
  }

  /**
   * CLM-42956 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * the target route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSamlConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/saml"));

    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    samlPage.identityProviderName().waitFor();
    samlPage.identityProviderName().fill("dirty-idp-name");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(samlPage.identityProviderName()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42956 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /saml is redirected to the Nexus One violations dashboard before
   * the admin form ever mounts. Covers the redirectTo function on the route.
   *
   * <p>
   * Log in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One. Going straight to the Nexus One URL while
   * logged out hits {@code ensureNexusOneShellAccess}, which bounces
   * unauthenticated requests back to Classic before the router — so the
   * route's own {@code redirectTo} would never fire.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSamlConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/saml"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    assertThat(samlPage.identityProviderName()).isHidden();
  }

  /**
   * CLM-42964: Classic User Tokens Configuration mounts natively at
   * {@code /userTokensConfiguration} on the Nexus One bundle, rendering the
   * Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedUserTokensConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    UserTokenConfigurationPage userTokensPage = new UserTokenConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(userTokensPage.pageHeading()).isVisible();
    assertThat(userTokensPage.tileHeading()).isVisible();
  }

  /**
   * CLM-42964 dirty-guard cancel path: toggling expiration dirties the form;
   * a hash navigation triggers the shell dirty-guard; Cancel keeps the user
   * on the config page with the toggle state preserved.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedUserTokensConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));

    UserTokenConfigurationPage userTokensPage = new UserTokenConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    userTokensPage.container().waitFor();
    // Read the initial toggle state so we can verify the flip after clicking.
    boolean wasChecked = userTokensPage.expirationToggleInput().isChecked();
    // Toggle the expiration setting to make the form dirty.
    // This is a client-only change and needs no server-side cleanup.
    userTokensPage.expirationToggle().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(userTokensPage.container()).isVisible();
    // Verify the dirty toggle state survived the cancel (state should be flipped).
    if (wasChecked) {
      assertThat(userTokensPage.expirationToggleInput()).not().isChecked();
    }
    else {
      assertThat(userTokensPage.expirationToggleInput()).isChecked();
    }
  }

  /**
   * CLM-42964 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * the target route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedUserTokensConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));

    UserTokenConfigurationPage userTokensPage = new UserTokenConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    userTokensPage.container().waitFor();
    // Toggle the expiration setting to make the form dirty.
    userTokensPage.expirationToggle().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(userTokensPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42964 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /userTokensConfiguration is redirected to the Nexus One violations
   * dashboard before the admin form ever mounts. Covers the redirectTo
   * function on the route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedUserTokensConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    UserTokenConfigurationPage userTokensPage = new UserTokenConfigurationPage();
    assertThat(userTokensPage.container()).isHidden();
  }

  /**
   * CLM-42964 save-through-shell: enabling expiration and saving persists
   * the configuration; reloading the page shows the saved state.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedUserTokensConfiguration_savePersistsThroughShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));

    UserTokenConfigurationPage userTokensPage = new UserTokenConfigurationPage();
    UserTokenConfigurationPageAssertions userTokensAssertions =
        new UserTokenConfigurationPageAssertions(userTokensPage);

    userTokensPage.container().waitFor();
    boolean initialExpirationEnabled = userTokensPage.expirationToggleInput().isChecked();
    String initialExpiryDays = userTokensPage.expiryDaysInput().inputValue();

    try {
      // Ensure expiration is enabled — only click if currently unchecked, so
      // this test works regardless of the toggle's initial state.
      if (!initialExpirationEnabled) {
        userTokensPage.expirationToggle().click();
      }
      userTokensPage.expiryDaysInput().fill("45");
      userTokensPage.updateButton().click();

      waitForSubmitMask();

      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));
      userTokensPage.container().waitFor();
      userTokensAssertions.shouldHaveExpirationToggleChecked();
      assertThat(userTokensPage.expiryDaysInput()).hasValue("45");
    }
    finally {
      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));
      userTokensPage.container().waitFor();
      boolean currentExpirationEnabled = userTokensPage.expirationToggleInput().isChecked();

      // Restore days BEFORE toggling expiration off (toggling off disables the days input).
      if (currentExpirationEnabled && !initialExpirationEnabled) {
        userTokensPage.expiryDaysInput().fill(initialExpiryDays);
      }
      if (currentExpirationEnabled != initialExpirationEnabled) {
        userTokensPage.expirationToggle().click();
      }
      // Restore days AFTER toggling expiration on (toggling on enables the days input).
      if (initialExpirationEnabled) {
        userTokensPage.expiryDaysInput().fill(initialExpiryDays);
      }
      userTokensPage.updateButton().click();
      waitForSubmitMask();
    }
  }

  // ===================================================================================
  // Roles embed tests (CLM-42196)
  // ===================================================================================

  /**
   * CLM-42196: Roles list page mounts natively at /roles on the Nexus One bundle,
   * rendering the Classic list inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedRolesList_rendersClassicPageInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    RolesPage rolesPage = new RolesPage();
    RolesPageAssertions rolesAssertions = new RolesPageAssertions(rolesPage);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    rolesAssertions.shouldShowContainer();
    rolesAssertions.shouldShowPageTitle("Roles");
  }

  /**
   * CLM-42196: Roles editor dirty-guard cancel path — editing the role name
   * dirties the form; navigation triggers the shell dirty-guard; Cancel keeps
   * the user on the editor page with the dirty value intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedRoleEditor_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles/_new_"));

    RolesPage rolesPage = new RolesPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    rolesPage.roleEditor().waitFor();
    // Dirty value is client-only; no server-side cleanup needed.
    rolesPage.roleNameInput().fill("dirty-role-name");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    // Cancel preserves the dirty value — hasValue, not just isVisible.
    assertThat(rolesPage.roleNameInput()).hasValue("dirty-role-name");
  }

  /**
   * CLM-42196: Roles editor dirty-guard continue path — Continue closes the modal
   * and lets the transition proceed; the editor unmounts and the user lands on
   * the roles list (another Classic-embedded page).
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedRoleEditor_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles/_new_"));

    RolesPage rolesPage = new RolesPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    rolesPage.roleEditor().waitFor();
    // Dirty value is client-only; no server-side cleanup needed.
    rolesPage.roleNameInput().fill("dirty-role-name");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(rolesPage.roleEditor()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42196 auth gate: a user without VIEW_ROLES navigating to /roles
   * is redirected to the Nexus One violations dashboard before the roles list
   * ever mounts.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedRoles_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    RolesPage rolesPage = new RolesPage();
    assertThat(rolesPage.container()).isHidden();
  }

  /**
   * CLM-42196: Save-through-shell — fill the role form, save, reload, and
   * verify the persisted values re-populate. Clean up by deleting the test role.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedRoleEditor_saveThroughShellPersistsData() {
    String roleName = "pw-embed-role-" + TemporaryEntity.uuid();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles/_new_"));

    RolesPage rolesPage = new RolesPage();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    rolesPage.roleEditor().waitFor();
    rolesPage.roleNameInput().fill(roleName);
    rolesPage.roleDescriptionInput().fill("Created via NOUX shell embed");
    rolesPage.roleEditorSaveButton().click();

    // Wait for save to complete and the list to re-render
    rolesPage.container().waitFor();
    assertThat(rolesPage.roleEditor()).isHidden();

    // Reload the embed URL and verify the role appears in the list
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/roles"));
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(rolesPage.roleItem(roleName)).isVisible();

    // Clean up: click the role, delete it
    rolesPage.roleItem(roleName).click();
    rolesPage.roleEditor().waitFor();
    rolesPage.deleteRoleButton().click();
    rolesPage.deleteModal().waitFor();
    rolesPage.deleteModalSubmit().click();
    rolesPage.container().waitFor();

    // Verify cleanup
    assertThat(rolesPage.roleItem(roleName)).hasCount(0);
  }

  // ===================================================================================
  // Advanced Search Configuration embed tests (CLM-42963)
  // ===================================================================================

  /**
   * CLM-42963: Classic advancedSearchConfig route is mounted inside Nexus One at
   * {@code /advancedSearchConfig} on the Nexus One bundle, rendering the Classic form
   * as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedAdvancedSearchConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(configPage.pageHeading()).isVisible();
  }

  /**
   * CLM-42963 dirty-guard cancel path: toggling the checkbox dirties the form;
   * a hash navigation triggers the shell dirty-guard; Cancel keeps the user on
   * the config page with the dirty value preserved.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdvancedSearchConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));

    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    configPage.container().waitFor();

    // Capture the initial state, then flip it to dirty the form. Read-then-flip
    // avoids the hardcoded assumption that the checkbox starts unchecked — a
    // prior test in this class (or a server-side setting from a previous run)
    // can leave it in either state.
    // The dirty value is client-only, so no server-side cleanup is needed.
    boolean wasChecked = configPage.enabledCheckbox().isChecked();
    configPage.enabledCheckbox().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.container()).isVisible();

    // Assert the checkbox is flipped from its initial state — the dirty value survived.
    if (wasChecked) {
      assertThat(configPage.enabledCheckbox()).not().isChecked();
    }
    else {
      assertThat(configPage.enabledCheckbox()).isChecked();
    }
  }

  /**
   * CLM-42963 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * the target route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdvancedSearchConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));

    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    configPage.container().waitFor();

    // Toggle the checkbox to dirty the form.
    configPage.enabledCheckbox().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42963 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /advancedSearchConfig is redirected to the Nexus One violations dashboard
   * before the admin form ever mounts.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdvancedSearchConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();
    assertThat(configPage.container()).isHidden();
  }

  /**
   * CLM-42963 save-through-shell path: the form's save action works when driven
   * through the shell's redux/router bridge. Fill the form, save, reload, and
   * assert the persisted value re-populates.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdvancedSearchConfiguration_saveThroughShellPersists() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));

    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();

    configPage.container().waitFor();

    // Capture the current server-side state so we can restore it in the finally
    // block regardless of whether the assertions below pass or fail.
    boolean wasEnabled = configPage.enabledCheckbox().isChecked();

    try {
      // Flip the checkbox to the opposite state and save through the shell's
      // redux/router bridge.
      if (wasEnabled) {
        configPage.enabledCheckbox().uncheck();
      }
      else {
        configPage.enabledCheckbox().check();
      }
      configPage.saveButton().click();
      waitForSubmitMask();

      // Reload the embed URL and assert the persisted value re-populates.
      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));
      configPage.container().waitFor();

      if (wasEnabled) {
        assertThat(configPage.enabledCheckbox()).not().isChecked();
      }
      else {
        assertThat(configPage.enabledCheckbox()).isChecked();
      }
    }
    finally {
      // Restore the original state for downstream tests. Must run even if the
      // assertions above throw, otherwise the mutated server state leaks.
      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));
      configPage.container().waitFor();
      if (configPage.enabledCheckbox().isChecked() != wasEnabled) {
        if (wasEnabled) {
          configPage.enabledCheckbox().check();
        }
        else {
          configPage.enabledCheckbox().uncheck();
        }
        configPage.saveButton().click();
        waitForSubmitMask();
      }
    }
  }

  /**
   * CLM-42962: Automatic Source Control Configuration mounts natively at
   * {@code /automaticSourceControlConfiguration} on the Nexus One bundle, rendering
   * the Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedAutomaticSourceControlConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    AutomaticSourceControlConfigurationPage autoScmPage = new AutomaticSourceControlConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(autoScmPage.container()).isVisible();
    assertThat(autoScmPage.pageHeading()).isVisible();
    assertThat(autoScmPage.toggleLabel()).isVisible();
  }

  /**
   * CLM-42962 dirty-guard cancel path: toggling the enabled checkbox dirties
   * the form; a hash navigation triggers the shell dirty-guard; Cancel keeps
   * the user on the config page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAutomaticSourceControlConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));

    AutomaticSourceControlConfigurationPage autoScmPage = new AutomaticSourceControlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    autoScmPage.container().waitFor();
    // Capture initial state, then flip it to create a dirty value. Read-then-flip
    // avoids the hardcoded assumption that the toggle starts unchecked — a prior
    // test in this class can leave it in either state.
    boolean wasChecked = autoScmPage.toggleInput().isChecked();
    autoScmPage.toggleLabel().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    // Cancel preserves the dirty value — assert the toggle is flipped from its initial state.
    assertThat(autoScmPage.container()).isVisible();
    if (wasChecked) {
      assertThat(autoScmPage.toggleInput()).not().isChecked();
    }
    else {
      assertThat(autoScmPage.toggleInput()).isChecked();
    }
  }

  /**
   * CLM-42962 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * another Classic-embedded page.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAutomaticSourceControlConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));

    AutomaticSourceControlConfigurationPage autoScmPage = new AutomaticSourceControlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    autoScmPage.container().waitFor();
    // Toggle to create dirty state
    autoScmPage.toggleLabel().click();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(autoScmPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42962 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /automaticSourceControlConfiguration is redirected to the Nexus One violations
   * dashboard before the admin form ever mounts. Covers the redirectTo
   * function on the route.
   *
   * <p>
   * Log in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One. Going straight to the Nexus One URL while
   * logged out hits {@code ensureNexusOneShellAccess}, which bounces
   * unauthenticated requests back to Classic before the router — so the
   * route's own {@code redirectTo} would never fire.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAutomaticSourceControlConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    AutomaticSourceControlConfigurationPage autoScmPage = new AutomaticSourceControlConfigurationPage();
    assertThat(autoScmPage.container()).isHidden();
  }

  /**
   * CLM-42962 save-through-shell: toggle the enabled checkbox, save, reload,
   * and verify the persisted state.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAutomaticSourceControlConfiguration_saveThroughShellPersists() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));

    AutomaticSourceControlConfigurationPage autoScmPage = new AutomaticSourceControlConfigurationPage();

    autoScmPage.container().waitFor();

    // Capture the current server-side state so we can restore it in the finally
    // block regardless of whether the assertions below pass or fail.
    boolean wasEnabled = autoScmPage.toggleInput().isChecked();

    try {
      // Toggle to the opposite state and save through the shell's redux/router bridge.
      autoScmPage.toggleLabel().click();
      autoScmPage.updateButton().click();
      waitForSubmitMask();

      // Reload the embed URL and assert the persisted value re-populates.
      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));
      autoScmPage.container().waitFor();

      if (wasEnabled) {
        assertThat(autoScmPage.toggleInput()).not().isChecked();
      }
      else {
        assertThat(autoScmPage.toggleInput()).isChecked();
      }
    }
    finally {
      // Restore the original state for downstream tests. Must run even if the
      // assertions above throw, otherwise the mutated server state leaks.
      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));
      autoScmPage.container().waitFor();
      if (autoScmPage.toggleInput().isChecked() != wasEnabled) {
        autoScmPage.toggleLabel().click();
        autoScmPage.updateButton().click();
        waitForSubmitMask();
      }
    }
  }

  // ===================================================================================
  // Proxy Configuration embed tests (CLM-42876)
  // ===================================================================================

  /**
   * CLM-42876: Proxy Configuration mounts natively at {@code /proxyConfig}
   * on the Nexus One bundle, rendering the Classic form as-is inside the
   * Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedProxyConfiguration_rendersClassicFormInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
    assertThat(proxyPage.hostName()).isVisible();
    assertThat(proxyPage.port()).isVisible();
    assertThat(proxyPage.saveButton()).isVisible();
  }

  /**
   * CLM-42876 dirty-guard cancel path: editing the host name dirties the form;
   * a hash navigation triggers the shell dirty-guard; Cancel keeps the user on
   * the Proxy page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedProxyConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    proxyPage.hostName().waitFor();
    // The dirty value is client-only — we never click save — so the next test's
    // @Before re-login reloads a fresh page and no server-side cleanup is required.
    proxyPage.hostName().fill("dirty-proxy-test.example.invalid");

    // Navigate to dashboard, which has no dirty guard
    playwrightRefreshOrOpen(NexusOnePage.url("/dashboard"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(proxyPage.hostName()).isVisible();
    assertThat(proxyPage.hostName()).hasValue("dirty-proxy-test.example.invalid");
  }

  /**
   * CLM-42876 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the proxy page unmounts and the user lands on
   * the destination page.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedProxyConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    proxyPage.hostName().waitFor();
    // Client-only dirty value — see cancel test above.
    proxyPage.hostName().fill("dirty-proxy-test.example.invalid");

    // Navigate to dashboard, which has no dirty guard
    playwrightRefreshOrOpen(NexusOnePage.url("/dashboard"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(proxyPage.hostName()).isHidden();
    // Verify we arrived at the dashboard, not just away from proxy
    assertThat(page).hasURL(Pattern.compile(".*/dashboard.*"));
  }

  /**
   * CLM-42876 save-through-shell path: the form's save action works when driven
   * through the shell's redux/router bridge. Fill the form, save, reload, and
   * assert the persisted value re-populates.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedProxyConfiguration_savePersistsThroughShellBridge() {
    // Standalone Classic save round-trip is covered by ProxyConfigurationPlaywrightTest.
    // This test exercises the shell-specific concern: that the Redux write path
    // (proxyConfigActions save → PUT → serverData replace → new GET on refresh) flows
    // through the NOUX shell's redux/router bridge and re-populates the form.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    ProxyConfigurationPageAssertions proxyAssertions = new ProxyConfigurationPageAssertions(proxyPage);

    proxyPage.hostName().waitFor();
    proxyPage.fillMinimal("proxy-embed-test.invalid", "8080");
    proxyPage.save();
    waitForSubmitMask();

    try {
      // Persistence assertions inside the try/finally so a mid-test failure still
      // fires the cleanup delete — the applied config affects the server's live
      // HTTP clients (proxy-embed-test.invalid:8080), so leaking it would poison
      // every later test in this class.
      playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));
      proxyAssertions.shouldShowHostname("proxy-embed-test.invalid");
      proxyAssertions.shouldShowPort("8080");
    }
    finally {
      // Clean up the persisted config so subsequent tests / test classes see a fresh state.
      proxyPage.clickDelete();
      proxyPage.confirmDelete();
      waitForSubmitMask();
      proxyAssertions.shouldBeEmpty();
    }
  }

  /**
   * CLM-42876 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /proxyConfig is redirected to the Nexus One violations dashboard before
   * the admin form ever mounts.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedProxyConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    assertThat(proxyPage.hostName()).isHidden();
  }
  // ===================================================================================
  // Atlassian Crowd Configuration embed tests (CLM-42957)
  // ===================================================================================

  /**
   * CLM-42957: Atlassian Crowd Configuration mounts natively at
   * {@code /crowd} on the Nexus One bundle, rendering the Classic form
   * as-is inside the Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedCrowdConfiguration_rendersClassicFormInsideNexusOneShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    CrowdConfigurationPage crowdPage = new CrowdConfigurationPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(crowdPage.container()).isVisible();
    assertThat(crowdPage.serverUrl()).isVisible();
    assertThat(crowdPage.applicationName()).isVisible();
    assertThat(crowdPage.applicationPassword()).isVisible();
    assertThat(crowdPage.saveButton()).isVisible();
  }

  /**
   * CLM-42957 dirty-guard cancel path: filling the Server url field dirties
   * the form; a hash navigation triggers the shell dirty-guard; Cancel keeps
   * the user on the config page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedCrowdConfiguration_dirtyGuardBlocksNavigationOnCancel() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));

    CrowdConfigurationPage crowdPage = new CrowdConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    crowdPage.container().waitFor();
    // Dirty value is client-only; no server-side cleanup needed.
    crowdPage.serverUrl().fill("http://dirty-crowd.invalid");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/dashboard"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(crowdPage.container()).isVisible();
    // The dirty value survives the cancel action.
    assertThat(crowdPage.serverUrl()).hasValue("http://dirty-crowd.invalid");
  }

  /**
   * CLM-42957 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the config page unmounts and the user lands on
   * another Classic-embedded page ({@code /successMetricsConfiguration})
   * whose classic-component mount asserts the transition actually completed.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedCrowdConfiguration_dirtyGuardAllowsNavigationOnContinue() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));

    CrowdConfigurationPage crowdPage = new CrowdConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    crowdPage.container().waitFor();
    crowdPage.serverUrl().fill("http://dirty-crowd.invalid");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/successMetricsConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(crowdPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42957 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /crowd is redirected to the Nexus One violations dashboard before the
   * admin form ever mounts. Covers the redirectTo function on the route.
   *
   * <p>
   * Log in on Classic first so the shared session cookie is present when
   * we navigate into Nexus One. Going straight to the Nexus One URL while
   * logged out hits {@code ensureNexusOneShellAccess}, which bounces
   * unauthenticated requests back to Classic before the router — so the
   * route's own {@code redirectTo} would never fire.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedCrowdConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    CrowdConfigurationPage crowdPage = new CrowdConfigurationPage();
    assertThat(crowdPage.container()).isHidden();
  }

  /**
   * CLM-42957 save-through-shell: fill the Crowd configuration form, save,
   * reload the embed URL, and assert the persisted values re-populate.
   * Then delete the configuration to leave a clean database for downstream
   * tests. This closes the gap between "the render test proves the form
   * mounts" and "the form's actions work when driven through the shell's
   * redux/router bridge."
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedCrowdConfiguration_saveThroughShellPersists() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));

    CrowdConfigurationPage crowdPage = new CrowdConfigurationPage();

    crowdPage.container().waitFor();
    String testServerUrl = "http://crowd-test.invalid:8095/crowd";
    String testAppName = "test-app-" + TemporaryEntity.uuid().substring(0, 8);
    String testAppPassword = "test-password-" + TemporaryEntity.uuid().substring(0, 8);

    crowdPage.serverUrl().fill(testServerUrl);
    crowdPage.applicationName().fill(testAppName);
    crowdPage.applicationPassword().fill(testAppPassword);

    crowdPage.saveButton().click();
    waitForSubmitMask();

    // Reload the embed URL and verify the values persisted.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));
    crowdPage.container().waitFor();

    try {
      assertThat(crowdPage.serverUrl()).hasValue(testServerUrl);
      assertThat(crowdPage.applicationName()).hasValue(testAppName);
    }
    finally {
      // Clean up: delete the configuration.
      crowdPage.deleteButton().click();
      assertThat(crowdPage.deleteModal()).isVisible();
      crowdPage.deleteModalSubmitButton().click();
      waitForSubmitMask();
    }

    // Verify empty state after delete.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/crowd"));
    crowdPage.container().waitFor();
    assertThat(crowdPage.serverUrl()).hasValue("");
    assertThat(crowdPage.applicationName()).hasValue("");
  }
  // ===================================================================================
  // Webhooks embed tests (CLM-42961)
  // ===================================================================================

  /**
   * CLM-42961: Webhooks list page mounts natively at {@code /webhooks/list}
   * on the Nexus One bundle, rendering the Classic list as-is inside the
   * Nexus One shell.
   */
  @Test
  @Category(SanityTest.class)
  public void testEmbeddedWebhooksList_rendersClassicListInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/list"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    WebhookListPage webhooksPage = new WebhookListPage();

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    assertThat(webhooksPage.container()).isVisible();
    assertThat(webhooksPage.heading()).isVisible();
    assertThat(webhooksPage.tileHeading()).isVisible();
  }

  /**
   * CLM-42961 dirty-guard cancel path: editing the URL field dirties the form;
   * a hash navigation triggers the shell dirty-guard; Cancel keeps the user on
   * the edit page with the dirty state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedWebhookEditor_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/create"));

    WebhookEditorPage editorPage = new WebhookEditorPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    editorPage.container().waitFor();
    // Dirty value is client-only; no server-side cleanup needed.
    editorPage.urlInput().fill("https://dirty-webhook-test.invalid");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/list"));
    assertThat(modal.container()).isVisible();

    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.urlInput()).hasValue("https://dirty-webhook-test.invalid");
  }

  /**
   * CLM-42961 dirty-guard continue path: Continue closes the modal and lets
   * the transition proceed; the edit page unmounts and the user lands on
   * another Classic-embedded page.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedWebhookEditor_dirtyGuardAllowsNavigationOnContinue() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/create"));

    WebhookEditorPage editorPage = new WebhookEditorPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    editorPage.container().waitFor();
    editorPage.urlInput().fill("https://dirty-webhook-test.invalid");

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();

    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(editorPage.container()).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  /**
   * CLM-42961 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /webhooks/list is redirected to the Nexus One violations dashboard before
   * the webhooks list ever mounts. Covers the redirectTo function on the route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedWebhooksList_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/list"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    WebhookListPage webhooksPage = new WebhookListPage();
    assertThat(webhooksPage.container()).isHidden();
  }

  /**
   * CLM-42961 save-through-shell: creating a webhook through the embedded editor
   * persists to the database; reloading the list shows the persisted webhook.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedWebhookEditor_saveThroughShellPersists() {
    setFeatures(LicensedFeature.values());

    String webhookUrl = "https://pw-webhook-test-" + TemporaryEntity.uuid().substring(0, 8) + ".invalid";

    // Create a new webhook
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/create"));

    WebhookEditorPage editorPage = new WebhookEditorPage();
    WebhookListPage listPage = new WebhookListPage();

    editorPage.container().waitFor();
    editorPage.urlInput().fill(webhookUrl);
    // Select an event type to make the webhook valid
    editorPage.eventTypeCheckbox("Application Evaluation").click();

    editorPage.submitButton().click();
    editorPage.pageTitle()
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

    // Verify the webhook appears in the list
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/list"));
    assertThat(listPage.webhookItemByUrl(webhookUrl)).isVisible();

    // Cleanup: delete the webhook
    listPage.webhookItemByUrl(webhookUrl).click();
    editorPage.container().waitFor();
    editorPage.deleteButton().click();
    assertThat(editorPage.deleteModal()).isVisible();
    editorPage.deleteModalContinueButton().click();
    editorPage.deleteModal()
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

    // Verify deletion
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/webhooks/list"));
    assertThat(listPage.webhookItemByUrl(webhookUrl)).not().isVisible();
  }
}
