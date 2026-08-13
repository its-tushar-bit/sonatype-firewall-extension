/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link SonatypeDeveloperRegressionPage}. */
public class SonatypeDeveloperRegressionAssertions
{
  private final SonatypeDeveloperRegressionPage page;

  public SonatypeDeveloperRegressionAssertions(SonatypeDeveloperRegressionPage page) {
    this.page = page;
  }

  public void shouldShowCiCdCardLearnMoreLink() {
    assertThat(page.ciCdCardLearnMoreLink()).isVisible();
  }

  public void shouldShowScmCardLearnMoreLink() {
    assertThat(page.scmCardLearnMoreLink()).isVisible();
  }

  public void shouldShowIdeCardLearnMoreLink() {
    assertThat(page.ideCardLearnMoreLink()).isVisible();
  }

  public void shouldShowPrioritiesPageSummary() {
    assertThat(page.prioritiesPageSummarySection()).isVisible();
  }

  public void shouldShowLoadError() {
    assertThat(page.loadError()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
  }
}
