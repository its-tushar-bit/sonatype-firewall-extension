/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ViolationTrendPlot
    extends BasicElement<ViolationTrendPlot>
{
  public ViolationTrendPlot(String selector) {
    super(selector);
  }

  public BarPlot deltaPlot() {
    return new BarPlot(childSelector(".component.table", "div.component.plot.xy-plot.bar-plot", ":nth-of-type(2)"));
  }

  public BarPlot newPlot() {
    return new BarPlot(childSelector(".component.table", "div.component.plot.xy-plot.bar-plot", ":nth-of-type(4)"));
  }

  public BarPlot waivedPlot() {
    return new BarPlot(childSelector(".component.table", "div.component.plot.xy-plot.bar-plot", ":nth-of-type(6)"));
  }

  public BarPlot fixedPlot() {
    return new BarPlot(childSelector(".component.table", "div.component.plot.xy-plot.bar-plot", ":nth-of-type(8)"));
  }

  public static class BarPlot
      extends BasicElement<BarPlot>
  {
    BarPlot(String... selector) {
      super(selector);
    }

    public SelenideElement bar(int index) {
      return child(".bar-area", "rect", nthChild(index + 1));
    }
  }
}
