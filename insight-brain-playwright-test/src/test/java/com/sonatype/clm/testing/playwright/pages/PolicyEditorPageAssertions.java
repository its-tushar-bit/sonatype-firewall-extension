/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link PolicyEditorPage}.
 */
public class PolicyEditorPageAssertions
{
  private final PolicyEditorPage page;

  public PolicyEditorPageAssertions(PolicyEditorPage page) {
    this.page = page;
  }

  public void shouldBeInheritedReadOnlyView() {
    assertThat(page.pageHeading()).hasText("Policy Settings");
    assertThat(page.policyName()).isDisabled();
    assertThat(page.deletePolicyButton()).hasCount(0);
  }
}
