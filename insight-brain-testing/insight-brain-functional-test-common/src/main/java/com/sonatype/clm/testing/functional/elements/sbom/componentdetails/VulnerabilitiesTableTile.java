/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.h2.util.StringUtils;

public class VulnerabilitiesTableTile
    extends BasicElement<VulnerabilitiesTableTile>
{
  static final String ROOT_SELECTOR = "#sbom-manager-cdp-vulnerabilities-tile";

  public VulnerabilitiesTableTile(String rootIdSelector) {
    super(StringUtils.isNullOrEmpty(rootIdSelector) ? ROOT_SELECTOR : ROOT_SELECTOR + "__" + rootIdSelector);
  }

  public SelenideElement header() {
    return child("header.nx-tile-header .nx-h2");
  }

  public ElementsCollection tableHeaders() {
    return children("table thead th");
  }

  public ElementsCollection tableRows() {
    return children("table tbody tr");
  }

  public SelenideElement getColumnData(int row, int column) {
    return tableRows().get(row).findAll("td").get(column);
  }

  public SelenideElement getColumnHeader(int column) {
    return tableHeaders().get(column);
  }
}
