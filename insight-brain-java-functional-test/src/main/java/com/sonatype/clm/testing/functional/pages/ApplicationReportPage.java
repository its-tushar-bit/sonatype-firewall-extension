/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IqSortingHeader;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
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

  public CipModal cipModal() {
    return new CipModal("#cip-modal");
  }

  public IqRadio showAggregatedViolationsRadio() {
    return new IqRadio(child("#aggregate-by-component-radio"));
  }

  public IqRadio showAllViolationsRadio() {
    return new IqRadio(child("#no-aggregation-radio"));
  }

  public AppReportHeaders headers() {
    return new AppReportHeaders();
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

  public static class CipModal
      extends BasicElement<CipModal>
  {
    public static final Condition ACTIVE_CLASS = cssClass("active");

    CipModal(String selector) {
      super(selector);
    }

    public SelenideElement header() {
      return child("#cip-modal-header");
    }

    public SelenideElement tabLink(int i) {
      return child(".nav-tabs li", nthChild(i));
    }

    public SelenideElement previousButton() {
      return child("#cip-modal-previous-button");
    }

    public SelenideElement nextButton() {
      return child("#cip-modal-next-button");
    }

    public SelenideElement closeButton() {
      return child("#cip-modal-close-button");
    }
  }

  public static class AppReportHeaders
      extends BasicElement<AppReportHeaders>
  {
    public AppReportHeaders() {
      super(ROOT, ".iq-table--application-report thead");
    }

    public IqSortingHeader threatHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--application-report-policy-threat-level a"));
    }

    public IqSortingHeader policyNameHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--application-report-policy-name a"));
    }

    public IqSortingHeader componentNameHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--application-report-component-display a"));
    }

    public SelenideElement policyNameFilterInput() {
      return child(".iq-cell--application-report-policy-name-filter input");
    }

    public SelenideElement componentNameFilterInput() {
      return child(".iq-cell--application-report-component-name-filter input");
    }
  }
}
