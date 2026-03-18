/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ListSimilarWaiversTable
    extends BasicElement<ListSimilarWaiversTable>
{
  static final String ROW_SELECTOR = "tbody .nx-table-row";

  public ListSimilarWaiversTable(String parentComponentSelector) {
    super(parentComponentSelector, "#list-similar-waivers-table");
  }

  public ListSimilarWaiversTableRow headerRow() {
    return new ListSimilarWaiversTableRow(childSelector("thead .nx-table-row"));
  }

  public ElementsCollection rows() {
    return children(ROW_SELECTOR);
  }

  public ListSimilarWaiversTableRow row(int i) {
    return new ListSimilarWaiversTableRow(childSelector(ROW_SELECTOR, nthChild(i)));
  }

  public SelenideElement noWaiversMessage() {
    return child("tbody tr td.nx-cell--meta-info");
  }

  public static class ListSimilarWaiversTableRow
      extends BasicElement<ListSimilarWaiversTableRow>
  {
    private static final String TABLE_CELL_SELECTOR = ".nx-cell";

    public ListSimilarWaiversTableRow(String selector) {
      super(selector);
    }

    public SelenideElement dateCreated() {
      return child(TABLE_CELL_SELECTOR, ".iq-waivers-table__created");
    }

    public SelenideElement waiverExpiration() {
      return child(TABLE_CELL_SELECTOR, ".iq-waivers-table__expiration");
    }

    public SelenideElement createdBy() {
      return child(TABLE_CELL_SELECTOR, ".iq-waivers-table__author");
    }

    public SelenideElement scope() {
      return child(TABLE_CELL_SELECTOR, ".iq-waivers-table__scope");
    }

    public SelenideElement components() {
      return child(TABLE_CELL_SELECTOR, ".iq-waivers-table__component");
    }

    public SelenideElement comments() {
      return child(TABLE_CELL_SELECTOR, ".iq-waivers-table__comment");
    }

    public SelenideElement deleteButton() {
      return child(".list-waivers-row__delete-btn");
    }
  }
}
