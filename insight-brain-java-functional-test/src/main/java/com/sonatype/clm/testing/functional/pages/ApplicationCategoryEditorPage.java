/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.IqAssociationEditor;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Selenide.$;

public class ApplicationCategoryEditorPage
{
  private static final String ROOT_ID = "#application-category-editor";

  public static final WebElementCondition NO_CATEGORIES_DEFINED = Condition.text("There are no items configured");

  public static String urlToEdit(Owner owner) {
    return urlToEdit(owner.getPublicId());
  }

  public static String urlToEdit(String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/application/{ownerId}/category", ownerId);
  }

  public static SelenideElement root() {
    return $(ROOT_ID);
  }

  public static IqAssociationEditor associationEditor() {
    return new IqAssociationEditor(ROOT_ID);
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static WebElementCondition titleText() {
    return Condition.text("Assign Application Categories");
  }

  public static SelenideElement subtitle() {
    return $(".nx-legend > span");
  }

  public static WebElementCondition subtitleText(String ownerName) {
    return Condition.text("Application Categories Assigned to " + ownerName);
  }

  public static SelenideElement updateButton() {
    return root().$(".nx-form__submit-btn ");
  }

  public static ErrorBox errorBox() {
    return new ErrorBox(ROOT_ID, ".nx-alert");
  }
}
