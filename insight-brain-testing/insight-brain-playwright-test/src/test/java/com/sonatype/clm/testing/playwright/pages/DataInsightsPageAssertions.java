/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DataInsightsPageAssertions
{
  private final DataInsightsPage page;

  public DataInsightsPageAssertions(DataInsightsPage page) {
    this.page = page;
  }

  /**
   * Only asserts the outer container — the inner {@code #labs-container} depends on the Looker
   * endpoint, which 404s under the embedded test server.
   */
  public void shouldShowContainer() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowLicenseGateError() {
    assertThat(page.container()).isVisible();
    assertThat(page.enterpriseReportingLicenseErrorMessage()).isVisible();
    assertThat(page.labsContainer()).isHidden();
  }
}
