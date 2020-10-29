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

  public ListWaiversPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton(ROOT);
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement waiverDetailsTitle() {
    return child(".nx-tile-header--hrule h2");
  }

  public SelenideElement waiverListTitle() {
    return child(".nx-tile-header__title h2");
  }

  public SelenideElement addWaiverButton() {
    return child(".nx-btn--tertiary");
  }

  public SelenideElement policyName() {
    return child(".list-waivers--threat-indicator .iq-threat-level");
  }

  public SelenideElement constraintName() {
    return child(".list-waivers--constraint div");
  }

  public ElementsCollection conditions() {
    return children(".list-waivers--conditions span");
  }

  public SelenideElement condition(int index) {
    return child(".list-waivers--conditions span", nthChild(index));
  }

  public SelenideElement componentName() {
    return child(".list-waivers--component-name div");
  }

  public WaiverListTable waiverListTable() {
    return new WaiverListTable();
  }

  public DeleteWaiverModal deleteWaiverModal() {
    return new DeleteWaiverModal();
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
      return child("tbody tr td.nx-cell--empty");
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

    public SelenideElement comments() {
      return child(".nx-cell", nthChild(5));
    }

    public SelenideElement deleteButton() {
      return child(".list-waivers-row__delete-btn");
    }
  }

  public class DeleteWaiverModal
      extends BasicElement<DeleteWaiverModal>
  {
    private static final String ROOT_SELECTOR = "#delete-waiver-modal";

    public SelenideElement root() {
      return $(ROOT_SELECTOR);
    }

    public SelenideElement header() {
      return child(".nx-modal-header");
    }

    public SelenideElement message() {
      return child(".nx-modal-content");
    }

    public SelenideElement cancelButton() {
      return child("#delete-waiver-modal-cancel-button");
    }

    public SelenideElement yesButton() {
      return child("#delete-waiver-modal-continue-button");
    }
  }
}
