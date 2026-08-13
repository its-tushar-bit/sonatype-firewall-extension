/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link EditAllObligationsModal}.
 */
public class EditAllObligationsModalAssertions
{
  private final EditAllObligationsModal page;

  public EditAllObligationsModalAssertions(EditAllObligationsModal page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.modal()).isVisible();
  }

  public void shouldBeHidden() {
    assertThat(page.modal()).isHidden();
  }

  public void shouldShowDefaultStatus(String expectedStatus) {
    assertThat(page.statusDropdown()).hasText(expectedStatus);
  }

  public void shouldHaveEmptyComment() {
    assertThat(page.commentInput()).isEmpty();
  }

  public void shouldShowScopeContaining(String expectedText) {
    assertThat(page.scopeSelectedOption()).containsText(expectedText);
  }

  public void shouldShowScopeValue(String expectedOrgId) {
    assertThat(page.scopeDropdown()).hasValue(expectedOrgId);
  }
}
