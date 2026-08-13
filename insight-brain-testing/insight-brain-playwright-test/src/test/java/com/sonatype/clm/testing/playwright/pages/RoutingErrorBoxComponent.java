/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

/**
 * Playwright page object for the routing error notification box.
 */
public class RoutingErrorBoxComponent
    extends BasePage
{
  /**
   * The error box mounts only after the SPA router applies the invalid hash and dispatches
   * {@code setError} ({@code allRoutes.js} {@code otherwise} rule). Use {@link PlaywrightTiming}
   * so CI cold-starts stay aligned with other boot-gated UI waits (modals, long operations).
   */
  private static final double ERROR_BOX_TIMEOUT_MS = PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS;

  public RoutingErrorBoxComponent() {
    super();
  }

  public Locator errorBox() {
    return locator(".iq-alert--error");
  }

  /**
   * The error message paragraph — the second {@code
   *
  <p>
   * } child of the error box (see
   * {@code App.jsx}, the box renders an icon + {@code <strong>Error</strong>} + an
   * "unrecoverable error" {@code
   *
  <p>
   * } + a "({error})" {@code
   *
  <p>
   * } that includes the
   * dispatched error string).
   *
   * <p>
   * Note: {@link #shouldHaveErrorText(String)} no longer asserts on this element — it asserts
   * on the parent {@link #errorBox()} so the assertion remains atomic across the parent/child
   * pair. This locator is kept for cases where a test needs to target the message itself
   * (e.g. text-content equality rather than substring containment).
   */
  public Locator errorMessage() {
    return errorBox().locator("p:last-child");
  }

}
