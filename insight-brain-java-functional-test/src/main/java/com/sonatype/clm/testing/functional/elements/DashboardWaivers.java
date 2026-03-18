/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardWaivers
{
  private static final String ROOT = "#dashboard-waivers";

  private static final String PAGINATOR = ".nx-btn-bar--indeterminate-pagination";

  public WaiversHeaders headers() {
    return new WaiversHeaders();
  }

  public WaiversResults results() {
    return new WaiversResults();
  }

  public WaiverResultsPaginator paginator() {
    return new WaiverResultsPaginator();
  }

  public ElementsCollection paginationButtons() {
    return $$(".nx-btn--pagination");
  }

  public SelenideElement mask() {
    return $(".iq-dashboard-form-mask");
  }

  public class WaiversResults
      extends BasicElement<WaiversResults>
  {
    WaiversResults() {
      super(ROOT, ".iq-dashboard-waivers-entries");
    }

    public ElementsCollection waivers() {
      return children(".iq-dashboard-waiver");
    }

    public WaiverTile firstWaiver() {
      return new WaiverTile(childSelector(".iq-dashboard-waiver:first-child"));
    }

    public WaiverTile lastWaiver() {
      return new WaiverTile(childSelector(".iq-dashboard-waiver", nthChild(waivers().size())));
    }

    public WaiverTile waiver(int index) {
      return new WaiverTile(childSelector(createSelector(".iq-dashboard-waiver", nthChild(index + 1))));
    }

    public List<WaiverTile> allWaivers() {
      List<WaiverTile> allWaivers = new ArrayList<>();

      for (int i = 0; i < waivers().size(); i++) {
        allWaivers.add(new WaiverTile(childSelector(createSelector(".iq-dashboard-waiver", nthChild(i + 1)))));
      }
      return allWaivers;
    }

    public SelenideElement noDataMessage() {
      return child(".nx-table-row:last-child");
    }
  }

  public static class WaiverResultsPaginator
      extends BasicElement<WaiverResultsPaginator>
  {
    WaiverResultsPaginator() {
      super(ROOT, ".nx-table-container__footer");
    }

    public SelenideElement buttonBar() {
      return child(PAGINATOR);
    }

    public SelenideElement nextPageButton() {
      return childXpath("//button[@aria-label='next page']");
    }

    public SelenideElement previousPageButton() {
      return childXpath("//button[@aria-label='previous page']");
    }
  }

  public class WaiversHeaders
      extends BasicElement<WaiversHeaders>
  {
    public WaiversHeaders() {
      super(ROOT, ".nx-table-row--header");
    }

    public NxSortingHeader threatHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(1))));
    }

    public NxSortingHeader dateHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(2))));
    }

    public NxSortingHeader expirationHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(3))));
    }

    public NxSortingHeader policyHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(4))));
    }

    public NxSortingHeader scopeHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(5))));
    }

    public NxSortingHeader componentHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(6))));
    }
  }

  public class WaiverTile
      extends BasicElement<WaiverTile>
  {
    public WaiverTile(String selector) {
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

    public SelenideElement createTime() {
      return child(createSelector(".nx-cell", nthChild(2)));
    }

    public SelenideElement expiryTime() {
      return child(createSelector(".nx-cell", nthChild(3)));
    }

    public SelenideElement policy() {
      return child(createSelector(".nx-cell", nthChild(4)));
    }

    public SelenideElement scope() {
      return child(createSelector(".nx-cell", nthChild(5)));
    }

    public SelenideElement component() {
      return child(createSelector(".nx-cell", nthChild(6)));
    }

    public SelenideElement chevron() {
      return child(createSelector(".nx-cell", nthChild(7)));
    }

    public SelenideElement componentEllipsis() {
      return component().$(".truncate-ellipsis");
    }

    public SelenideElement upgradeAvailable() {
      return child(createSelector(".iq-upgrade-cell"));
    }
  }
}
