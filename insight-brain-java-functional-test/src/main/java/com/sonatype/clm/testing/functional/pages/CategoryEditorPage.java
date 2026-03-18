/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.NxColorPicker;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.google.common.base.Joiner;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class CategoryEditorPage
{
  private static final NxColorPicker nxColorPicker = new NxColorPicker("#editor-category-color-picker");

  public static String urlToEdit(String ownerId, String categoryId) {
    return urlToCreate(ownerId) + "/" + categoryId;
  }

  public static String urlToCreate(String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/organization/{ownerId}/category", ownerId);
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement categoryNameDiv() {
    return $("#editor-category-name .nx-text-input");
  }

  public static SelenideElement categoryName() {
    return $("#editor-category-name .nx-text-input .nx-text-input__input");
  }

  public static SelenideElement categoryInvalidMessage() {
    return $("#editor-category-name > .nx-text-input > .nx-field-validation-message");
  }

  public static SelenideElement descriptionDiv() {
    return $("#editor-category-description .nx-text-input");
  }

  public static SelenideElement description() {
    return $("#editor-category-description .nx-text-input .nx-text-input__input");
  }

  public static SelenideElement descriptionInvalidMessage() {
    return $("#editor-category-description > .nx-text-input > .nx-field-validation-message");
  }

  public static NxColorPicker nxColorPicker() {
    return nxColorPicker;
  }

  public static SelenideElement saveButton() {
    return $("#create-edit-category .nx-form__submit-btn");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-category-button");
  }

  public static NxDeleteModal getDeleteModal() {
    return new NxDeleteModal("#category-delete-modal");
  }

  public static WebElementCondition deleteWarningText() {
    return deleteWarningText(null);
  }

  public static WebElementCondition deleteWarningText(String applicationNames) {
    String baseMessage = "Are you sure you want to delete this application category?";
    return text(applicationNames == null
        ? baseMessage
        : baseMessage + " It is in use by the following applications: " + applicationNames + ".");
  }

  public static WebElementCondition associatedPoliciesText(String... policyNames) {
    return text("You cannot delete this application category because it is associated with the following policies: " +
        Joiner.on(", ").join(policyNames));
  }
}
