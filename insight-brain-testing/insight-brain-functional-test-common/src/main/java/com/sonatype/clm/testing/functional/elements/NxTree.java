/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

public class NxTree
    extends BasicElement<NxTree>
{
  public NxTree(String selector) {
    super(selector);
  }

  public ElementsCollection treeItems() {
    return children(".nx-tree__item");
  }

  public ElementsCollection clickableTreeItems() {
    return children(".nx-tree__item .nx-text-link");
  }

  public ElementsCollection collapseIcons() {
    return children(".nx-tree__collapse-click");
  }

  public ElementsCollection nonClickableTreeItems() {
    return children(".nx-tree__item .iq-matched-hash-tree-label");
  }

  public ElementsCollection collapsibleTreeItems() {
    return children(".nx-tree__item--collapsible");
  }

  public ElementsCollection threatIndicators() {
    return children(".nx-threat-indicator");
  }

  public SelenideElement dependencyTypeIndicator(SelenideElement treeItem) {
    return treeItem.find(By.cssSelector(".iq-dependency-indicator"));
  }

  public SelenideElement collapseIconFor(SelenideElement treeItem) {
    return treeItem.find(By.cssSelector(".nx-tree__collapse-click"));
  }
}
