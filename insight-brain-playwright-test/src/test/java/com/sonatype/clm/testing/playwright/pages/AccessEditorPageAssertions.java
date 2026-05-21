/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link AccessEditorPage}.
 */
public class AccessEditorPageAssertions
{
  private final AccessEditorPage page;

  public AccessEditorPageAssertions(AccessEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.root()).isVisible();
    assertThat(page.form()).isVisible();
  }

  public void shouldHaveAssociatedMember(String memberText) {
    assertThat(page.associatedMembers().filter(new Locator.FilterOptions().setHasText(memberText)).first())
        .isVisible();
  }

  public void shouldNotShowSubmitError() {
    assertThat(page.submitError()).isHidden();
  }
}
