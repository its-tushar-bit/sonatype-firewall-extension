/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Helper for the NxToggle pattern: a CSS-hidden {@code <input>} (used for state assertions —
 * {@code isChecked}/{@code isDisabled}/{@code isAttached}) paired with a clickable wrapping
 * {@code <label>} (used for user-driven interactions). Construct from caller-supplied input and
 * label locators so each page object can anchor on whatever's stable for that toggle (id,
 * className, ARIA name, etc.).
 *
 * <p>
 * Will be replaced by the shared utility class proposed in PR #16466 once it lands.
 */
public class NxToggle
{
  private final Locator input;

  private final Locator label;

  public NxToggle(Locator input, Locator label) {
    this.input = input;
    this.label = label;
  }

  /** State-bearing input — use for {@code isChecked}, {@code isDisabled}, {@code isAttached}. */
  public Locator input() {
    return input;
  }

  /** Clickable label — use for click interactions and visibility assertions. */
  public Locator label() {
    return label;
  }

  public void click() {
    label.click();
  }
}
