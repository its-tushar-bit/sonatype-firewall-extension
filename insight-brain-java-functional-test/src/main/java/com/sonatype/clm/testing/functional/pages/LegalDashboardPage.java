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

public class LegalDashboardPage extends BasicElement<LegalDashboardPage>
{
  public static final String ROOT = "#legal-dashboard-container";

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
    return BaseUrl.resolvePageUrl("/legal/dashboard?legalComponentsTabEnabled");
  }

  public SelenideElement componentsTab() {
    return children(".nx-tab").get(1);
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
}
