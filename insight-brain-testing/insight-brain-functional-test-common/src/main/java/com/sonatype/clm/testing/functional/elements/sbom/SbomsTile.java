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

public class SbomsTile
    extends BasicElement<SbomsTile>
{
  public SbomsTile() {
    super("#owner-pill-sboms");
  }

  public SelenideElement table() {
    return child(".nx-table");
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

  public ElementsCollection tableBodyRowsColumns(int row) {
    return tableBodyRows().get(row).findAll("td");
  }

  public SelenideElement releaseStatusColumn(int row) {
    return tableBodyRows().get(row)
        .find(".sbom-manager-owner-summary-sboms-tile-table__releaseStatusPercentage");
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

  public ElementsCollection paginationButtons() {
    return footer().findAll("button.nx-btn--pagination");
  }

  public SelenideElement importButton() {
    return child("#import-sbom-button");
  }

  public ElementsCollection tableRows() {
    return $$(".nx-table .nx-table-row");
  }

  public SelenideElement getRowInvalidSbomIndicatorFromRow(int row) {
    return tableRows().get(row).$(".sbom-manager-invalid-sbom-indicator");
  }

  public ElementsCollection actionsSbomOptions() {
    return $$(".nx-dropdown-menu .nx-dropdown-button");
  }

  public SelenideElement actions(int row) {
    return tableRows().get(row).find("button.nx-icon-dropdown__toggle");
  }

  public SelenideElement deleteSbomModal() {
    return child("#delete-sbom-version-modal");
  }

  public SelenideElement deleteSbomModalPrimaryButton() {
    return deleteSbomModal().find("button.nx-btn--primary");
  }

  public SelenideElement billOfMaterialsLink(int row) {
    return tableRows().get(row).find("a.sbom-manager-owner-summary-sboms-tile-table__version-link");
  }
}
