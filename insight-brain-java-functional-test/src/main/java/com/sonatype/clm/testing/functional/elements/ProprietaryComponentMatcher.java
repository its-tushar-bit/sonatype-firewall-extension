/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;

public class ProprietaryComponentMatcher
{
  private SelenideElement listItem;

  public ProprietaryComponentMatcher(String rootSelector, MatcherType type, String name) {
    this.listItem = $$(rootSelector + " .nx-list__subtext")
        .findBy(text(type.listValue))
        .parent()
        .findAll(".nx-list__text")
        .findBy(text(name))
        .parent();
  }

  public SelenideElement name() {
    return listItem;
  }

  public SelenideElement deleteButton() {
    return listItem.find(".nx-btn--icon-only");
  }

  public enum MatcherType
  {
    PACKAGE("Package"),
    REGEX("Regular Expression", "RegEx");

    public final String name;

    public final String listValue;

    MatcherType(String name, String listValue) {
      this.name = name;
      this.listValue = listValue;
    }

    MatcherType(String name) {
      this(name, name);
    }
  }
}
