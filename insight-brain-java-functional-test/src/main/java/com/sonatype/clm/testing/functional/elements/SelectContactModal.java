/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.name;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class SelectContactModal
{
  public static Condition headerText() {
    return text("Select Application Contact");
  }

  public static SelenideElement root() {
    return $("#select-application-contact-modal");
  }

  public static SelenideElement header() {
    return root().$(".clm-modal-header");
  }

  public static SelenideElement body() {
    return root().$(".clm-modal-body");
  }

  public static SelenideElement currentUserLabel() {
    return root().$("#current-contact");
  }

  public static SelenideElement searchBox() {
    return root().$("#user-search-input");
  }

  public static SelenideElement searchButton() {
    return root().$("#user-search-button");
  }

  public static ElementsCollection users() {
    return root().findAll("td");
  }

  public static SelenideElement updateButton() {
    return root().$(".btn-primary");
  }

  public static SelenideElement cancelButton() {
    return root().$("#cancel-select-contact-button");
  }

  public static SelenideElement removeButton() {
    return root().$(".btn-tertiary");
  }

  public static SelenideElement userRadio(final String usersName) {
    SelenideElement td = root().findAll(By.tagName("td")).findBy(text(usersName));
    if (td != null) {
      return td.find("input");
    }
    return null;
  }
}
