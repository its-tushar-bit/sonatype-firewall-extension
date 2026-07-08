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
 * Component helper for {@code NxRadio} elements.
 * <p>
 * {@code NxRadio} renders as {@code <label class="nx-radio-checkbox">} wrapping a 0×0
 * {@code <input type="radio">}. Use {@link #label()} for click and visibility assertions;
 * use {@link #input()} for checked/disabled state assertions only.
 *
 * @param accessibleName the visible label text of the radio button (String or Pattern)
 * @param parent the locator that scopes the search (typically the enclosing form or fieldset)
 */
public class NxRadioComponent
{
  private final String accessibleNameStr;

  private final Pattern accessibleNamePattern;

  private final Locator parent;

  public NxRadioComponent(String accessibleName, Locator parent) {
    this.accessibleNameStr = accessibleName;
    this.accessibleNamePattern = null;
    this.parent = parent;
  }

  /** Pattern constructor — use when the radio label is dynamic (e.g. "Inherit from parent (Enabled/Disabled)"). */
  public NxRadioComponent(Pattern accessibleName, Locator parent) {
    this.accessibleNameStr = null;
    this.accessibleNamePattern = accessibleName;
    this.parent = parent;
  }

  /**
   * The 0×0 {@code <input type="radio">} — use for checked/disabled state assertions only.
   * <p>
   * String names use {@code setExact(true)} to prevent substring matches (e.g. "Enabled"
   * matching "Inherit from parent (Enabled)"). Pattern names use regex matching.
   */
  public Locator input() {
    Locator.GetByRoleOptions opts = new Locator.GetByRoleOptions();
    if (accessibleNamePattern != null) {
      opts.setName(accessibleNamePattern);
    }
    else {
      opts.setName(accessibleNameStr).setExact(true);
    }
    return parent.getByRole(AriaRole.RADIO, opts);
  }

  /**
   * The outer {@code <label class="nx-radio-checkbox">} element — use for click and visibility
   * assertions.
   * <p>
   * Uses {@code hasText} filtering rather than {@code filter({ has: input() })} because
   * Playwright evaluates the {@code has} locator relative to each candidate element (i.e.
   * relative to each {@code <label>}). The {@code input()} locator is anchored to {@code
   * parent}, which is an ANCESTOR of each candidate label; within the subtree of a
   * {@code <label>}, looking for the parent selector finds nothing, so the filter always
   * returns empty.
   * <p>
   * String names use a JS-compatible regex anchor — no {@code (?s)} inline flag and no
   * {@code \Q...\E} quoting (both are Java-only syntax that Playwright rejects in the browser).
   */
  public Locator label() {
    Locator.LocatorOptions opts = new Locator.LocatorOptions();
    if (accessibleNamePattern != null) {
      opts.setHasText(accessibleNamePattern);
    }
    else {
      // Anchor to full label text to avoid partial matches (e.g. "Disable" matching "Disabled")
      opts.setHasText(Pattern.compile("^\\s*" + escapeRegex(accessibleNameStr) + "\\s*$"));
    }
    return parent.locator("label.nx-radio-checkbox", opts);
  }

  /** Escapes regex metacharacters for use in a JavaScript {@code RegExp} (no {@code \Q...\E} quoting). */
  private static String escapeRegex(String text) {
    return text.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
  }
}
