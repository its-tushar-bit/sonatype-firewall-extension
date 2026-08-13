/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link SamlConfigurationPage}.
 */
public class SamlConfigurationPageAssertions
{
  private final SamlConfigurationPage page;

  public SamlConfigurationPageAssertions(SamlConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.identityProviderName()).isVisible();
    assertThat(page.identityProviderMetadataXml()).isVisible();
    assertThat(page.usernameAttribute()).isVisible();
    assertThat(page.firstNameAttribute()).isVisible();
    assertThat(page.lastNameAttribute()).isVisible();
    assertThat(page.emailAttribute()).isVisible();
    assertThat(page.groupsAttribute()).isVisible();
    assertThat(page.validateResponseSignature()).isVisible();
    assertThat(page.validateAssertionSignature()).isVisible();
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

  public void shouldShowDeleteModalHidden() {
    assertThat(page.deleteModal()).isHidden();
  }

  public void shouldShowIqServerMetadataDownloadEnabled() {
    assertThat(page.downloadIqServerMetadataLink()).isEnabled();
  }
}
