/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
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

  public static class ComponentWaiversPopoverTable
      extends BasicElement<ComponentWaiversPopoverTable>
  {
    public static final String TABLE_ROOT = ".iq-policy-violations-table";

    public static final String ROW_SELECTOR = "tbody .nx-table-row";

    private ComponentWaiversPopoverTable(String selectorWithParent) {
      super(selectorWithParent);
    }

    private static ComponentWaiversPopoverTable getTableForParent(String parentSelector) {
      String combinedSelector = SelectorUtils.createSelector(parentSelector, TABLE_ROOT);
      return new ComponentWaiversPopoverTable(combinedSelector);
    }

    public ElementsCollection getRows() {
      return children("tbody > tr");
    }

    public SelenideElement getRow(int rowIndex) {
      return child("tbody > tr:nth-child(" + rowIndex + ")");
    }

    public WaiverRow row(int index) {
      return new WaiverRow(childSelector(ROW_SELECTOR, nthChild(index)));
    }

    public ElementsCollection getCellsByNthRow(int rowIndex) {
      return children("tbody > tr:nth-child(" + rowIndex + ") .nx-cell");
    }

    public SelenideElement deleteWaiverButton(int rowIndex) {
      return getRow(rowIndex).find(".list-waivers-row__delete-btn");
    }

    public SelenideElement emptyTableMessage() {
      return getRow(1).find(".nx-cell--meta-info");
    }
  }

  public static class WaiverRow
      extends BasicElement<WaiverRow>
  {
    public WaiverRow(String selector) {
      super(selector);
    }

    public SelenideElement dateCreated() {
      return child(".waiver-row-date-created");
    }

    public SelenideElement scope() {
      return child(".waiver-row-scope");
    }

    public SelenideElement components() {
      return child(".waiver-row-component");
    }

    public SelenideElement createdBy() {
      return child(".waiver-row-author");
    }

    public SelenideElement comments() {
      return child(".waiver-row-author");
    }

    public SelenideElement deleteButton() {
      return child(".list-waivers-row__delete-btn");
    }
  }
}
