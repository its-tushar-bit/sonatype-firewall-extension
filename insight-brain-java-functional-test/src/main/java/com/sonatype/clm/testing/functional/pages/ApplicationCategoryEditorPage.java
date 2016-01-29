/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AssociationEditor;
import com.sonatype.clm.testing.functional.elements.ErrorBox;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ApplicationCategoryEditorPage
{
  public static final Condition NO_CATEGORIES_DEFINED = Condition.text("No application categories defined.");

  public static String urlToEdit(String ownerId) {
    return "new/assets/index.html#/management/edit/application/" + ownerId + "/category";
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

  public static ErrorBox errorBox() {
    return new ErrorBox(root().$(".clm-alert"));
  }
}
