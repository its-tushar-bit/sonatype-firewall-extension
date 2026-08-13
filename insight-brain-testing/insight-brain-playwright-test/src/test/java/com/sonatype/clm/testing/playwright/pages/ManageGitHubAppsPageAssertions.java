/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link ManageGitHubAppsPage}. */
public class ManageGitHubAppsPageAssertions
{
  private final ManageGitHubAppsPage page;

  public ManageGitHubAppsPageAssertions(ManageGitHubAppsPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.pageHeading()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void shouldShowDeleteConfirmModal() {
    assertThat(page.deleteConfirmModal()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(page.deleteConfirmModalHeading()).isVisible();
    assertThat(page.deleteConfirmButton()).isVisible();
    assertThat(page.deleteCancelButton()).isVisible();
  }

  public void shouldHideDeleteConfirmModal() {
    assertThat(page.deleteConfirmModal()).isHidden();
  }

  public void shouldHaveAppCount(int count) {
    assertThat(page.appTableRows()).hasCount(count,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void shouldShowEmptyState() {
    assertThat(page.emptyStateParagraph()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }
}
