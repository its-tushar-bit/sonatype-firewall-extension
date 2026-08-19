/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-only page object for the User Activity Details screen
 * ({@code UserActivityDetails.jsx}, URL fragment {@code /users/activity/{username}}).
 * <p>
 * Covers selectors used exclusively by {@code UserActivityDetailsRegressionPlaywrightTest}:
 * column headers, showing-activities summary, back button, filter drawer, and export state.
 * The sanity-level page object {@code UserActivityDetailsPage} must NOT be modified.
 */
public class UserActivityDetailsRegressionPage
    extends BasePage
{
  public UserActivityDetailsRegressionPage() {
    super();
  }

  /** Direct URL to the User Activity Details screen for a given username. */
  public static String url(String username) {
    return "/assets/index.html#/users/activity/" + username;
  }

  /** The main details table ({@code NxTable id="user-activity-details-table"}, role {@code table}). */
  public Locator detailsTable() {
    return page.getByRole(AriaRole.TABLE);
  }

  /**
   * "Timestamp" sortable column header — default sort key, descending.
   * CSS selector used: NxTableCell isSortable wraps text in a button with aria-label
   * "Timestamp descending", so getByRole(COLUMNHEADER, setName("Timestamp")) does not match.
   */
  public Locator timestampColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Timestamp"));
  }

  /** "Domain" sortable column header. */
  public Locator domainColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Domain"));
  }

  /** "Type" sortable column header. */
  public Locator typeColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Type"));
  }

  /** "Error" sortable column header. */
  public Locator errorColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Error"));
  }

  /** "Request URI" sortable column header. */
  public Locator requestUriColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Request URI"));
  }

  /** "Method" sortable column header. */
  public Locator methodColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("Method"));
  }

  /** "IP Address" sortable column header. */
  public Locator ipAddressColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("IP Address"));
  }

  /** "User Agent" sortable column header. */
  public Locator userAgentColumnHeader() {
    return detailsTable().locator("th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText("User Agent"));
  }

  /** Timestamp cell in the first data row (CSS class {@code timestamp-cell}). Used to verify sort order. */
  public Locator firstTimestampCell() {
    return detailsTable().locator("td.timestamp-cell").first();
  }

  /**
   * "Showing N activities" summary paragraph.
   * Pattern match used because N is a runtime value.
   */
  public Locator showingActivitiesSummary() {
    return page.getByText(Pattern.compile("Showing \\d+ activities"));
  }

  /**
   * Back button rendered by {@code MenuBarBackButton} (with {@code stateName="users.activity"}).
   * Navigates back to the User Activity Overview screen.
   * Accessible name: "Back to User Activity" (rendered by RSC {@code NxBackButton}).
   */
  public Locator backButton() {
    return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Back to User Activity"));
  }

  /** "Export Activity" button in the tile header actions area. */
  public Locator exportActivityButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Export Activity"));
  }

  /**
   * Export error alert — rendered by {@code NxErrorAlert} when the export API call fails.
   * Text: "Failed to export user activity detail data: …"
   */
  public Locator exportErrorAlert() {
    return page.getByText(Pattern.compile("Failed to export user activity detail data"));
  }

  /** "Filter" button that opens the details filter drawer. */
  public Locator filterButton() {
    return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Filter"));
  }

  /**
   * The filter drawer ({@code NxDrawer} / {@code PortalDrawer}).
   * NxDrawer renders as {@code <dialog role="dialog" aria-modal="false">} via RSC
   * {@code AbstractDialog}; no accessible name is wired from {@code NxDrawer.HeaderTitle}.
   * Filtered by the "Filters" heading text to disambiguate from any NxModal dialogs.
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
   * "Activity Type" tree-view section toggle inside the filter drawer
   * ({@code NxStatefulTreeViewMultiSelect id="user-activity-type-filter"}).
   */
  public Locator activityTypeSectionToggle() {
    return filterDrawer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(Pattern.compile("Activity Type")));
  }

  /**
   * "Domain" tree-view section toggle inside the filter drawer
   * ({@code NxStatefulTreeViewMultiSelect id="user-activity-domain-filter"}).
   */
  public Locator domainSectionToggle() {
    return filterDrawer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(Pattern.compile("Domain")));
  }

  /**
   * "Error Type" tree-view section toggle inside the filter drawer
   * ({@code NxStatefulTreeViewMultiSelect id="user-activity-error-type-filter"}).
   */
  public Locator errorTypeSectionToggle() {
    return filterDrawer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(Pattern.compile("Error Type")));
  }

  /**
   * An activity-type option by its visible label text (e.g. "login") inside the filter drawer.
   * RSC {@code NxStatefulTreeViewMultiSelect} renders a CSS-hidden {@code <input>}
   * next to a visible {@code <label>}; {@code getByText} targets the label so {@code click()} works.
   * Requires the Activity Type group to be expanded first.
   */
  public Locator activityTypeOption(String optionName) {
    return filterDrawer().getByText(optionName, new Locator.GetByTextOptions().setExact(true));
  }

  /**
   * Stale-filter overlay mask — visible when {@code filtersAreDirty} is true.
   * CSS selector used: the mask div has no ARIA role or accessible name.
   */
  public Locator filterMask() {
    return locator(".form-mask.iq-dashboard-form-mask");
  }
}
