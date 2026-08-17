/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.SbomManagerManagementTreePage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the SBOM Manager Management Tree page
 * ({@code #/sbomManager/management/tree}, license-gated).
 */
public class SbomManagerManagementTreePlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  @BeforeEach
  public void seedAndNavigate() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);

    tempEntity.newOrganization();

    playwrightRefreshOrOpen(SbomManagerManagementTreePage.url());
    playwrightLogin();
  }

  /** OwnersTreePage renders with heading, filter input, expand/collapse buttons, and ≥1 tree item. */
  @Test
  @Tag("regression")
  public void testManagementTree_rendersWithInheritanceHierarchy() {
    SbomManagerManagementTreePage treePage = new SbomManagerManagementTreePage();

    assertThat(treePage.heading())
        .containsText("Inheritance Hierarchy");
    assertThat(treePage.filterInput())
        .isVisible(VISIBLE_OPTS);
    assertThat(treePage.expandAllButton())
        .isVisible(VISIBLE_OPTS);
    assertThat(treePage.collapseAllButton())
        .isVisible(VISIBLE_OPTS);
    assertThat(treePage.treeItemLabels().first())
        .isVisible(VISIBLE_OPTS);
  }

  /**
   * Dashboard → Solution Switcher → "SBOM Manager" → sidebar "Organizations" navigates to the
   * Organization Management page. Sidebar "Organizations" routes to
   * {@code sbomManager.management.view}, not the tree view — the tree itself is covered by
   * {@link #testManagementTree_rendersWithInheritanceHierarchy}.
   */
  @Test
  @Tag("regression")
  public void testSolutionSwitcherAndSidebar_navigateToOrganizationManagement() {
    playwrightRefreshOrOpen(DashboardPage.url());

    SbomManagerManagementTreePage treePage = new SbomManagerManagementTreePage();
    treePage.solutionSwitcherToggle().click();
    treePage.solutionSwitcherSbomManagerLink().click();

    SidebarComponent sidebar = new SidebarComponent();
    sidebar.sbomManagerOrganizationsButton().click();
    page.waitForURL("**/sbomManager/management/view**");

    assertThat(treePage.ownerManagerContainer()).isVisible(VISIBLE_OPTS);
  }
}
