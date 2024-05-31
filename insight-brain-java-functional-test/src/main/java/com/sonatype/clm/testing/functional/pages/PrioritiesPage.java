/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class PrioritiesPage
{
  private PrioritiesPage() {
    // No op
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement triggeredByDetails() {
    return $(".nx-page-title__description");
  }

  public static SelenideElement prioritiesTable() {
    return $(".nx-table-container .nx-table");
  }
}
