/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.Iterator;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

public class ConditionUtils
{
  public static void shouldHave(final ElementsCollection elements, final WebElementCondition condition) {
    for (Iterator<SelenideElement> iterator = elements.iterator(); iterator.hasNext();) {
      iterator.next().shouldHave(condition);
    }
  }

  public static void shouldNotHave(final ElementsCollection elements, final WebElementCondition condition) {
    for (Iterator<SelenideElement> iterator = elements.iterator(); iterator.hasNext();) {
      iterator.next().shouldNotHave(condition);
    }
  }
}
