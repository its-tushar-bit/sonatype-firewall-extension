/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ApiDocumentationPageAssertions
{

  private static final LocatorAssertions.IsVisibleOptions SWAGGER_LOAD_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private final ApiDocumentationPage page;

  public ApiDocumentationPageAssertions(ApiDocumentationPage page) {
    this.page = page;
  }

  public void shouldShowSwaggerLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.firstOpblockSummary()).isVisible(SWAGGER_LOAD_OPTS);
  }

  public void shouldShowExecuteButtonReady() {
    assertThat(page.executeButton()).isVisible(SWAGGER_LOAD_OPTS);
  }
}
