/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxSortingHeader;
import com.sonatype.clm.testing.functional.elements.IqVulnerabilityModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
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

  public NxBackButton backButton() {
    return new NxBackButton("#menu-bar__back-button-container");
  }

  public SelenideElement reportTitle() {
    return child("#raw-data-report-title");
  }

  public SelenideElement description() {
    return child(".nx-page-title__description");
  }

  public ResultTable resultTable() {
    return new ResultTable();
  }

  public SelenideElement noResultsRow() {
    return child(".nx-cell--meta-info");
  }

  public IqVulnerabilityModal vulnerabilityModal() {
    return new IqVulnerabilityModal("#vulnerability-details-modal");
  }

  public AppReportRawDataHeaders headers() {
    return new AppReportRawDataHeaders();
  }

  public static class ResultTable
      extends BasicElement<ResultTable>
  {
    static final String ROW_SELECTOR = "tbody .nx-table-row";

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
      return child(".nx-cell", nthChild(1));
    }

    public SelenideElement license() {
      return child(".nx-cell", nthChild(2));
    }

    public SelenideElement securityIssue() {
      return child(".nx-cell", nthChild(3));
    }

    public SelenideElement securityIssueLink() {
      return child(".nx-cell:nth-child(3) a");
    }

    public SelenideElement cvssScore() {
      return child(".nx-cell", nthChild(4));
    }

    public SelenideElement declaredLicenses() {
      return child(".raw-license-tooltip strong");
    }

    public SelenideElement observedLicenses() {
      return child(".raw-license-tooltip span");
    }
  }

  public static class AppReportRawDataHeaders
      extends BasicElement<AppReportRawDataHeaders>
  {
    private static final String HEADER_CLASS_NAME = ".nx-cell--header";

    public AppReportRawDataHeaders() {
      super(ROOT, "#raw-data-report-results thead");
    }

    public NxSortingHeader componentHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(1))));
    }

    public NxSortingHeader licensesHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(2))));
    }

    public NxSortingHeader securityIssueHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(3))));
    }

    public NxSortingHeader cvssScoreHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(4))));
    }

    public SelenideElement componentFilterInput() {
      return child("#raw-data-component-filter");
    }

    public SelenideElement licenseFilterInput() {
      return child("#raw-data-license-filter");
    }

    public SelenideElement securityCodeFilterInput() {
      return child("#raw-data-security-filter");
    }

    public SelenideElement cvssMinFilterInput() {
      return child("#raw-data-cvss-min-filter");
    }

    public SelenideElement cvssMaxFilterInput() {
      return child("#raw-data-cvss-max-filter");
    }

    public void clearFilterField(SelenideElement element) {
      while (!element.getAttribute("value").equals("")) {
        element.sendKeys(Keys.BACK_SPACE);
      }
    }
  }
}
