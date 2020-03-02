/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

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
}
