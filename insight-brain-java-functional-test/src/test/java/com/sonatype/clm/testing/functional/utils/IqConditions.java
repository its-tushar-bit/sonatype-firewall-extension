/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.WebElementsCollection;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.ElementsCollection.elementsToString;

public class IqConditions
{

  public static CollectionCondition cssValues(String propertyName, String... values) {
    return new Css(propertyName, values);
  }

  private static class Css
      extends CollectionCondition
  {
    private final String propertyName;

    private final String[] values;

    public Css(String propertyName, String... values) {
      this.propertyName = propertyName;
      this.values = values;
    }

    @Override
    public boolean apply(List<WebElement> elements) {
      if (elements.size() != values.length) {
        return false;
      }

      for (int i = 0; i < values.length; i++) {
        if (!values[i].equals(elements.get(i).getCssValue(propertyName))) {
          return false;
        }
      }

      return true;
    }

    @Override
    public void fail(WebElementsCollection collection,
                     List<WebElement> actualElements,
                     Exception lastError,
                     long timeoutMs)
    {
      @SuppressWarnings("serial")
      UIAssertionError error = new UIAssertionError(": expected: " + Arrays.toString(values) + ", \ncollection: "
          + collection.description() + "\nElements: " + elementsToString(actualElements), lastError)
      {
      };
      error.timeoutMs = timeoutMs;
      throw error;
    }

    @Override
    public String toString() {
      return String.format("Elements have CSS property %s with values %s", values, Arrays.toString(values));
    }
  }
}
