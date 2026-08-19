/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Web-first assertions for {@link ArtifactoryRepoConfigPage}.
 */
public class ArtifactoryRepoConfigPageAssertions
{
  private final ArtifactoryRepoConfigPage page;

  public ArtifactoryRepoConfigPageAssertions(ArtifactoryRepoConfigPage page) {
    this.page = page;
  }

  /** Page content has loaded — container is attached and visible. */
  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
  }

  /** "Inherit" radio label is not visible — expected at root org. */
  public void shouldNotShowInheritRadio() {
    assertThat(page.inheritRadio().label()).not().isVisible();
  }

  /** "Inherit" radio label is visible — expected at child org / application. */
  public void shouldShowInheritRadio() {
    assertThat(page.inheritRadio().label()).isVisible();
  }

  /** "Disable" radio label is visible. */
  public void shouldShowDisableRadio() {
    assertThat(page.disableRadio().label()).isVisible();
  }

  /** "Enable" radio label is visible — root org only. */
  public void shouldShowEnableRadio() {
    assertThat(page.enableRadio().label()).isVisible();
  }

  /** "Enable and Override Repository Connections" radio label is visible — child org only. */
  public void shouldShowEnableAndOverrideRadio() {
    assertThat(page.enableAndOverrideRadio().label()).isVisible();
  }

  /** The "Allow Override" checkbox label is visible — only present at org-level nodes. */
  public void shouldShowAllowOverrideCheckbox() {
    assertThat(page.allowOverrideCheckboxLabel()).isVisible();
  }

  /**
   * Lock alert is visible, indicating parent org has disabled override.
   * Text: "The inherited configuration cannot be overridden."
   */
  public void shouldShowLockAlert() {
    assertThat(page.lockAlert()).isVisible();
  }

  /** "LOCAL" section header is visible — shown when Enable mode is active. */
  public void shouldShowLocalHeader() {
    assertThat(page.localHeader()).isVisible();
  }

  /** Connection list LOCAL header is not visible — shown when Disable mode is active. */
  public void shouldNotShowLocalHeader() {
    assertThat(page.localHeader()).not().isVisible();
  }

  /** Empty list placeholder text is visible — shown in Enable mode when no connections exist. */
  public void shouldShowEmptyListMessage() {
    assertThat(page.emptyListMessage()).isVisible();
  }

  /** Add modal is visible. */
  public void shouldShowAddModal() {
    assertThat(page.addModal()).isVisible();
  }

  /** Add modal is not visible. */
  public void shouldHideAddModal() {
    assertThat(page.addModal()).not().isVisible();
  }

  /** Delete confirmation modal is visible. */
  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  /** Delete confirmation modal is not visible. */
  public void shouldHideDeleteModal() {
    assertThat(page.deleteModal()).not().isVisible();
  }

  /** Edit button for an existing connection row is visible. */
  public void shouldShowEditButton() {
    assertThat(page.editButton()).isVisible();
  }

  /** Delete button for an existing connection row is visible. */
  public void shouldShowDeleteButton() {
    assertThat(page.deleteButton()).isVisible();
  }

  /** Edit button is no longer visible — connection was removed. */
  public void shouldNotShowEditButton() {
    assertThat(page.editButton()).not().isVisible();
  }

  /**
   * Hovers the Add button and asserts the tooltip explains that override must be allowed.
   * Used when {@code allowChange=false} (parent disabled override) — the button is
   * {@code aria-disabled} and shows this message on hover instead of opening the modal.
   */
  public void shouldShowLockedAddButtonTooltip() {
    page.addButton().hover();
    assertThat(page.tooltip()).containsText("Parent organizations must Allow Override.");
  }
}
