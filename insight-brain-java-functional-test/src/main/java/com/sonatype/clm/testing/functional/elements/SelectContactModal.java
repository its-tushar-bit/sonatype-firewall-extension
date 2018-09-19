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
  private static final String ROOT = "#select-application-contact-modal";

  public static Condition headerText() {
    return text("Select Contact");
  }

  public static SelenideElement header() {
    return $(ROOT + " .iq-modal-header");
  }

  public static SelenideElement body() {
    return $(ROOT + " .iq-modal-content");
  }

  public static SelenideElement currentUserLabel() {
    return $(ROOT + " #current-contact");
  }

  public static SelenideElement searchBox() {
    return $(ROOT + " #user-search-input");
  }

  public static SelenideElement searchButton() {
    return $(ROOT + " #user-search-button");
  }

  public static ElementsCollection users() {
    return $$(ROOT + " iq-radio");
  }

  public static SelenideElement updateButton() {
    return $(ROOT + " .iq-btn--primary");
  }

  public static SelenideElement cancelButton() {
    return $(ROOT + " #cancel-select-contact-button");
  }

  public static SelenideElement removeButton() {
    return $(ROOT + " .iq-btn--tertiary");
  }

  public static IqRadio userRadio(final String usersName) {
    SelenideElement item = $$(ROOT + " iq-radio").findBy(text(usersName));
    if (item != null) {
      return new IqRadio(item);
    }
    return null;
  }
}
