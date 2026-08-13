/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link HeaderRegressionComponent}.
 */
public class HeaderRegressionAssertions
{
  private final HeaderRegressionComponent page;

  public HeaderRegressionAssertions(HeaderRegressionComponent page) {
    this.page = page;
  }

  public void shouldShowHelpMenuDropdown() {
    assertThat(page.helpMenuDropdown()).isVisible();
  }

  public void shouldShowGettingStartedLink() {
    assertThat(page.gettingStartedLink()).isVisible();
  }

  /** Divergence: manual says "Documentation"; live label is "Online Help". */
  public void shouldShowOnlineHelpLink() {
    assertThat(page.onlineHelpLink()).isVisible();
  }

  /** Divergence: manual says "support"; live label is "Request Support". */
  public void shouldShowRequestSupportLink() {
    assertThat(page.requestSupportLink()).isVisible();
  }

  /** Asserts the named System Preferences menu item is visible in the open dropdown. */
  public void shouldShowSystemConfigMenuLink(String label) {
    assertThat(page.systemConfigMenuLink(label)).isVisible();
  }

}
