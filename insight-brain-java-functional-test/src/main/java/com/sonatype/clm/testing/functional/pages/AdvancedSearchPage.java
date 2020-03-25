/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ReactTextInput;
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
    return child("#advanced-search-disabled-error");
  }

  public ReactTextInput searchInput() {
    return new ReactTextInput(child("#advanced-search-input"));
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
    return $("#advanced-search-page > div > div:nth-child(5) > table > tbody > tr > td:nth-child(3) > a");
  }

  public SelenideElement queryError() {
    return $("#advanced-search-query-error");
  }
}
