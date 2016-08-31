/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.TrendRow;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class TrendsModal extends BasicElement<TrendsModal>
{

  private static final String ROOT = "#policy-trends-dialog";

  public static Condition NEUTRAL = cssClass("neutral");
  public static Condition NATURAL = cssClass("natural");
  public static Condition INVERSE = cssClass("inverse");
  
  public TrendsModal() {
    super(ROOT);
  }

  public SelenideElement closeButton() {
    return $("#policy-trends-dialog-close");
  }

  public SelenideElement contentsTable() {
    return $("#policySummaryData");
  }

  public ElementsCollection rows() {
    return children("tbody tr");
  }

  public TrendRow discoveredRow() {
    return new TrendRow(childSelector("tbody tr", nthChild(4)));
  }

  public TrendRow fixedRow() {
    return new TrendRow(childSelector("tbody tr", nthChild(3)));
  }

  public TrendRow pendingRow() {
    return new TrendRow(childSelector("tbody tr", nthChild(1)));
  }

  public TrendRow waivedRow() {
    return new TrendRow(childSelector("tbody tr", nthChild(2)));
  }
}
