/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link ComponentLabelEditorPage}.
 */
public class ComponentLabelEditorPageAssertions
{
  private final ComponentLabelEditorPage page;

  public ComponentLabelEditorPageAssertions(ComponentLabelEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.heading()).isVisible();
    assertThat(page.heading()).hasText("Component Label Settings");
  }

  /** Asserts the editor is in Create mode: submit button shows "Create". */
  public void shouldBeInCreateMode() {
    assertThat(page.submitButton()).hasText("Create");
  }

  /** Asserts the editor is in Edit mode: submit button shows "Update". */
  public void shouldBeInEditMode() {
    assertThat(page.submitButton()).hasText("Update");
  }

  /**
   * Asserts that the given label name appears as a list item in the Component Labels tile,
   * confirming the newly created label was saved and is visible in the local labels section.
   */
  public void shouldHaveLocalComponentLabel(String labelName) {
    assertThat(page.componentLabelsLocalListItem(labelName)).isVisible();
  }

  /** Asserts the given label name is no longer present in the Component Labels tile. */
  public void shouldNotHaveLocalComponentLabel(String labelName) {
    assertThat(page.componentLabelsLocalListItem(labelName)).isHidden();
  }

  /** Asserts the delete confirmation modal ({@code id="label-config-delete-modal"}) is visible. */
  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  public void shouldShowAddLabelButtonInPreviewMode() {
    TierGatedEditorAssertions.shouldShowAddButtonInPreviewMode(page, "Label");
  }

  public void shouldShowAddLabelButtonInEnterpriseMode() {
    TierGatedEditorAssertions.shouldShowAddButtonInEnterpriseMode(page, "Label");
  }

  public void shouldShowReadOnlyViewWithLabelName(String expectedName) {
    TierGatedEditorAssertions.shouldShowReadOnlyViewWithName(page, expectedName);
  }
}
