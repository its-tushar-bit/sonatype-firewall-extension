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
    return BaseUrl.resolvePageUrl("/searchResults");
  }

  public SelenideElement advancedSearchEnabledContent() {
    return child("#advanced-search-enabled-content");
  }

  public SelenideElement advancedSearchDisabledError() {
    return child("#advanced-search-disabled-error");
  }

  public SelenideElement searchInput() {
    return child("#global-search-input");
  }

  public SelenideElement searchButton() {
    return child("#global-search-button");
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

  public SelenideElement firstResultResultNumber() {
    return $("#search-results > div > div:nth-child(2) > span");
  }

  public SelenideElement lastResultResultNumber() {
    return $("#search-results > div > div:nth-child(11) > span");
  }

  // Search results table child 1 is some icon representing what the document is
  // Second item is the text, such as Organization or Application
  // 3rd is the actual result (and there is an > a if it is a link to an entity)
  public SelenideElement firstSearchResultLink() {
    return $("#advanced-search-results-table > tbody > tr > td:nth-child(3) > a");
  }

  public SelenideElement errors() {
    return $("#advanced-search-error");
  }
}
