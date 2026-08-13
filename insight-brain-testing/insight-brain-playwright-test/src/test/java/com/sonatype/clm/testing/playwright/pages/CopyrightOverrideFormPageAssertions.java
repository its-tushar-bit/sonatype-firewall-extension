/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CopyrightOverrideFormPageAssertions
{
  private final CopyrightOverrideFormPage page;

  public CopyrightOverrideFormPageAssertions(CopyrightOverrideFormPage page) {
    this.page = page;
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

  public void shouldHaveCopyrightRowCount(int expectedCount) {
    assertThat(page.copyrightRows()).hasCount(expectedCount);
  }

  public void shouldShowAddCopyrightButton() {
    assertThat(page.addCopyrightButton()).isVisible();
  }

  public void shouldShowScopeDropdown() {
    assertThat(page.scopeDropdown()).isVisible();
  }

  public void shouldHaveScopeValue(String expectedValue) {
    assertThat(page.scopeDropdown()).hasValue(expectedValue);
  }

  public void shouldHaveSaveButtonEnabled() {
    assertThat(page.saveButton()).isEnabled();
  }

  public void shouldShowCopyrightTile() {
    assertThat(page.copyrightTile()).isVisible();
  }

  public void shouldShowToggleAt(int index) {
    assertThat(page.copyrightToggleAt(index)).isVisible();
  }

  public void shouldHaveToggleLabelText(int index, String expectedText) {
    assertThat(page.copyrightToggleAt(index)).hasText(expectedText);
  }

  public void shouldShowTextInputAt(int index) {
    assertThat(page.copyrightTextInputAt(index)).isVisible();
  }

  public void shouldShowValidationAlert(String expectedText) {
    assertThat(page.validationAlert()).containsText(expectedText);
  }
}
