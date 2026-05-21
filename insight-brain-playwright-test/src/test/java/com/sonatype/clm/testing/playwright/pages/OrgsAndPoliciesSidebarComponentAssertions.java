/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link OrgsAndPoliciesSidebarComponent}.
 */
public class OrgsAndPoliciesSidebarComponentAssertions
{
  private final OrgsAndPoliciesSidebarComponent page;

  public OrgsAndPoliciesSidebarComponentAssertions(OrgsAndPoliciesSidebarComponent page) {
    this.page = page;
  }

  public void shouldBeVisibleWithSelectedOwner() {
    assertThat(page.container()).isVisible();
    assertThat(page.selectedOwner()).isVisible();
  }
}
