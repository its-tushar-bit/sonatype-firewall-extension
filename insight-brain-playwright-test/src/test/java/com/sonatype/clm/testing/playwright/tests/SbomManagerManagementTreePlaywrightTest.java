/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.SbomManagerManagementTreePage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Regression tests for the SBOM Manager Management Tree page
 * ({@code #/sbomManager/management/tree}, license-gated).
 */
public class SbomManagerManagementTreePlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  @Before
  public void seedAndNavigate() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);

    tempEntity.newOrganization();

    playwrightRefreshOrOpen(SbomManagerManagementTreePage.url());
    playwrightLogin();
  }

  /**
   * OwnersTreePage renders with heading, filter input, expand/collapse buttons, and ≥1 tree item. "Can Be
   * Automated: No" overridden.
   */
  @Test
  @Category(RegressionTest.class)
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
}
