/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AssociationEditor;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ApplicationCategoryEditorPage
{
  private static final String ROOT_ID = "#application-category-editor";

  public static final Condition NO_CATEGORIES_DEFINED = Condition.text("No application categories defined.");

  public static String urlToEdit(Owner owner) {
    return urlToEdit(owner.getPublicId());
  }

  public static String urlToEdit(String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/application/{ownerId}/category", ownerId);
  }

  public static SelenideElement root() {
    return $(ROOT_ID);
  }

  public static AssociationEditor associationEditor() {
    return new AssociationEditor(ROOT_ID);
  }

  public static SelenideElement title() {
    return $(".iq-tile h2");
  }

  public static Condition titleText() {
    return Condition.text("Assign Application Categories");
  }

  public static SelenideElement subtitle() {
    return $("label[for=associated-categories]");
  }

  public static Condition subtitleText(String ownerName) {
    return Condition.text("Application Categories Assigned to " + ownerName);
  }

  public static SelenideElement updateButton() {
    return root().$("button[type=submit]");
  }

  public static ErrorBox errorBox() {
    return new ErrorBox(ROOT_ID, ".iq-alert");
  }
}
