/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.nio.file.Path;
import java.util.regex.Pattern;

import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Import SBOM modal launched from the SBOMs tile on application OwnerSummary.
 *
 * <p>
 * The modal multiplexes UploadPage/VersionConfirm/EvaluationInProgress/SbomSummary/
 * ValidationError behind a single {@code NxModal} whose {@code aria-labelledby} points at the
 * inner page's {@code
 *
<h2>}. Each transition swaps the heading text, so the modal's accessible
 * name shifts across the flow — that IS the contract we select on per page state.
 */
public class ImportSbomModalPage
    extends BasePage
{
  /** The five heading strings the modal cycles through; any one matches the modal currently open. */
  private static final Pattern ANY_PAGE_HEADING = Pattern.compile(
      "Import File for Application|File Uploaded\\. Import in Progress…|File Imported|Import Complete|Your SBOM failed validation|Error Importing SBOM");

  public ImportSbomModalPage() {
    super();
  }

  /** Matches the modal regardless of which inner page is showing — accessible name shifts per page. */
  public Locator container() {
    return page.getByRole(AriaRole.DIALOG,
        new Page.GetByRoleOptions().setName(ANY_PAGE_HEADING));
  }

  /** The modal scoped to a specific inner page; useful for asserting transitions. */
  public Locator containerOnPage(String pageHeading) {
    return page.getByRole(AriaRole.DIALOG,
        new Page.GetByRoleOptions().setName(pageHeading));
  }

  public Locator uploadPage() {
    return containerOnPage("Import File for Application");
  }

  public Locator versionConfirmPage() {
    return containerOnPage("File Uploaded. Import in Progress…");
  }

  public Locator evaluationInProgressPage() {
    return containerOnPage("File Imported");
  }

  public Locator importCompletePage() {
    return containerOnPage("Import Complete");
  }

  public Locator validationErrorPage() {
    return containerOnPage("Your SBOM failed validation");
  }

  public Locator unknownErrorPage() {
    return containerOnPage("Error Importing SBOM");
  }

  public Locator importButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Import").setExact(true));
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  /**
   * Submit button on the version-confirm page — labelled "Import" while in that state. Scoped
   * via {@link #versionConfirmPage()} so it doesn't multi-match the upload page's Import button.
   */
  public Locator versionConfirmSubmitButton() {
    return versionConfirmPage().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Import").setExact(true));
  }

  /** NxFileUpload renders a plain {@code <div>} with no role; CSS class is RSC's only stable hook. */
  public Locator fileUploadDropZone() {
    return container().locator(".nx-file-upload");
  }

  /**
   * The "form validation errors" alert NxStatefulForm renders when {@code validationErrors} is set.
   * On the upload page, this is the alert shown for upload-time rejection.
   */
  public Locator uploadPageErrorAlert() {
    return container().getByRole(AriaRole.ALERT,
        new Locator.GetByRoleOptions().setName("form validation errors"));
  }

  /**
   * Application Version input on the version-confirm page — labelled "Application Version"
   * by its {@code NxFormGroup}. Scoped by {@code role="textbox"} to disambiguate from the
   * tooltip-icon sibling which shares the form-group's accessible name.
   */
  public Locator versionInput() {
    return versionConfirmPage().getByRole(AriaRole.TEXTBOX,
        new Locator.GetByRoleOptions().setName("Application Version"));
  }

  /** Total-components data row on the summary page. The data id is the only stable hook here. */
  public Locator totalComponentsData() {
    return importCompletePage().locator("#import-sbom-modal-summary-total-components");
  }

  public void uploadFile(Path filePath) {
    FileChooser fileChooser = page.waitForFileChooser(
        () -> uploadPage().locator(".nx-file-upload__select-btn").click());
    fileChooser.setFiles(filePath);
  }

  /**
   * Fills "1.0" if the server returned no version, then submits. Waits for {@link #versionInput()}
   * to be visible first so the helper can be called immediately after the modal transitions to the
   * version-confirm page without racing the mount.
   */
  public void confirmVersionAndSubmit() {
    versionInput().waitFor();
    if (versionInput().inputValue().isBlank()) {
      versionInput().fill("1.0");
    }
    versionConfirmSubmitButton().click();
  }
}
