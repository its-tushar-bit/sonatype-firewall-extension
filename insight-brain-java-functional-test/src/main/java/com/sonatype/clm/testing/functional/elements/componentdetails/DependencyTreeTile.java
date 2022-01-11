/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxTree;

import com.codeborne.selenide.SelenideElement;

public class DependencyTreeTile
    extends BasicElement<ClaimTabContent>
{
  public static final String ROOT_SELECTOR = ".component-details-dependency-tree-tile";

  public DependencyTreeTile() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement title() {
    return child(".nx-h2");
  }

  public NxTree tree() {
    return new NxTree(".iq-dependency-tree");
  }

  public SelenideElement unavailableAlert() {
    return child(".component-details-dependency-tree-tile__unavailable-tree-alert");
  }
}
