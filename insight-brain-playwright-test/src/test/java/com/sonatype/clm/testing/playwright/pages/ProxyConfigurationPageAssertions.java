/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link ProxyConfigurationPage}.
 */
public class ProxyConfigurationPageAssertions
{
  private final ProxyConfigurationPage page;

  public ProxyConfigurationPageAssertions(ProxyConfigurationPage page) {
    this.page = page;
  }

  public void shouldHaveNoLoadError() {
    assertThat(page.loadError()).isHidden();
  }

  public void shouldShowHostname(String expected) {
    assertThat(page.hostName()).hasValue(expected);
  }

  public void shouldShowPort(String expected) {
    assertThat(page.port()).hasValue(expected);
  }

  public void shouldShowUsername(String expected) {
    assertThat(page.username()).hasValue(expected);
  }

  public void shouldShowPassword(String expected) {
    assertThat(page.password()).hasValue(expected);
  }

  public void shouldShowExcludeHosts(String expected) {
    assertThat(page.excludeHosts()).hasValue(expected);
  }

  public void shouldShowEmptyUsername() {
    assertThat(page.username()).isEmpty();
  }

  public void shouldShowEmptyExcludeHosts() {
    assertThat(page.excludeHosts()).isEmpty();
  }

  public void shouldBeEmpty() {
    assertThat(page.hostName()).isEmpty();
    assertThat(page.port()).isEmpty();
    assertThat(page.username()).isEmpty();
    assertThat(page.password()).isEmpty();
    assertThat(page.excludeHosts()).isEmpty();
    assertThat(page.deleteButton()).isDisabled();
  }

  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  public void shouldHideDeleteModal() {
    assertThat(page.deleteModal()).isHidden();
  }
}
