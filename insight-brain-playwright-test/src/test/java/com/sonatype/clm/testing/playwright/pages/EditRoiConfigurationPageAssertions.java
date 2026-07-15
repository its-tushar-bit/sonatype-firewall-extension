/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class EditRoiConfigurationPageAssertions
{
  private final EditRoiConfigurationPage page;

  public EditRoiConfigurationPageAssertions(EditRoiConfigurationPage page) {
    this.page = page;
  }

  public void shouldShowFirewallInputs() {
    assertThat(page.container()).isVisible();
    assertThat(page.malwareAttacksPreventedInput()).isVisible();
    assertThat(page.namespaceAttacksPreventedInput()).isVisible();
    assertThat(page.safeComponentsAutoSelectedInput()).isVisible();
  }

  public void shouldShowRestoreDefaultsModal() {
    assertThat(page.restoreDefaultsModal()).isVisible();
  }

  /**
   * Playwright web-first sync barrier used after a save action. NxLoadWrapper unmounts the
   * form children during the save round-trip; the Update button re-appearing signals the
   * save has settled and the next action can proceed.
   */
  public void shouldHaveSettledAfterSave() {
    assertThat(page.updateButton()).isVisible();
  }
}
