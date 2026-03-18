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
import static com.codeborne.selenide.Condition.text;

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

  public static String sbomManagerUrl() {
    return BaseUrl.resolvePageUrl("/sbomManager/advancedSearch");
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

  public SelenideElement queryBuilderSearchTermsContainer() {
    return child("#iq-adv-search__query-builder-search-terms");
  }

  public SelenideElement queryBuilderEasyContainer() {
    return child("#iq-adv-search__query-builder-easy");
  }

  public SelenideElement prefixTagWithId(String id) {
    return child("#advanced-search-query-builder-tag-" + id);
  }

  public SelenideElement componentSearchRadioButtons() {
    return $("#filter-component-results-options");
  }

  public SelenideElement showAllComponentsRadio() {
    return child("#show-all-components-true");
  }

  public SelenideElement firstResultCardOrgName() {
    return child(
        ".iq-adv-search__results-container > section:nth-child(1) >" +
            " div > table > tbody > tr:nth-child(1) > td:nth-child(3) > a");
  }

  public SelenideElement firstResultCardAppName() {
    return child(
        ".iq-adv-search__results-container > section:nth-child(1) >" +
            " div > table > tbody > tr:nth-child(2) > td:nth-child(3) > a");
  }

  public SelenideElement secondResultCardOrgName() {
    return child(
        ".iq-adv-search__results-container > section:nth-child(2) >" +
            " div > table > tbody > tr:nth-child(1) > td:nth-child(3) > a");
  }

  public SelenideElement secondResultCardAppName() {
    return child(
        ".iq-adv-search__results-container > section:nth-child(2) >" +
            " div > table > tbody > tr:nth-child(2) > td:nth-child(3) > a");
  }

  // Query Builder Elements
  public SelenideElement queryBuilderToggleButton() {
    return child(".iq-adv-search__query-builder-button");
  }

  public SelenideElement searchTermsToggleButton() {
    return child(".iq-adv-search__search-terms-button");
  }

  public SelenideElement addSearchItemButton() {
    return child(".iq-adv-search__query-builder .nx-btn--primary");
  }

  public SelenideElement queryBuilderEmptyState() {
    return child(".iq-adv-search__query-builder-content-empty");
  }

  public SelenideElement searchRow(int index) {
    return child(".iq-adv-search__query-builder-content > :nth-child(" + index + ")");
  }

  public SelenideElement searchRowOperatorDropdown(int index) {
    return child(".iq-adv-search__query-builder-content > :nth-child(" + index + ")").find(
        ".iq-adv-search__operator .nx-dropdown");
  }

  public SelenideElement searchRowFieldDropdown(int index) {
    return child(".iq-adv-search__query-builder-content > :nth-child(" + index + ")").find(
        ".iq-adv-search__field .nx-dropdown");
  }

  public SelenideElement searchRowMatchDropdown(int index) {
    return child(".iq-adv-search__query-builder-content > :nth-child(" + index + ")").find(
        ".iq-adv-search__match .nx-dropdown");
  }

  public SelenideElement searchRowValueInput(int index) {
    return child(".iq-adv-search__query-builder-content > :nth-child(" + index + ")").find(
        ".iq-adv-search__value input");
  }

  public SelenideElement searchRowRemoveButton(int index) {
    return child(".iq-adv-search__query-builder-content > :nth-child(" + index + ")").find(
        ".iq-adv-search__trash button");
  }

  // Dropdown option selectors
  public SelenideElement operatorOption(String operator) {
    return child(".nx-dropdown-menu").$$("button").findBy(text(operator));
  }

  public SelenideElement fieldOption(String fieldValue) {
    return child(".nx-dropdown-menu").$$("button").findBy(text(fieldValue));
  }

  public SelenideElement matchOption(String matchType) {
    return child(".nx-dropdown-menu").$$("button").findBy(text(matchType));
  }
}
