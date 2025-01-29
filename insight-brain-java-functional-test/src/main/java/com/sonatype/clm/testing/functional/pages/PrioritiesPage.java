/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.elements.NxDropdown;

import static com.codeborne.selenide.Selectors.by;
import static com.codeborne.selenide.Selenide.$;

public class PrioritiesPage
{
  private PrioritiesPage() {
    // No op
  }

  public static SelenideElement title() {
    return $("h2");
  }

  public static SelenideElement summaryTile() {
    return $(by("data-testid", "iq-priorities-page-summary-section"));
  }

  public static SelenideElement backLink() {
    return $(".nx-text-link.iq-priorities-page-breadcrumbs-crumb");
  }

  public static SelenideElement prioritiesTable() {
    return $(".iq-priorities-page-table");
  }

  public static ElementsCollection prioritiesTableRows() {
    return prioritiesTable()
        .findAll(by("data-analytics-id", "sonatype-developer-priorities-page-component-row"));
  }

  public static NxDropdown viewDropdown() {
    return new NxDropdown(".iq-priorities-page-view-dropdown");
  }

  public static SelenideElement lastPageLink() {
    return $(by("aria-label", "goto last page"));
  }
}
