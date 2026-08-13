/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CrowdConfigurationPageAssertions
{
  private final CrowdConfigurationPage page;

  public CrowdConfigurationPageAssertions(CrowdConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.serverUrl()).isVisible();
    assertThat(page.applicationName()).isVisible();
    assertThat(page.applicationPassword()).isVisible();
    assertThat(page.saveButton()).isVisible();
    assertThat(page.deleteButton()).isVisible();
  }

  public void shouldShowSaveButtonEnabled() {
    assertThat(page.saveButton()).isEnabled();
  }

  public void shouldShowSaveButtonDisabled() {
    assertThat(page.saveButton()).isDisabled();
  }

  public void shouldShowDeleteButtonEnabled() {
    assertThat(page.deleteButton()).isEnabled();
  }

  public void shouldShowDeleteButtonDisabled() {
    assertThat(page.deleteButton()).isDisabled();
  }

  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }
}
