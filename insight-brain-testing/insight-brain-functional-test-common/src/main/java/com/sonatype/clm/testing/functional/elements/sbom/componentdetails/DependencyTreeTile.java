/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class DependencyTreeTile
    extends BasicElement<DependencyTreeTile>
{
  static final String ROOT_SELECTOR = ".sbom-component-details-dependency-tree-tile";

  public DependencyTreeTile() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child("header.nx-tile-header .nx-h2");
  }

  public SelenideElement content() {
    return child(".nx-tile-content");
  }
}
