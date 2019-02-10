/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.WebElementsCollection;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.ElementsCollection.elementsToString;
import static java.util.stream.Collectors.toList;

public class IqConditions
{
  public static CollectionCondition cssValues(String propertyName, String... values) {
    return new Css(propertyName, values);
  }

  public static CollectionCondition allHaveClass(String className) {
    return new AllHaveClass(className);
  }

  private static class AllHaveClass
      extends CollectionCondition
  {
    private String className;

    public AllHaveClass(String className) {
      this.className = className;
    }

    @Override
    public boolean apply(List<WebElement> input) {
      for (WebElement element : input) {
        if (!cssClass(className).apply(element)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void fail(WebElementsCollection collection,
                     List<WebElement> elements,
                     Exception lastError,
                     long timeoutMs)
    {
      throw new IqAssertionError(
          "\nActual: " + getSafeValues(elements, element -> element.getAttribute("class")) + "\nExpected: " + className
              + "\nCollection: " + collection.description() + "\nElements: " + elementsToString(elements),
          lastError, timeoutMs);
    }
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
        if (!matches(values[i], elements.get(i).getCssValue(propertyName))) {
          return false;
        }
      }

      return true;
    }

    private boolean matches(String expected, String actual) {
      if (expected.equals(actual)) {
        return true;
      }
      if (propertyName.endsWith("color")) {
        Color expectedColor = Color.fromString(expected);
        Color actualColor = Color.fromString(actual);
        // the Firefox driver is known to loose the alpha value so check only RGB
        if (expectedColor.asHex().equals(actualColor.asHex())) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void fail(WebElementsCollection collection,
                     List<WebElement> elements,
                     Exception lastError,
                     long timeoutMs)
    {
      throw new IqAssertionError("\nActual: " + getSafeValues(elements, element -> element.getCssValue(propertyName))
          + "\nExpected: " + Arrays.toString(values) + "\nCollection: " + collection.description() + "\nElements: "
          + elementsToString(elements), lastError, timeoutMs);
    }

    @Override
    public String toString() {
      return String.format("Elements have CSS property %s with values %s", propertyName, Arrays.toString(values));
    }
  }

  private static List<String> getSafeValues(List<WebElement> elements, Function<WebElement, String> valueGetter) {
    return elements != null ? elements.stream().map(element -> getSafeValue(element, valueGetter)).collect(toList())
        : Arrays.asList();
  }

  private static String getSafeValue(WebElement element, Function<WebElement, String> valueGetter) {
    try {
      return valueGetter.apply(element);
    }
    catch (Exception e) {
      return "(unknown)";
    }
  }

  @SuppressWarnings("serial")
  private static class IqAssertionError
      extends UIAssertionError
  {
    protected IqAssertionError(String message, Throwable cause, long timeoutMs) {
      super(message, cause);
      this.timeoutMs = timeoutMs;
    }
  }
}
