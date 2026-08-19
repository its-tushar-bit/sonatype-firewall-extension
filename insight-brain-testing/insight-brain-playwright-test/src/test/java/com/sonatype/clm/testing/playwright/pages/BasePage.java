/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Base class for all Playwright page objects.
 *
 * Wraps a Playwright {@link Page} and provides convenience methods for
 * locating elements, navigating, and asserting. Unlike Selenide's
 * {@code BasicElement} which wraps a CSS selector, this wraps the
 * entire page and returns {@link Locator} objects that auto-wait and
 * auto-retry, eliminating stale-element issues.
 *
 * <p>
 * The active {@link Page} is stored in a {@link ThreadLocal} that
 * {@code AbstractIqUiTest} sets in its {@code @Before} and clears in
 * {@code @After}. Page objects use the no-arg constructor to pick it up
 * automatically, removing the need to pass {@code page} through every
 * constructor call in test code.
 */
public abstract class BasePage
{
  private static final ThreadLocal<Page> CURRENT_PAGE = new ThreadLocal<>();

  /**
   * Sets the current {@link Page} for the calling thread.
   * <p>
   * <b>Caller responsibility:</b> every {@code setCurrent(page)} <em>must</em> be paired with a
   * subsequent {@link #clearCurrent()} (typically in a {@code finally} block or an {@code @After}
   * / {@code TestRule} cleanup) — otherwise the {@code ThreadLocal} retains a reference to the
   * (closed) {@link Page} and leaks it into the next test reusing the same Surefire thread.
   * {@code AbstractIqUiTest} already wires this correctly via its lifecycle {@code TestRule}.
   */
  public static void setCurrent(Page page) {
    CURRENT_PAGE.set(page);
  }

  /**
   * Clears the current {@link Page} for the calling thread. Required to release the
   * {@code ThreadLocal} reference (see {@link #setCurrent(Page)}). Called by
   * {@code AbstractIqUiTest}'s lifecycle rule.
   */
  public static void clearCurrent() {
    CURRENT_PAGE.remove();
  }

  protected final Page page;

  public Page playwrightPage() {
    return page;
  }

  /**
   * Constructs a page object using the thread-local current page.
   *
   * @throws IllegalStateException if no current page has been set
   */
  protected BasePage() {
    this.page = CURRENT_PAGE.get();
    if (this.page == null) {
      throw new IllegalStateException(
          getClass().getSimpleName()
              + ": no current Page — call BasePage.setCurrent(page) in @Before (AbstractIqUiTest does this automatically)");
    }
  }

  protected Locator locator(String cssSelector) {
    return page.locator(cssSelector);
  }

  protected Locator byTestId(String testId) {
    return page.getByTestId(testId);
  }

  protected Locator byRole(AriaRole role) {
    return page.getByRole(role);
  }

  protected Locator byRole(AriaRole role, String name) {
    return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
  }

  protected Locator byRole(AriaRole role, Pattern name) {
    return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
  }

  /** {@code NxLoadError} alert scoped to a container. */
  protected Locator nxLoadErrorAlert(Locator scope) {
    return scope.locator(".nx-alert--load-error");
  }

  protected Locator nxLoadErrorAlert() {
    return locator(".nx-alert--load-error");
  }

  /** Retry button inside a {@code NxLoadError} alert. */
  protected Locator nxLoadErrorRetryButton(Locator scope) {
    return nxLoadErrorAlert(scope).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Retry"));
  }

  protected Locator nxLoadErrorRetryButton() {
    return nxLoadErrorAlert().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Retry"));
  }

  /**
   * Inline validation message rendered by {@code NxTextInput} / {@code NxFormGroup}. Walks up from
   * the given input to its enclosing {@code .nx-form-group} then locates the sibling
   * {@code .nx-field-validation-message} — dashes-separated, not BEM double-underscore.
   */
  protected Locator nxFieldValidationMessage(Locator input) {
    return input.locator("xpath=ancestor::*[contains(@class,'nx-form-group')][1]")
        .locator(".nx-field-validation-message");
  }

  protected Locator nxToggleLabel(String accessibleName) {
    return page.locator("label.nx-toggle")
        .filter(new Locator.FilterOptions().setHasText(exactTextPattern(accessibleName)));
  }

  protected Locator nxToggleLabel(Locator scope, String accessibleName) {
    return scope.locator("label.nx-toggle")
        .filter(new Locator.FilterOptions().setHasText(exactTextPattern(accessibleName)));
  }

  private static Pattern exactTextPattern(String text) {
    return Pattern.compile("^" + REGEX_METACHARS.matcher(text).replaceAll("\\\\$0") + "$");
  }

  protected Locator nxToggleInput(String accessibleName) {
    return nxToggleLabel(accessibleName).locator("input.nx-toggle__input");
  }

  protected Locator nxToggleInput(Locator scope, String accessibleName) {
    return nxToggleLabel(scope, accessibleName).locator("input.nx-toggle__input");
  }

  /**
   * IQ global left navigation ({@code IqSidebarNav.jsx} / {@code NxGlobalSidebar2}).
   * Matches the frontend unit test: {@code getByRole('navigation', { name: 'global sidebar' })}.
   */
  protected Locator globalSidebarNavigation() {
    return byRole(AriaRole.NAVIGATION, "global sidebar");
  }

  /**
   * A sidebar nav link by its visible text (e.g. {@code "Dashboard"}, {@code "Orgs and Policies"}).
   */
  protected Locator globalSidebarLink(String linkName) {
    return globalSidebarNavigation().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(linkName));
  }

  protected Locator byText(String text) {
    return page.getByText(text);
  }

  protected Locator byLabel(String label) {
    return page.getByLabel(label);
  }

  protected Locator byPlaceholder(String placeholder) {
    return page.getByPlaceholder(placeholder);
  }

  protected Locator byAltText(String altText) {
    return page.getByAltText(altText);
  }

  protected Locator byTitle(String title) {
    return page.getByTitle(title);
  }

  protected void navigateTo(String url) {
    page.navigate(url);
  }

  protected void waitForPageLoad() {
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  protected void waitForDomReady() {
    page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  }

  protected void waitForVisible(String selector) {
    page.locator(selector)
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
  }

  protected void waitForVisible(String selector, double timeoutMs) {
    page.locator(selector)
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeoutMs));
  }

  protected void waitForHidden(String selector) {
    page.locator(selector)
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  protected void waitForHidden(String selector, double timeoutMs) {
    page.locator(selector)
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(timeoutMs));
  }

  /**
   * Waits for an element to become visible and then hidden again.
   * Replaces Selenide's {@code NxSubmitMask.seeAndWaitForDismissal()} pattern.
   */
  protected void waitForVisibleThenHidden(String selector) {
    waitForVisibleThenHidden(selector, PlaywrightTiming.SHORT_UI_CUE_MS, PlaywrightTiming.ELEMENT_TIMEOUT_MS);
  }

  protected void waitForVisibleThenHidden(String selector, double visibleTimeoutMs, double hiddenTimeoutMs) {
    Locator element = page.locator(selector);
    try {
      element.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.VISIBLE)
          .setTimeout(visibleTimeoutMs));
      element.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.HIDDEN)
          .setTimeout(hiddenTimeoutMs));
    }
    catch (TimeoutError ignored) {
      // Element may have appeared and disappeared faster than Playwright could observe it
      // (e.g. transient submit-mask). Anything other than a TimeoutError — page crash, browser
      // closed, etc. — should propagate so the test fails loudly.
    }
  }

  protected void shouldBeVisible(String selector) {
    assertThat(page.locator(selector)).isVisible();
  }

  protected void shouldBeHidden(String selector) {
    assertThat(page.locator(selector)).isHidden();
  }

  protected void shouldHaveText(String selector, String expectedText) {
    assertThat(page.locator(selector)).hasText(expectedText);
  }

  protected void shouldContainText(String selector, String expectedText) {
    assertThat(page.locator(selector)).containsText(expectedText);
  }

  protected void shouldHaveValue(String selector, String expectedValue) {
    assertThat(page.locator(selector)).hasValue(expectedValue);
  }

  protected void shouldHaveCount(String selector, int expectedCount) {
    assertThat(page.locator(selector)).hasCount(expectedCount);
  }

  protected void shouldHaveCssClass(String selector, String cssClass) {
    assertThat(page.locator(selector)).hasClass(cssClassPattern(cssClass));
  }

  /**
   * Cache compiled {@link Pattern}s for class-name assertions. Class boundaries are detected via
   * {@code (?<![\w-]) ... (?![\w-])} so {@code foo} does not match inside {@code foo-bar} —
   * {@code \b} alone would because {@code \b} treats {@code -} as a word boundary.
   *
   * <p>
   * Cannot use {@link Pattern#quote(String)}: Playwright serialises the pattern to Chromium's
   * JavaScript {@code RegExp} which does not support {@code \Q...\E} (they parse as literal
   * {@code Q}/{@code E}). We escape regex metacharacters character-by-character instead.
   */
  private static final ConcurrentMap<String, Pattern> CSS_CLASS_PATTERN_CACHE = new ConcurrentHashMap<>();

  private static final Pattern REGEX_METACHARS = Pattern.compile("[\\\\^$.|?*+()\\[\\]{}]");

  static Pattern cssClassPattern(String cssClass) {
    return CSS_CLASS_PATTERN_CACHE.computeIfAbsent(cssClass,
        c -> Pattern.compile(".*(?<![\\w-])" + REGEX_METACHARS.matcher(c).replaceAll("\\\\$0") + "(?![\\w-]).*"));
  }

  /**
   * MUI tooltip popup rendered in a portal — only present in the DOM after the trigger element
   * is hovered. Use {@code assertThat(tooltip()).containsText("...")} after calling
   * {@code trigger.hover()} to verify the tooltip text.
   */
  public Locator tooltip() {
    return page.getByRole(AriaRole.TOOLTIP);
  }

  /**
   * JS-RegExp-safe escape for runtime input interpolated into a Playwright {@link Pattern}.
   * Don't use {@link Pattern#quote(String)} — its {@code \Q…\E} silently breaks under JS RegExp.
   */
  public static String escapeForJsRegex(String text) {
    return text.replaceAll("[\\\\^$.|?*+()\\[\\]{}]", "\\\\$0");
  }
}
