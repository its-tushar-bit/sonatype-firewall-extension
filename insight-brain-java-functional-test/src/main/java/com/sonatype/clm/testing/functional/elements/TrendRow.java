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

public class TrendRow extends BasicElement<TrendRow>
{

  public TrendRow(String selector) {
    super(selector);
  }

  public SelenideElement category() {
    return child("td", nthChild(1));
  }

  public SelenideElement count() {
    return child("td", nthChild(2));
  }

  public SelenideElement averageAge() {
    return child("td", nthChild(3));
  }

  public SelenideElement ninetyPercentileAge() {
    return child("td", nthChild(4));
  }

  public TrendDelta delta() {
    return new TrendDelta(childSelector("td", nthChild(5)));
  }

  public ElementsCollection barChartPoints() {
    return children("td", nthChild(8), "title");
  }

}
