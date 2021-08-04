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

public class ComponentWaiversPopover
    extends BasicElement<ComponentWaiversPopover>
{
  public static final String ROOT = "#component-waivers-container";

  public ComponentWaiversPopover() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child(".component-waivers-header__title-text");
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

    public SelenideElement deleteWaiverButton(int rowIndex) {
      return getRow(rowIndex).find(".nx-btn--delete-waiver");
    }

    public SelenideElement emptyTableMessage() {
      return getRow(1).find(".nx-cell--meta-info");
    }
  }
}

