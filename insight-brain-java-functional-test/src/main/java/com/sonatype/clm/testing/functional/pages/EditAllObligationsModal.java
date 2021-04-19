/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditAllObligationsModal
    extends BasicElement<EditAllObligationsModal>
{
  public EditAllObligationsModal() {
    super("#all-obligations-modal");
  }

  public SelenideElement statusDropdown() {
    return $("#all-obligations-status-dropdown button span span");
  }

  public SelenideElement flaggedDropdownOption() {
    return $("#FLAGGED-dropdown-option");
  }

  public SelenideElement ignoredDropdownOption() {
    return $("#IGNORED-dropdown-option");
  }

  public SelenideElement openDropdownOption() {
    return $("#OPEN-dropdown-option");
  }

  public SelenideElement fulfilledDropdownOption() {
    return $("#FULFILLED-dropdown-option");
  }

  public SelenideElement scopeDropdown() {
    return $("#all-obligations-scope-selection");
  }

  public SelenideElement commentTextInput() {
    return child(".nx-text-input__input");
  }

  public Button save() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancel() {
    return new Button(childSelector(".nx-form__cancel-btn"));
  }
}
