/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * MTIQ-specific assertions for {@link SourceControlConfigurationPage} — verifies the provider
 * dropdown is filtered to exactly four entries plus the placeholder (5 total).
 */
public class MtiqSourceControlConfigurationPageAssertions
{
  private final SourceControlConfigurationPage page;

  public MtiqSourceControlConfigurationPageAssertions(SourceControlConfigurationPage page) {
    this.page = page;
  }

  public void shouldListOnlyMtiqProviders() {
    Locator options = page.providerSelect().locator("option");
    // 4 MTIQ-allowed providers + 1 "-- Not Configured --" placeholder = 5 total.
    assertThat(options).hasCount(5,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(options.filter(new Locator.FilterOptions().setHasText("Azure DevOps"))).hasCount(1);
    assertThat(options.filter(new Locator.FilterOptions().setHasText("Bitbucket"))).hasCount(1);
    // Exact pattern avoids substring collision between "GitHub" and "GitLab".
    assertThat(options.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^GitHub$")))).hasCount(1);
    assertThat(options.filter(new Locator.FilterOptions().setHasText("GitLab"))).hasCount(1);
  }
}
