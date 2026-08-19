/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Focused wait helpers for Playwright tests.
 *
 * <p>
 * URL waits use a substring predicate so SPA hash routes (e.g. {@code #/dashboard/...})
 * match reliably. Conditional waits delegate to {@link Awaitility} so polling is decoupled
 * from the Playwright {@link Page} (no {@code page.waitForTimeout} fixed sleep).
 */
public final class PlaywrightWaitUtils
{
  private PlaywrightWaitUtils() {
  }

  /**
   * Wait until the full URL (including hash) contains the given substring.
   */
  public static void waitForUrl(Page page, String urlSubstring) {
    Objects.requireNonNull(urlSubstring, "urlSubstring");
    page.waitForURL(
        url -> url != null && url.contains(urlSubstring),
        new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_SUBSTRING_TIMEOUT_MS));
  }

  /**
   * Wait for a condition to become true, polling at intervals.
   */
  public static void waitForCondition(BooleanSupplier condition, long timeoutMs, long pollIntervalMs) {
    waitForCondition(condition, timeoutMs, pollIntervalMs,
        "Condition not met within " + timeoutMs + "ms");
  }

  /**
   * Wait for a condition to become true, polling at intervals.
   *
   * <p>
   * Transient {@link PlaywrightException}s thrown by the supplied condition (for example,
   * short-lived DOM re-renders, detached nodes, brief network blips) are ignored so polling
   * continues until timeout. Any other runtime exception propagates immediately so the test
   * fails fast with the real cause.
   */
  public static void waitForCondition(
      BooleanSupplier condition,
      long timeoutMs,
      long pollIntervalMs,
      String timeoutMessage)
  {
    try {
      Awaitility.await()
          .atMost(Duration.ofMillis(timeoutMs))
          .pollInterval(Duration.ofMillis(pollIntervalMs))
          .ignoreException(PlaywrightException.class)
          .until(condition::getAsBoolean);
    }
    catch (ConditionTimeoutException e) {
      throw new AssertionError(timeoutMessage, e);
    }
  }

  /**
   * Wait until a locator becomes visible.
   */
  public static void waitForVisible(Locator locator, long timeoutMs, long pollIntervalMs) {
    waitForCondition(
        locator::isVisible,
        timeoutMs,
        pollIntervalMs,
        "Timed out waiting for locator to become visible: " + locator);
  }

  /**
   * Wait until a locator becomes visible.
   */
  public static void waitForVisible(Locator locator) {
    waitForVisible(locator, PlaywrightTiming.ELEMENT_TIMEOUT_MS, PlaywrightTiming.POLL_INTERVAL_MS);
  }

  /**
   * Wait until a locator becomes hidden.
   */
  public static void waitForHidden(Locator locator, long timeoutMs, long pollIntervalMs) {
    waitForCondition(
        locator::isHidden,
        timeoutMs,
        pollIntervalMs,
        "Timed out waiting for locator to become hidden: " + locator);
  }

  /**
   * Wait until a locator becomes hidden.
   */
  public static void waitForHidden(Locator locator) {
    waitForHidden(locator, PlaywrightTiming.ELEMENT_TIMEOUT_MS, PlaywrightTiming.POLL_INTERVAL_MS);
  }

  /**
   * Click and wait until another locator becomes visible.
   */
  public static void clickAndWaitForVisible(
      Locator clickTarget,
      Locator observedLocator,
      long timeoutMs,
      long pollIntervalMs)
  {
    clickTarget.click();
    waitForVisible(observedLocator, timeoutMs, pollIntervalMs);
  }

  /**
   * Click and wait until another locator becomes hidden.
   */
  public static void clickAndWaitForHidden(
      Locator clickTarget,
      Locator observedLocator,
      long timeoutMs,
      long pollIntervalMs)
  {
    clickTarget.click();
    waitForHidden(observedLocator, timeoutMs, pollIntervalMs);
  }

  /**
   * Click and wait until URL contains the expected fragment.
   */
  public static void clickAndWaitForUrlContains(
      Page page,
      Locator clickTarget,
      String urlSubstring)
  {
    clickTarget.click();
    waitForUrl(page, urlSubstring);
  }
}
