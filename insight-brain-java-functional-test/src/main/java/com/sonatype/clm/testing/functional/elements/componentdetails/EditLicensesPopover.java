/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class EditLicensesPopover
    extends BasicElement<EditLicensesPopover>
{
  static final String POPOVER_SELECTOR = "#edit-licenses-popover";

  public EditLicensesPopover() {
    super(POPOVER_SELECTOR);
  }

  public SelenideElement getCloseButton() {
    return child("#edit-licenses-popover-close-btn");
  }
  
  public SelenideElement popoverTitle() {
    return child("#edit-licenses-popover-header");
  }
}
