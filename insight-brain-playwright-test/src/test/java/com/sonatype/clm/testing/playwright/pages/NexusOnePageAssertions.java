/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.Map;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertions companion for {@link NexusOnePage}.
 */
public class NexusOnePageAssertions
{
  /** Midpoint perceived brightness — above is "light", below is "dark". */
  private static final double MIDPOINT = 0.5d;

  /**
   * In-page JavaScript probe used by {@link #assertBrightnessEventually}. Reads the computed
   * CSS value via {@code getComputedStyle}, lets the browser parse it by assigning to a
   * canvas {@code fillStyle} (which accepts every CSS color form Chromium understands —
   * legacy {@code rgb()}/{@code rgba()}, hex, named colours, modern whitespace-syntax
   * {@code rgb(R G B / A)}, {@code oklch()}, {@code color-mix()}, etc.), then samples a 1×1
   * pixel and returns the perceived brightness ({@code max(r, g, b) / 255} — the V component
   * of HSV, same definition the Selenide predecessor used via {@code java.awt.Color.RGBtoHSB})
   * alongside the original CSS string for diagnostics.
   *
   * <p>
   * Delegating colour parsing to the browser keeps the helper working as Chromium evolves its
   * CSSOM serialization and avoids a Java-side regex that would have to grow forever. The
   * Selenium predecessor used {@code org.openqa.selenium.support.Color.fromString(...)};
   * Playwright Java has no equivalent helper, but the browser-canvas trick is strictly more
   * robust than either option (no parse failures possible — anything {@code fillStyle} doesn't
   * accept silently falls back to opaque black, which a stable theme should never produce).
   */
  private static final String BRIGHTNESS_PROBE_SCRIPT =
      """
          (el, prop) => {
            const css = window.getComputedStyle(el).getPropertyValue(prop);
            const ctx = document.createElement('canvas').getContext('2d');
            ctx.fillStyle = css;
            ctx.fillRect(0, 0, 1, 1);
            const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
            return { css, brightness: Math.max(r, g, b) / 255 };
          }
          """;

  private final NexusOnePage page;

  public NexusOnePageAssertions(NexusOnePage page) {
    this.page = page;
  }

  /**
   * The Nexus One SPA is mounted and its themed content surface is on screen.
   */
  public void shouldBeVisible() {
    assertThat(page.pageSurface()).isVisible();
  }

  /**
   * The single page-level h1 displays the given text — used to differentiate the Platform
   * Home route ({@code "Nexus One"}) from a Coming Soon route ({@code "Coming Soon"}).
   */
  public void shouldHaveHeadingText(String expected) {
    assertThat(page.heading()).hasText(expected);
  }

  /**
   * The page surface is rendered with a light background and the heading with dark text.
   * Polls until the rendered colours fall on the expected side of {@link #MIDPOINT} or the
   * element timeout elapses — the theme swap is asynchronous after a
   * {@code prefers-color-scheme} change (the {@code useNoscTheme} hook reacts to a
   * {@code matchMedia} event).
   */
  public void shouldHaveLightAppearance() {
    assertBrightnessEventually(page.pageSurface(), "background-color", true, "light background");
    assertBrightnessEventually(page.heading(), "color", false, "dark heading text");
  }

  /**
   * The page surface is rendered with a dark background and the heading with light text.
   * See {@link #shouldHaveLightAppearance()} for polling rationale.
   */
  public void shouldHaveDarkAppearance() {
    assertBrightnessEventually(page.pageSurface(), "background-color", false, "dark background");
    assertBrightnessEventually(page.heading(), "color", true, "light heading text");
  }

  // ---- internals ----

  /**
   * Polls the {@link #BRIGHTNESS_PROBE_SCRIPT} until the perceived brightness lands on the
   * expected side of {@link #MIDPOINT}, or the element timeout elapses.
   * {@link PlaywrightWaitUtils#waitForCondition} is the project-mandated wait primitive
   * ({@code PlaywrightStabilityRulesCheck} bans {@link Thread#sleep}); transient
   * {@link com.microsoft.playwright.PlaywrightException}s from a detached node mid-rerender
   * are ignored by the helper so the loop keeps polling. The most recently observed CSS
   * value and brightness are captured in a method-local {@link ProbeState} so the timeout
   * message can quote them — the lambda needs a mutable reference and Java requires captured
   * locals to be effectively final, so a tiny holder is the cleanest workaround. The state is
   * deliberately NOT promoted to instance fields because it is method scratchpad, not object
   * state; promoting it would let the second call from {@link #shouldHaveLightAppearance()}
   * (heading colour) silently overwrite the first call's record (background colour).
   *
   * @param expectLight {@code true} ⇒ expect brightness {@code &gt; 0.5}; {@code false}
   *          ⇒ expect brightness {@code &lt; 0.5}.
   */
  private void assertBrightnessEventually(
      Locator locator,
      String cssProperty,
      boolean expectLight,
      String description)
  {
    ProbeState state = new ProbeState();
    try {
      PlaywrightWaitUtils.waitForCondition(
          () -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> probe =
                (Map<String, Object>) locator.evaluate(BRIGHTNESS_PROBE_SCRIPT, cssProperty);
            state.css = String.valueOf(probe.get("css"));
            state.brightness = ((Number) probe.get("brightness")).doubleValue();
            return expectLight ? state.brightness > MIDPOINT : state.brightness < MIDPOINT;
          },
          PlaywrightTiming.ELEMENT_TIMEOUT_MS,
          PlaywrightTiming.POLL_INTERVAL_MS,
          "waiting for " + description);
    }
    catch (AssertionError timedOut) {
      throw new AssertionError(String.format(
          "Timed out waiting for %s; last %s = '%s' (brightness %.3f, midpoint %.3f)",
          description, cssProperty, state.css, state.brightness, MIDPOINT), timedOut);
    }
  }

  /** Mutable holder for the diagnostic state captured by an in-flight brightness poll. */
  private static final class ProbeState
  {
    String css = "<not yet read>";

    double brightness = Double.NaN;
  }
}
