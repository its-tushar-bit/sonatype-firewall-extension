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

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardViolations
{
  private static final String ROOT = "#dashboard-violations";

  public static final Condition CRITICAL = Condition.cssClass("nx-threat-indicator--critical");

  public static final Condition SEVERE = Condition.cssClass("nx-threat-indicator--severe");

  public static final Condition MODERATE = Condition.cssClass("nx-threat-indicator--moderate");

  public static final Condition LOW = Condition.cssClass("nx-threat-indicator--low");

  public ViolationsHeaders headers() {
    return new ViolationsHeaders();
  }

  public ViolationsResults results() {
    return new ViolationsResults();
  }

  public ElementsCollection paginationButtons() {
    return $$(".nx-btn--pagination");
  }

  public class ViolationsResults
      extends BasicElement<ViolationsResults>
  {
    ViolationsResults() {
      super(ROOT, ".iq-dashboard-violation-entries");
    }

    public ElementsCollection violations() {
      return children(".iq-dashboard-violation");
    }

    public ViolationTile violation(int index) {
      return new ViolationTile(childSelector(createSelector(".iq-dashboard-violation", nthChild(index + 1))));
    }

    public ViolationTile firstViolation() {
      return new ViolationTile(childSelector(".iq-dashboard-violation:first-child"));
    }

    public ViolationTile lastViolation() {
      return new ViolationTile(createSelector(".iq-dashboard-violation", nthChild(violations().size())));
    }

    public SelenideElement noDataMessage() {
      return child(".nx-table-row:last-child");
    }

    public SelenideElement mask() {
      return $(".iq-dashboard-form-mask");
    }
  }

  public class ViolationsHeaders
      extends BasicElement<ViolationsHeaders>
  {
    public ViolationsHeaders() {
      super(ROOT, ".nx-table-row--header");
    }

    public NxSortingHeader threatHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(1))));
    }

    public NxSortingHeader policyHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(2))));
    }

    public NxSortingHeader applicationHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(3))));
    }

    public NxSortingHeader componentHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(4))));
    }

    public NxSortingHeader ageHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(5))));
    }
  }

  public class ViolationTile
      extends BasicElement<ViolationTile>
  {
    public ViolationTile(String selector) {
      super(selector);
    }

    public SelenideElement threatCell() {
      return child(".iq-threat-cell");
    }

    public SelenideElement threatIndicator() {
      return child(".iq-threat-cell .nx-threat-indicator");
    }

    public SelenideElement threatNumber() {
      return child(".iq-threat-cell .nx-threat-number");
    }

    public SelenideElement policy() {
      return child(".iq-policy-cell");
    }

    public SelenideElement application() {
      return child(createSelector(".nx-cell", nthChild(3)));
    }

    public SelenideElement component() {
      return child(createSelector(".nx-cell", nthChild(4)));
    }

    public SelenideElement age() {
      return child(createSelector(".nx-cell", nthChild(5)));
    }

    public SelenideElement chevron() {
      return child(createSelector(".nx-cell", nthChild(6)));
    }

    public SelenideElement componentEllipsis() {
      return component().$(".truncate-ellipsis");
    }
  }
}
