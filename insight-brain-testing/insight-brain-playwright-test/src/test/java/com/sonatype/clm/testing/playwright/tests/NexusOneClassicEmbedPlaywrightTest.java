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
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.TimeoutError;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPage;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.AutomaticApplicationsConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.AutomaticApplicationsConfigurationPageAssertions;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  // Auth reuse (CLM-45709): the admin session captured on the first login in this class is
  // replayed into every later test's fresh BrowserContext via reusableStorageState(), so most
  // tests skip the UI login instead of logging in once per test. Tests that log out (the
  // unauthorized-redirect cases) null the field, so the class does roughly one admin login plus
  // one per logout. Static so it survives across this class's methods within the single reused
  // fork; volatile for visibility. Tests run sequentially (-T 1), so no locking.
  private static volatile String reusableAdminStorageState;

  @BeforeEach
  public void enablePreviewUiAndLogin() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    originalSuccessMetricsEnabled =
        lookup(SystemConfigurationPropertyDAO.class).get(SuccessMetricsService.PROPERTY_ENABLED);
    lookup(SystemConfigurationPropertyDAO.class).set(SuccessMetricsService.PROPERTY_ENABLED, "true");

    if (reusableAdminStorageState == null) {
      loginAsAdminAndCaptureSession();
      return;
    }

    // The context was seeded with the captured admin session (see reusableStorageState()), so we
    // are authenticated without a login round-trip. If that session is stale — expired over a long
    // class run, or invalidated by an earlier logout — self-heal by discarding it and logging in
    // fresh, which recaptures a live session for the following tests.
    playwrightRefreshOrOpen(LoginPage.rootUrl());
    if (!isAuthenticatedHeaderVisible()) {
      reusableAdminStorageState = null;
      loginAsAdminAndCaptureSession();
    }
  }

  private void loginAsAdminAndCaptureSession() {
    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();
    reusableAdminStorageState = context.storageState();
  }

  @Override
  protected String reusableStorageState() {
    return reusableAdminStorageState;
  }

  @Override
  protected void invalidateReusableLogin() {
    // A test logged out (e.g. the unauthorized-redirect cases), which deletes the captured admin
    // session server-side; drop it so the next test logs in fresh and recaptures a live session.
    reusableAdminStorageState = null;
  }

  @AfterEach
  public void resetPreviewUiAndSuccessMetricsConfig() {
    // Dismiss any dirty-guard modal a failed test may have left open, so the
    // next test doesn't hit a blocked transition on refresh.
    new UnsavedChangesModalComponent().continueIfOpen();
    // Unroute any Automatic Applications stubs so a later test in this class
    // doesn't inherit them. page.unroute is a no-op if the pattern was never
    // routed by the current test.
    page.unroute(AUTO_APP_CONFIG_ROUTE);
    page.unroute(ORGANIZATIONS_ROUTE);
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    if (originalSuccessMetricsEnabled != null) {
      lookup(SystemConfigurationPropertyDAO.class)
          .set(SuccessMetricsService.PROPERTY_ENABLED, originalSuccessMetricsEnabled);
    }
  }

  // CLM-42877: Route stubs for the Automatic Applications Configuration render test.
  // The reducer filters out the system root org
  // (`configuration/automaticApplicationsConfiguration/automaticApplicationsConfigurationActions.js`
  // filters `org.id !== 'ROOT_ORGANIZATION_ID'`), so if the embedded IQ server has only
  // the root org the form renders `<NxErrorAlert>No parent organizations found</NxErrorAlert>`
  // instead of the Parent Organization select — and `shouldRenderPageLayout()` fails on
  // `getByLabel("Parent Organization")`. Same stubbing pattern as
  // AutomaticApplicationsConfigurationPlaywrightTest.
  private static final String AUTO_APP_CONFIG_ROUTE = "**/rest/config/automaticApplications**";

  private static final String ORGANIZATIONS_ROUTE = "**/rest/organization*";

  private static final String AUTO_APP_CONFIG_DISABLED_NO_PARENT_JSON =
      "{\"enabled\":false,\"parentOrganizationId\":null}";

  private static final String AUTO_APP_ORGANIZATIONS_JSON =
      "[{\"id\":\"pw-test-org\",\"name\":\"pw-test-org\"}]";

  private void stubAutomaticApplicationsRoutes(String autoAppConfigJson, String organizationsJson) {
    stubJson(ORGANIZATIONS_ROUTE, organizationsJson);
    stubJson(AUTO_APP_CONFIG_ROUTE, autoAppConfigJson);
  }

  private void stubJson(String routePattern, String body) {
    page.route(routePattern, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody(body)));
  }

  /**
   * Runs the shared shell dirty-guard flow for a config page: dirty the form, attempt to navigate
   * away and Cancel (which keeps the user on the still-dirty page), then attempt the same
   * navigation again and Continue (which discards and lands on the target). Cancel preserves the
   * dirty state, so the second navigation re-triggers the guard without re-dirtying — this keeps
   * the flow correct for both fill-based and toggle-based forms.
   *
   * @param pageUrl the embed path of the config page under test
   * @param pageContainer the page's root/container locator (waited for, asserted visible after
   *          Cancel and hidden after Continue)
   * @param makeDirty dirties the form so the unsaved-changes guard will fire
   * @param awayUrl the embed path navigated to in order to trigger the guard
   */
  private void assertDirtyGuardCancelThenContinue(
      String pageUrl,
      Locator pageContainer,
      Runnable makeDirty,
      String awayUrl)
  {
    assertDirtyGuardCancelThenContinue(pageUrl, pageContainer, makeDirty, awayUrl, () -> {
    });
  }

  /**
   * Variant of {@link #assertDirtyGuardCancelThenContinue(String, Locator, Runnable, String)} that
   * also runs {@code afterCancel} once Cancel has kept the user on the page, to assert the unsaved
   * edit itself survived (e.g. the edited field still holds its dirty value / the toggle stayed
   * flipped) rather than only that navigation was aborted.
   *
   * @param afterCancel extra assertions run after Cancel keeps the user on the still-dirty page
   */
  private void assertDirtyGuardCancelThenContinue(
      String pageUrl,
      Locator pageContainer,
      Runnable makeDirty,
      String awayUrl,
      Runnable afterCancel)
  {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl(pageUrl));
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    pageContainer.waitFor();
    makeDirty.run();

    // Cancel keeps the user on the still-dirty page.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl(awayUrl));
    assertThat(modal.container()).isVisible();
    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(pageContainer).isVisible();
    afterCancel.run();

    // Continue discards and navigates away (form is still dirty from above).
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl(awayUrl));
    assertThat(modal.container()).isVisible();
    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(pageContainer).isHidden();
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  @Test
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
  public void testEmbeddedLegal_rendersClassicAlpDashboardInShell() {
    setFeatures(LicensedFeature.values());

    // CLM-44467: clean /legal redirects to Classic Legal Obligations (applicationsDashboard),
    // same pattern as Orgs & Policies — not the native LEGAL_VIOLATION triage list.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/legal"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    LegalDashboardPage legalDashboard = new LegalDashboardPage();
    LegalDashboardPageAssertions legalAssertions = new LegalDashboardPageAssertions(legalDashboard);

    Pattern applicationsDashboardUrl =
        Pattern.compile(".*/nexus-one/index\\.html#/legal/applicationsDashboard(?:\\?.*)?$");
    page.waitForURL(
        applicationsDashboardUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));
    assertThat(page).hasURL(applicationsDashboardUrl);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).hasAttribute("aria-current", "page");
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
  @Tag("regression")
  public void testLegacyComingSoonLegalUrl_redirectsToClassicAlpDashboard() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/legal"));

    // Legacy /coming-soon/legal follows the clean /legal embed entry → Classic ALP dashboard.
    Pattern applicationsDashboardUrl =
        Pattern.compile(".*/nexus-one/index\\.html#/legal/applicationsDashboard(?:\\?.*)?$");
    page.waitForURL(
        applicationsDashboardUrl, new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    assertThat(page).hasURL(applicationsDashboardUrl);
    assertThat(embedPage.leftNavLink("Legal")).hasAttribute("aria-current", "page");
    assertThat(embedPage.classicComponentMount()).isVisible();
  }

  @Test
  @Tag("regression")
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
  @Tag("regression")
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
    assertFalse(navigatedWhileBlocked, "LeftNav must not be clickable while the copyright modal is open");
    assertThat(copyrightPage.modal()).isVisible();

    copyrightPage.clickCancel();
    assertThat(copyrightPage.modal()).not().isVisible();
    dashboardLink.click();
    assertThat(page).hasURL(Pattern.compile(".*/dashboard.*"));
  }

  @Test
  @Tag("regression")
  public void testEmbeddedLegal_advancedLegalPackUnlicensed_hidesNavEntry() {
    // CLM-44467 restores the Classic ALP gate on LeftNav Legal — stripping ADVANCED_LEGAL_PACK
    // must hide the rail entry (native LEGAL_VIOLATION triage remains at /legal-risk).
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/home"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).not().isVisible();
  }

  @Test
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42186 dirty-guard: toggling the switch dirties the form; navigating away triggers the shell
   * dirty-guard. Cancel keeps the user on the still-dirty config page, then Continue discards and
   * lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedSuccessMetricsConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    SuccessMetricsConfigurationPage configPage = new SuccessMetricsConfigurationPage();
    assertDirtyGuardCancelThenContinue(
        "/successMetricsConfiguration",
        configPage.container(),
        () -> configPage.enabledToggle().click(),
        "/coming-soon/success-metrics",
        () -> assertThat(configPage.enabledToggleInput()).not().isChecked());
  }

  /**
   * CLM-42465: Users admin page mounts natively at {@code /users} on the
   * Nexus One bundle, rendering the Classic user list as-is inside the
   * Nexus One shell. This is a list page with no dirty guard.
   */
  @Test
  @Tag("sanity")
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
  @Tag("regression")
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
   * CLM-42465 dirty-guard: filling a field dirties the create-user form; navigating away triggers
   * the shell dirty-guard. Cancel keeps the user on the form, then Continue discards and lands on
   * the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedCreateUser_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    UserManagementPage userPage = new UserManagementPage();
    assertDirtyGuardCancelThenContinue(
        "/users/_new_",
        userPage.userForm(),
        () -> userPage.firstNameInput().fill("dirty"),
        "/successMetricsConfiguration");
  }

  /**
   * CLM-42464: Administrators list page mounts natively at
   * {@code /administrators} on the Nexus One bundle, rendering
   * the Classic list as-is inside the Nexus One shell.
   */
  @Test
  @Tag("sanity")
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
  @Tag("sanity")
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
   * CLM-42464 dirty-guard: selecting a user from the search dropdown dirties the edit form;
   * navigating away triggers the shell dirty-guard. Cancel keeps the user on the edit page, then
   * Continue discards and lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedAdministratorsEdit_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    String searchableUserItem = seedSearchableAdministratorCandidate();
    AdministratorsEditPage editPage = new AdministratorsEditPage();
    assertDirtyGuardCancelThenContinue(
        "/administrators/" + Role.POLICY_ADMIN_ROLE_ID,
        editPage.root(),
        () -> editPage.searchAndAddByText("*", searchableUserItem),
        "/administrators");
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * Base URL dirty-guard: editing the URL field dirties the form; navigating away triggers the
   * shell dirty-guard. Cancel keeps the user on the config page, then Continue discards and lands
   * on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedBaseUrlConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    assertDirtyGuardCancelThenContinue(
        "/baseUrl",
        configPage.saveButton(),
        () -> configPage.baseUrlAttribute().fill(DIRTY_GUARD_TEST_BASE_URL),
        "/successMetricsConfiguration");
  }

  /**
   * CLM-42206: Classic System Notice Configuration mounts natively at
   * {@code /systemNoticeConfiguration} on the Nexus One bundle, rendering the
   * Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Tag("sanity")
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
   * CLM-42206 dirty-guard: typing in the notice text field dirties the form; navigating away
   * triggers the shell dirty-guard. Cancel keeps the user on the config page, then Continue
   * discards and lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedSystemNoticeConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    SystemNoticePage noticePage = new SystemNoticePage();
    assertDirtyGuardCancelThenContinue(
        "/systemNoticeConfiguration",
        noticePage.container(),
        () -> noticePage.noticeText().fill("dirty notice text"),
        "/coming-soon/success-metrics");
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * Mail configuration dirty-guard: filling the hostname field dirties the form; navigating away
   * triggers the shell dirty-guard. Cancel keeps the user on the config page, then Continue
   * discards and lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedMailConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    MailConfigurationPage mailPage = new MailConfigurationPage();
    assertDirtyGuardCancelThenContinue(
        "/mailConfig",
        mailPage.container(),
        () -> mailPage.hostnameInput().fill("dirty-test-mail.example.invalid"),
        "/systemNoticeConfiguration");
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42956 dirty-guard: typing in the identity provider name field dirties the form; navigating
   * away triggers the shell dirty-guard. Cancel keeps the user on the config page, then Continue
   * discards and lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedSamlConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    assertDirtyGuardCancelThenContinue(
        "/saml",
        samlPage.identityProviderName(),
        () -> samlPage.identityProviderName().fill("dirty-idp-name"),
        "/systemNoticeConfiguration",
        () -> assertThat(samlPage.identityProviderName()).hasValue("dirty-idp-name"));
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42964 dirty-guard: toggling expiration dirties the form; navigating away triggers the
   * shell dirty-guard. Cancel keeps the user on the config page, then Continue discards and lands
   * on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedUserTokensConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    // Inlined rather than using assertDirtyGuardCancelThenContinue because the post-Cancel check
    // asserts the toggle flipped relative to its initial state, read after the page loads.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/userTokensConfiguration"));

    UserTokenConfigurationPage userTokensPage = new UserTokenConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    userTokensPage.container().waitFor();
    // Read the initial toggle state so we can verify the flip after clicking.
    boolean wasChecked = userTokensPage.expirationToggleInput().isChecked();
    // Toggle the expiration setting to make the form dirty (client-only, no server cleanup).
    userTokensPage.expirationToggle().click();

    // Cancel keeps the user on the still-dirty page with the toggle state flipped.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();
    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(userTokensPage.container()).isVisible();
    if (wasChecked) {
      assertThat(userTokensPage.expirationToggleInput()).not().isChecked();
    }
    else {
      assertThat(userTokensPage.expirationToggleInput()).isChecked();
    }

    // Continue discards and navigates away (form is still dirty from above).
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42196 dirty-guard: editing the role name dirties the editor; navigating away triggers the
   * shell dirty-guard. Cancel keeps the user on the editor, then Continue discards and lands on
   * the roles list.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedRoleEditor_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    RolesPage rolesPage = new RolesPage();
    assertDirtyGuardCancelThenContinue(
        "/roles/_new_",
        rolesPage.roleEditor(),
        () -> rolesPage.roleNameInput().fill("dirty-role-name"),
        "/roles",
        () -> assertThat(rolesPage.roleNameInput()).hasValue("dirty-role-name"));
  }

  /**
   * CLM-42196 auth gate: a user without VIEW_ROLES navigating to /roles
   * is redirected to the Nexus One violations dashboard before the roles list
   * ever mounts.
   */
  @Test
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42963 dirty-guard: toggling the checkbox dirties the form; navigating away triggers the
   * shell dirty-guard. Cancel keeps the user on the config page, then Continue discards and lands
   * on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedAdvancedSearchConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    // Inlined rather than using assertDirtyGuardCancelThenContinue because the post-Cancel check
    // asserts the checkbox flipped relative to its initial state, read after the page loads.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/advancedSearchConfig"));

    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    configPage.container().waitFor();
    // Read-then-flip avoids assuming the checkbox's initial state (a prior test or server-side
    // setting can leave it either way). The dirty value is client-only, so no server cleanup.
    boolean wasChecked = configPage.enabledCheckbox().isChecked();
    configPage.enabledCheckbox().click();

    // Cancel keeps the user on the still-dirty page with the checkbox flipped.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));
    assertThat(modal.container()).isVisible();
    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(configPage.container()).isVisible();
    if (wasChecked) {
      assertThat(configPage.enabledCheckbox()).not().isChecked();
    }
    else {
      assertThat(configPage.enabledCheckbox()).isChecked();
    }

    // Continue discards and navigates away (form is still dirty from above).
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
  @Tag("regression")
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
  @Tag("regression")
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
   * CLM-42877: The Automatic Applications Configuration page should render with the Nexus One shell
   * when accessed via the embedded route, showing the classic form inside the modern layout.
   *
   * <p>
   * Stubs the organizations and config endpoints because the reducer filters out the system
   * root org — without at least one non-root org, the form renders the "No parent organizations
   * found" error alert instead of the Parent Organization select. See
   * {@link AutomaticApplicationsConfigurationPlaywrightTest} for the same pattern in the
   * Classic-only tests.
   */
  @Test
  @Tag("sanity")
  public void testEmbeddedAutomaticApplicationsConfiguration_rendersClassicFormInsideNexusOneShell() {
    stubAutomaticApplicationsRoutes(AUTO_APP_CONFIG_DISABLED_NO_PARENT_JSON, AUTO_APP_ORGANIZATIONS_JSON);
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticApplicationsConfiguration"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    AutomaticApplicationsConfigurationPage autoAppsPage = new AutomaticApplicationsConfigurationPage();
    AutomaticApplicationsConfigurationPageAssertions autoAppsAssertions =
        new AutomaticApplicationsConfigurationPageAssertions(autoAppsPage);

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    autoAppsAssertions.shouldRenderPageLayout();
  }

  /**
   * CLM-42877 dirty-guard: toggling the enable switch dirties the form; navigating away triggers
   * the shell dirty-guard. Cancel keeps the user on the config page, then Continue discards and
   * lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedAutomaticApplicationsConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    // Stub organizations + config so NxStatefulForm's spinner clears and the enabled toggle label
    // is present (see the render test above for the same stubbing rationale).
    stubAutomaticApplicationsRoutes(AUTO_APP_CONFIG_DISABLED_NO_PARENT_JSON, AUTO_APP_ORGANIZATIONS_JSON);
    AutomaticApplicationsConfigurationPage autoAppsPage = new AutomaticApplicationsConfigurationPage();
    assertDirtyGuardCancelThenContinue(
        "/automaticApplicationsConfiguration",
        autoAppsPage.tile(),
        () -> {
          autoAppsPage.enabledToggleLabel().waitFor();
          autoAppsPage.enabledToggleLabel().click();
        },
        "/systemNoticeConfiguration");
  }

  /**
   * CLM-42877: A user without MANAGE_AUTOMATIC_APPLICATION_CREATION permission should be redirected
   * to the violations dashboard when attempting to access the Automatic Applications Configuration page.
   * This test verifies the permission gate works correctly by using a user with no specific permissions.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedAutomaticApplicationsConfiguration_unauthorizedUserRedirectsToViolations() {
    User nonAdminUser = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLogout();
    playwrightLoginAt(LoginPage.rootUrl(),
        nonAdminUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticApplicationsConfiguration"));

    page.waitForURL("**/nexus-one/index.html#/dashboard/violations");
    AutomaticApplicationsConfigurationPage autoAppsPage = new AutomaticApplicationsConfigurationPage();
    assertThat(autoAppsPage.tile()).isHidden();
  }

  /**
   * CLM-42962: Automatic Source Control Configuration mounts natively at
   * {@code /automaticSourceControlConfiguration} on the Nexus One bundle, rendering
   * the Classic form as-is inside the Nexus One shell.
   */
  @Test
  @Tag("sanity")
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
   * CLM-42962 dirty-guard: toggling the enabled checkbox dirties the form; navigating away triggers
   * the shell dirty-guard. Cancel keeps the user on the config page, then Continue discards and
   * lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedAutomaticSourceControlConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    // Inlined rather than using assertDirtyGuardCancelThenContinue because the post-Cancel check
    // asserts the toggle flipped relative to its initial state, read after the page loads.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/automaticSourceControlConfiguration"));

    AutomaticSourceControlConfigurationPage autoScmPage = new AutomaticSourceControlConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    autoScmPage.container().waitFor();
    // Read-then-flip avoids assuming the toggle's initial state (a prior test can leave it either
    // way). The dirty value is client-only, so no server cleanup is needed.
    boolean wasChecked = autoScmPage.toggleInput().isChecked();
    autoScmPage.toggleLabel().click();

    // Cancel keeps the user on the still-dirty page with the toggle flipped.
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/systemNoticeConfiguration"));
    assertThat(modal.container()).isVisible();
    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(autoScmPage.container()).isVisible();
    if (wasChecked) {
      assertThat(autoScmPage.toggleInput()).not().isChecked();
    }
    else {
      assertThat(autoScmPage.toggleInput()).isChecked();
    }

    // Continue discards and navigates away (form is still dirty from above).
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42876 dirty-guard: editing the host name dirties the form; navigating to the dashboard (a
   * native NOUX route with no dirty guard) triggers the shell dirty-guard. Cancel keeps the user on
   * the still-dirty Proxy page, then Continue discards and lands on the dashboard.
   *
   * <p>
   * Inlined rather than using {@link #assertDirtyGuardCancelThenContinue} because this pair
   * navigates via {@link NexusOnePage#url} to {@code /dashboard} (which has no
   * {@link NexusOneClassicEmbedPage#classicComponentMount()}) and asserts the destination by URL.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedProxyConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/proxyConfig"));

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    proxyPage.hostName().waitFor();
    // The dirty value is client-only — we never click save — so the next test's
    // @Before re-login reloads a fresh page and no server-side cleanup is required.
    proxyPage.hostName().fill("dirty-proxy-test.example.invalid");

    // Cancel keeps the user on the still-dirty page.
    playwrightRefreshOrOpen(NexusOnePage.url("/dashboard"));
    assertThat(modal.container()).isVisible();
    modal.cancelButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(proxyPage.hostName()).isVisible();
    assertThat(proxyPage.hostName()).hasValue("dirty-proxy-test.example.invalid");

    // Continue discards and navigates to the dashboard (form is still dirty from above).
    playwrightRefreshOrOpen(NexusOnePage.url("/dashboard"));
    assertThat(modal.container()).isVisible();
    modal.continueButton().click();
    assertThat(modal.container()).isHidden();
    assertThat(proxyPage.hostName()).isHidden();
    assertThat(page).hasURL(Pattern.compile(".*/dashboard.*"));
  }

  /**
   * CLM-42876 save-through-shell path: the form's save action works when driven
   * through the shell's redux/router bridge. Fill the form, save, reload, and
   * assert the persisted value re-populates.
   */
  @Test
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42957 dirty-guard: filling the Server URL field dirties the form; navigating away triggers
   * the shell dirty-guard. Cancel keeps the user on the config page, then Continue discards and
   * lands on the target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedCrowdConfiguration_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    setFeatures(LicensedFeature.values());
    CrowdConfigurationPage crowdPage = new CrowdConfigurationPage();
    assertDirtyGuardCancelThenContinue(
        "/crowd",
        crowdPage.container(),
        () -> crowdPage.serverUrl().fill("http://dirty-crowd.invalid"),
        "/successMetricsConfiguration",
        () -> assertThat(crowdPage.serverUrl()).hasValue("http://dirty-crowd.invalid"));
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
  @Tag("regression")
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
  @Tag("regression")
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
  @Tag("sanity")
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
   * CLM-42961 dirty-guard: editing the URL field dirties the editor; navigating away triggers the
   * shell dirty-guard. Cancel keeps the user on the editor, then Continue discards and lands on the
   * target.
   */
  @Test
  @Tag("regression")
  public void testEmbeddedWebhookEditor_dirtyGuardCancelKeepsPageThenContinueNavigatesAway() {
    WebhookEditorPage editorPage = new WebhookEditorPage();
    assertDirtyGuardCancelThenContinue(
        "/webhooks/create",
        editorPage.container(),
        () -> editorPage.urlInput().fill("https://dirty-webhook-test.invalid"),
        "/systemNoticeConfiguration",
        () -> assertThat(editorPage.urlInput()).hasValue("https://dirty-webhook-test.invalid"));
  }

  /**
   * CLM-42961 auth gate: a user without CONFIGURE_SYSTEM navigating to
   * /webhooks/list is redirected to the Nexus One violations dashboard before
   * the webhooks list ever mounts. Covers the redirectTo function on the route.
   */
  @Test
  @Tag("regression")
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
  @Tag("regression")
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
