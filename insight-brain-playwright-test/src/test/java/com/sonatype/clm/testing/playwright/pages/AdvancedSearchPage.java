/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Advanced Search screen ({@code advancedSearch/AdvancedSearch.jsx}).
 */
public class AdvancedSearchPage
    extends BasePage
{
  private static final String ROOT = "#advanced-search-page";

  public AdvancedSearchPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/advancedSearch";
  }

  public static String urlWithSearchQuery(String rawQuery) {
    return "/assets/index.html#/advancedSearch?search=" + URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);
  }

  public static String hashRouteWithSearchQuery(String rawQuery) {
    return "#/advancedSearch?search=" + URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return locator(ROOT + " #advanced-search-page-title");
  }

  public Locator queryInput() {
    return locator(ROOT + " #advanced-search-form").getByRole(AriaRole.TEXTBOX).first();
  }

  public Locator searchSubmitButton() {
    return locator(ROOT + " #advanced-search-button");
  }

  public Locator resultCountHeading() {
    return locator(ROOT + " #advanced-search-result-count");
  }

  public void ensureDeepLinkKeywordApplied(String keyword) {
    try {
      page.waitForFunction(
          "(kw) => { const el = document.querySelector('#advanced-search-input'); "
              + "return el != null && el.value === kw; }",
          keyword);
    }
    catch (PlaywrightException e) {
      runKeywordSearch(keyword);
    }
  }

  public void runKeywordSearch(String keyword) {
    queryInput().fill(keyword);
    assertThat(searchSubmitButton()).isEnabled();
    searchSubmitButton().click();
    assertThat(resultCountHeading()).isVisible();
  }
}
