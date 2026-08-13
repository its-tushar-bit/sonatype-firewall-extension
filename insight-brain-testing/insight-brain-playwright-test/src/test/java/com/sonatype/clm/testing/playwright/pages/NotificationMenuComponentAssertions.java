/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link NotificationMenuComponent}.
 */
public class NotificationMenuComponentAssertions
{
  private final NotificationMenuComponent page;

  public NotificationMenuComponentAssertions(NotificationMenuComponent page) {
    this.page = page;
  }

  public void shouldShowItemAge(int index, String expected) {
    assertThat(page.notificationItemAge(index)).containsText(expected);
  }

  public void shouldShowItemSummary(int index, String expected) {
    assertThat(page.notificationItemSummary(index)).containsText(expected);
  }

  public void shouldShowDetailModal() {
    assertThat(page.detailModal()).isVisible();
  }

  public void shouldHideDetailModal() {
    assertThat(page.detailModal()).isHidden();
  }

  public void shouldShowDetailContent(String expectedHeader, String expectedBody) {
    assertThat(page.detailHeader()).containsText(expectedHeader);
    assertThat(page.detailBody()).containsText(expectedBody);
  }

  public void shouldHideDot() {
    assertThat(page.notificationDot()).isHidden();
  }
}
