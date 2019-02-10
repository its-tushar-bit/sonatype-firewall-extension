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

  public static final Condition CRITICAL = Condition.cssClass("critical");

  public static final Condition SEVERE = Condition.cssClass("severe");

  public static final Condition MODERATE = Condition.cssClass("moderate");

  public static final Condition LOW = Condition.cssClass("low");

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
      super(ROOT, ".iq-tile--dashboard-table-container");
    }

    public ElementsCollection violations() {
      return children("tr[violations-table-row]");
    }

    public ViolationTile violation(int index) {
      return new ViolationTile(childSelector(createSelector("tr[violations-table-row]", nthChild(index + 1))));
    }

    public ViolationTile firstViolation() {
      return new ViolationTile(childSelector("tr[violations-table-row]:first-child"));
    }

    public ViolationTile lastViolation() {
      return new ViolationTile(childSelector("tr[violations-table-row]:last-of-type"));
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("#dashboard-common-results-no-data");
    }

    public SelenideElement mask() {
      return child(".form-mask");
    }
  }

  public class ViolationsHeaders
      extends BasicElement<ViolationsHeaders>
  {
    public ViolationsHeaders() {
      super(ROOT, ".iq-dashboard-headers");
    }

    public IqSortingHeader threatHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--threat a"));
    }

    public IqSortingHeader policyHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--policy a"));
    }

    public IqSortingHeader applicationHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--application a"));
    }

    public IqSortingHeader componentHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--component a"));
    }

    public IqSortingHeader ageHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--age a"));
    }
  }

  public class ViolationTile
      extends BasicElement<ViolationTile>
  {
    public ViolationTile(String selector) {
      super(selector);
    }

    public SelenideElement threatBar() {
      return child(".iq-cell--threat .iq-threat-indication");
    }

    public SelenideElement threatNumber() {
      return child(".iq-cell--threat .iq-threat-number");
    }

    public SelenideElement policy() {
      return child(".iq-cell--policy");
    }

    public SelenideElement application() {
      return child(".iq-cell--application");
    }

    public SelenideElement component() {
      return child(".iq-cell--component");
    }

    public SelenideElement componentLink() {
      return child(".iq-cell--component a");
    }

    public SelenideElement age() {
      return child(".iq-cell--age");
    }

    public SelenideElement latestReport() {
      return child(".iq-cell--report a");
    }

    public SelenideElement componentEllipsis() {
      return component().$(".truncate-ellipsis");
    }
  }
}
