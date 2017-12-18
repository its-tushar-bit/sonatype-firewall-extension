/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.function.Function;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.cssClass;

/**
 * Utilities for detecting and affecting scroll position. NOTE: The implementations here assume that the element's
 * offsetParent is what is scrolling.
 */
public class ScrollUtil
{
  // JavaScript creating a few local variables that are needed by several functions in this class
  private static final String JS_LOCAL_VARS = //
    "var el = $(arguments[0]), parent = el.parent(), parentPadding = parseFloat(parent.css('padding-top')); ";

  /**
   * Attempts to scroll the parent of `element` such that `element` is at the top of the visible area of the parent
   */
  public static void scrollToTop(SelenideElement element) {
    JavascriptExecutor executor = getExecutor();

    // match the scroll position of the parent with the offset of the element, taking padding into account
    executor.executeScript(JS_LOCAL_VARS + "parent[0].scrollTop = el[0].offsetTop - parentPadding;", element);

    // wait until the scroll completes and the values match what we set them to
    Selenide.Wait() //
        .withMessage("Report did not complete loading") //
        .until(webDriver -> ((JavascriptExecutor) webDriver)
            .executeScript(JS_LOCAL_VARS + "return parent[0].scrollTop === el[0].offsetTop - parentPadding;", element)
            .equals(Boolean.TRUE));
  }

  public static final Condition scrolledOffTop = new ScrolledOffTop();

  public static final Condition scrollSpyInitialized = cssClass("scroll-spy-initialized");

  /**
   * Selenide's typical `visible` condition doesn't consider whether or not the element is scrolled into view.
   * This condition checks the offsetTop of the element and the scrollTop of the parent to see if the element is
   * scrolled above the visible area of its parent, even partially
   */
  public static class ScrolledOffTop
      extends Condition
  {
    private JavascriptExecutor executor = getExecutor();

    ScrolledOffTop() {
      super("scrolled-off-top");
    }

    @Override
    public boolean apply(WebElement element) {
      return (Boolean) executor
          .executeScript(JS_LOCAL_VARS + "return parent[0].scrollTop > el[0].offsetTop - parentPadding;", element);
    }
  }

  private static JavascriptExecutor getExecutor() {
    return (JavascriptExecutor) WebDriverRunner.getWebDriver();
  }

  /**
   * Scrolls the given element into view. {@link SelenideElement#scrollTo()} tries to scroll the entire window which
   * doesn't help for containers with scrollable content.
   */
  public static SelenideElement scrollIntoView(final SelenideElement element) {
    return scrollIntoView(element, true);
  }

  /**
   * Scrolls the given element into view. {@link SelenideElement#scrollTo()} tries to scroll the entire window which
   * doesn't help for containers with scrollable content.
   */
  public static SelenideElement scrollIntoView(final SelenideElement element, final boolean alignToTop) {
    Selenide.Wait().ignoring(StaleElementReferenceException.class).until(webDriver -> ((JavascriptExecutor) webDriver)
        .executeScript("arguments[0].scrollIntoView(arguments[1]); return 1", element, alignToTop));
    awaitEndOfScrolling(element);
    return element;
  }

  /**
   * Waits for any scrolling affecting the given element to finish to ensure later clicks don't miss their target.
   */
  public static void awaitEndOfScrolling(final SelenideElement element) {
    Selenide.Wait().until(new Function<WebDriver, Boolean>()
    {
      Point location;

      @Override
      public Boolean apply(WebDriver input) {
        Point oldLocation = location;
        location = element.getLocation();
        return location.equals(oldLocation);
      }
    });
  }
}
