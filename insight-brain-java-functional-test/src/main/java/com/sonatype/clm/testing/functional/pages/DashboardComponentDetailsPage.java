/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class DashboardComponentDetailsPage
{
  public static String url(String hash) {
    return BaseUrl.uriBuilder().fragment("/dashboard/component/{hash}").build(hash).toString();
  }

  private static final String ROOT = ".component-container";

  public SelenideElement header() {
    return $("#component-name");
  }

  public SelenideElement breadCrumb() {
    return $(createSelector(ROOT, " [breadcrumb]"));
  }

  public SelenideElement breadCrumbLink() {
    return $(createSelector(ROOT, " [breadcrumb] a"));
  }
}
