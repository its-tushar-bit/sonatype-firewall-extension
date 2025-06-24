/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxDropdown;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import static com.codeborne.selenide.Selectors.by;

public class PrioritiesPage
    extends BasicElement<PrioritiesPage>
{
  public PrioritiesPage() {
    super(".iq-priorities-page");
  }

  public static String url(String applicationId, String scanId) {
    return BaseUrl.resolvePageUrl("/developer/priorities/{publicAppId}/{scanId}", applicationId, scanId);
  }

  public SelenideElement title() {
    return child("h2");
  }

  public SelenideElement summaryTile() {
    return getElement().$(by("data-testid", "iq-priorities-page-summary-section"));
  }

  public SelenideElement backLink() {
    return child(".nx-text-link.iq-priorities-page-breadcrumbs-crumb");
  }

  public SelenideElement prioritiesTable() {
    return child(".iq-priorities-table");
  }

  public ElementsCollection prioritiesTableRows() {
    return prioritiesTable()
        .findAll(by("data-analytics-id", "sonatype-developer-priorities-page-component-row"));
  }

  public SelenideElement prioritiesTableCell(int rowNum, int colNum) {
    return prioritiesTable().$("tbody tr", rowNum).$("td", colNum);
  }

  public SelenideElement rowComponentLink(int rowNum) {
    return prioritiesTableCell(rowNum, 1).$("a");
  }

  public NxDropdown viewDropdown() {
    return new NxDropdown(childSelector(".iq-priorities-page-view-dropdown"));
  }

  public SelenideElement lastPageLink() {
    return getElement().$(by("aria-label", "goto last page"));
  }

  public SelenideElement createPullRequestButton(int rowNum) {
    return prioritiesTableCell(rowNum, 5).find("button");
  }

  public SelenideElement pullRequestCreationLoadingSpinner(int rowNum) {
    return prioritiesTableCell(rowNum, 5).find(".nx-loading-spinner");
  }

  public SelenideElement viewPullRequestLink(int rowNum) {
    return prioritiesTableCell(rowNum, 5).find(".iq-pr-status__view-pr-link");
  }

  public SelenideElement retryCreatePullRequestButton(int rowNum) {
    return prioritiesTableCell(rowNum, 5).find(".iq-pr-status__btn--failed");
  }

  public SelenideElement viewViolationsLink(int rowNum) {
    return prioritiesTableCell(rowNum, 5).find(".iq-pr-status__view-violations-link");
  }

  public SelenideElement nextPageButton() {
    return getElement().$(by("aria-label", "goto next page"));
  }

  public SelenideElement componentNameFilter() {
    return child("#priorities-component-name-filter");
  }

  public SelenideElement currentPageButton() {
    return getElement().$(by("aria-current", "page"));
  }

  public SelenideElement directDependencyIndicator(int rowNum) {
    return prioritiesTableCell(rowNum, 1).$(".iq-dependency-indicator.direct");
  }

  public SelenideElement innerSourceDependencyIndicator(int rowNum) {
    return prioritiesTableCell(rowNum, 1).$(".iq-dependency-indicator.inner-source");
  }
}
