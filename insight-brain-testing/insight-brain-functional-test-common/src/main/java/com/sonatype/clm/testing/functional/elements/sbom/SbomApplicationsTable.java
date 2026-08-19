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

public class SbomApplicationsTable
    extends BasicElement<SbomApplicationsTable>
{
  public SbomApplicationsTable() {
    super(".sbom-manager-applications-table");
  }

  public SelenideElement applicationNameFilter() {
    return child("#application-name-filter");
  }

  public SelenideElement table() {
    return child(".sbom-manager-applications-table__table");
  }

  public ElementsCollection tableRows() {
    return $$(".nx-table .nx-table-row");
  }

  public ElementsCollection tableHeaders() {
    return children("table thead th");
  }

  public ElementsCollection tableBodyRows() {
    return children("table tbody tr");
  }

  public SelenideElement releaseStatusColumn(int row) {
    return tableBodyRows().get(row)
        .find(".sbom-manager-applications-table__releaseStatusPercentage");
  }

  public ElementsCollection vulnerabilitiesColumns(int row) {
    return tableBodyRows().get(row).findAll(".nx-small-vulnerability-counter__count");
  }

  public ElementsCollection tableBodyRowsColumns(int row) {
    return tableBodyRows().get(row).findAll("td");
  }

  public SelenideElement columnHeader(int column) {
    return tableHeaders().get(column);
  }

  public SelenideElement footer() {
    return child("nav.nx-btn-bar--pagination");
  }

  public SelenideElement paginationStatus() {
    return child(".sbom-manager-applications-table__pagination-status");
  }

  public ElementsCollection paginationButtons() {
    return footer().findAll("button.nx-btn--pagination");
  }
}
