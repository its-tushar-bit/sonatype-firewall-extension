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

public class ListWaiversTable
    extends BasicElement<ListWaiversTable>
{
  static final String ROW_SELECTOR = "tbody .nx-table-row";

  public ListWaiversTable(String parentComponentSelector) {
    super(parentComponentSelector, "#list-waivers-table");
  }

  public ListWaiversTableRow headerRow() {
    return new ListWaiversTableRow(childSelector("thead .nx-table-row"));
  }

  public ElementsCollection rows() {
    return children(ROW_SELECTOR);
  }

  public ListWaiversTableRow row(int i) {
    return new ListWaiversTableRow(childSelector(ROW_SELECTOR, nthChild(i)));
  }

  public ListAutoWaiverTableRow autoWaiverRow(int i) {
    return new ListAutoWaiverTableRow(childSelector(ROW_SELECTOR, nthChild(i)));
  }

  public SelenideElement noWaiversMessage() {
    return child("tbody tr td.nx-cell--meta-info");
  }

  public static class ListWaiversTableRow
      extends BasicElement<ListWaiversTableRow>
  {
    private static final String TABLE_CELL_SELECTOR = ".nx-cell";

    public ListWaiversTableRow(String selector) {
      super(selector);
    }

    public SelenideElement dateCreated() {
      return child(".iq-waivers-table__created");
    }

    public SelenideElement duration() {
      return child(TABLE_CELL_SELECTOR, nthChild(1));
    }

    public SelenideElement waiverExpiration() {
      return child(".iq-waivers-table__expiration");
    }

    public SelenideElement waiverDetails() {
      return child(TABLE_CELL_SELECTOR, nthChild(2));
    }

    public SelenideElement createdBy() {
      return child(".iq-waivers-table__author");
    }

    public SelenideElement scope() {
      return child(".iq-waivers-table__scope");
    }

    public SelenideElement components() {
      return child(".iq-waivers-table__component");
    }

    public SelenideElement comments() {
      return child(".iq-waivers-table__comment");
    }

    public SelenideElement deleteButton() {
      return child(".list-waivers-row__delete-btn");
    }
  }

  public static class ListAutoWaiverTableRow
      extends BasicElement<ListAutoWaiverTableRow>
  {
    private static final String TABLE_CELL_SELECTOR = ".nx-cell";

    public ListAutoWaiverTableRow(String selector) {
      super(selector);
    }

    public SelenideElement dateCreated() {
      return child(".iq-auto-waiver-table__created");
    }

    public SelenideElement duration() {
      return child(TABLE_CELL_SELECTOR, nthChild(1));
    }

    public SelenideElement waiverExpiration() {
      return child(".iq-auto-waiver-table__expiration");
    }

    public SelenideElement waiverVersion() {
      return child(".iq-auto-waiver-table__version");
    }

    public SelenideElement waiverDetails() {
      return child(TABLE_CELL_SELECTOR, nthChild(2));
    }

    public SelenideElement createdBy() {
      return child(".iq-auto-waiver-table__author");
    }

    public SelenideElement scope() {
      return child(".iq-auto-waiver-table__scope");
    }

    public SelenideElement components() {
      return child(".iq-auto-waiver-table__component");
    }

    public SelenideElement revocationButton() {
      return child(".list-auto-waiver-row__revocation-btn");
    }
  }
}
