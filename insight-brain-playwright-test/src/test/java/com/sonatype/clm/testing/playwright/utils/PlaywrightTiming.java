/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import com.microsoft.playwright.assertions.LocatorAssertions;

/**
 * Standard timeouts for Playwright functional tests ("flakiness budget" defaults).
 * Prefer these values or {@link PlaywrightWaitUtils} helpers over ad-hoc literals.
 */
public final class PlaywrightTiming
{
  private PlaywrightTiming() {
  }

  /** Waiting for an element to become visible/hidden or for text to match. */
  public static final long ELEMENT_TIMEOUT_MS = 10_000L;

  /**
   * Standard {@link LocatorAssertions.IsVisibleOptions} using {@link #ELEMENT_TIMEOUT_MS}.
   * Use as the first (gate) assertion after each navigation or significant UI transition;
   * subsequent assertions on the same rendered page need no explicit timeout.
   */
  public static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(ELEMENT_TIMEOUT_MS);

  /**
   * Element visibility/hidden waits that legitimately exceed {@link #ELEMENT_TIMEOUT_MS} —
   * e.g. components whose render is gated on a network round-trip (login modal poll, user
   * token modal, owner summary refresh).
   */
  public static final long SLOW_ELEMENT_TIMEOUT_MS = 15_000L;

  /** Poll interval inside {@link PlaywrightWaitUtils#waitForCondition}. */
  public static final long POLL_INTERVAL_MS = 200L;

  /** Login modal, long-running masks, or slow SPA shells. */
  public static final long MODAL_OR_LOGIN_TIMEOUT_MS = 30_000L;

  /** Hash / SPA routes — substring match in URL. */
  public static final long URL_SUBSTRING_TIMEOUT_MS = 30_000L;

  /** Full URL equality waits ({@link com.microsoft.playwright.Page#waitForURL}). */
  public static final long URL_EXACT_TIMEOUT_MS = 15_000L;

  /** Expect a short-lived UI cue (e.g. submit mask) to appear before hidden. */
  public static final long SHORT_UI_CUE_MS = 2_000L;

  /**
   * Brief UI transitions such as dashboard-filter thumb show/hide or a chip animating in/out.
   * Sits between {@link #SHORT_UI_CUE_MS} (mask flicker) and {@link #ELEMENT_TIMEOUT_MS} (full
   * element wait). Prefer the longer values if the component depends on a network round-trip.
   */
  public static final long BRIEF_UI_TRANSITION_MS = 5_000L;

  /** Operations that may legitimately take longer (large tables, heavy dashboard). */
  public static final long LONG_OPERATION_TIMEOUT_MS = 60_000L;

  /** Async evaluation or file-import operations that may exceed {@link #LONG_OPERATION_TIMEOUT_MS}. */
  public static final long ASYNC_EVALUATION_TIMEOUT_MS = 90_000L;

}
