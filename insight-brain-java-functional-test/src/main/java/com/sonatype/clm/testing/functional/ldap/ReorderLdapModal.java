/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.ldap;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ReorderLdapModal
    extends BasicElement<ReorderLdapModal>
{
  private static final String ROOT = "#ldap-server-ordering-modal";

  public static final Condition SELECTED = cssClass("selected");

  public ReorderLdapModal() {
    super(ROOT);
  }

  public SelenideElement row(int i) {
    return child("li", nthChild(i + 1));
  }

  public SelenideElement moveDownButton() {
    return child("#move-down");
  }

  public SelenideElement moveToFirstButton() {
    return child("#move-to-first");
  }

  public SelenideElement moveToLastButton() {
    return child("#move-to-last");
  }

  public SelenideElement moveUpButton() {
    return child("#move-up");
  }

  private ElementsCollection rows() {
    return children("li");
  }

  public void assertUpDownButtonEnabled(boolean expectedUp, boolean expectedDown) {
    if (expectedUp) {
      moveUpButton().shouldBe(enabled);
      moveToFirstButton().shouldBe(enabled);
    }
    else {
      moveUpButton().shouldBe(disabled);
      moveToFirstButton().shouldBe(disabled);
    }

    if (expectedDown) {
      moveDownButton().shouldBe(enabled);
      moveToLastButton().shouldBe(enabled);
    }
    else {
      moveDownButton().shouldBe(disabled);
      moveToLastButton().shouldBe(disabled);
    }
  }

  public void assertOrder(String... serverNames) {
    rows().shouldHave(texts(serverNames));
  }

  public SelenideElement saveButton() {
    return child("button[type=submit]");
  }
}
