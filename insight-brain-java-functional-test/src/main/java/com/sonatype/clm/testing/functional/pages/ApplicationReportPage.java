/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ApplicationReportPage
    extends BasicElement<ApplicationReportPage>
{
  public static final String ROOT = "#application-report";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}", app.getPublicId(), scanId);
  }

  public ApplicationReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return $(".iq-tile-header__title");
  }

  public SelenideElement reportDate() {
    return $(".iq-tile-header__subtitle");
  }

  public IQDropdown optionsDropdown() {
    return new IQDropdown("#options-dropdown");
  }

  public IQThreatIndicators threatIndicators() {
    return new IQThreatIndicators(childSelector(".iq-threat-indicators"));
  }

  public IQCoverageIndicator coverageIndicator() {
    return new IQCoverageIndicator(childSelector(".iq-coverage-indicator"));
  }

  public ElementsCollection resultRows() {
    return children(".iq-table--application-report tbody .iq-table-row");
  }

  public ResultRow resultRow(int i) {
    return new ResultRow(childSelector(".iq-table--application-report tbody .iq-table-row", nthChild(i)));
  }

  public static class ResultRow
      extends BasicElement<ResultRow>
  {
    public ResultRow(String childSelector) {
      super(childSelector);
    }

    public SelenideElement threatBar() {
      return child(".iq-threat-indication");
    }

    public SelenideElement threatNumber() {
      return child(".iq-threat-number");
    }

    public SelenideElement policyName() {
      return child(".iq-cell--application-report-policy-name");
    }

    public SelenideElement componentName() {
      return child(".iq-cell--application-report-component-display");
    }

    public SelenideElement waivedIndicator() {
      return child(".iq-text-indicator--waived");
    }
  }

  public static class IQThreatIndicators
  extends BasicElement<IQThreatIndicators>
  {
    public IQThreatIndicators(String selector) {
      super(selector);
    }

    public SelenideElement critical() {
      return child(".iq-threat-indicator.critical");
    }

    public SelenideElement severe() {
      return child(".iq-threat-indicator.severe");
    }

    public SelenideElement moderate() {
      return child(".iq-threat-indicator.moderate");
    }

    public SelenideElement caption() {
      return child(".iq-caption__text");
    }

    public SelenideElement subCaption() {
      return child(".iq-caption__sub-text");
    }
  }

  public static class IQCoverageIndicator
      extends BasicElement<IQCoverageIndicator>
  {
    public IQCoverageIndicator(String selector) {
      super(selector);
    }

    public SelenideElement caption() {
      return child(".iq-caption__text");
    }

    public SelenideElement subCaption() {
      return child(".iq-caption__sub-text");
    }

    public SelenideElement donutChart() {
      return child("span[coverage-donut] svg");
    }
  }
}
