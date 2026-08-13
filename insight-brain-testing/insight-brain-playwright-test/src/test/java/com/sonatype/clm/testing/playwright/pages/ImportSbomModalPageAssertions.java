/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link ImportSbomModalPage}. */
public class ImportSbomModalPageAssertions
{
  private final ImportSbomModalPage page;

  public ImportSbomModalPageAssertions(ImportSbomModalPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldBeHidden() {
    assertThat(page.container()).isHidden();
  }

  public void shouldShowFileUploadAndButtons() {
    assertThat(page.fileUploadDropZone()).isVisible();
    assertThat(page.importButton()).isVisible();
    assertThat(page.cancelButton()).isVisible();
  }

  /** File name renders inside the modal after selection — don't pin the assertion to an element. */
  public void shouldShowSelectedFile(String fileName) {
    assertThat(page.container().getByText(fileName)).isVisible();
  }

  /**
   * Reserved for fixtures that trigger upload-time rejection. None ship today, but the
   * helper exists for negative-path coverage: either the validation or unknown error page
   * mounts, or the upload page surfaces a form-level error alert.
   */
  public void shouldShowAnyErrorState() {
    assertThat(
        page.validationErrorPage()
            .or(page.unknownErrorPage())
            .or(page.uploadPageErrorAlert())
            .first())
                .isVisible(new LocatorAssertions.IsVisibleOptions()
                    .setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
  }

  /** Use when the fixture uploads and the specific destination state isn't deterministic. */
  public void shouldTransitionPastUploadPage() {
    assertThat(page.uploadPage()).isHidden(
        new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
  }

  public void shouldShowVersionConfirmPage() {
    assertThat(page.versionConfirmPage()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void shouldShowEvaluationInProgress() {
    assertThat(page.evaluationInProgressPage()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /** Generous timeout — full pipeline (upload → detect → commit → evaluate → summary). */
  public void shouldShowImportComplete() {
    assertThat(page.importCompletePage()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ASYNC_EVALUATION_TIMEOUT_MS));
    assertThat(page.totalComponentsData()).isVisible();
    // Guard against an "import succeeded but found nothing" regression — every fixture
    // we ship contains at least one component, so the count must be non-zero.
    assertThat(page.totalComponentsData()).not().containsText("0");
  }
}
