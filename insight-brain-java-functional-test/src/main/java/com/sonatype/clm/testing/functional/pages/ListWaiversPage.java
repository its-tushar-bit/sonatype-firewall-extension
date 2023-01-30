/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ListWaiversPage
    extends BasicElement<ListWaiversPage>
{
  public static final String ROOT = "#list-waivers-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/waivers/{id}", violationId);
  }

  public static String urlWithQueryParams(String violationId, String type, String sidebarReference) {
    return BaseUrl.resolvePageUrl(
        "/waivers/{id}?type={type}&sidebarReference={sidebarReference}",
        violationId,
        type,
        sidebarReference
    );
  }

  public ListWaiversPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton("#menu-bar__back-button-container");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement waiverDetailsTitle() {
    return child("#list-waivers-details .nx-tile-header__title h2");
  }

  public SelenideElement waiverListTitle() {
    return child("#list-waivers-applicable .nx-tile-header__title h2");
  }

  public SelenideElement addWaiverButton() {
    return child("#add-waiver-btn");
  }

  public SelenideElement policyName() {
    return child(".iq-threat-level");
  }

  public SelenideElement constraintName() {
    return child("#list-waivers-constraint-name");
  }

  public ElementsCollection conditions() {
    return children(".list-waivers-condition");
  }

  public SelenideElement componentName() {
    return child("#list-waivers-component-name");
  }

  public WaiverListTable waiverListTable() {
    return new WaiverListTable();
  }

  public RequestWaiversPopover requestWaiversPopover() {
    return new RequestWaiversPopover();
  }

  public SelenideElement requestWaiverButton() {
    return child("#request-waiver-btn");
  }

  public class RequestWaiversPopover
      extends BasicElement<RequestWaiversPopover>
  {
    private static final String ROOT_SELECTOR = "#request-waivers";

    public SelenideElement root() {
      return $(ROOT_SELECTOR);
    }

    public SelenideElement requestWaiverHeader() {
      return child(".iq-popover-header__title-text");
    }

    public SelenideElement requestWaiverReadOnlyData() {
      return child(".nx-read-only");
    }

    public SelenideElement requestWaiverPolicyViolationId() {
      return child("#request-waivers-policy-violation-id");
    }

    public SelenideElement requestWaiverCancelButton() {
      return child("#request-waivers-close-button");
    }
  }

  public class WaiverListTable
      extends BasicElement<WaiverListTable>
  {
    static final String ROW_SELECTOR = "tbody .nx-table-row";

    WaiverListTable() {
      super(ROOT, "#list-waivers-page-waiver-table");
    }

    public WaiverListRow headerRow() {
      return new WaiverListRow(childSelector("thead .nx-table-row"));
    }

    public ElementsCollection rows() {
      return children(ROW_SELECTOR);
    }

    public WaiverListRow row(int i) {
      return new WaiverListRow(childSelector(ROW_SELECTOR, nthChild(i)));
    }

    public SelenideElement noWaiversMessage() {
      return child("tbody tr td.nx-cell--meta-info");
    }
  }

  public class WaiverListRow
      extends BasicElement<WaiverListRow>
  {
    public WaiverListRow(String selector) {
      super(selector);
    }

    public SelenideElement dateCreated() {
      return child(".nx-cell", nthChild(1));
    }

    public SelenideElement scope() {
      return child(".nx-cell", nthChild(2));
    }

    public SelenideElement components() {
      return child(".nx-cell", nthChild(3));
    }

    public SelenideElement waiverExpiration() {
      return child(".nx-cell", nthChild(4));
    }

    public SelenideElement createdBy() {
      return child(".nx-cell", nthChild(5));
    }

    public SelenideElement comments() {
      return child(".nx-cell", nthChild(6));
    }

    public SelenideElement deleteButton() {
      return child(".list-waivers-row__delete-btn");
    }
  }
}
