/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Firewall Container Repository Results page.
 * Root element is {@code #container-repository-results-page}.
 */
public class FirewallContainerRepositoryResultsPage
    extends BasePage
{
  private static final String ROOT = "#container-repository-results-page";

  public FirewallContainerRepositoryResultsPage() {
    super();
  }

  public static String url(String repositoryId) {
    return "/assets/index.html#/firewall/container/repository/" + repositoryId + "/results";
  }

  // --------------- Locators ---------------

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return locator("#container-repository-results-page__title");
  }

  public Locator resultsTable() {
    return locator("#container-repository-results-table");
  }
}
