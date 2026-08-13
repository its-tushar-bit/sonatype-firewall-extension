/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link AdministratorsEditPage}.
 */
public class AdministratorsEditPageAssertions
{
  private final AdministratorsEditPage page;

  public AdministratorsEditPageAssertions(AdministratorsEditPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.root()).isVisible();
  }

  public void shouldShowRoleName(String expected) {
    assertThat(page.roleName()).hasText(expected);
  }

  public void shouldShowRoleDescription(String expected) {
    assertThat(page.roleDescription()).containsText(expected);
  }

  public void shouldHaveAddedItemCount(int expected) {
    assertThat(page.addedItems()).hasCount(expected);
  }

  public void shouldHaveAddedItemTexts(String... expectedTexts) {
    assertThat(page.addedItems()).hasText(expectedTexts);
  }
}
