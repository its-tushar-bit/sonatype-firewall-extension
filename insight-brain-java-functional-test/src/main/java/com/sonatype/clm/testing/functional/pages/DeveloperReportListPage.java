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

public class DeveloperReportListPage
    extends BasicElement<DeveloperReportListPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/developer/reports");
  }

  public static SelenideElement reportListTable() {
    return $("#iq-violation-table-body");
  }

  public static SelenideElement title() {
    return $("h1");
  }
}
