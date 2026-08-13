/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link ZscalerConfigurationPage}. */
public class ZscalerConfigurationPageAssertions
{
  private final ZscalerConfigurationPage page;

  public ZscalerConfigurationPageAssertions(ZscalerConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.username()).isVisible();
    assertThat(page.password()).isVisible();
    assertThat(page.hostname()).isVisible();
    assertThat(page.apiKey()).isVisible();
    assertThat(page.configuredFormatsFieldset()).isVisible();
    assertThat(page.eulaCheckbox()).isVisible();
    assertThat(page.saveButton()).isVisible();
  }

  public void shouldShowDeleteButtonDisabled() {
    assertThat(page.deleteButton()).isDisabled();
  }

  public void shouldShowDeleteButtonEnabled() {
    assertThat(page.deleteButton()).isEnabled();
  }

  public void shouldShowCancelButtonDisabled() {
    assertThat(page.cancelButton()).isDisabled();
  }

  public void shouldShowTestConfigButtonDisabled() {
    assertThat(page.testConfigButton()).hasClass(BasePage.cssClassPattern("disabled"));
  }

  public void shouldShowTestConfigButtonEnabled() {
    assertThat(page.testConfigButton()).not().hasClass(BasePage.cssClassPattern("disabled"));
  }

  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  public void shouldShowDeleteModalHidden() {
    assertThat(page.deleteModal()).isHidden();
  }

  public void shouldShowAuthErrorMessage() {
    assertThat(page.loadError()).isVisible();
  }

  public void shouldShowTestConfigSuccess() {
    assertThat(page.testConfigSuccessAlert()).isVisible();
  }

  public void shouldShowTestConfigError() {
    assertThat(page.loadError()).isVisible();
  }
}
