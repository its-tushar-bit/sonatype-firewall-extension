/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ColorPicker;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class CategoryEditorPage
{

  private static SelenideElement root = $("#category-editor");

  private static final ColorPicker colorPicker = new ColorPicker("#editor-category-color-picker");

  public static String urlToEdit(String ownerId, String categoryId) {
    return "new/assets/index.html#/management/edit/organization/" + ownerId + "/category/" + categoryId;
  }

  public static String urlToCreate(String ownerId) {
    return "new/assets/index.html#/management/edit/organization/" + ownerId + "/category";
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
}
