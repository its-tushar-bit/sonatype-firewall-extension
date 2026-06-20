/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-only page object for the SBOM Manager Component Details page.
 * Contains locators for the row-actions dropdown, Copy Annotation modal, and
 * Delete Annotation modal that are not part of the sanity test surface.
 * Do not add methods to the existing {@link SbomComponentDetailsPage}.
 */
public class SbomComponentDetailsRegressionPage
    extends BasePage
{
  public SbomComponentDetailsRegressionPage() {
    super();
  }

  /**
   * The {@code NxIconDropdown} toggle button for the per-vulnerability row actions menu.
   * Rendered in {@code VulnerabilitiesTile.jsx} with
   * {@code aria-label="{vulnerability.issue}-actions"}.
   * <p>
   * CSS attribute selector is used intentionally — {@code getByRole(BUTTON, issue + "-actions")}
   * times out consistently because the {@code NxIconDropdown} toggle's computed accessible name
   * is not resolved by Playwright's ARIA role engine at click time.
   *
   * @param issue the vulnerability issue ID (e.g. "ABC-123")
   */
  public Locator vulnerabilityOptionsButton(String issue) {
    return locator("[aria-label=\"" + issue + "-actions\"]");
  }

  /**
   * The "Copy Annotation" button inside the open options dropdown.
   * Only present when {@code vulnerability.latestPreviousAnnotation} is non-null,
   * i.e. there is a VEX annotation on the same component/vulnerability in a prior SBOM version.
   */
  public Locator copyAnnotationButton() {
    return byRole(AriaRole.BUTTON, "Copy Annotation");
  }

  /**
   * The {@code CopyAnnotationModal} container ({@code #copy-vex-annotation-modal}).
   * Rendered by {@code CopyAnnotationModal.jsx} when "Copy Annotation" is clicked.
   */
  public Locator copyAnnotationModal() {
    return locator("#copy-vex-annotation-modal");
  }

  /**
   * The {@code
   *
  <h2>} heading inside the Copy Annotation modal.
   * Text reads: "Copy annotation for {vulnerability.issue}".
   */
  public Locator copyAnnotationModalTitle() {
    return locator("#copy-vex-annotation-modal")
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  /** Click the options dropdown toggle for the given vulnerability row. */
  public void clickVulnerabilityOptionsButton(String issue) {
    vulnerabilityOptionsButton(issue).click();
  }

  /** Click the "Copy Annotation" button in the open options dropdown. */
  public void clickCopyAnnotationButton() {
    copyAnnotationButton().click();
  }

  /**
   * The "Delete Annotation" button inside the open options dropdown.
   * Only present when {@code isRowAnnotated(vulnerability, analysisStatusesOptions)} returns true,
   * i.e. the current SBOM version has a VEX annotation for the vulnerability.
   */
  public Locator deleteAnnotationButton() {
    return byRole(AriaRole.BUTTON, "Delete Annotation");
  }

  /**
   * The {@code DeleteAnnotationModal} container ({@code #delete-vex-annotation-modal}).
   * Rendered by {@code DeleteAnnotationModal.jsx} when "Delete Annotation" is clicked.
   */
  public Locator deleteAnnotationModal() {
    return locator("#delete-vex-annotation-modal");
  }

  /**
   * The {@code
   *
  <h2>} heading inside the Delete Annotation modal.
   * Text reads: "Delete annotation for {vulnerability.issue}".
   */
  public Locator deleteAnnotationModalTitle() {
    return locator("#delete-vex-annotation-modal")
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  /** Click the "Delete Annotation" button in the open options dropdown. */
  public void clickDeleteAnnotationButton() {
    deleteAnnotationButton().click();
  }
}
