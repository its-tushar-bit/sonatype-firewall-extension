/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqSystemPreferencesPageAssertions
{
  private final MtiqSystemPreferencesPage page;

  public MtiqSystemPreferencesPageAssertions(MtiqSystemPreferencesPage page) {
    this.page = page;
  }

  public void administratorsLinkShouldBeVisible() {
    assertThat(page.administratorsLink()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  public void baseUrlLinkShouldBeHidden() {
    assertThat(page.baseUrlLink())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void emailLinkShouldBeHidden() {
    assertThat(page.emailLink())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }
}
