/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link LicenseThreatGroupEditorPage}.
 */
public class LicenseThreatGroupEditorPageAssertions
{
  private final LicenseThreatGroupEditorPage page;

  public LicenseThreatGroupEditorPageAssertions(LicenseThreatGroupEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.heading()).isVisible();
    assertThat(page.heading()).hasText("License Threat Group Settings");
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
   * Asserts that the given group name appears as a cell in the local LTG table,
   * confirming the newly created group was saved and is visible in the tile.
   */
  public void shouldHaveLocalThreatGroup(String groupName) {
    // Local LTG rows load from the API after the tile structure renders — use a longer timeout.
    assertThat(page.ltgCellInTile(groupName))
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /** Asserts the delete confirmation modal is visible. */
  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  /**
   * Asserts the given group name is no longer present in the local LTG table,
   * confirming a successful delete.
   */
  public void shouldNotHaveLocalThreatGroup(String groupName) {
    assertThat(page.ltgCellInTile(groupName)).isHidden();
  }
}
