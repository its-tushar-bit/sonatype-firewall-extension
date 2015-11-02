/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ColorPicker;
import com.codeborne.selenide.SelenideElement;
import static com.codeborne.selenide.Selenide.$;

public class LabelEditorPage
{
  private static final ColorPicker colorPicker = new ColorPicker($("#editor-label-color-picker"));

  public static String urlToEdit(String ownerType, String ownerId, String labelId) {
    return "new/assets/index.html#/management/edit/" + ownerType + "/" + ownerId + "/label/" + labelId;
  }

  public static String urlToCreate(String ownerType, String ownerId) {
    return "new/assets/index.html#/management/edit/" + ownerType + "/" + ownerId + "/label";
  }

  public static SelenideElement title() {
    return  $("#label-editor").$("h2");
  }

  public static SelenideElement labelName() {
    return $("#editor-label-name");
  }

  public static SelenideElement description() {
    return $("#editor-label-description");
  }

  public static ColorPicker colorPicker() {
    return colorPicker;
  }

  public static SelenideElement saveButton() {
    return $("button[type^=submit]");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-label-button");
  }
}
