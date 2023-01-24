/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxTransferList;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LTGEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static String urlToEdit(Owner owner, String licenseThreatGroupId) {
    return urlToEdit(owner.getType(), owner.getPublicId(), licenseThreatGroupId);
  }

  public static String urlToEdit(OwnerType ownerType, String ownerId, String licenseThreatGroupId) {
    return urlToCreate(ownerType, ownerId) + "/" + licenseThreatGroupId;
  }

  public static String urlToCreate(Owner owner) {
    return urlToCreate(owner.getType(), owner.getPublicId());
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/licenseThreatGroup", ownerType, ownerId);
  }

  public static SelenideElement title() {
    return $(".nx-h1");
  }

  public static SelenideElement ltgName() {
    return $(".nx-text-input__input");
  }

  public static NxTransferList picker() {
    return new NxTransferList("#editor-ltg-included-licenses");
  }

  public static SelenideElement saveButton() {
    return $(".nx-form__submit-btn");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-ltg-button");
  }

  public static NxDeleteModal deleteModal() {
    return new NxDeleteModal("#ltg-config-delete-modal");
  }

  public static SelenideElement getInputValidationElement(SelenideElement element) {
    return element.closest(".nx-form-group").find(".nx-field-validation-message");
  }
}
