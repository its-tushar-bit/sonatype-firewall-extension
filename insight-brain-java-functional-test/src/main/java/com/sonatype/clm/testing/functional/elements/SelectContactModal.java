/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class SelectContactModal
{
  private static final String ROOT = "#select-contact-modal";

  public static Condition headerText() {
    return text("Select Contact");
  }

  public static SelenideElement header() {
    return $(ROOT + " .nx-modal-header");
  }

  public static SelenideElement body() {
    return $(ROOT + " .nx-modal-content");
  }

  public static SelenideElement searchBox() {
    return $(ROOT + " input[role=combobox]");
  }

  public static ElementsCollection users() {
    return $$(ROOT + " .nx-dropdown-menu button[role=option]");
  }

  public static SelenideElement updateButton() {
    return $(ROOT + " .nx-btn--primary");
  }

  public static SelenideElement cancelButton() {
    return $(ROOT + " .nx-btn--secondary");
  }

  public static SelenideElement removeButton() {
    return $(ROOT + " .nx-btn--primary");
  }

  public static SelenideElement userContact(final String usersName) {
    return $$(ROOT + " .nx-dropdown-menu button[role=option]").findBy(text(usersName));
  }
}
