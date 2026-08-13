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
 * Page object for the User Activity Overview screen
 * ({@code UserActivityOverview.jsx}, URL fragment {@code /users/activity}).
 * <p>
 * Requires {@code USER_ACTIVITY_TRACKING} feature flag to be enabled before navigation.
 */
public class UserActivityOverviewPage
    extends BasePage
{
  public UserActivityOverviewPage() {
    super();
  }

  /** Direct URL to the User Activity Overview screen. */
  public static String url() {
    return "/assets/index.html#/users/activity";
  }

  /** Top-level user-activity table ({@code NxTable id="user-activity-table"}, role {@code table}). */
  public Locator overviewTable() {
    return page.getByRole(AriaRole.TABLE);
  }

  /**
   * "Username" sortable column header in the overview table.
   * CSS selector used: NxTableCell isSortable wraps text in a button with aria-label
   * "Username unsorted", so getByRole(COLUMNHEADER, setName("Username")) does not match.
   */
  public Locator usernameColumnHeader() {
    return overviewTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Username"));
  }

  /**
   * "Login Count (past N …)" sortable column header.
   * CSS selector + substring filter used for the same reason as usernameColumnHeader.
   */
  public Locator loginCountColumnHeader() {
    return overviewTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Login Count"));
  }

  /**
   * "Last Active" sortable column header.
   * CSS selector used for the same reason as usernameColumnHeader.
   */
  public Locator lastActiveColumnHeader() {
    return overviewTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Last Active"));
  }

  /** Search-by-username filter input ({@code NxFilterInput id="user-search"}). */
  public Locator searchInput() {
    return byPlaceholder("Search by user name");
  }

  /** "Export Activity" button in the tile header actions area. */
  public Locator exportActivityButton() {
    return byRole(AriaRole.BUTTON, "Export Activity");
  }

  /** "Filter" button that opens the filter drawer. */
  public Locator filterButton() {
    return byRole(AriaRole.BUTTON, "Filter");
  }

  /**
   * "Showing N of M users" summary paragraph.
   * Pattern match used because N and M are runtime values.
   */
  public Locator showingSummary() {
    return page.getByText(Pattern.compile("Showing \\d+ of \\d+ users"));
  }

  /**
   * The filter drawer ({@code NxDrawer} / {@code PortalDrawer}).
   * NxDrawer renders as {@code <dialog role="dialog" aria-modal="false">} via RSC
   * {@code AbstractDialog}; no accessible name is wired from {@code NxDrawer.HeaderTitle}.
   * Filtered by the "Filters" heading text to disambiguate from any NxModal dialogs.
   *
   * <p>
   * TODO: Extract filterDrawer/filterApplyButton/filterResetButton/filterMask to a shared
   * {@code FilterDrawerComponent} — the same pattern is duplicated in
   * {@link UserActivityDetailsRegressionPage}. Defer to follow-up PR.
   */
  public Locator filterDrawer() {
    return page.getByRole(AriaRole.DIALOG)
        .filter(new Locator.FilterOptions().setHasText("Filters"));
  }

  /** "Apply" button inside the filter drawer — disabled when {@code filtersAreDirty} is false. */
  public Locator filterApplyButton() {
    return filterDrawer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Apply"));
  }

  /** "Reset" button inside the filter drawer — disabled when {@code filtersAreDirty} is false. */
  public Locator filterResetButton() {
    return filterDrawer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Reset"));
  }

  /**
   * "Time Frame" tree-view toggle button inside the filter drawer.
   * Clicking it expands / collapses the time-frame radio options.
   */
  public Locator timeFrameTreeViewToggle() {
    return filterDrawer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(Pattern.compile("Time Frame")));
  }

  /**
   * A time-frame radio option by its visible label text (e.g. "past 7 days").
   * RSC {@code NxStatefulTreeViewRadioSelect} renders a CSS-hidden {@code <input role="menuitemradio">}
   * next to a visible {@code <label>}; {@code getByText} targets the label so {@code click()} works.
   * Requires the Time Frame group to be expanded first.
   */
  public Locator timeFrameOption(String optionName) {
    return filterDrawer().getByText(optionName, new Locator.GetByTextOptions().setExact(true));
  }

  /**
   * Stale-filter overlay mask — visible when {@code filtersAreDirty} is true.
   * CSS selector used: the mask div has no ARIA role or accessible name.
   */
  public Locator filterMask() {
    return locator(".form-mask.iq-dashboard-form-mask");
  }

  /**
   * A row in the overview table matching the given username text.
   * Used to verify a user appears in the result and to click through to the details page.
   */
  public Locator userRow(String username) {
    return overviewTable().getByRole(AriaRole.ROW)
        .filter(new Locator.FilterOptions().setHasText(username));
  }

  /**
   * First data row in the overview table (excludes the header row).
   * Used to verify that sort order changes when a column header is clicked.
   */
  public Locator firstDataRow() {
    return overviewTable().locator("tbody").getByRole(AriaRole.ROW).first();
  }

  /** Empty-state message rendered when no users match the current filter / search. */
  public Locator emptyStateMessage() {
    return overviewTable().getByText("No user activity found for the selected criteria.");
  }
}
