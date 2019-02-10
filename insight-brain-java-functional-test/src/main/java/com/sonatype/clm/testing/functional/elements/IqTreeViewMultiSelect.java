/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class IqTreeViewMultiSelect
    extends BasicElement<IqTreeViewMultiSelect>
{
  public IqTreeViewMultiSelect(final String selector) {
    super(selector);
  }

  public SelenideElement twisty() {
    return child(".iq-tree-view__trigger");
  }

  public ElementsCollection multiSelectList() {
    return children(".iq-tree-view__child");
  }

  public ElementsCollection singleSelectList() {
    return children("iq-radio.iq-tree-view__child");
  }

  public IqCheckbox checkboxItem(int index) {
    return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(index)));
  }

  public IqRadio radioItem(int index) {
    return new IqRadio(child(".iq-tree-view__children iq-radio.iq-tree-view__child", nthChild(index)));
  }

  public IqCheckbox allItems() {
    return checkboxItem(1);
  }

  public SelenideElement counter() {
    return child(".iq-counter");
  }

  public SelenideElement anchor() {
    return child("a");
  }

  public SelenideElement tooltip() {
    return $(".tooltip-inner");
  }
}
