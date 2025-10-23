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

public class UserActivityDetailsPage
{
  public static final String USER_ACTIVITY_DETAILS_PATH = "#/users/activity";

  public static String url(String username) {
    return USER_ACTIVITY_DETAILS_PATH + "/" + username;
  }

  public static String url() {
    return USER_ACTIVITY_DETAILS_PATH;
  }

  // Page header elements
  public SelenideElement pageTitle() {
    return $("h1, .nx-page-title h1, .nx-h1");
  }

  public SelenideElement backButton() {
    return $(".nx-back-button");
  }

  // Tile header elements
  public SelenideElement tileHeader() {
    return $(".nx-tile-header");
  }

  public SelenideElement tileTitle() {
    return tileHeader().$("h2, .nx-h3");
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

  // User activity details table
  public UserActivityTable activityDetailsTable() {
    return new UserActivityTable("user-activity-details-table");
  }

  public SelenideElement tableContainer() {
    return $(".nx-table-container.user-activity-details-table");
  }

  // Summary information
  public SelenideElement summaryInfo() {
    return $(".activity-summary");
  }

  public SelenideElement activityCountSummary() {
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

  // Pagination elements (indeterminate pagination)
  public SelenideElement pagination() {
    return $(".nx-table-container__footer");
  }

  public SelenideElement previousPageButton() {
    return pagination().$("button[aria-label*='Previous'], button");
  }

  public SelenideElement nextPageButton() {
    return pagination().$("button[aria-label*='Next']");
  }

  // Page interaction methods
  public void clickExportButton() {
    exportButton().click();
  }

  public void clickFilterButton() {
    filterButton().click();
  }

  public void clickBackButton() {
    backButton().click();
  }

  public void navigateBackToOverview() {
    clickBackButton();
  }

  // Pagination methods
  public void goToPreviousPage() {
    previousPageButton().click();
  }

  public void goToNextPage() {
    nextPageButton().click();
  }

  public boolean canGoToPreviousPage() {
    return previousPageButton().isEnabled();
  }

  public boolean canGoToNextPage() {
    return nextPageButton().isEnabled();
  }

  // Validation methods
  public boolean hasActivities() {
    return !activityDetailsTable().isEmpty();
  }

  public boolean hasEmptyMessage() {
    return activityDetailsTable().hasEmptyMessage();
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

  public String getTileTitle() {
    return tileTitle().text();
  }

  public String getActivityCountText() {
    return activityCountSummary().text();
  }

  public int getActivityCount() {
    return activityDetailsTable().getRowCount();
  }

  public String getUsernameFromTitle() {
    String title = getPageTitle();
    // Extract username from title like "username Activity (Past 30 Days)"
    return title.split(" Activity")[0];
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

  public void applyActivityTypeFilter(String activityType) {
    openFilterDrawer();
    filterDrawer().selectActivityType(activityType);
    filterDrawer().applyFilters();
  }

  public void applyDomainFilter(String domain) {
    openFilterDrawer();
    filterDrawer().selectDomain(domain);
    filterDrawer().applyFilters();
  }

  public void applyErrorStatusFilter(String errorStatus) {
    openFilterDrawer();
    filterDrawer().selectErrorStatus(errorStatus);
    filterDrawer().applyFilters();
  }

  public void applyMultipleFilters(String activityType, String domain, String errorStatus) {
    openFilterDrawer();
    if (activityType != null) {
      filterDrawer().selectActivityType(activityType);
    }
    if (domain != null) {
      filterDrawer().selectDomain(domain);
    }
    if (errorStatus != null) {
      filterDrawer().selectErrorStatus(errorStatus);
    }
    filterDrawer().applyFilters();
  }

  public void resetFilters() {
    openFilterDrawer();
    filterDrawer().resetFilters();
  }

  // Export methods
  public void exportUserActivityDetails() {
    clickExportButton();
    // Wait for export to complete
    waitForExportCompletion();
  }

  private void waitForExportCompletion() {
    // Wait for export button to return to normal state
    exportButton().shouldNotHave(com.codeborne.selenide.Condition.text("Exporting..."));
  }

  // Sorting methods
  public void sortByTimestamp() {
    activityDetailsTable().clickTimestampHeader();
  }

  public void sortByDomain() {
    activityDetailsTable().clickDomainHeader();
  }

  public void sortByType() {
    activityDetailsTable().clickTypeHeader();
  }

  // Helper methods for common assertions
  public void waitForPageLoad() {
    pageTitle().shouldBe(com.codeborne.selenide.Condition.visible);
    tileContent().shouldBe(com.codeborne.selenide.Condition.visible);
  }

  public void waitForActivitiesToLoad() {
    activityDetailsTable().table().shouldBe(com.codeborne.selenide.Condition.visible);
  }

  // Validate specific activity data
  public String getFirstActivityTimestamp() {
    return activityDetailsTable().timestampCell(0).text();
  }

  public String getFirstActivityDomain() {
    return activityDetailsTable().domainCell(0).text();
  }

  public String getFirstActivityType() {
    return activityDetailsTable().typeCell(0).text();
  }

  public String getFirstActivityUri() {
    return activityDetailsTable().uriCell(0).text();
  }

  public String getFirstActivityMethod() {
    return activityDetailsTable().methodCell(0).text();
  }

  public String getFirstActivityIpAddress() {
    return activityDetailsTable().ipAddressCell(0).text();
  }

  public String getFirstActivityUserAgent() {
    return activityDetailsTable().userAgentCell(0).text();
  }
}
