/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link ApplicationCategoryEditorPage}.
 */
public class ApplicationCategoryEditorPageAssertions
{
  private final ApplicationCategoryEditorPage page;

  public ApplicationCategoryEditorPageAssertions(ApplicationCategoryEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.heading()).isVisible();
    assertThat(page.heading()).hasText("Application Category Settings");
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
   * Asserts the given category name appears as a list item in the local section of the
   * Application Categories tile, confirming the newly created category was saved.
   */
  public void shouldHaveLocalCategory(String categoryName) {
    assertThat(page.categoryLocalListItem(categoryName)).isVisible();
  }

  /** Asserts the delete confirmation modal is visible. */
  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  /**
   * Asserts the given category name appears in the "Assigned" section of the Application
   * Categories tile on an application summary, confirming the category was successfully assigned.
   * Uses the same list-item locator as {@link #shouldHaveLocalCategory(String)} — both sections
   * render {@code NxList} items containing the category name text.
   */
  public void shouldHaveAssignedCategory(String categoryName) {
    assertThat(page.categoryLocalListItem(categoryName)).isVisible();
  }

  /**
   * Asserts the given category name is no longer present in the local section of the
   * Application Categories tile, confirming a successful delete.
   */
  public void shouldNotHaveLocalCategory(String categoryName) {
    assertThat(page.categoryLocalListItem(categoryName)).isHidden();
  }

  public void shouldShowAddCategoryButtonInPreviewMode() {
    TierGatedEditorAssertions.shouldShowAddButtonInPreviewMode(page, "Category");
  }

  public void shouldShowAddCategoryButtonInEnterpriseMode() {
    TierGatedEditorAssertions.shouldShowAddButtonInEnterpriseMode(page, "Category");
  }

  public void shouldShowReadOnlyViewWithCategoryName(String expectedName) {
    TierGatedEditorAssertions.shouldShowReadOnlyViewWithName(page, expectedName);
  }
}
