/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class IdeIntegrationsCardPageAssertions
{
  private final IdeIntegrationsCardPage page;

  public IdeIntegrationsCardPageAssertions(IdeIntegrationsCardPage page) {
    this.page = page;
  }

  public void shouldShowCardWithHeadingAndFooterLink() {
    assertThat(page.card()).isVisible();
    assertThat(page.heading()).isVisible();
    assertThat(page.footerLink()).isVisible();
  }

  public void shouldHaveFooterLinkPointingToIdeRoute() {
    // The link target resolves to the developer.dashboard.ide route (see route.js); assert the
    // href ends with the exact route fragment to avoid a match-everything regex.
    assertThat(page.footerLink()).hasAttribute("href", Pattern.compile("/developer/dashboard/ide$"));
  }

}
