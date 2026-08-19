/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Web-first assertions for {@link UserActivityDetailsRegressionPage}.
 */
public class UserActivityDetailsRegressionPageAssertions
{
  private final UserActivityDetailsRegressionPage page;

  public UserActivityDetailsRegressionPageAssertions(UserActivityDetailsRegressionPage page) {
    this.page = page;
  }

  /** "Timestamp" column header is visible. */
  public void shouldShowTimestampColumnHeader() {
    assertThat(page.timestampColumnHeader()).isVisible();
  }

  /** "Domain" column header is visible. */
  public void shouldShowDomainColumnHeader() {
    assertThat(page.domainColumnHeader()).isVisible();
  }

  /** "Type" column header is visible. */
  public void shouldShowTypeColumnHeader() {
    assertThat(page.typeColumnHeader()).isVisible();
  }

  /** "Error" column header is visible. */
  public void shouldShowErrorColumnHeader() {
    assertThat(page.errorColumnHeader()).isVisible();
  }

  /** "Request URI" column header is visible. */
  public void shouldShowRequestUriColumnHeader() {
    assertThat(page.requestUriColumnHeader()).isVisible();
  }

  /** "Method" column header is visible. */
  public void shouldShowMethodColumnHeader() {
    assertThat(page.methodColumnHeader()).isVisible();
  }

  /** "IP Address" column header is visible. */
  public void shouldShowIpAddressColumnHeader() {
    assertThat(page.ipAddressColumnHeader()).isVisible();
  }

  /** "User Agent" column header is visible. */
  public void shouldShowUserAgentColumnHeader() {
    assertThat(page.userAgentColumnHeader()).isVisible();
  }

  /** "Showing N activities" summary text is visible. */
  public void shouldShowActivitiesSummary() {
    assertThat(page.showingActivitiesSummary()).isVisible();
  }

  /** Back button is visible. */
  public void shouldShowBackButton() {
    assertThat(page.backButton()).isVisible();
  }

  /** "Export Activity" button is visible. */
  public void shouldShowExportActivityButton() {
    assertThat(page.exportActivityButton()).isVisible();
  }

  /** "Export Activity" button is enabled — user has activity records. */
  public void shouldShowExportActivityEnabled() {
    assertThat(page.exportActivityButton()).isEnabled();
  }

  /** "Export Activity" button is disabled — no activity records. */
  public void shouldShowExportActivityDisabled() {
    assertThat(page.exportActivityButton()).isDisabled();
  }

  /** The filter drawer is visible. */
  public void shouldShowFilterDrawer() {
    assertThat(page.filterDrawer()).isVisible();
  }

  /** "Apply" button inside the filter drawer is disabled — filters are not dirty. */
  public void shouldShowFilterApplyDisabled() {
    assertThat(page.filterApplyButton()).isDisabled();
  }

  /** "Reset" button inside the filter drawer is disabled — filters are not dirty. */
  public void shouldShowFilterResetDisabled() {
    assertThat(page.filterResetButton()).isDisabled();
  }

  /** "Activity Type" section toggle is visible inside the filter drawer. */
  public void shouldShowActivityTypeSectionToggle() {
    assertThat(page.activityTypeSectionToggle()).isVisible();
  }

  /** "Domain" section toggle is visible inside the filter drawer. */
  public void shouldShowDomainSectionToggle() {
    assertThat(page.domainSectionToggle()).isVisible();
  }

  /** "Error Type" section toggle is visible inside the filter drawer. */
  public void shouldShowErrorTypeSectionToggle() {
    assertThat(page.errorTypeSectionToggle()).isVisible();
  }

  /** Timestamp column header has the given {@code aria-sort} attribute value. */
  public void shouldHaveTimestampSortDirection(String direction) {
    assertThat(page.timestampColumnHeader()).hasAttribute("aria-sort", direction);
  }

  /** First data row's timestamp cell contains the expected text (e.g. a date portion). */
  public void shouldHaveFirstTimestampContaining(String text) {
    assertThat(page.firstTimestampCell()).containsText(text);
  }

  /** "Apply" button inside the filter drawer is enabled — filters are dirty. */
  public void shouldShowFilterApplyEnabled() {
    assertThat(page.filterApplyButton()).isEnabled();
  }

  /** "Reset" button inside the filter drawer is enabled — filters are dirty. */
  public void shouldShowFilterResetEnabled() {
    assertThat(page.filterResetButton()).isEnabled();
  }

  /** The filter drawer is not visible — it was closed. */
  public void shouldHideFilterDrawer() {
    assertThat(page.filterDrawer()).not().isVisible();
  }

  /** The stale-filter mask is visible — {@code filtersAreDirty} is true. */
  public void shouldShowFilterMask() {
    assertThat(page.filterMask()).isVisible();
  }

  /**
   * No export error alert is visible — the export completed without error.
   * The alert reads "Failed to export user activity detail data: …" on failure.
   */
  public void shouldNotShowExportError() {
    assertThat(page.exportErrorAlert()).not().isVisible();
  }

  /**
   * "Export Activity" button text has reverted from "Exporting…" to "Export Activity" after
   * export completes, confirming the Redux {@code fulfilled} action ran (not {@code rejected}).
   */
  public void shouldShowExportActivityReady() {
    assertThat(page.exportActivityButton()).hasText("Export Activity");
  }
}
