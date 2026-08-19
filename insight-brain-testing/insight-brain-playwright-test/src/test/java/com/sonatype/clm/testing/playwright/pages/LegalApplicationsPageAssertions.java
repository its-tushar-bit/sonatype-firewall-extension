/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link LegalApplicationsPage}.
 */
public class LegalApplicationsPageAssertions
{
  private final LegalApplicationsPage page;

  public LegalApplicationsPageAssertions(LegalApplicationsPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.title()).hasText("Legal Obligations");
    assertThat(page.tableBodyRows().first()).isVisible();
  }

  public void shouldShowColumnHeader(String columnText) {
    assertThat(page.columnHeader(columnText)).isVisible();
  }

  public void shouldShowAppRow(String appName) {
    assertThat(page.appRow(appName)).isVisible();
  }
}
