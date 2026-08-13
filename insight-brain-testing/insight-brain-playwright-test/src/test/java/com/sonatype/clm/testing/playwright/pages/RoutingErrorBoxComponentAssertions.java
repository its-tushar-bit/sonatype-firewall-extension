/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link RoutingErrorBoxComponent}.
 */
public class RoutingErrorBoxComponentAssertions
{
  private final RoutingErrorBoxComponent page;

  public RoutingErrorBoxComponentAssertions(RoutingErrorBoxComponent page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.errorBox())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  public void shouldBeHidden() {
    assertThat(page.errorBox())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  public void shouldHaveErrorText(String expectedText) {
    assertThat(page.errorBox()).containsText(expectedText,
        new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }
}
