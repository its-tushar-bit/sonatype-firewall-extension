/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqHeaderDivergencesPageAssertions
{
  private final MtiqHeaderDivergencesPage page;

  public MtiqHeaderDivergencesPageAssertions(MtiqHeaderDivergencesPage page) {
    this.page = page;
  }

  public void footerVersionTextShouldBeHidden() {
    assertThat(page.footerVersionText())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void notificationsMenuButtonShouldBeHidden() {
    assertThat(page.notificationsMenuButton())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /** Button is {@code disabled={!serverData}} until {@code GET /api/v2/config/mail} responds. */
  public void deleteConfigButtonShouldBeEnabled() {
    assertThat(page.deleteConfigButton())
        .isEnabled(new LocatorAssertions.IsEnabledOptions()
            .setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
  }

  public void deleteModalShouldBeVisible() {
    assertThat(page.deleteModal()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  public void deleteModalWarningShouldContain(String expectedText) {
    assertThat(page.deleteModalWarning()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(page.deleteModalWarning()).containsText(expectedText);
  }
}
