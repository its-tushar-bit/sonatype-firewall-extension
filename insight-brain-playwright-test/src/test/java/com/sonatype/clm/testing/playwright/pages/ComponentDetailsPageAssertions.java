/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertions companion for {@link ComponentDetailsPage}.
 */
public class ComponentDetailsPageAssertions
{
  private final ComponentDetailsPage page;

  public ComponentDetailsPageAssertions(ComponentDetailsPage page) {
    this.page = page;
  }

  public void shouldShowHeaderTitle() {
    assertThat(page.headerTitle()).isVisible();
  }

  public void shouldHaveComponentTabCount(int expectedCount) {
    assertThat(page.componentDetailsTabs()).hasCount(expectedCount);
  }

  public void shouldShowSecurityTabPanel() {
    assertThat(page.securityTabPanel()).isVisible();
  }

  public void shouldShowVulnerabilityTableWithRowCount(int expectedRowCount) {
    assertThat(page.iqVulnerabilityTable()).isVisible();
    assertThat(page.iqVulnerabilityTableBodyRows()).hasCount(expectedRowCount);
  }

  public void shouldShowLegalTabWithLicenseDetections() {
    assertThat(page.legalTabPanel()).isVisible();
    assertThat(page.licenseDetectionsTile()).isVisible();
  }

  public void shouldShowViolationsTabContent() {
    assertThat(page.violationsTileTitle()).isVisible();
  }

  public void shouldShowSecurityTabContent() {
    assertThat(page.vulnerabilitiesTileTitle()).isVisible();
  }

  public void shouldShowLegalTabContent() {
    assertThat(page.legalLicenseDetectionsTile()).isVisible();
  }

  public void shouldShowOverviewTabContent() {
    assertThat(page.overviewComponentInformationTile()).isVisible();
  }

  public void shouldShowOverviewForVersionGraph(boolean expectVersionExplorerTile) {
    this.shouldShowOverviewTabContent();
    if (expectVersionExplorerTile) {
      this.shouldShowVersionExplorerTile();
    }
  }

  public void shouldShowLabelsTabContent() {
    assertThat(page.labelsTileTitle()).isVisible();
  }

  public void shouldShowAuditLogTabContent() {
    assertThat(page.auditLogTable()).isVisible();
  }

  public void shouldShowVersionExplorerTile() {
    assertThat(page.versionExplorerTile()).isVisible();
    assertThat(page.versionExplorerTileHeader()).containsText("Version Explorer");
  }

  public void shouldShowRecommendedVersionsList() {
    assertThat(page.recommendedVersionsList()).isVisible();
  }
}
