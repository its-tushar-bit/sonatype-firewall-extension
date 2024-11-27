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

public class NxTransferList
    extends BasicElement<NxTransferList>
{
  public NxTransferList(String selector) {
    super(selector);
  }

  public ElementsCollection availableItems() {
    return children(".nx-fieldset:nth-of-type(1) .nx-transfer-list__item-list .nx-transfer-list__item");
  }

  /**
   * NxTransferList uses NxScrollRender, NxScrollRender adds 2 extra divs to the children, one on top and one on bottom.
   * So we need to add 2 to the nthChild index to get the correct child.
   * WARNING: if the list is scrolled, this method won't work coherently since the scroll re-renders the list.
   */
  public SelenideElement availableItem(int num) {
    return child(".nx-fieldset:nth-of-type(1) .nx-transfer-list__item-list .nx-transfer-list__item", nthChild(num + 2));
  }

  public SelenideElement availableFooter() {
    return child(".nx-fieldset:nth-of-type(1) .nx-transfer-list__footer");
  }

  public ElementsCollection transferredItems() {
    return children(".nx-fieldset:nth-of-type(2) .nx-transfer-list__item-list .nx-transfer-list__item");
  }

  /**
   * NxTransferList uses NxScrollRender, NxScrollRender adds 2 extra divs to the children, one on top and one on bottom.
   * So we need to add 2 to the nthChild index to get the correct child.
   * WARNING: if the list is scrolled, this method won't work coherently since the scroll re-renders the list.
   */
  public SelenideElement transferredItem(int num) {
    return child(".nx-fieldset:nth-of-type(2) .nx-transfer-list__item-list .nx-transfer-list__item", nthChild(num + 2));
  }

  public SelenideElement transferredFooter() {
    return child(".nx-fieldset:nth-of-type(2) .nx-transfer-list__footer");
  }

  public SelenideElement filter() {
    return child(".nx-filter-input input");
  }
}
