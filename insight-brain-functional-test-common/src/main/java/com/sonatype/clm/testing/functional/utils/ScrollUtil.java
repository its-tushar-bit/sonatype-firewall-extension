/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.time.Duration;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

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
    try {
      Selenide.Wait() //
          .withMessage(element.getSearchCriteria() + " did not scroll to destination") //
          .until(webDriver -> ((JavascriptExecutor) webDriver).executeScript(
              JS_LOCAL_VARS + "return Math.abs(parent[0].scrollTop - (el[0].offsetTop - parentPadding)) < 2;", element)
              .equals(Boolean.TRUE));
    }
    catch (TimeoutException e) {
      throw (UIAssertionError) UIAssertionError.wrap(WebDriverRunner.driver(), e, Configuration.timeout);
    }
  }

  public static final WebElementCondition scrolledOffTop = new ScrolledOffTop();

  /**
   * Selenide's typical `visible` condition doesn't consider whether or not the element is scrolled into view.
   * This condition checks the offsetTop of the element and the scrollTop of the parent to see if the element is
   * scrolled above the visible area of its parent, even partially
   */
  public static class ScrolledOffTop
      extends WebElementCondition
  {
    private JavascriptExecutor executor = getExecutor();

    ScrolledOffTop() {
      super("scrolled-off-top");
    }

    @Override
    public CheckResult check(Driver driver, WebElement element) {
      Boolean scrolledOffTop = (Boolean) executor
          .executeScript(JS_LOCAL_VARS + "return parent[0].scrollTop > el[0].offsetTop - parentPadding;", element);

      return new CheckResult(scrolledOffTop, element);
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
    return awaitEndOfScrolling(element.scrollIntoView(alignToTop));
  }

  /**
   * Scrolls the given element into view with behavior 'instant'.
   * {@link SelenideElement#scrollTo()} tries to scroll the entire window which
   * doesn't help for containers with scrollable content.
   */
  public static SelenideElement scrollIntoViewInstantly(final SelenideElement element) {
    return awaitEndOfScrolling(element.scrollIntoView("{ behavior: \"instant\", block: \"end\" }"));
  }

  /**
   * Waits for any scrolling affecting the given element to finish to ensure later clicks don't miss their target.
   */
  public static SelenideElement awaitEndOfScrolling(final SelenideElement element) {
    SelenideElement selenideElement = element.shouldBe(new WebElementCondition("done scrolling")
    {
      Point previousLocation;

      Point currentLocation;

      @Override
      public CheckResult check(Driver driver, WebElement element) {
        previousLocation = currentLocation;
        currentLocation = element.getLocation();
        boolean doneScrolling = currentLocation.equals(previousLocation);
        return new CheckResult(doneScrolling, element);
      }
    }, Duration.ofMillis(Configuration.timeout * 2));

    return selenideElement;
  }
}
