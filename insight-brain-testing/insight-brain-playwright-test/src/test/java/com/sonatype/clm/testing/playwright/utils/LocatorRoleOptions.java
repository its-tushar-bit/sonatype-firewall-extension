/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import com.microsoft.playwright.Locator;

/**
 * Static factory methods for {@link Locator.GetByRoleOptions}, eliminating the
 * {@code new Locator.GetByRoleOptions().setX(...)} boilerplate at call sites.
 *
 * <p>
 * Intended for static import:
 *
 * <pre>
 *   import static com.sonatype.clm.testing.playwright.utils.LocatorRoleOptions.*;
 *
 *   locator.getByRole(AriaRole.BUTTON,  withNameExact("Search"));
 *   locator.getByRole(AriaRole.HEADING, withLevel(1));
 *   locator.getByRole(AriaRole.LINK,    withName("Export Results"));
 * </pre>
 */
public final class LocatorRoleOptions
{
  private LocatorRoleOptions() {
  }

  /** Options that match elements whose accessible name contains {@code name} (Playwright default — substring). */
  public static Locator.GetByRoleOptions withName(String name) {
    return new Locator.GetByRoleOptions().setName(name);
  }

  /** Options that match elements whose accessible name is exactly {@code name}. */
  public static Locator.GetByRoleOptions withNameExact(String name) {
    return new Locator.GetByRoleOptions().setName(name).setExact(true);
  }

  /** Options that match heading elements at the given ARIA heading level (1–6). */
  public static Locator.GetByRoleOptions withLevel(int level) {
    return new Locator.GetByRoleOptions().setLevel(level);
  }
}
