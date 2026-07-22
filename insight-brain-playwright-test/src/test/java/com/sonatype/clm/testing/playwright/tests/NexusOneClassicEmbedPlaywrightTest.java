/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.TimeoutError;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.BaseUrlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.CopyrightOverrideFormPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.InnerSourceRepositoryEditorPage;
import com.sonatype.clm.testing.playwright.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.playwright.pages.LegalDashboardPage;
import com.sonatype.clm.testing.playwright.pages.LegalDashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.NexusOneClassicEmbedPage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePageAssertions;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPage;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsPage;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePage;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.pages.UserManagementPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.HdsStubs;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
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

  private static final String ADMINISTRATORS_TEST_USER_ITEM = "Jane Doe (test-b)";

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
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/api"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    ApiDocumentationPageAssertions apiAssertions = new ApiDocumentationPageAssertions(new ApiDocumentationPage());

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    apiAssertions.shouldShowSwaggerLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedSuccessMetrics_rendersClassicLandingInsideNexusOneShell() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/success-metrics"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    SuccessMetricsPage successMetrics = new SuccessMetricsPage();
    SuccessMetricsPageAssertions successMetricsAssertions = new SuccessMetricsPageAssertions(successMetrics);

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
  public void testEmbeddedLegal_rendersClassicLandingInsideNexusOneShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/legal"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    LegalDashboardPage legalDashboard = new LegalDashboardPage();
    LegalDashboardPageAssertions legalAssertions = new LegalDashboardPageAssertions(legalDashboard);

    // The Coming Soon entry redirects straight to the Applications tab rather than mounting a
    // component of its own — see nexus-one/routes.tsx's NATIVE_CLASSIC_EMBED_REDIRECTS comment.
    assertThat(page).hasURL(Pattern.compile(".*/legal/applicationsDashboard.*"));

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    legalAssertions.shouldBeVisible();

    // The PR's headline behavior: tab clicks must resolve in-shell rather than failing silently
    // (the tab-switch states live in the nexus-one bundle's own router, not Classic's).
    legalDashboard.componentsTab().click();

    assertThat(page).hasURL(Pattern.compile(".*/legal/componentsDashboard.*"));
    legalAssertions.shouldShowComponentsTabActive();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();
    assertThat(embedPage.leftNavLink("Legal")).hasAttribute("aria-current", "page");

    // The filter toggle's Redux state always flipped correctly, but the drawer it opens is a
    // PortalDrawer targeting document.querySelector('.nx-page') — a class only Classic's own root
    // App.jsx provided, not the Nexus One shell. Without it here, the drawer silently rendered null.
    legalDashboard.openFilterDrawer();
    assertThat(legalDashboard.filterDrawer()).isVisible();
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
  public void testEmbeddedLegal_advancedLegalPackUnlicensed_hidesNavEntry() {
    // AC #5 (CLM-42162): the Legal rail entry only shows for tenants entitled to it — gate is
    // unchanged from Classic's own IqSidebarNav (isLicensed && isLegalEnabled).
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/home"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Legal")).not().isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedReporting_enterpriseReportingLicensed_rendersEnterpriseInShell() {
    setFeatures(LicensedFeature.values());
    stubEnterpriseReportingHds();

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/reports"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    EnterpriseReportingPageAssertions enterpriseAssertions =
        new EnterpriseReportingPageAssertions(new EnterpriseReportingPage());

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

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/reports"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OperationalReportingPageAssertions operationalAssertions =
        new OperationalReportingPageAssertions(new OperationalReportingPage());

    assertThat(embedPage.leftNav()).isVisible();
    assertThat(embedPage.leftNavLink("Operational Reporting")).isVisible();
    assertThat(embedPage.classicComponentMount()).isVisible();
    assertThat(embedPage.classicGlobalSidebar()).not().isVisible();

    operationalAssertions.shouldBeLoaded();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedOrgsAndPolicies_rendersRootOrgSummaryInsideNexusOneShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/orgs-and-policies"));

    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    OwnerSummaryPageAssertions ownerSummaryAssertions = new OwnerSummaryPageAssertions(ownerSummary);

    // The Coming Soon entry redirects straight to the root org's summary rather than mounting a
    // component of its own - see nexus-one/routes.tsx's NATIVE_CLASSIC_EMBED_REDIRECTS comment.
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
  public void testEmbeddedOrgsAndPolicies_policyEditorNavigationStaysInShell() {
    setFeatures(LicensedFeature.values());

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/orgs-and-policies"));

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

    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/coming-soon/orgs-and-policies"));

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
   * CLM-42464 dirty-guard cancel path: selecting a user from the search dropdown
   * adds them to addedUsers, setting isDirty=true; a hash navigation triggers
   * the shell dirty-guard; Cancel keeps the user on the edit page with the dirty
   * state intact.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEmbeddedAdministratorsEdit_dirtyGuardBlocksNavigationOnCancel() {
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators/" + Role.POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage editPage = new AdministratorsEditPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();

    editPage.root().waitFor();
    editPage.searchAndAddByText("*", ADMINISTRATORS_TEST_USER_ITEM);

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
    playwrightRefreshOrOpen(NexusOneClassicEmbedPage.embedUrl("/administrators/" + Role.POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage editPage = new AdministratorsEditPage();
    UnsavedChangesModalComponent modal = new UnsavedChangesModalComponent();
    NexusOneClassicEmbedPage embedPage = new NexusOneClassicEmbedPage();

    editPage.root().waitFor();
    editPage.searchAndAddByText("*", ADMINISTRATORS_TEST_USER_ITEM);

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
}
