/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LTGEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static final int NUM_THREAT_LEVELS = 11;

  public static String urlToEdit(String ownerId, String licenseThreatGroupId) {
    return "new/assets/index.html#/management/edit/organization/" + ownerId + "/licenseThreatGroup/" +
        licenseThreatGroupId;
  }

  public static String urlToCreate(String ownerId) {
    return "new/assets/index.html#/management/edit/organization/" + ownerId + "/licenseThreatGroup";
  }

  public static SelenideElement title() {
    return $("#ltg-editor").$("h2");
  }

  public static SelenideElement ltgName() {
    return $("#editor-ltg-name");
  }

  public static SelenideElement saveButton() {
    return $("#save-ltg-button");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-ltg-button");
  }
}
