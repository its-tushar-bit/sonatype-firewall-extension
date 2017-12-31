/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ColorPicker;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LabelEditorPage
{
  private static final ColorPicker colorPicker = new ColorPicker("#editor-label-color-picker");

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
    return BaseUrl.uriBuilder().fragment("/management/edit/{ownerType}/{ownerId}/label").build(ownerType, ownerId)
        .toString();
  }

  public static SelenideElement title() {
    return $("#label-editor").$("h2");
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
