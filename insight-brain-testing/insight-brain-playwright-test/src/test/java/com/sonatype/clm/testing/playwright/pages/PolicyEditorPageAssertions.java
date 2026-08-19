/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link PolicyEditorPage}.
 */
public class PolicyEditorPageAssertions
{
  private final PolicyEditorPage page;

  public PolicyEditorPageAssertions(PolicyEditorPage page) {
    this.page = page;
  }

  public void shouldBeInheritedReadOnlyView() {
    assertThat(page.pageHeading()).hasText("Policy Settings");
    assertThat(page.policyName()).isDisabled();
    assertThat(page.deletePolicyButton()).hasCount(0);
  }

  /**
   * Verifies the editor opened in edit mode: submit button reads "Update", policy name is
   * pre-filled with {@code expectedName}, and all four editor sections are visible.
   */
  public void shouldBeInEditModeWithExpectedName(String expectedName) {
    assertThat(page.saveButton()).hasText("Update");
    assertThat(page.policyName()).hasValue(expectedName);
    assertThat(page.threatLevelDropdown()).isVisible();
    assertThat(page.inheritanceSection()).isVisible();
    assertThat(page.constraintsSection()).isVisible();
    assertThat(page.actionsSection()).isVisible();
    assertThat(page.notificationsSection()).isVisible();
  }

  /**
   * Asserts the NxSubmitMask transitions to success then auto-dismisses after a policy save.
   */
  public void shouldShowSaveSuccessMask() {
    Locator successMask = page.saveSuccessMask();
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
  }

  /**
   * Asserts the delete-policy modal is open and its consequences list is rendered.
   */
  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
    assertThat(page.deleteModalConsequencesList()).isVisible();
  }

  /**
   * Asserts the editor is in SBOM Manager read-only mode: no Delete button, info alert visible.
   */
  public void shouldBeInSbomManagerReadOnlyMode() {
    assertThat(page.deletePolicyButton()).hasCount(0);
    assertThat(page.sbomManagerInfoAlert()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(page.lifecycleLink()).isVisible();
  }
}
