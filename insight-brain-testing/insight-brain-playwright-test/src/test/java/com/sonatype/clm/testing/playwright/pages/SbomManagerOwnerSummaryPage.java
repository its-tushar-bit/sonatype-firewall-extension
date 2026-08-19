/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class SbomManagerOwnerSummaryPage
    extends BasePage
{
  public SbomManagerOwnerSummaryPage() {
    super();
  }

  public Locator importButton() {
    return byRole(AriaRole.BUTTON, "Import");
  }

  public Locator importSbomModal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator importSbomModalHeader() {
    return importSbomModal().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  /** File input has no ARIA role; the input element itself is the presence assertion. */
  public Locator fileUploadInput() {
    return importSbomModal().locator("input[type='file']");
  }

  public void clickImportButton() {
    importButton().click();
  }
}
