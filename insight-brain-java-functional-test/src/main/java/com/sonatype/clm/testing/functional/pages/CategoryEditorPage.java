/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ColorPicker;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.google.common.base.Joiner;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class CategoryEditorPage
{
  private static SelenideElement root = $("#category-editor");

  private static final ColorPicker colorPicker = new ColorPicker("#editor-category-color-picker");

  public static String urlToEdit(String ownerId, String categoryId) {
    return urlToCreate(ownerId) + "/" + categoryId;
  }

  public static String urlToCreate(String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/organization/{ownerId}/category", ownerId);
  }

  public static SelenideElement root() {
    return root;
  }

  public static SelenideElement title() {
    return root().$("h2");
  }

  public static SelenideElement categoryName() {
    return $("#editor-category-name");
  }

  public static SelenideElement description() {
    return $("#editor-category-description");
  }

  public static ColorPicker colorPicker() {
    return colorPicker;
  }

  public static SelenideElement saveButton() {
    return root().$("button[type^=submit]");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-category-button");
  }

  public static Condition deleteWarningText() {
    return deleteWarningText(null);
  }

  public static Condition deleteWarningText(String applicationNames) {
    String baseMessage = "Are you sure you want to delete this application category?";
    return text(applicationNames == null ? baseMessage :
        baseMessage + " It is in use by the following applications: " + applicationNames + ".");
  }

  public static class DeleteErrorModal
  {
    private static final String ROOT_SELECTOR = "#delete-application-category-error-modal";

    public static SelenideElement root() {
      return $(ROOT_SELECTOR);
    }

    public static SelenideElement message() {
      return $(SelectorUtils.createSelector(ROOT_SELECTOR, ".iq-alert"));
    }

    public static SelenideElement closeButton() {
      return $(SelectorUtils.createSelector(ROOT_SELECTOR, ".btn"));
    }

    public static Condition associatedPoliciesText(String... policyNames) {
      return text("You cannot delete this application category because it is associated with the following policies: " +
          Joiner.on(", ").join(policyNames));
    }
  }
}
