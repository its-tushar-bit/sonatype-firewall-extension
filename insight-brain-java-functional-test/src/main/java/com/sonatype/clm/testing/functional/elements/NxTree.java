/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;

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
}
