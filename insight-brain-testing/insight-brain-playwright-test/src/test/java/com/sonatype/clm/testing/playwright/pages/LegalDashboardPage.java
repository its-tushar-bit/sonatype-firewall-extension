/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class LegalDashboardPage
    extends BasePage
{
  private static final String ROOT = "#legal-dashboard-container";

  private static final Locator.GetByRoleOptions APPLICATIONS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Applications");

  private static final Locator.GetByRoleOptions COMPONENTS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Components");

  private static final String APPLICATION_HEADER_TEXT = "Application";

  private static final String LAST_SCAN_HEADER_TEXT = "Last Scan";

  private static final String APP_CATEGORIES_HEADER_TEXT = "App Categories";

  private static final String COMPONENTS_REVIEWED_HEADER_TEXT = "Components Reviewed";

  private static final String COMPONENT_OBLIGATIONS_HEADER_TEXT = "Component Obligations";

  private static final Locator.GetByRoleOptions SEARCH_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Search");

  public LegalDashboardPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/legal/dashboard";
  }

  public static String sbomManagerUrl() {
    return "/assets/index.html#/sbomManager/legal/dashboard";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator applicationsTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, APPLICATIONS_TAB_OPTS);
  }

  public Locator componentsTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, COMPONENTS_TAB_OPTS);
  }

  public Locator applicationsTable() {
    return locator("#legal-dashboard-applications-table");
  }

  public Locator applicationsTableRows() {
    return applicationsTable().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator applicationNameColumnHeader() {
    return applicationsTable().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(APPLICATION_HEADER_TEXT));
  }

  public Locator lastScanTimeColumnHeader() {
    return applicationsTable().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(LAST_SCAN_HEADER_TEXT));
  }

  public Locator appCategoriesColumnHeader() {
    return applicationsTable().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(APP_CATEGORIES_HEADER_TEXT));
  }

  public Locator componentsReviewedColumnHeader() {
    return applicationsTable().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(COMPONENTS_REVIEWED_HEADER_TEXT));
  }

  public Locator applicationsChevronColumnHeader() {
    return locator("#legal-dashboard-applications-table thead th.nx-cell--row-btn");
  }

  public Locator applicationsPagination() {
    return container().getByRole(AriaRole.NAVIGATION, new Locator.GetByRoleOptions().setName("pagination"));
  }

  public Locator paginationNextButton() {
    return applicationsPagination().getByLabel("goto next page");
  }

  public Locator paginationPreviousButton() {
    return applicationsPagination().getByLabel("goto previous page");
  }

  public Locator paginationPageButton(int pageNumber) {
    return applicationsPagination().getByRole(AriaRole.BUTTON)
        .filter(
            new Locator.FilterOptions().setHasText(Pattern.compile("^" + pageNumber + "$")));
  }

  public Locator componentsTable() {
    return locator("#legal-dashboard-components-table");
  }

  public Locator componentsTableRows() {
    return componentsTable().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator componentNameColumnHeader() {
    return locator("#component-component-name-header");
  }

  public Locator licenseColumnHeader() {
    return locator("#component-license-name-header");
  }

  public Locator applicationCountColumnHeader() {
    return locator("#component-application-count-header");
  }

  public Locator componentObligationsColumnHeader() {
    return componentsTable().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(COMPONENT_OBLIGATIONS_HEADER_TEXT));
  }

  public Locator componentsActionColumnHeader() {
    return locator("#legal-dashboard-components-table thead th:nth-child(5)");
  }

  public Locator componentSearchBox() {
    return container().getByRole(AriaRole.TEXTBOX);
  }

  public Locator componentSearchButton() {
    return container().getByRole(AriaRole.BUTTON, SEARCH_BUTTON_OPTS);
  }

  public Locator componentSearchValidationError() {
    return container().getByRole(AriaRole.ALERT);
  }

  public Locator componentsPagination() {
    return container().getByRole(AriaRole.NAVIGATION, new Locator.GetByRoleOptions().setName("pagination"));
  }

  public Locator filterToggleButton() {
    return locator("#filter-toggle");
  }

  public Locator filterDirtyAsterisk() {
    return locator("#filter-toggle-dirty-asterisk");
  }

  public Locator filterDrawer() {
    return locator("#iq-legal-dashboard-filter-drawer");
  }

  public Locator filterOrgAppGroup() {
    return locator("#legal-org-app-filters");
  }

  public Locator filterOrgAppCollapsibleTrigger(int index) {
    return filterOrgAppGroup().getByRole(AriaRole.BUTTON).nth(index);
  }

  public Locator filterOrganizationsTrigger() {
    return filterOrgAppCollapsibleTrigger(0);
  }

  public Locator filterApplicationsTrigger() {
    return filterOrgAppCollapsibleTrigger(1);
  }

  public Locator filterApplicationsGroup() {
    return locator("#application-filter");
  }

  public Locator filterCategoryGroup() {
    return locator("#legal-category-filter");
  }

  public Locator filterStageGroup() {
    return locator("#legal-stage-filter");
  }

  public Locator filterProgressGroup() {
    return locator("#legal-progress-options-filter");
  }

  public Locator filterProgressGroupExpandTrigger() {
    return filterProgressGroup().getByRole(AriaRole.BUTTON);
  }

  public Locator filterProgressGroupCheckboxAt(int index) {
    return filterProgressGroup().locator("label").nth(index);
  }

  public Locator filterOrgAppGroupFirstCheckbox() {
    return filterOrgAppGroup().locator("label").first();
  }

  public Locator filterApplicationsGroupCheckboxAt(int index) {
    return filterApplicationsGroup().locator("label").nth(index);
  }

  public Locator filterApplyButton() {
    return filterDrawer().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Apply"));
  }

  public Locator filterSaveButton() {
    return filterDrawer().getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator filterRevertButton() {
    return filterDrawer().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Revert"));
  }

  public Locator saveFilterModal() {
    return locator("#save-filter-modal");
  }

  public Locator saveFilterModalNameInput() {
    return locator("#save-filter-modal #filter-name-section input");
  }

  public Locator saveFilterModalSaveButton() {
    return saveFilterModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator createAttributionReportButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create Attribution Report"));
  }

  public Locator createReportCancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.BACK_BUTTON_OPTS);
  }

  public Locator createReportGenerateButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Generate Report"));
  }

  public void switchToComponentsTab() {
    componentsTab().click();
  }

  public void openFilterDrawer() {
    filterToggleButton().click();
    filterDrawer().waitFor();
  }

  public void closeFilterDrawer() {
    filterToggleButton().click();
    filterDrawer().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
        .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
  }

  public void clickCreateAttributionReport() {
    createAttributionReportButton().click();
  }

  public void searchComponent(String name) {
    componentSearchBox().fill(name);
  }

  public void waitForApplicationsLoaded() {
    applicationsTable().waitFor();
    applicationsTableRows().first().waitFor();
    applicationsTableRows().first().locator(".nx-cell__row-btn").waitFor();
  }

  public void expandOrganizationsAndApplicationsGroups() {
    filterOrganizationsTrigger().click();
    filterOrganizationsTrigger().waitFor();
    filterApplicationsTrigger().click();
    filterApplicationsTrigger().waitFor();
  }
}
