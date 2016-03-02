/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class DashboardPage
{
  public static String URL = "assets/index.html#/dashboard/newest-risk";

  public static String NEW_URL = "new/assets/index.html#/dashboard/newest-risk";

  public static SelenideElement body() {
    return $(".dashboard-body-container");
  }
}
