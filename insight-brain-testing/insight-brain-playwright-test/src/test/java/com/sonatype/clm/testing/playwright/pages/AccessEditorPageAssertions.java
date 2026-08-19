/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link AccessEditorPage}.
 */
public class AccessEditorPageAssertions
{
  private final AccessEditorPage page;

  public AccessEditorPageAssertions(AccessEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.root()).isVisible();
    assertThat(page.form()).isVisible();
  }

  public void shouldHaveAssociatedMember(String memberText) {
    assertThat(page.associatedMembers().filter(new Locator.FilterOptions().setHasText(memberText)).first())
        .isVisible();
  }

  public void shouldNotShowSubmitError() {
    assertThat(page.submitError()).isHidden();
  }

  /** Asserts the editor is in "New Role" mode: heading, role dropdown, and Create button. */
  public void shouldBeInNewMode() {
    assertThat(page.heading()).hasText("New Role");
    assertThat(page.roleSelect()).isVisible();
    assertThat(page.submitButton()).hasText("Create");
  }

  /**
   * Asserts the editor is in "Edit Role" mode: heading, role-name subtitle, no dropdown, Update button.
   */
  public void shouldBeInEditMode(String expectedRoleName) {
    assertThat(page.heading()).hasText("Edit Role");
    assertThat(page.headingSubtitle()).hasText(expectedRoleName);
    assertThat(page.roleSelect()).isHidden();
    assertThat(page.submitButton()).hasText("Update");
  }

  public void shouldShowSearchResultContaining(String text) {
    assertThat(page.searchResults().filter(new Locator.FilterOptions().setHasText(text)).first())
        .isVisible();
  }

  /**
   * Asserts the "Associated Members" transfer-list footer shows the expected count text,
   * e.g. {@code "1 User and 0 Groups Added"} or {@code "0 Users and 0 Groups Added"}.
   */
  public void shouldHaveTransferListFooter(String expectedText) {
    assertThat(page.associatedTransferList()).containsText(expectedText);
  }

  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  public void shouldNotShowDeleteModal() {
    assertThat(page.deleteModal()).isHidden();
  }

  /**
   * Asserts that the RSC NxStatefulForm validation-error alert is visible, indicating the form
   * blocked a submit attempt due to validation errors (role or members missing in new mode).
   */
  public void shouldShowValidationErrors() {
    assertThat(page.validationErrors()).isVisible();
  }

}
