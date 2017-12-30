/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LTGEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static String urlToEdit(OwnerType ownerType, String ownerId, String licenseThreatGroupId) {
    return urlToCreate(ownerType, ownerId) + "/" + licenseThreatGroupId;
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return BaseUrl.uriBuilder().fragment("/management/edit/{ownerType}/{ownerId}/licenseThreatGroup")
        .build(ownerType.toString(), ownerId).toString();
  }

  public static SelenideElement title() {
    return $("#ltg-editor").$("h2");
  }

  public static SelenideElement ltgName() {
    return $("#editor-ltg-name");
  }

  public static DoubleColumnPicker picker() {
    return new DoubleColumnPicker();
  }

  public static SelenideElement saveButton() {
    return $("#save-ltg-button");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-ltg-button");
  }
}
