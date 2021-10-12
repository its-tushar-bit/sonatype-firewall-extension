/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class VersionGraph
    extends BasicElement<VersionGraph>
{
  private static final String GRAPH_SELECTOR = "#aiVersionChartViz svg";

  public VersionGraph() {
    super(GRAPH_SELECTOR);
  }

  public SelenideElement getGraph() {
    return $(GRAPH_SELECTOR);
  }

  public SelenideElement selectVersion(int idx) {
    return child("rect[pointer-events]", nthChild(idx + 1));
  }
}
