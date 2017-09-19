/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class DashboardComponentDetails
    extends BasicElement<DashboardComponentDetails>
{
  public static String url(String hash) {
    return BaseUrl.uriBuilder().fragment("/dashboard/component/{hash}").build(hash).toString();
  }

  public DashboardComponentDetails() {
    super(".component-container");
  }

  public SelenideElement header() {
    return child("h2");
  }

  public SelenideElement breadCrumb() {
    return child(" [breadcrumb]");
  }

  public SelenideElement breadCrumbLink() {
    return child(" [breadcrumb] a");
  }
}
