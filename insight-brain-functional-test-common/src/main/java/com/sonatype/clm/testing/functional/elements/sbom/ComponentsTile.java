/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$$;

public class ComponentsTile
    extends BasicElement<ComponentsTile>
{
  public ComponentsTile() {
    super(".sbom-manager-bill-of-materials-components-tile");
  }

  public ElementsCollection rows() {
    return children(".nx-table-row");
  }

  public ElementsCollection tableHeaders() {
    return children("table thead th");
  }

  public ElementsCollection tableBodyRows() {
    return children("table tbody tr");
  }

  public ElementsCollection vulnerabilitiesColumns(int row) {
    return tableBodyRows().get(row).findAll(".nx-small-threat-counter__count");
  }

  public SelenideElement releaseStatusPercentageColumn(int row) {
    return tableBodyRows().get(row)
        .find(".sbom-manager-bill-of-materials-components-tile__release-status-percentage");
  }

  public SelenideElement nameColum(int row) {
    return tableBodyRows().get(row)
        .find(".sbom-manager-bill-of-materials-components-tile__component-name-content");
  }

  public SelenideElement licenseColumn(int row) {
    return tableBodyRows().get(row)
        .find(".sbom-manager-bill-of-materials-components-tile__licenses");
  }

  public SelenideElement overriddenPill(int row) {
    return tableBodyRows().get(row)
        .find(".sbom-manager-bill-of-materials-components-tile__overridden-pill");
  }

  public SelenideElement noComponentsColumn() {
    return tableBodyRows().get(0).find("td");
  }

  public ElementsCollection tableBodyRowsColumns(int row) {
    return tableBodyRows().get(row).findAll("td");
  }

  public SelenideElement columnHeader(int column) {
    return tableHeaders().get(column);
  }

  public SelenideElement header() {
    return child("header.nx-tile-header .nx-h2");
  }

  public SelenideElement footer() {
    return child("nav.nx-btn-bar--pagination");
  }

  public SelenideElement paginationStatus() {
    return child(".sbom-manager-bill-of-materials-components-tile__pagination-status");
  }

  public ElementsCollection paginationButtons() {
    return footer().findAll("button.nx-btn--pagination");
  }

  public SelenideElement filterByButton() {
    return child("button.nx-btn.nx-btn--tertiary");
  }

  public ElementsCollection tableRows() {
    return $$(".nx-table .nx-table-row");
  }

  public SelenideElement inputComponentSearch() {
    return child("#component-search");
  }

  public SelenideElement getLoadingSpinner() {
    return child(".nx-loading-spinner");
  }
}
