/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link ApplicationLatestEvaluationsPage}. */
public class ApplicationLatestEvaluationsPageAssertions
{
  private final ApplicationLatestEvaluationsPage page;

  public ApplicationLatestEvaluationsPageAssertions(ApplicationLatestEvaluationsPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldHaveHeadingContaining(String expectedSubstring) {
    assertThat(page.pageHeading()).containsText(expectedSubstring);
  }

  public void shouldShowStageDescription() {
    assertThat(page.stageDescription()).isVisible();
  }
}
