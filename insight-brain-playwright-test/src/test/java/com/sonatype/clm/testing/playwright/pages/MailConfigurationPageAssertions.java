/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MailConfigurationPageAssertions
{
  private final MailConfigurationPage page;

  public MailConfigurationPageAssertions(MailConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.tileHeading()).isVisible();
    assertThat(page.hostnameInput()).isVisible();
    assertThat(page.portInput()).isVisible();
    assertThat(page.usernameInput()).isVisible();
    assertThat(page.passwordInput()).isVisible();
    assertThat(page.systemEmailInput()).isVisible();
    assertThat(page.saveButton()).isVisible();
    assertThat(page.cancelButton()).isVisible();
    assertThat(page.deleteButton()).isVisible();
  }

  public void shouldShowTestEmailWidgets() {
    assertThat(page.testEmailRecipientInput()).isVisible();
    assertThat(page.sendTestEmailButton()).isVisible();
  }

  public void shouldHaveHostname(String expected) {
    assertThat(page.hostnameInput()).hasValue(expected);
  }

  public void shouldHavePort(String expected) {
    assertThat(page.portInput()).hasValue(expected);
  }

  public void shouldHaveSystemEmail(String expected) {
    assertThat(page.systemEmailInput()).hasValue(expected);
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

  public void shouldShowTestEmailErrorAlert() {
    assertThat(page.testEmailErrorAlert()).isVisible();
  }
}
