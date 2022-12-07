/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ComponentWaiversPopover
    extends BasicElement<ComponentWaiversPopover>
{
  public static final String ROOT = "#component-waivers-container";

  public ComponentWaiversPopover() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child(".iq-popover-header__title-text");
  }

  public SelenideElement closePopoverButton() {
    return child("#component-waivers-close-btn");
  }

  public ComponentWaiversPopoverTable componentWaiversPopoverTable() {
    return ComponentWaiversPopoverTable.getTableForParent(ROOT);
  }

  public DeleteWaiverModal deleteWaiverModal() {
    return new ListWaiversPage.DeleteWaiverModal();
  }

  public static class ComponentWaiversPopoverTable
      extends BasicElement<ComponentWaiversPopoverTable>
  {
    public static final String TABLE_ROOT = ".iq-policy-violations-table";

    public static final String ROW_SELECTOR = "tbody .nx-table-row";

    private static ComponentWaiversPopoverTable getTableForParent(String parentSelector) {
      String combinedSelector = SelectorUtils.createSelector(parentSelector, TABLE_ROOT);
      return new ComponentWaiversPopoverTable(combinedSelector);
    }

    private ComponentWaiversPopoverTable(String selectorWithParent) {
      super(selectorWithParent);
    }

    public ElementsCollection getRows() {
      return children("tbody > tr");
    }

    public SelenideElement getRow(int rowIndex) {
      return child("tbody > tr:nth-child(" + rowIndex + ")");
    }

    public ComponentWaiversPopoverTableRow row(int index) {
      return new ComponentWaiversPopoverTableRow(childSelector(ROW_SELECTOR, nthChild(index)));
    }

    public ElementsCollection getCellsByNthRow(int rowIndex) {
      return children("tbody > tr:nth-child(" + rowIndex + ") .nx-cell");
    }

    public SelenideElement deleteWaiverButton(int rowIndex) {
      return getRow(rowIndex).find(".iq-component-violations-waivers-table__delete-btn");
    }

    public SelenideElement emptyTableMessage() {
      return getRow(1).find(".nx-cell--meta-info");
    }
  }

  public static class ComponentWaiversPopoverTableRow
      extends BasicElement<ComponentWaiversPopoverTableRow>
  {
    public ComponentWaiversPopoverTableRow(String selector) {
      super(selector);
    }

    public SelenideElement policyConstraint() {
      return child(".nx-cell", nthChild(1));
    }

    public SelenideElement dateCreated() {
      return child(".nx-cell", nthChild(2));
    }

    public SelenideElement scope() {
      return child(".nx-cell", nthChild(3));
    }

    public SelenideElement components() {
      return child(".nx-cell", nthChild(4));
    }

    public SelenideElement createdBy() {
      return child(".nx-cell", nthChild(5));
    }

    public SelenideElement comments() {
      return child(".nx-cell", nthChild(6));
    }

    public SelenideElement deleteButton() {
      return child(".iq-component-violations-waivers-table__delete-btn");
    }
  }
}

