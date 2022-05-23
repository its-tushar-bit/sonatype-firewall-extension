/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.NxColorPicker;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LabelEditorPage
{
  private static final NxColorPicker nxColorPicker = new NxColorPicker("#editor-label-color-picker");

  public static String urlToEdit(Owner owner, String labelId) {
    return urlToEdit(owner.getType(), owner.getPublicId(), labelId);
  }

  public static String urlToEdit(OwnerType ownerType, String ownerId, String labelId) {
    return urlToCreate(ownerType, ownerId) + "/" + labelId;
  }

  public static String urlToCreate(Owner owner) {
    return urlToCreate(owner.getType(), owner.getPublicId());
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/label", ownerType, ownerId);
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement labelNameDiv() {
    return $("#editor-label-name .nx-text-input");
  }

  public static SelenideElement labelName() {
    return $("#editor-label-name .nx-text-input .nx-text-input__input");
  }

  public static SelenideElement labelInvalidMessage() {
    return $("#editor-label-name > .nx-text-input > .nx-text-input__invalid-message");
  }

  public static SelenideElement descriptionDiv() {
    return $("#editor-label-description .nx-text-input");
  }

  public static SelenideElement description() {
    return $("#editor-label-description .nx-text-input .nx-text-input__input");
  }

  public static SelenideElement descriptionInvalidMessage() {
    return $("#editor-label-description > .nx-text-input > .nx-text-input__invalid-message");
  }

  public static NxColorPicker nxColorPicker() {
    return nxColorPicker;
  }

  public static SelenideElement saveButton() {
    return $(".nx-form__submit-btn");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-label-button");
  }

  public static NxDeleteModal getDeleteModal() {
    return new NxDeleteModal("#label-config-delete-modal");
  }
}
