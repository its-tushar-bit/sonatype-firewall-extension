/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LegalDashboardPageAssertions
{
  private final LegalDashboardPage page;

  public LegalDashboardPageAssertions(LegalDashboardPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
    assertThat(page.applicationsTab()).hasAttribute("aria-selected", "true");
  }

  public void shouldShowApplicationsTableWithRows() {
    assertThat(page.applicationsTable()).isVisible();
    assertThat(page.applicationsTableRows().first()).isVisible();
  }

  public void shouldShowApplicationsTableWithRowCount(int expectedCount) {
    assertThat(page.applicationsTableRows()).hasCount(expectedCount);
  }

  public void shouldShowComponentsTabActive() {
    assertThat(page.componentsTab()).hasAttribute("aria-selected", "true");
  }

  public void shouldShowComponentsTableWithRows() {
    assertThat(page.componentsTable()).isVisible();
    assertThat(page.componentsTableRows().first()).isVisible();
  }

  public void shouldShowComponentsTableWithRowCount(int expectedCount) {
    assertThat(page.componentsTableRows()).hasCount(expectedCount);
  }

  public void shouldShowFilterDirtyAsterisk() {
    assertThat(page.filterDirtyAsterisk()).isVisible();
  }

  public void shouldNotShowFilterDirtyAsterisk() {
    assertThat(page.filterDirtyAsterisk()).isHidden();
  }

  public void shouldShowPagination() {
    assertThat(page.applicationsPagination()).isVisible();
  }

  public void shouldShowAppCategoriesColumn() {
    assertThat(page.appCategoriesColumnHeader()).isVisible();
  }

  public void shouldShowAllApplicationsColumns() {
    assertThat(page.applicationNameColumnHeader()).isVisible();
    assertThat(page.lastScanTimeColumnHeader()).isVisible();
    assertThat(page.appCategoriesColumnHeader()).isVisible();
    assertThat(page.componentsReviewedColumnHeader()).isVisible();
    assertThat(page.applicationsChevronColumnHeader()).isVisible();
  }

  public void shouldShowAllComponentsColumns() {
    assertThat(page.componentNameColumnHeader()).isVisible();
    assertThat(page.licenseColumnHeader()).isVisible();
    assertThat(page.applicationCountColumnHeader()).isVisible();
    assertThat(page.componentObligationsColumnHeader()).isVisible();
    assertThat(page.componentsActionColumnHeader()).isVisible();
  }

  public void shouldHaveColumnSortDir(Locator columnHeader, String expectedDir) {
    assertThat(columnHeader).hasAttribute("aria-sort", expectedDir);
  }

  public void shouldShowComponentsPagination() {
    assertThat(page.componentsPagination()).isVisible();
  }

  public void shouldShowCreateAttributionReportDisabled() {
    assertThat(page.createAttributionReportButton()).isDisabled();
  }

  public void shouldShowAttributionReportModal() {
    assertThat(page.createReportGenerateButton()).isVisible();
    assertThat(page.createReportCancelButton()).isVisible();
  }
}
