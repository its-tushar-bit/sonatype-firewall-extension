/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxTransferList;

import com.codeborne.selenide.ElementsCollection;
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

  public Button saveButton() {
    return new Button(".nx-form__submit-btn");
  }

  public SelenideElement popoverTitle() {
    return child("#edit-licenses-popover-header");
  }

  public SelenideElement effectiveLicenses() {
    return child("#effective-licenses-container");
  }

  public SelenideElement declaredLicenses() {
    return child("#declared-licenses-container");
  }

  public SelenideElement observedLicenses() {
    return child("#observed-licenses-container");
  }

  public ElementsCollection getItems(SelenideElement parent) {
    return parent.findAll(".license-list-item");
  }

  public ElementsCollection availableScopes() {
    return children(".iq-edit-licenses-form__scope .nx-radio");
  }

  public NxRadio scope(int index) {
    return new NxRadio(this.availableScopes().get(index));
  }

  public ElementsCollection statuses() {
    return children("#status-select option");
  }

  public SelenideElement status() {
    return child("#status-select");
  }

  public ElementsCollection selectedLicensesCheckBoxElements() {
    return children(".iq-edit-licenses-form__selected-licenses .nx-checkbox");
  }

  public List<NxCheckbox> selectedLicensesCheckbox() {
    List<NxCheckbox> checkboxes = new ArrayList<>();
    ElementsCollection selenideCheckboxes = selectedLicensesCheckBoxElements();
    for (SelenideElement checkbox : selenideCheckboxes) {
      checkboxes.add(new NxCheckbox(checkbox));
    }
    return checkboxes;
  }

  public SelenideElement comment() {
    return child(".iq-edit-licenses-form__comment .nx-text-input textarea");
  }

  public NxTransferList overriddenField() {
    return new NxTransferList(".iq-edit-licenses-form__overridden");
  }

  public SelenideElement unsavedModal() { 
    return child("#unsaved-modal"); 
  }

  public SelenideElement unsavedModalContinueButton() { 
    return child("#unsaved-changes-modal-continue-button"); 
  }
}
