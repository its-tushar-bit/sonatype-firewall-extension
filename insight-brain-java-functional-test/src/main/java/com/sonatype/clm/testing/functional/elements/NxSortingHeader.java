/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class NxSortingHeader
    extends BasicElement<NxSortingHeader>
{
  private static final Condition UP = Condition.cssClass("fa-sort-up");

  private static final Condition DOWN = Condition.cssClass("fa-sort-down");

  public NxSortingHeader(String selector) {
    super(selector);
  }

  public NxSortArrows sortArrows() {
    return new NxSortArrows(".nx-cell__sort-icons");
  }

  public static class NxSortArrows
      extends BasicElement<NxSortArrows>
  {
    NxSortArrows(String selector) {
      super(selector);
    }

    public SelenideElement shouldBeUp() {
      return child(createSelector(".svg-inline--fa", nthChild(2))).shouldHave(UP);
    }

    public SelenideElement shouldBeDown() {
      return child(createSelector(".svg-inline--fa", nthChild(2))).shouldHave(DOWN);
    }
  }
}
