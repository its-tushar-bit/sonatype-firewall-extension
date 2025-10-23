/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.UserActivityFilter;
import com.sonatype.clm.testing.functional.elements.UserActivityTable;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selectors.byText;

public class UserActivityOverviewPage
{
  public static final String USER_ACTIVITY_PATH = "#/users/activity";

  public static String url() {
    return USER_ACTIVITY_PATH;
  }

  // Page header elements
  public SelenideElement pageTitle() {
    return $("h1, .nx-page-title h1, .nx-h1");
  }

  public SelenideElement tileHeader() {
    return $(".nx-tile-header");
  }

  // Search functionality
  public SelenideElement searchInput() {
    return $("#user-search, .nx-filter-input input, input[placeholder*='Search']");
  }

  public SelenideElement searchIcon() {
    return searchInput().parent().$(".nx-icon, .fa-search");
  }

  // Action buttons in header
  public SelenideElement exportButton() {
    return $(byText("Export Activity"));
  }

  public SelenideElement filterButton() {
    return $(byText("Filter"));
  }

  // Main content area
  public SelenideElement tileContent() {
    return $(".nx-tile-content");
  }

  // Error handling
  public SelenideElement errorAlert() {
    return $(".nx-error-alert, .nx-alert--error");
  }

  public SelenideElement loadWrapper() {
    return $(".nx-load-wrapper");
  }

  public SelenideElement loadingSpinner() {
    return $(".nx-loading-spinner, .loading");
  }

  // Access denied / permission error elements
  public SelenideElement accessDeniedMessage() {
    return $(byText("access denied"));
  }

  public SelenideElement permissionErrorMessage() {
    return $(byText("permissions"));
  }

  public SelenideElement featureDisabledMessage() {
    return $(byText("feature"));
  }

  // User activity table
  public UserActivityTable userActivityTable() {
    return new UserActivityTable("user-activity-table");
  }

  public SelenideElement tableContainer() {
    return $(".nx-table-container.user-activity-table");
  }

  // Summary information
  public SelenideElement summaryInfo() {
    return $(".activity-summary");
  }

  public SelenideElement userCountSummary() {
    return summaryInfo().$(byText("Showing"));
  }

  // Filter drawer
  public UserActivityFilter filterDrawer() {
    return new UserActivityFilter();
  }

  // User activity mask (when filters are dirty)
  public SelenideElement userActivityMask() {
    return $(".user-activity-mask, .nx-mask");
  }

  // Page interaction methods
  public void searchForUser(String username) {
    searchInput().setValue(username);
  }

  public void clearSearch() {
    searchInput().clear();
  }

  public void clickExportButton() {
    exportButton().click();
  }

  public void clickFilterButton() {
    filterButton().click();
  }

  public void clickUserRow(int index) {
    userActivityTable().clickUserRow(index);
  }

  public void clickFirstUser() {
    userActivityTable().clickFirstUserRow();
  }

  // Navigation methods
  public void navigateToUserDetails(String username) {
    // Search for the user and click on their row
    searchForUser(username);
    // Wait for search results and click first result
    userActivityTable().waitForTable();
    clickFirstUser();
  }

  // Validation methods
  public boolean hasUsers() {
    return !userActivityTable().isEmpty();
  }

  public boolean hasEmptyMessage() {
    return userActivityTable().hasEmptyMessage();
  }

  public boolean isLoading() {
    return loadingSpinner().isDisplayed();
  }

  public boolean hasPermissionError() {
    return permissionErrorMessage().exists() || accessDeniedMessage().exists();
  }

  public boolean hasFeatureDisabledError() {
    return featureDisabledMessage().exists();
  }

  public String getPageTitle() {
    return pageTitle().text();
  }

  public String getUserCountText() {
    return userCountSummary().text();
  }

  public int getUserCount() {
    return userActivityTable().getRowCount();
  }

  // Filter interaction methods
  public void openFilterDrawer() {
    clickFilterButton();
    filterDrawer().waitForOpen();
  }

  public void closeFilterDrawer() {
    filterDrawer().closeFilter();
    filterDrawer().waitForClosed();
  }

  public void applyTimeFilter(String timeRange) {
    openFilterDrawer();
    switch (timeRange.toLowerCase()) {
      case "24h":
      case "1":
        filterDrawer().selectTimeRange24Hours();
        break;
      case "7d":
      case "7":
        filterDrawer().selectTimeRange7Days();
        break;
      case "30d":
      case "30":
        filterDrawer().selectTimeRange30Days();
        break;
      default:
        // Default to 30 days if unknown time range
        filterDrawer().selectTimeRange30Days();
        break;
    }
    filterDrawer().applyFilters();
  }

  public void resetFilters() {
    openFilterDrawer();
    filterDrawer().resetFilters();
  }

  // Export methods
  public void exportUserActivity() {
    clickExportButton();
    // Wait for export to complete
    waitForExportCompletion();
  }

  private void waitForExportCompletion() {
    // Wait for export button to return to normal state
    exportButton().shouldNotHave(com.codeborne.selenide.Condition.text("Exporting..."));
  }

  // Helper methods for common assertions
  public void waitForPageLoad() {
    pageTitle().shouldBe(com.codeborne.selenide.Condition.visible);
    tileContent().shouldBe(com.codeborne.selenide.Condition.visible);
  }

  public void waitForUsersToLoad() {
    userActivityTable().table().shouldBe(com.codeborne.selenide.Condition.visible);
  }
}
