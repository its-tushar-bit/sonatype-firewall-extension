/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link SidebarComponent}.
 */
public class SidebarComponentAssertions
{
  private static final Pattern OPEN_CLASS_REGEX = Pattern.compile(".*\\bopen\\b.*");

  private static final Pattern CLOSED_CLASS_REGEX = Pattern.compile(".*\\bclosed\\b.*");

  private static final double STATE_TIMEOUT_MS = 15_000;

  private final SidebarComponent page;

  public SidebarComponentAssertions(SidebarComponent page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldHaveEmptyLinks() {
    assertThat(page.sidebarLinks()).isEmpty();
  }

  public void shouldShowDashboard() {
    assertThat(page.dashboardButton()).isVisible();
  }

  public void shouldBeOpen() {
    assertThat(page.container()).hasClass(OPEN_CLASS_REGEX,
        new LocatorAssertions.HasClassOptions().setTimeout(STATE_TIMEOUT_MS));
  }

  public void shouldBeClosed() {
    assertThat(page.container()).hasClass(CLOSED_CLASS_REGEX,
        new LocatorAssertions.HasClassOptions().setTimeout(STATE_TIMEOUT_MS));
  }
}
