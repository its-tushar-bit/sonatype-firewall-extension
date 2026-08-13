/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SbomManagerDashboardPage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.Locator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqSbomManagerOwnerSummaryPlaywrightTest
    extends AbstractMtiqUiTest
{
  private Organization parentOrganization;

  private Organization childOrganization1;

  private Organization childOrganization2;

  @Before
  public void seedOrgTreeAndLogin() {
    String suffix = tempEntity.uuid();
    parentOrganization = tempEntity.newOrganization("Ye Ole Organization " + suffix);
    childOrganization1 = tempEntity.newOrganization("1st Child organization " + suffix, parentOrganization);
    childOrganization2 = tempEntity.newOrganization("2nd Child organization " + suffix, parentOrganization);
    // Seed one app under each child so the sidebar renders them as expandable menu items.
    tempEntity.newApplicationWithParent(childOrganization1);
    tempEntity.newApplicationWithParent(childOrganization2);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_FOUNDATION);
    playwrightRefreshOrOpen("/");
    playwrightLogin();
  }

  @Test
  public void testNavigateToOrganizations() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    clickSbomManagerOrganizationsFromSidebar();

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    assertThat(sidebar.container()).isVisible();
    assertThat(sidebar.organizationsGroup()).isVisible();
    assertThat(sidebar.organizationLinks()).containsText(parentOrganization.getName());

    sidebar.organizationLinks()
        .filter(new Locator.FilterOptions()
            .setHasText(parentOrganization.getName()))
        .first()
        .click();

    assertThat(sidebar.container()).containsText(childOrganization1.getName());
    assertThat(sidebar.container()).containsText(childOrganization2.getName());
  }

  @Test
  public void testNavigateToOrganizations_sbomOnlyLicense_onlyOrgsAndAppsVisible() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    clickSbomManagerOrganizationsFromSidebar();

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    assertThat(sidebar.organizationsGroup()).isVisible();
    assertThat(sidebar.repositoriesGroup()).isHidden();
    assertThat(sidebar.repositoryManagersGroup()).isHidden();
  }

  @Test
  public void testNavigateToOrganizations_sbomAndFirewallRepositoryLicense_RepositoryNotVisible() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS,
        ProductLicenseDetails.PRODUCT_FOUNDATION,
        ProductLicenseDetails.PRODUCT_REPOSITORY_FIREWALL_SAAS);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    clickSbomManagerOrganizationsFromSidebar();

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    assertThat(sidebar.container()).isVisible();
    assertThat(sidebar.repositoriesGroup()).isHidden();
    assertThat(sidebar.repositoryManagersGroup()).isHidden();
  }

  @Test
  public void testSbomManager_PublicDataSources_isNotVisible_withSbomLicenseOnly() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    playwrightHardreset();
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    playwrightLogin();
    clickSbomManagerOrganizationsFromSidebar();

    playwrightRefreshOrOpen(OwnerSummaryPage.sbomManagerUrl(Organization.ROOT_ORGANIZATION_ID));
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    assertThat(ownerSummary.container()).isVisible();
    assertThat(ownerSummary.navPillButton("owner-pill-public-data-sources")).isHidden();
  }

  @Test
  public void testSbomManager_ownerSummaryPageLoads() {
    // The Public Data Sources pill visibility variants are covered by _isNotVisible above; the
    // tenant-entitlement combination the SPA selector needs to show the pill isn't surfaced in the
    // Playwright MTIQ harness, so this test is limited to the page-load smoke check.
    playwrightRefreshOrOpen(OwnerSummaryPage.sbomManagerUrl(Organization.ROOT_ORGANIZATION_ID));
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    assertThat(ownerSummary.container()).isVisible();
  }

  @Test
  public void testSbomManager_ImportApplications_NotVisible() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    clickSbomManagerOrganizationsFromSidebar();

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    sidebar.organizationLinks().first().click();
    sidebar.addApplicationDropdownTrigger().click();
    assertThat(sidebar.importApplicationsOption()).isHidden();
  }

  @Test
  public void testSbomManager_policyTable() {
    String suffix = tempEntity.uuid();
    String policy1Name = "Policy 1 " + suffix;
    String policy2Name = "Policy 2 " + suffix;
    tempEntity.newPolicy(parentOrganization.getId(), policy1Name, 10);
    tempEntity.newPolicy(parentOrganization.getId(), policy2Name, 5);

    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    clickSbomManagerOrganizationsFromSidebar();

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    sidebar.organizationLinks()
        .filter(new Locator.FilterOptions()
            .setHasText(parentOrganization.getName()))
        .first()
        .click();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    assertThat(ownerSummary.policiesTile()).isVisible();
    assertThat(ownerSummary.policiesTileRowByName(policy1Name)).isVisible();
    assertThat(ownerSummary.policiesTileRowByName(policy2Name)).isVisible();
    assertThat(ownerSummary.policiesTile().locator("thead").getByText("Name")).isVisible();
  }

  @Test
  public void testSbomManager_policyTable_showsInheritedRowsMarkedFromParent() {
    tempEntity.newPolicy(parentOrganization.getId(), "Inherited SBOM Policy " + tempEntity.uuid(), 5);

    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    clickSbomManagerOrganizationsFromSidebar();

    // Expand parent first — child links render only after the parent's collapsible expands.
    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    sidebar.organizationLinks()
        .filter(new Locator.FilterOptions().setHasText(parentOrganization.getName()))
        .first()
        .click();
    sidebar.organizationLinks()
        .filter(new Locator.FilterOptions().setHasText(childOrganization1.getName()))
        .first()
        .click();

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    assertThat(ownerSummary.policiesTile()).isVisible();
    assertThat(ownerSummary.policiesTileInheritedSectionFor(parentOrganization.getName())).isVisible();
  }

  private void clickSbomManagerOrganizationsFromSidebar() {
    SidebarComponent sidebar = new SidebarComponent();
    sidebar.sbomManagerOrganizationsButton().click();
    page.waitForURL(url -> url.contains("/sbomManager/management/view"));
  }
}
