/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.PlaywrightException;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Organization and application smoke tests using Playwright and TemporaryEntity.
 * <p>
 * UI tests rely on {@link #openDashboardAndLoginAsAdmin()} (dashboard + admin session). Pure data
 * tests still run login;
 * split into another class if you need faster runs without browser setup.
 * <p>
 * Manual pause: {@code -Dplaywright.manualPause=true} (headed mode is forced when that flag is on).
 */
public class OrganizationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String AUTOMATION_TEST_ORG_NAME = "Automation Test Org";

  private static final String PW_APP_ORG_NAME = "PW App Org";

  private static final String PW_TEST_APP_NAME = "PW Test App";

  private static final String PW_TEST_APP_PUBLIC_ID = "pw-test-app";

  private static final String PW_ORG_ALPHA_NAME = "PW Org Alpha";

  private static final String PW_ORG_BETA_NAME = "PW Org Beta";

  private static final String PW_ORG_GAMMA_NAME = "PW Org Gamma";

  private static final String TEMPORARY_ORG_NAME = "Temporary Org";

  private static final String TEMPORARY_APP_NAME = "Temporary App";

  private static final String TEMPORARY_APP_PUBLIC_ID = "temp-app";

  @BeforeEach
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  /**
   * Root org owner summary ({@link OwnerSummaryPage#urlToRootOrg()}).
   * Load wait is included in {@link #playwrightNavigateTo(String)}.
   */
  private void navigateToRootOrganizationSummary() {
    playwrightNavigateTo(OwnerSummaryPage.urlToRootOrg());
  }

  private void assertRootOrgOwnerSummaryVisible() {
    new HeaderComponentAssertions(new HeaderComponent()).shouldBeLoggedIn();
    new OwnerSummaryPageAssertions(new OwnerSummaryPage()).shouldBeVisible();
  }

  private void assertOrganizationNameMatchesFixture(Organization org, String expectedName) {
    assertThat(org.getName()).isEqualTo(expectedName);
  }

  private void assertThreeOrganizationsMatchFixtureNames(
      Organization org1,
      Organization org2,
      Organization org3)
  {
    assertThat(org1.getName()).isEqualTo(PW_ORG_ALPHA_NAME);
    assertThat(org2.getName()).isEqualTo(PW_ORG_BETA_NAME);
    assertThat(org3.getName()).isEqualTo(PW_ORG_GAMMA_NAME);
  }

  private void assertApplicationMatchesFixture(Application app) {
    assertThat(app.getName()).isEqualTo(PW_TEST_APP_NAME);
    assertThat(app.getPublicId()).isEqualTo(PW_TEST_APP_PUBLIC_ID);
  }

  private void assertOrgAndApplicationNonNull(Organization org, Application app) {
    assertThat(org).isNotNull();
    assertThat(app).isNotNull();
  }

  /**
   * Skips the test when the owner-summary nav pill is absent (feature/license not enabled). Pill
   * {@code data-testid} is {@code {pillTargetId}-button}.
   */
  private void assumeOwnerSummaryFeaturePillVisible(String pillTargetId, String featureLabel) {
    try {
      page.getByTestId(pillTargetId + "-button").waitFor();
    }
    catch (PlaywrightException e) {
      Assumptions.assumeTrue(false, featureLabel + " is not available for this license or configuration");
    }
  }

  @Test
  @Tag("sanity")
  public void testCreateOrganizationWithTemporaryEntity() {
    Organization org = tempEntity.newOrganization(AUTOMATION_TEST_ORG_NAME);
    assertOrganizationNameMatchesFixture(org, AUTOMATION_TEST_ORG_NAME);

    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
  }

  @Test
  @Tag("sanity")
  public void testCreateApplicationWithTemporaryEntity() {
    Organization org = tempEntity.newOrganization(PW_APP_ORG_NAME);
    Application app = tempEntity.newApplication(PW_TEST_APP_NAME, PW_TEST_APP_PUBLIC_ID, org.getId());

    assertApplicationMatchesFixture(app);
  }

  @Test
  @Tag("sanity")
  public void testCreateMultipleOrganizations() {
    Organization org1 = tempEntity.newOrganization(PW_ORG_ALPHA_NAME);
    Organization org2 = tempEntity.newOrganization(PW_ORG_BETA_NAME);
    Organization org3 = tempEntity.newOrganization(PW_ORG_GAMMA_NAME);

    assertThreeOrganizationsMatchFixtureNames(org1, org2, org3);
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    playwrightManualPauseIfEnabled();
  }

  /**
   * Root org owner summary: open the Policies scroll-tab, then assert the Policies tile (title, add
   * button, policy table or empty list). Login is covered by {@link #openDashboardAndLoginAsAdmin()}.
   * UI checks live on {@link OwnerSummaryPage} per {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md} §4–§5.
   */
  @Test
  @Tag("sanity")
  public void testOrgPolicies() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openPoliciesSectionFromNavPills();
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowPoliciesTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgLegacyViolations() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(OwnerSummaryPage.OWNER_PILL_LEGACY_VIOLATIONS, "Legacy Violations");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LEGACY_VIOLATIONS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowLegacyViolationsTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgContinuousMonitoring() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(OwnerSummaryPage.OWNER_PILL_CONTINUOUS_MONITORING, "Continuous monitoring");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_CONTINUOUS_MONITORING);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowContinuousMonitoringTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgProprietaryComponents() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(OwnerSummaryPage.OWNER_PILL_PROPRIETARY_COMPONENTS, "Proprietary Components");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_PROPRIETARY_COMPONENTS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowProprietaryComponentsTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgComponentLabels() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowComponentLabelsTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgLicenseThreatGroups() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowLicenseThreatGroupsTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgInnerSourceRepository() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(
        OwnerSummaryPage.OWNER_PILL_INNERSOURCE_REPOSITORY, "InnerSource Repositories");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_INNERSOURCE_REPOSITORY);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowInnerSourceRepositoryTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgAccess() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_ACCESS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowAccessTile();
  }

  @Test
  @Tag("sanity")
  public void testOrgAutoWaivers() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(OwnerSummaryPage.OWNER_PILL_AUTO_WAIVERS, "Auto-Waivers");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_AUTO_WAIVERS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowAutoWaiversTile();
  }

  /**
   * Root org owner summary: open the page-title "Actions" dropdown ({@code #iq-owner-actions-dropdown})
   * and verify the org-only options: <em>Org ID to Clipboard</em>, <em>Edit Org Name / Icon</em>,
   * <em>Import Policies</em>. UI assertions live on {@link OwnerSummaryPage} per
   * {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md} §4–§5.
   */
  @Test
  @Tag("sanity")
  public void testOrgActionsDropdown() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerActionsDropdown();
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowOrganizationActionsMenu();
  }

  @Test
  @Tag("sanity")
  public void testOrganizationAutoCleanup() {
    Organization org = tempEntity.newOrganization(TEMPORARY_ORG_NAME);
    Application app = tempEntity.newApplication(
        TEMPORARY_APP_NAME, TEMPORARY_APP_PUBLIC_ID, org.getId());

    assertOrgAndApplicationNonNull(org, app);
  }
}
