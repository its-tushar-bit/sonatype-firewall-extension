/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Web-first assertions for {@link UserActivityOverviewPage}.
 */
public class UserActivityOverviewPageAssertions
{
  private final UserActivityOverviewPage page;

  public UserActivityOverviewPageAssertions(UserActivityOverviewPage page) {
    this.page = page;
  }

  /** Table is visible — basic page-load check. */
  public void shouldShowTable() {
    assertThat(page.overviewTable()).isVisible();
  }

  /** "Username" column header is visible. */
  public void shouldShowUsernameColumnHeader() {
    assertThat(page.usernameColumnHeader()).isVisible();
  }

  /** "Login Count (…)" column header is visible. */
  public void shouldShowLoginCountColumnHeader() {
    assertThat(page.loginCountColumnHeader()).isVisible();
  }

  /** "Last Active" column header is visible. */
  public void shouldShowLastActiveColumnHeader() {
    assertThat(page.lastActiveColumnHeader()).isVisible();
  }

  /** "Export Activity" button is visible. */
  public void shouldShowExportActivityButton() {
    assertThat(page.exportActivityButton()).isVisible();
  }

  /** "Filter" button is visible. */
  public void shouldShowFilterButton() {
    assertThat(page.filterButton()).isVisible();
  }

  /** "Showing N of M users" summary text is visible. */
  public void shouldShowShowingSummary() {
    assertThat(page.showingSummary()).isVisible();
  }

  /** The named user appears in the overview table. */
  public void shouldShowUser(String username) {
    assertThat(page.userRow(username)).isVisible();
  }

  /** The table shows the empty-state message — no users match current filter / search. */
  public void shouldShowEmptyState() {
    assertThat(page.emptyStateMessage()).isVisible();
  }

  /** The filter drawer is visible. */
  public void shouldShowFilterDrawer() {
    assertThat(page.filterDrawer()).isVisible();
  }

  /** The filter drawer is not visible. */
  public void shouldHideFilterDrawer() {
    assertThat(page.filterDrawer()).not().isVisible();
  }

  /** "Apply" button inside the filter drawer is disabled — filters are not dirty. */
  public void shouldShowApplyDisabled() {
    assertThat(page.filterApplyButton()).isDisabled();
  }

  /** "Apply" button inside the filter drawer is enabled — filters are dirty. */
  public void shouldShowApplyEnabled() {
    assertThat(page.filterApplyButton()).isEnabled();
  }

  /** "Reset" button inside the filter drawer is disabled — filters are not dirty. */
  public void shouldShowResetDisabled() {
    assertThat(page.filterResetButton()).isDisabled();
  }

  /** "Reset" button inside the filter drawer is enabled — filters are dirty. */
  public void shouldShowResetEnabled() {
    assertThat(page.filterResetButton()).isEnabled();
  }

  /** Stale-filter mask is visible over the table — {@code filtersAreDirty} is true. */
  public void shouldShowFilterMask() {
    assertThat(page.filterMask()).isVisible();
  }

  /** Stale-filter mask is not visible — {@code filtersAreDirty} is false. */
  public void shouldHideFilterMask() {
    assertThat(page.filterMask()).not().isVisible();
  }

  /** "Export Activity" button is disabled — no data or loading. */
  public void shouldShowExportActivityDisabled() {
    assertThat(page.exportActivityButton()).isDisabled();
  }

  /** Login Count column header has the given {@code aria-sort} attribute value. */
  public void shouldHaveLoginCountSortDirection(String direction) {
    assertThat(page.loginCountColumnHeader()).hasAttribute("aria-sort", direction);
  }
}
