/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class LegalDashboardPage extends BasicElement<LegalDashboardPage>
{
  public static final String ROOT = "#legal-dashboard-container";

  private static final String CHECKBOX_SELECTOR = ".nx-checkbox";

  public LegalDashboardPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/legal/dashboard");
  }

  public static String url(boolean enableComponentDetails) {
    if (!enableComponentDetails) {
      return url();
    }
    return BaseUrl.resolvePageUrl("/legal/dashboard");
  }

  public static String sbomManagerUrl() {
    return BaseUrl.resolvePageUrl("/sbomManager/legal/dashboard");
  }

  public SelenideElement componentsTab() {
    return children(".nx-tab").get(1);
  }

  public SelenideElement applicationsTab() {
    return children(".nx-tab").get(0);
  }

  public ElementsCollection tableRows() {
    return children(".nx-table-row.nx-clickable");
  }

  public ElementsCollection componentsTableComponentNameCols() {
    return children(".legal-dashboard-components-component-name");
  }

  public ElementsCollection componentsTableLicenseNameCols() {
    return children(".legal-dashboard-components-licenses");
  }

  public ElementsCollection componentsTableApplicationCountCols() {
    return children(".legal-dashboard-components-occurrences");
  }

  public ElementsCollection pageButtons() {
    return children(".nx-btn--pagination");
  }

  public SelenideElement componentsTableComponentNameHeaderSortBtn() {
    return child("#component-component-name-header .nx-cell__sort-btn");
  }

  public SelenideElement componentsTableLicenseNameHeaderSortBtn() {
    return child("#component-license-name-header .nx-cell__sort-btn");
  }

  public SelenideElement componentsTableApplicationCountHeaderSortBtn() {
    return child("#component-application-count-header .nx-cell__sort-btn");
  }

  public SelenideElement componentsTableComponentNameHeader() {
    return child("#component-component-name-header");
  }

  public SelenideElement componentsTableLicenseNameHeader() {
    return child("#component-license-name-header");
  }

  public SelenideElement componentsTableApplicationCountHeader() {
    return child("#component-application-count-header");
  }

  public SelenideElement componentsSearchInput() {
    return child("#legal-dashboard-component-searchbox-container .nx-text-input input[type='text']");
  }

  public SelenideElement componentsSearchButton() {
    return child("#legal-dashboard-component-searchbox-container .nx-btn--primary");
  }

  public SelenideElement componentsSearchInputErrorMessage() {
    return child("#legal-dashboard-component-searchbox-container .nx-text-input__invalid-message");
  }

  public SelenideElement noComponentsFoundMessage() {
    return child("#legal-dashboard-components-table .nx-cell.nx-cell--meta-info");
  }

  public SelenideElement selectedPaginationPage() {
    return child(".nx-btn--pagination.selected");
  }

  public SelenideElement filterButton() {
    return child("#filter-toggle");
  }

  public ElementsCollection filterCollapsibleItems() {
    return $$("#iq-legal-dashboard-filter-drawer .nx-collapsible-items");
  }

  public SelenideElement createAttributionReportButton() {
    return children("#create-attribution-report-btn").get(0);
  }

  public SelenideElement generateAttributionReportButton() {
    return children("#create-report-generate-report-button").get(0);
  }

  public ElementsCollection filterOrganizationsCheckBoxes() {
    return filterCollapsibleItems().get(0).findAll(CHECKBOX_SELECTOR);
  }

  public ElementsCollection filterApplicationsCheckBoxes() {
    return filterCollapsibleItems().get(1).findAll(CHECKBOX_SELECTOR);
  }

  public ElementsCollection filterApplicationCategoriesCheckBoxes() {
    return filterCollapsibleItems().get(2).findAll(CHECKBOX_SELECTOR);
  }

  public ElementsCollection filterStagesCheckBoxes() {
    return filterCollapsibleItems().get(3).findAll(CHECKBOX_SELECTOR);
  }

  public ElementsCollection filterReviewProgressCheckBoxes() {
    return filterCollapsibleItems().get(4).findAll(CHECKBOX_SELECTOR);
  }

  public SelenideElement filterApplyButton() {
    return $("#legal-dashboard-filter-apply");
  }
}
