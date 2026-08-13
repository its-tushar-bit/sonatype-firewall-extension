/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Helper for {@code NxRadio} (and {@code NxCheckbox}) controls, whose markup is
 * {@code <label><input role="radio" hidden/><span>VisibleText</span></label>}.
 *
 * <p>
 * The {@code <input>} is the element with the ARIA role and the {@code checked} state — use
 * {@link #input()} for {@code isChecked} / {@code isAttached} assertions. The input is also
 * sized {@code 0x0} and CSS-hidden, so it can't receive clicks or be checked for
 * {@code isVisible}; use {@link #label()} for clicks and visibility assertions.
 *
 * <p>
 * Construct from an accessible name plus an optional parent locator to scope the lookup
 * (modal, fieldset, named section). Both regex names and exact-text names are supported.
 */
public class NxRadio
{
  private final Locator input;

  private final Locator label;

  private NxRadio(Locator input, Locator label) {
    this.input = input;
    this.label = label;
  }

  /** Within {@code parent}, locate a radio whose accessible name equals {@code name}. */
  public static NxRadio of(Locator parent, String name) {
    Locator input = parent.getByRole(AriaRole.RADIO,
        new Locator.GetByRoleOptions().setName(name));
    Locator label = parent.getByText(name, new Locator.GetByTextOptions().setExact(true));
    return new NxRadio(input, label);
  }

  /**
   * Within {@code parent}, locate a radio whose accessible name matches {@code namePattern}.
   * <p>
   * Use this when the visible label is dynamic — e.g. {@code "Inherit from <parent>"} —
   * and you cannot pin an exact string at compile time.
   */
  public static NxRadio of(Locator parent, Pattern namePattern) {
    Locator input = parent.getByRole(AriaRole.RADIO,
        new Locator.GetByRoleOptions().setName(namePattern));
    Locator label = parent.getByText(namePattern);
    return new NxRadio(input, label);
  }

  /** The {@code <input role="radio">} — use for {@code isChecked} / {@code isAttached}. */
  public Locator input() {
    return input;
  }

  /** The visible {@code <label>} — use for clicks and visibility assertions. */
  public Locator label() {
    return label;
  }
}
