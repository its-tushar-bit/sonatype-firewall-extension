/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PrioritiesPageAssertions
{
  // Priority column is icon-only (aria-label="Priority"); text-cell headers only.
  private static final String[] EXPECTED_TEXT_COLUMN_HEADERS = {
    "Component", "Build Action", "Reachability", "Suggested Remediation", "Next Step"
  };

  private final PrioritiesPage page;

  public PrioritiesPageAssertions(PrioritiesPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowExpiredWaiverIconOnRow(Locator row) {
    assertThat(page.expiredWaiverIcon(row)).isVisible();
  }

  public void shouldShowSoonToExpireWaiverIconOnRow(Locator row) {
    assertThat(page.soonToExpireWaiverIcon(row)).isVisible();
  }

  public void shouldShowPageHeader() {
    assertThat(page.pageHeader()).isVisible();
    assertThat(page.metadataRowTriggeredByLabel()).isVisible();
  }

  public void shouldShowBreadcrumbLink(String linkText) {
    assertThat(page.breadcrumbLink(linkText)).isVisible();
  }

  public void shouldHaveHeaderTitleText(String expectedTitle) {
    assertThat(page.pageHeaderTitle()).hasText(expectedTitle);
  }

  public void shouldShowTableColumnHeaders() {
    for (String label : EXPECTED_TEXT_COLUMN_HEADERS) {
      assertThat(page.columnHeaderByText(label)).isVisible();
    }
  }

  /** Call {@link PrioritiesPage#openViewDropdown()} before invoking this. */
  public void shouldShowViewDropdownLinks() {
    assertThat(page.viewDropdownLifecycleReportLink()).isVisible();
    assertThat(page.viewDropdownDependenciesLink()).isVisible();
  }

  public void shouldShowFullyWaivedLabelOnRow(Locator row) {
    assertThat(page.buildActionCell(row).getByText("Waived")).isVisible();
  }

  public void shouldShowWaiveViolationsRecommendationOnRow(Locator row) {
    assertThat(row.getByText("Waive violations")).isVisible();
    // NxTooltip portal mounts on hover — trigger it, then verify the nudge-auto-waiver text.
    page.recommendationCell(row).hover();
    assertThat(page.tooltip()).containsText("Ask an administrator to configure Automated Waivers");
  }

  public void shouldShowAtLeastOneDependencyIndicatorInTable() {
    // Canned report yields multiple Direct and Transitive indicators; .first() avoids strict-mode collision.
    assertThat(page.dependencyIndicatorByTitle("Direct Dependency").first()).isVisible();
    assertThat(page.dependencyIndicatorByTitle("Transitive Dependency").first()).isVisible();
  }

  public void shouldShowLicenseLockScreen() {
    assertThat(page.licenseLockScreen()).isVisible();
  }

  /** Runtime filter value may contain regex metacharacters — escape for JS RegExp. */
  public void shouldHaveComponentNameFilterUrlParam(String expectedFilter) {
    assertThat(page.playwrightPage()).hasURL(
        Pattern.compile(".*componentNameFilter=" + BasePage.escapeForJsRegex(expectedFilter) + ".*"));
  }

  public void shouldHaveFilterOnPolicyActionsUrlParamOn() {
    assertThat(page.playwrightPage()).hasURL(Pattern.compile(".*filterOnPolicyActions=true.*"));
  }
}
