/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RepositoriesSummaryPage
{

  static final String REPOSITORIES_SUMMARY_SELECTOR = "#repositories-summary";

  public static String URL = "new/assets/index.html#/management/view/repositories";

  public static class SummaryTile
  {
    private static SelenideElement root() {
      return $(REPOSITORIES_SUMMARY_SELECTOR);
    }

    public static SelenideElement name() {
      return $(SelectorUtils.selector(REPOSITORIES_SUMMARY_SELECTOR, "h1"));
    }

    public static SelenideElement configButton() {
      return $("#repositories-config-button");
    }

    public static SelenideElement accessButton() {
      return $("#repositories-access-button");
    }
  }
}
