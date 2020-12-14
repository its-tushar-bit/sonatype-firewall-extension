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

public class NxTreeViewMultiSelect
    extends BasicElement<NxTreeViewMultiSelect>
{
  public NxTreeViewMultiSelect(final String selector) {
    super(selector);
  }

  public SelenideElement twisty() {
    return child(".nx-tree-view__trigger");
  }

  public ElementsCollection multiSelectList() {
    return children(".nx-tree-view__child");
  }

  public ElementsCollection singleSelectList() {
    return children(".nx-tree-view__child.nx-radio");
  }

  public NxCheckbox checkboxItem(int index) {
    return new NxCheckbox(child(".nx-tree-view__children .nx-tree-view__child.nx-checkbox", nthChild(index)));
  }

  public NxRadio radioItem(int index) {
    return new NxRadio(child(".nx-tree-view__children .nx-tree-view__child.nx-radio", nthChild(index)));
  }

  public NxCheckbox allItems() {
    return checkboxItem(1);
  }

  public SelenideElement counter() {
    return child(".nx-counter");
  }
}
