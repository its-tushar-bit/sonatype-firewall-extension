/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AssociationEditor;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ApplicationCategoryEditorPage
{
  public static String urlToEdit(String ownerId) {
    return "new/assets/index.html#/management/application/" + ownerId + "/categories/edit";
  }

  public static SelenideElement root() {
    return $("#application-category-editor");
  }

  public static AssociationEditor associationEditor() {
    return new AssociationEditor(root());
  }

  public static SelenideElement title() {
    return $(".tile h2");
  }

  public static SelenideElement updateButton() {
    return root().$("button[type=submit]");
  }

}
