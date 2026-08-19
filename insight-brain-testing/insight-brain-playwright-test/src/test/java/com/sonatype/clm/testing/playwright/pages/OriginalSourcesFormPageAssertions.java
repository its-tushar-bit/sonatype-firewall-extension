/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link OriginalSourcesFormPage}.
 */
public class OriginalSourcesFormPageAssertions
{
  private final OriginalSourcesFormPage page;

  public OriginalSourcesFormPageAssertions(OriginalSourcesFormPage page) {
    this.page = page;
  }

  public void shouldShowTile() {
    assertThat(page.tile()).isVisible();
  }

  public void shouldShowModal() {
    assertThat(page.modal()).isVisible();
  }

  public void shouldHideModal() {
    assertThat(page.modal()).isHidden();
  }

  public void shouldShowModalHeaderText(String expectedText) {
    assertThat(page.modalHeader()).hasText(expectedText);
  }

  public void shouldShowAddLinkButton() {
    assertThat(page.addLinkButton()).isVisible();
  }

  public void shouldHaveSourceRowCount(int expectedCount) {
    assertThat(page.sourceRows()).hasCount(expectedCount);
  }

  public void shouldShowScopeDropdown() {
    assertThat(page.scopeDropdown()).isVisible();
  }

  public void shouldHideScopeDropdown() {
    assertThat(page.scopeDropdown()).isHidden();
  }

  public void shouldHaveSaveButtonDisabled() {
    assertThat(page.saveButton()).isDisabled();
  }

  public void shouldHaveSaveButtonEnabled() {
    assertThat(page.saveButton()).isEnabled();
  }

  public void shouldHaveSourceInputDisabled(int index) {
    assertThat(page.sourceUrlInputAt(index)).isDisabled();
  }

  public void shouldHaveSourceInputEnabled(int index) {
    assertThat(page.sourceUrlInputAt(index)).isEnabled();
  }

  public void shouldShowSourceInputAt(int index) {
    assertThat(page.sourceUrlInputAt(index)).isVisible();
  }

  public void shouldShowToggleAt(int index) {
    assertThat(page.sourceToggleAt(index)).isVisible();
  }

  public void shouldHaveToggleLabelText(int index, String expectedText) {
    assertThat(page.sourceToggleAt(index)).hasText(expectedText);
  }

  public void shouldShowValidationAlert(String expectedText) {
    assertThat(page.validationAlert()).containsText(expectedText);
  }

  public void shouldNotShowValidationAlert() {
    assertThat(page.validationAlert()).isHidden();
  }

  public void shouldHaveSourceInputMaxLength(int index, String expectedMaxLength) {
    assertThat(page.sourceUrlInputAt(index)).hasAttribute("maxlength", expectedMaxLength);
  }
}
