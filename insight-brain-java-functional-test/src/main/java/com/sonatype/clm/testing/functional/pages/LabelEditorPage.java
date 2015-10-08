/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LabelEditorPage
{
  public static String url(String ownerType, String ownerId, String labelId) {
    return "new/assets/index.html#/management/" + ownerType + "/" + ownerId + "/label/" + labelId + "/edit";
  }

  public static SelenideElement labelName() {
    return $("#editor-label-name");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-label-button");
  }
}
