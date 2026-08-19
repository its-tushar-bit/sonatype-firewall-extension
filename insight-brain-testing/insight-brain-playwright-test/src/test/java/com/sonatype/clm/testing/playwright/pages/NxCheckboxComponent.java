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
 * Component helper for {@code NxCheckbox} elements.
 * <p>
 * {@code NxCheckbox} renders as {@code <label class="nx-radio-checkbox">} wrapping a 0×0
 * {@code <input type="checkbox">}. Use {@link #label()} for click and visibility assertions;
 * use {@link #input()} for checked/disabled state assertions only.
 *
 * @param accessibleName the visible label text of the checkbox
 * @param parent the locator that scopes the search (typically the enclosing form or modal)
 */
public class NxCheckboxComponent
{
  private final String accessibleName;

  private final Locator parent;

  public NxCheckboxComponent(String accessibleName, Locator parent) {
    this.accessibleName = accessibleName;
    this.parent = parent;
  }

  /** The 0×0 {@code <input type="checkbox">} — use for checked/disabled state assertions only. */
  public Locator input() {
    return parent.getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(accessibleName).setExact(true));
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
   * Uses a JS-compatible regex anchor — no {@code (?s)} inline flag and no {@code \Q...\E}
   * quoting (both are Java-only syntax that Playwright rejects in the browser).
   */
  public Locator label() {
    return parent.locator("label.nx-radio-checkbox",
        new Locator.LocatorOptions().setHasText(
            Pattern.compile("^\\s*" + escapeRegex(accessibleName) + "\\s*$")));
  }

  /** Escapes regex metacharacters for use in a JavaScript {@code RegExp} (no {@code \Q...\E} quoting). */
  private static String escapeRegex(String text) {
    return text.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
  }
}
