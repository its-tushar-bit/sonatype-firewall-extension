/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class EditLicensesModal
    extends BasicElement<EditLicensesModal>
{
  public EditLicensesModal() {
    super("#edit-licenses-modal");
  }

  public SelenideElement header() {
    return child(".nx-h2");
  }

  public SelenideElement scopeDropdown() {
    return child("#edit-licenses-scope-selection");
  }

  public SelenideElement statusDropdown() {
    return child("#edit-licenses-status-selection");
  }

  public SelenideElement statusOpenOption() {
    return $("#edit-license-status-option-Open");
  }

  public SelenideElement statusAcknowledgedOption() {
    return $("#edit-license-status-option-Acknowledged");
  }

  public SelenideElement statusSelectedOption() {
    return $("#edit-license-status-option-Selected");
  }

  public SelenideElement statusOverrriddenOption() {
    return $("#edit-license-status-option-Overridden");
  }

  public SelenideElement statusConfirmedption() {
    return $("#edit-license-status-option-Confirmed");
  }

  public NxCheckbox getCheckboxAt(int i) {
    return new NxCheckbox(child(".nx-checkbox", nthChild(i + 1)));
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

  public static class License
      extends BasicElement<License>
  {
    License(String selector) {
      super(selector);
    }
  }
}
