/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.CheckResult.Verdict;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementsCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.CollectionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;

import static com.codeborne.selenide.Condition.cssClass;
import static java.util.stream.Collectors.toList;

public class IqConditions
{
  public static WebElementsCondition cssValues(String propertyName, String... values) {
    return new Css(propertyName, values);
  }

  public static WebElementsCondition allHaveClass(String className) {
    return new AllHaveClass(className);
  }

  private static class AllHaveClass
      extends WebElementsCondition
  {
    private final String className;

    public AllHaveClass(String className) {
      this.className = className;
    }

    @Override
    public CheckResult check(final CollectionSource collection) {
      for (WebElement element : collection.getElements()) {
        if (cssClass(className).check(WebDriverRunner.driver(), element).verdict() == Verdict.REJECT) {
          return CheckResult.rejected("className not found: " + className, null);
        }
      }
      return CheckResult.accepted();
    }

    @Override
    public void fail(
        final CollectionSource collection,
        final CheckResult lastCheckResult,
        @Nullable final Exception lastError,
        final long timeoutMs)
    {
      List<WebElement> elements = collection.getElements();
      String elementsString = elements.stream().map(WebElement::getText).collect(Collectors.joining(","));
      throw new IqAssertionError(
          "\nActual: " + getSafeValues(elements, element -> element.getAttribute("class")) + "\nExpected: " + className
              + "\nCollection: " + collection.description() + "\nElements: " + elementsString,
          lastError, timeoutMs);
    }

    @Override
    public boolean missingElementsSatisfyCondition() {
      return false;
    }

    @Override
    public String toString() {
      return "AllHaveClass" + className;
    }
  }

  private static class Css
      extends WebElementsCondition
  {
    private final String propertyName;

    private final String[] values;

    public Css(String propertyName, String... values) {
      this.propertyName = propertyName;
      this.values = values;
    }

    @NotNull
    @Override
    public CheckResult check(final CollectionSource collection) {
      List<WebElement> elements = collection.getElements();

      if (elements.size() != values.length) {
        return CheckResult.rejected("Sizes differ", elements.size());
      }

      for (int i = 0; i < values.length; i++) {
        String cssValue = elements.get(i).getCssValue(propertyName);
        if (!matches(values[i], cssValue)) {
          return CheckResult.rejected("Values do not match", cssValue);
        }
      }

      return CheckResult.accepted();
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
    public void fail(
        final CollectionSource collection,
        final CheckResult lastCheckResult,
        @Nullable final Exception lastError,
        final long timeoutMs)
    {
      List<WebElement> elements = collection.getElements();
      String elementsString = elements.stream().map(WebElement::getText).collect(Collectors.joining(", "));

      throw new IqAssertionError("\nActual: " + getSafeValues(elements, element -> element.getCssValue(propertyName))
          + "\nExpected: " + Arrays.toString(values) + "\nCollection: " + collection.description() + "\nElements: "
          + elementsString, lastError, timeoutMs);
    }

    @Override
    public boolean missingElementsSatisfyCondition() {
      return false;
    }

    @Override
    public String toString() {
      return String.format("Elements have CSS property %s with values %s", propertyName, Arrays.toString(values));
    }
  }

  private static List<String> getSafeValues(List<WebElement> elements, Function<WebElement, String> valueGetter) {
    return elements != null
        ? elements.stream().map(element -> getSafeValue(element, valueGetter)).collect(toList())
        : Collections.emptyList();
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
      super(WebDriverRunner.driver(), message, cause, timeoutMs);
    }
  }
}
