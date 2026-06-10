/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link RevokeLegacyViolationModalPage}.
 */
public class RevokeLegacyViolationModalPageAssertions
{
  private final RevokeLegacyViolationModalPage page;

  public RevokeLegacyViolationModalPageAssertions(RevokeLegacyViolationModalPage page) {
    this.page = page;
  }

  /** Asserts the Revoke Legacy Violation modal is visible with the expected heading. */
  public void shouldBeVisible() {
    assertThat(page.modal()).isVisible();
    assertThat(page.heading()).hasText("Revoke Legacy Violation Status");
  }
}
