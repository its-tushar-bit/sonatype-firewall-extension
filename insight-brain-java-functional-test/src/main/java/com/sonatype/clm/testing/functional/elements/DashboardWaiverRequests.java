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

public class DashboardWaiverRequests
{
  private static final String ROOT = "#dashboard-waivers";

  private static final String PAGINATOR = ".nx-btn-bar--indeterminate-pagination";

  public WaiverRequestsHeaders headers() {
    return new WaiverRequestsHeaders();
  }

  public WaiverRequestsResults results() {
    return new WaiverRequestsResults();
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

  public class WaiverRequestsResults
      extends BasicElement<WaiverRequestsResults>
  {
    WaiverRequestsResults() {
      super(ROOT, "#iq-dashboard-waiver-requests-table");
    }

    public ElementsCollection waiverRequests() {
      return children(".iq-dashboard-waiver-request");
    }

    public WaiverRequestTile firstWaiverRequest() {
      return new WaiverRequestTile(childSelector(".iq-dashboard-waiver-request:first-child"));
    }

    public WaiverRequestTile lastWaiverRequest() {
      return new WaiverRequestTile(childSelector(".iq-dashboard-waiver-request", nthChild(waiverRequests().size())));
    }

    public WaiverRequestTile waiverRequest(int index) {
      return new WaiverRequestTile(childSelector(createSelector(".iq-dashboard-waiver-request", nthChild(index + 1))));
    }

    public List<WaiverRequestTile> allWaiverRequests() {
      List<WaiverRequestTile> allWaiverRequests = new ArrayList<>();

      for (int i = 0; i < waiverRequests().size(); i++) {
        allWaiverRequests
            .add(new WaiverRequestTile(childSelector(createSelector(".iq-dashboard-waiver-request", nthChild(i + 1)))));
      }
      return allWaiverRequests;
    }

    public SelenideElement noDataMessage() {
      return child(".iq-dashboard-waivers-entries .nx-table-row:last-child");
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

  public class WaiverRequestTile
      extends BasicElement<WaiverRequestTile>
  {
    public WaiverRequestTile(String selector) {
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

    public SelenideElement requester() {
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

    public SelenideElement status() {
      return child(createSelector(".nx-cell", nthChild(7)));
    }

    public SelenideElement chevron() {
      return child(createSelector(".nx-cell", nthChild(8)));
    }

    public SelenideElement componentEllipsis() {
      return component().$(".truncate-ellipsis");
    }
  }

  public class WaiverRequestsHeaders
      extends BasicElement<WaiverRequestsHeaders>
  {
    public WaiverRequestsHeaders() {
      super(ROOT, ".nx-table-row--header");
    }

    public NxSortingHeader threatHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(1))));
    }

    public NxSortingHeader dateHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(2))));
    }

    public NxSortingHeader requesterHeader() {
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

    public NxSortingHeader statusHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(7))));
    }
  }
}
