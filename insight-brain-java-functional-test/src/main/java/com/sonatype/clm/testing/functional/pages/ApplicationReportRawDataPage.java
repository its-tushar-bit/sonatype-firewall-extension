/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.elements.IqSortingHeader;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ApplicationReportRawDataPage
    extends BasicElement<ApplicationReportRawDataPage>
{
  public static final String ROOT = "#application-report-raw-data";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/raw", app.getPublicId(), scanId);
  }

  public ApplicationReportRawDataPage() {
    super(ROOT);
  }

  public IqBackButton backButton() {
    return new IqBackButton(ROOT);
  }

  public SelenideElement reportTitle() {
    return child("#raw-data-report-title");
  }

  public SelenideElement reportSubtitle() {
    return child(".iq-tile-header__subtitle");
  }

  public ResultTable resultTable() {
    return new ResultTable();
  }

  public SelenideElement noResultsRow() {
    return child(".iq-cell--empty");
  }

  public VulnerabilityModal vulnerabilityModal() {
    return new VulnerabilityModal();
  }

  public AppReportRawDataHeaders headers() {
    return new AppReportRawDataHeaders();
  }

  public static class VulnerabilityModal
      extends BasicElement<VulnerabilityModal>
  {
    VulnerabilityModal() {
      super("#vulnerability-details-modal");
    }

    public SelenideElement header() {
      return child(".nx-modal-header");
    }

    public SelenideElement content() {
      return child(".nx-modal-content");
    }

    public SelenideElement closeButton() {
      return child(".nx-modal-footer .nx-btn-bar .nx-btn");
    }
  }

  public static class ResultTable
      extends BasicElement<ResultTable>
  {
    static final String ROW_SELECTOR = "tbody .iq-table-row";

    ResultTable() {
      super(ROOT, "#raw-data-report-results");
    }

    public ElementsCollection resultRows() {
      return children(ROW_SELECTOR);
    }

    public ResultRow resultRow(int i) {
      return new ResultRow(childSelector(ROW_SELECTOR, nthChild(i)));
    }
  }

  public static class ResultRow
      extends BasicElement<ApplicationReportPage.ResultRow>
  {
    public ResultRow(String selector) {
      super(selector);
    }

    public SelenideElement component() {
      return child(".iq-cell", nthChild(1));
    }

    public SelenideElement securityIssue() {
      return child(".iq-cell", nthChild(3));
    }

    public SelenideElement cvssScore() {
      return child(".iq-cell", nthChild(4));
    }

    public SelenideElement declaredLicenses() {
      return child("raw-license-display strong");
    }

    public SelenideElement observedLicenses() {
      return child("raw-license-display span");
    }
  }

  public static class AppReportRawDataHeaders
      extends BasicElement<AppReportRawDataHeaders>
  {
    public AppReportRawDataHeaders() {
      super(ROOT, "#raw-data-report-results thead");
    }

    public IqSortingHeader componentHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--report-raw-data-component a"));
    }

    public IqSortingHeader licensesHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--report-raw-data-license a"));
    }

    public IqSortingHeader securityIssueHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--report-raw-data-security-code a"));
    }

    public IqSortingHeader cvssScoreHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--report-raw-data-cvss a"));
    }

    public SelenideElement componentFilterInput() {
      return child(".iq-cell--report-raw-data-component input");
    }

    public SelenideElement licenseFilterInput() {
      return child(".iq-cell--report-raw-data-license input");
    }

    public SelenideElement securityCodeFilterInput() {
      return child(".iq-cell--report-raw-data-security-code input");
    }

    public SelenideElement cvssMinFilterInput() {
      return child("#raw-data-cvss-min-filter");
    }

    public SelenideElement cvssMaxFilterInput() {
      return child("#raw-data-cvss-max-filter");
    }
  }
}
