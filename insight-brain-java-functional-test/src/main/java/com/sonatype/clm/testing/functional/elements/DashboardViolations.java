/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardViolations
{
  private static final String ROOT = "#dashboard-violations";
  public static Condition CRITICAL = Condition.cssClass("critical");
  public static Condition SEVERE = Condition.cssClass("severe");
  public static Condition MODERATE = Condition.cssClass("moderate");
  public static Condition LOW = Condition.cssClass("low");

  public ViolationsHeaders headers() {
    return new ViolationsHeaders();
  }

  public ViolationsResults results() {
    return new ViolationsResults();
  }

  public class ViolationsResults
      extends BasicElement<ViolationsResults>
  {
    ViolationsResults() {
      super(ROOT, ".dashboard-results");
    }

    public ElementsCollection violations() {
      return children(".tile");
    }

    public ViolationTile violation(int index) {
      return new ViolationTile(childSelector(createSelector("violations-table-row", nthChild(index + 1))));
    }

    public ViolationTile firstViolation() {
      return new ViolationTile(childSelector("violations-table-row:first-child"));
    }

    public ViolationTile lastViolation() {
      return new ViolationTile(childSelector("violations-table-row:last-child"));
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("#no-data");
    }
  }

  public class ViolationsHeaders
      extends BasicElement<ViolationsHeaders>
  {
    public ViolationsHeaders() {
      super(ROOT, ".dashboard-headers");
    }

    public SelenideElement threatHeader() {
      return child(".threat a");
    }

    public SelenideElement policyHeader() {
      return child(".policy a");
    }

    public SelenideElement applicationHeader() {
      return child(".application a");
    }

    public SelenideElement componentHeader() {
      return child(".component a");
    }

    public SelenideElement ageHeader() {
      return child(".age a");
    }
  }

  public class ViolationTile extends BasicElement<ViolationTile> {

    public ViolationTile(String selector) {
      super(selector);
    }

    public SelenideElement threatBar() {
      return child(".clm-bar");
    }

    public SelenideElement threatNumber() {
      return child(".threat-number");
    }

    public SelenideElement policy() {
      return child(".policy");
    }

    public SelenideElement application() {
      return child(".application");
    }

    public SelenideElement component() {
      return child(".component a");
    }

    public SelenideElement age() {
      return child(".age");
    }

    public SelenideElement latestReport() {
      return child(".report a");
    }
  }
}
