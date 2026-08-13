/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Encapsulates a single query-builder row in the Advanced Search query builder.
 *
 * <p>
 * Each row ({@code .iq-adv-search__query-row}) contains an optional operator dropdown
 * (rows after the first), a field dropdown, a match-type dropdown, a value input, and a
 * remove (trash) button. All inner CSS selectors are scoped to the row element so they
 * never leak across row boundaries.
 *
 * <p>
 * Obtain an instance via {@link AdvancedSearchPage#queryRow(int)}.
 */
public class QueryRow
{
  private final Locator row;

  QueryRow(Locator row) {
    this.row = row;
  }

  // ── Locators (for assertions) ────────────────────────────────────────────────

  /**
   * The toggle button of the field dropdown ({@code .iq-adv-search__field}).
   * Used to assert which field is currently selected for this row.
   */
  public Locator fieldButton() {
    return row.locator(".iq-adv-search__field").getByRole(AriaRole.BUTTON);
  }

  /**
   * The toggle button of the operator dropdown ({@code .iq-adv-search__operator}).
   * Only present for rows after the first. Used to assert the current operator ("AND"/"OR").
   */
  public Locator operatorButton() {
    return row.locator(".iq-adv-search__operator").getByRole(AriaRole.BUTTON);
  }

  // ── Interactions ────────────────────────────────────────────────────────────

  /**
   * Opens the field dropdown and selects the option matching {@code fieldLabel}.
   * {@code .iq-adv-search__field} is an unlabelled layout container used as a scope anchor;
   * the toggle and option interactions use {@code getByRole(BUTTON)} and text filtering.
   */
  public void selectField(String fieldLabel) {
    Locator section = row.locator(".iq-adv-search__field");
    section.getByRole(AriaRole.BUTTON).click();
    section.locator(".nx-dropdown-button")
        .filter(new Locator.FilterOptions().setHasText(fieldLabel))
        .click();
  }

  /**
   * Opens the match-type dropdown and selects {@code matchType} ("Partial Match" or "Exact Match").
   */
  public void setMatchType(String matchType) {
    Locator section = row.locator(".iq-adv-search__match");
    section.getByRole(AriaRole.BUTTON).click();
    section.locator(".nx-dropdown-button")
        .filter(new Locator.FilterOptions().setHasText(matchType))
        .click();
  }

  /**
   * Opens the operator dropdown and selects {@code operator} ("AND" or "OR").
   * Only applicable to rows after the first.
   */
  public void setOperator(String operator) {
    Locator section = row.locator(".iq-adv-search__operator");
    section.getByRole(AriaRole.BUTTON).click();
    section.locator(".nx-dropdown-button")
        .filter(new Locator.FilterOptions().setHasText(operator))
        .click();
  }

  /** Fills the value text input for this row. */
  public void setValue(String value) {
    row.locator(".iq-adv-search__value")
        .getByRole(AriaRole.TEXTBOX)
        .fill(value);
  }

  /** Clicks the trash icon button to remove this row from the query builder. */
  public void remove() {
    row.locator(".iq-adv-search__trash")
        .getByRole(AriaRole.BUTTON)
        .click();
  }
}
