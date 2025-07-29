/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ApplicationReportVulnerabilitiesPage
    extends BasicElement<ApplicationReportVulnerabilitiesPage>
{
  public static final String ROOT = "#application-report-vulnerabilities";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/vulnerabilities",
        app.getPublicId(), scanId);
  }

  public ApplicationReportVulnerabilitiesPage() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child("#application-report-vulnerabilities-title");
  }

  public SelenideElement subtitle() {
    return child(".nx-tile-header__subtitle");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public VulnerabilityTable table() {
    return new VulnerabilityTable();
  }

  public static class VulnerabilityTable
      extends BasicElement<VulnerabilityTable>
  {
    static final String ROW_SELECTOR = "tbody .nx-table-row";

    VulnerabilityTable() {
      super(ROOT, "#application-report-vulnerabilities-table");
    }

    public ElementsCollection rows() {
      return children(ROW_SELECTOR);
    }

    public VulnerabilityRow row(int i) {
      return new VulnerabilityRow(childSelector(ROW_SELECTOR, nthChild(i)));
    }
  }

  public static class VulnerabilityRow
      extends BasicElement<VulnerabilityRow>
  {
    public VulnerabilityRow(String selector) {
      super(selector);
    }

    public SelenideElement component() {
      return child(".nx-cell", nthChild(4));
    }

    public SelenideElement securityIssue() {
      return child(".nx-cell", nthChild(2));
    }

    public SelenideElement detailsLink() {
      return child(".nx-cell", nthChild(2), "a");
    }

    public SelenideElement cvssScore() {
      return child(".nx-cell", nthChild(3));
    }

    public SelenideElement policyThreatLevel() {
      return child(".nx-cell", nthChild(1));
    }

    public SelenideElement waived() {
      return component().$(".iq-text-indicator--waived");
    }

    public SelenideElement legacyViolationGranted() {
      return component().$(".iq-text-indicator--legacy-violation");
    }
  }
}
