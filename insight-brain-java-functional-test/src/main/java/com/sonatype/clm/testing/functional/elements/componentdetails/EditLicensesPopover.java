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
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxTransferList;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class EditLicensesPopover
    extends BasicElement<EditLicensesPopover>
{
  static final String POPOVER_SELECTOR = "#edit-licenses-popover";

  // keeps the same order displayed in the form
  public enum RepositoryComponentLicensesScopes
  {
    REPOSITORY,
    ALL_REPOSITORIES,
    ORGANIZATION
  }

  public enum LicensesStatuses
  {
    OPEN,
    ACKNOWLEDGED,
    OVERRIDDEN,
    SELECTED,
    CONFIRMED,
    INHERITED,
  }

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

  public ElementsCollection availableLicensesTransferListItems() {
    return children(".nx-transfer-list__half:nth-child(1) .nx-transfer-list__item");
  }

  public ElementsCollection selectedLicensesTransferListItems() {
    return children(".nx-transfer-list__half:nth-child(2) .nx-transfer-list__item");
  }

  public NxFormSelect licensesScopesDropdown() {
    return new NxFormSelect("#iq-edit-license-scope");
  }

  public ElementsCollection availableScopes() {
    NxFormSelect dropdown = licensesScopesDropdown();
    return dropdown.listItems();
  }

  public SelenideElement scope(int index) {
    NxFormSelect dropdown = licensesScopesDropdown();
    return dropdown.listItem(index);
  }
}
