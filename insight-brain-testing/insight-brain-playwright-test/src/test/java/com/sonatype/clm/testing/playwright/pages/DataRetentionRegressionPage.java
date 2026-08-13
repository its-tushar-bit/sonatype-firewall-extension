/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Regression-only locators for the Data Retention editor. */
public class DataRetentionRegressionPage
    extends BasePage
{
  private static final String DATA_RETENTION_POLICIES_API = "/api/v2/dataRetentionPolicies/";

  public DataRetentionRegressionPage() {
    super();
  }

  public Locator pageHeading() {
    return byRole(AriaRole.HEADING, "Data Retention");
  }

  /**
   * "Application Reports" section heading inside the data-retention editor.
   * ID selector {@code #retention-editor} used as a structural scope anchor because the
   * heading text "Application Reports" also appears in stale DOM nodes during SPA navigation;
   * scoping prevents false positives without an ARIA region or accessible name on the container.
   */
  public Locator applicationReportsSection() {
    return locator("#retention-editor").getByText("Application Reports");
  }

  /**
   * "Success Metrics" section heading inside the data-retention editor.
   * ID selector {@code #retention-editor} used as a structural scope anchor for the same reason
   * as {@link #applicationReportsSection()} — stale-DOM collision during navigation.
   */
  public Locator successMetricsSection() {
    return locator("#retention-editor").getByText("Success Metrics");
  }

  public Locator updateButton() {
    return byRole(AriaRole.BUTTON, "Update");
  }

  /** Visible after clicking Update with no changes (NxStatefulForm MSG_NO_CHANGES_TO_SAVE). */
  public Locator formValidationErrors() {
    return byRole(AriaRole.ALERT, "form validation errors");
  }

  /**
   * Label wrapper for the "Don't Purge" NxRadio in the given stage fieldset.
   * NxRadio hides the native input (position:absolute, 1px) — click must target the label.
   */
  public Locator doNotPurgeLabelForStage(String stageId) {
    return locator("label:has(input[name='" + stageId + "'])").filter(
        new Locator.FilterOptions().setHasText("Don't Purge"));
  }

  /**
   * Native radio input for "Don't Purge" in the given stage — used for {@code isChecked} only.
   * ID selector {@code #retention-editor-{stageId}} used as a structural scope anchor because
   * every stage fieldset contains a "Don't Purge" radio with the same accessible name; scoping
   * by stage ID is the only way to avoid strict-mode violations without adding ARIA labels to
   * individual fieldsets.
   */
  public Locator doNotPurgeRadioForStage(String stageId) {
    return locator("#retention-editor-" + stageId)
        .getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Don't Purge"));
  }

  /**
   * Clicks Update and waits for the PUT response (HTTP 200).
   * Keeps the API URL out of the test and groups the save action with its network gate.
   */
  public void clickUpdateAndWaitForSave() {
    page.waitForResponse(
        response -> response.url().contains(DATA_RETENTION_POLICIES_API)
            && response.status() == 200,
        () -> updateButton().click());
  }

  /**
   * Data Retention NxTile on the Owner Summary ({@code #owner-pill-retention}).
   * ID selector used because the tile container is a bare {@code <div>} with no ARIA role or
   * accessible name; {@code getByRole} cannot scope to it by name. Used as a structural anchor
   * for the inner "Edit" button query in {@link #clickDataRetentionEditButton()}.
   * {@code OwnerSummaryPage} has the constant but no tile locator method for it.
   */
  public Locator dataRetentionTile() {
    return locator("#owner-pill-retention");
  }

  /**
   * Clicks the "Edit" NxButton inside the Data Retention tile.
   * The tile uses {@code NxButton} (not {@code <a>}); scoped to avoid colliding with
   * other "Edit" buttons on the Owner Summary.
   */
  public void clickDataRetentionEditButton() {
    dataRetentionTile()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit"))
        .click();
  }
}
