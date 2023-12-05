/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class AdvancedSearchPage
    extends BasicElement<AdvancedSearchPage>
{
  public static final String ROOT = "#advanced-search-page";

  public AdvancedSearchPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/advancedSearch");
  }

  public SelenideElement advancedSearchPageTitle() {
    return child("#advanced-search-page-title");
  }

  public SelenideElement advancedSearchDisabledError() {
    return child(".nx-alert--load-error");
  }

  public SelenideElement searchInput() {
    return child("#advanced-search-input");
  }

  public SelenideElement searchButton() {
    return child("#advanced-search-button");
  }

  public SelenideElement resultCount() {
    return child("#advanced-search-result-count");
  }

  public SelenideElement currentPageInfo() {
    return child("#advanced-search-current-page-info");
  }

  public SelenideElement nextPageButton() {
    return child("#advanced-search-next-page-button");
  }

  public SelenideElement previousPageButton() {
    return child("#advanced-search-previous-page-button");
  }

  // Search results table child 1 is some icon representing what the document is
  public SelenideElement firstSearchResultLink() {
    return child("table.nx-table--advanced-search td:nth-child(3) > a");
  }

  public SelenideElement queryError() {
    return child(".nx-alert--error");
  }

  public SelenideElement helpContainerToggle() {
    return $("#advanced-search-help-container-toggle");
  }

  public SelenideElement helpContainer() {
    return $("#advanced-search-help-container");
  }

  public SelenideElement queryBuilderButton() {
    return child("#advanced-search-query-builder-toggle-button");
  }

  public SelenideElement queryBuilderContainer() {
    return child("#advanced-search-query-builder-container");
  }

  public SelenideElement prefixTagWithId(String id) {
    return child("#advanced-search-query-builder-tag-" +  id);
  }

  public SelenideElement componentSearchRadioButtons() {
    return $("#filter-component-results-options");
  }

  public SelenideElement showAllComponentsRadio() {
    return child("#show-all-components-true");
  }

  public SelenideElement firstResultCardOrgName() {
    return child("section:nth-child(6) > div > table > tbody > tr:nth-child(1) > td:nth-child(3) > a");
  }

  public SelenideElement firstResultCardAppName() {
    return child("section:nth-child(6) > div > table > tbody > tr:nth-child(2) > td:nth-child(3) > a");
  }

  public SelenideElement secondResultCardOrgName() {
    return child("section:nth-child(7) > div > table > tbody > tr:nth-child(1) > td:nth-child(3) > a");
  }

  public SelenideElement secondResultCardAppName() {
    return child("section:nth-child(7) > div > table > tbody > tr:nth-child(2) > td:nth-child(3) > a");
  }
}
