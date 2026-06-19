/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WaivedComponentUpgradesPageAssertions
{
  private final WaivedComponentUpgradesPage page;

  public WaivedComponentUpgradesPageAssertions(WaivedComponentUpgradesPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.tileHeading()).isVisible();
    assertThat(page.monitoringToggle()).isVisible();
    assertThat(page.updateButton()).isVisible();
    assertThat(page.cancelButton()).isVisible();
  }

  public void shouldHaveMonitoringToggleChecked() {
    assertThat(page.monitoringToggleInput()).isChecked();
  }

  public void shouldHaveMonitoringToggleUnchecked() {
    assertThat(page.monitoringToggleInput()).not().isChecked();
  }
}
