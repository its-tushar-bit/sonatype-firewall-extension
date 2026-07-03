/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the SBOM Manager Owner Summary page ({@code #/sbomManager/management/view/application/{id}}). */
public class SbomManagerOwnerSummaryPage
    extends BasePage
{
  private static final String ROOT = "#owner-summary";

  public SbomManagerOwnerSummaryPage() {
    super();
  }

  /** "Import" button; only visible when selected owner is an application. */
  public Locator importButton() {
    return byRole(AriaRole.BUTTON, "Import");
  }

  public Locator importSbomModal() {
    return locator("#import-sbom-modal");
  }

  public Locator importSbomModalHeader() {
    return locator("#import-sbom-modal-header");
  }

  /** File input element — NxFormGroup has no ARIA role, so the input itself is the presence assertion. */
  public Locator fileUploadInput() {
    return locator("#import-sbom-modal input[type='file']");
  }

  public void clickImportButton() {
    importButton().click();
  }
}
