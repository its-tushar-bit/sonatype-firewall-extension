/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.elements.IqSortingHeader;
import com.sonatype.clm.testing.functional.elements.IqToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ApplicationReportPage
    extends BasicElement<ApplicationReportPage>
{
  public static final String ROOT = "application-report";

  public static final Condition DIRECT_DEPENDENCY_CLASS = cssClass("direct");

  public static final Condition TRANSITIVE_DEPENDENCY_CLASS = cssClass("transitive");

  public static final Condition INNER_SOURCE_DEPENDENCY_CLASS = cssClass("inner-source");

  private static final String DEPENDENCY_INDICATOR_SELECTOR = ".iq-dependency-indicator";

  private static final String TRANSITIVE_VIOLATIONS_COUNT_SELECTOR =
      ".iq-transitive-violations-count";

  private static final String ROW_SELECTOR = ".iq-table--application-report tbody .iq-table-row";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/policy", app.getPublicId(),
        scanId);
  }
  
  public ApplicationReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return child(".nx-page-title .nx-h1");
  }

  public SelenideElement scanTriggerType() {
    return child("#application-report-scan-trigger-type");
  }

  public SelenideElement forContinuousMonitoring() {
    return child("#application-report-for-monitoring");
  }

  public SelenideElement reevaluation() {
    return child("#application-report-reevaluation");
  }

  public SelenideElement reportDate() {
    return child("#application-report-time");
  }

  public SelenideElement commitHash() {
    return child("#application-report-commit");
  }

  public IqBackButton backButton() {
    return new IqBackButton(ROOT);
  }

  public SelenideElement reevaluateButton() {
    return child("#reevaluate-button");
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

  public IQGrandfatheringIndicator grandfatheringIndicator() {
    return new IQGrandfatheringIndicator(childSelector(".iq-grandfathering-indicator"));
  }

  public SelenideElement filterToggle() {
    return child("#filter-toggle");
  }

  public SelenideElement goToDependencyTreeButton() {
    return child("#go-to-dependency-tree");
  }

  public ApplicationReportFilter filterPanel() {
    return new ApplicationReportFilter();
  }

  public ElementsCollection resultRows() {
    return children(ROW_SELECTOR);
  }

  public ResultRow resultRow(int i) {
    return new ResultRow(childSelector(ROW_SELECTOR, nthChild(i)));
  }

  public ResultRow lastResultRow() {
    return new ResultRow(childSelector(ROW_SELECTOR + ":last-child"));
  }

  public ElementsCollection getThreatBars(String threatLevel) {
    return children(ROW_SELECTOR, ".iq-threat-indication." + threatLevel);
  }

  public ElementsCollection rowsWithDependencyInfo() {
    return children(ROW_SELECTOR).filter(new Condition("hasDependencyIndicator") {
      @Override
      public boolean apply(WebElement webElement) {
        return !webElement.findElements(By.cssSelector(DEPENDENCY_INDICATOR_SELECTOR)).isEmpty();
      }
    });
  }

  public CipModal cipModal() {
    return new CipModal("#cip-modal");
  }

  public IqToggle aggregateByComponentToggle() {
    return new IqToggle(child("iq-toggle"));
  }

  public AppReportHeaders headers() {
    return new AppReportHeaders();
  }

  public static class ResultRow
      extends BasicElement<ResultRow>
  {
    public ResultRow(String selector) {
      super(selector);
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

    public SelenideElement grandfatheredIndicator() {
      return child(".iq-text-indicator--grandfathered");
    }

    public SelenideElement waivedIndicator() {
      return child(".iq-text-indicator--waived");
    }

    public ElementsCollection dependencyIndicators() {
      return children(DEPENDENCY_INDICATOR_SELECTOR);
    }

    public ElementsCollection transitiveViolationsCount() {
      return children(TRANSITIVE_VIOLATIONS_COUNT_SELECTOR);
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

  public static class IQGrandfatheringIndicator
      extends BasicElement<IQGrandfatheringIndicator>
  {
    public IQGrandfatheringIndicator(String selector) {
      super(selector);
    }

    public SelenideElement caption() {
      return child(".iq-caption__text");
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

    public SelenideElement dependencyIndicator() {
      return header().$(".iq-dependency-indicator--long");
    }

    public SelenideElement dependencyInnerSourceIndicator() {
      return header().$(".iq-modal-header__inner-source-info");
    }

    public SelenideElement tabLink(int i) {
      return child(".iq-tab-bar a", nthChild(i));
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

    public SelenideElement backButton() {
      return child("#cip-modal-back-button");
    }

    public CipOccurrencesTab getOccurrencesTab() {
      return new CipOccurrencesTab(".tab-content > cip-occurrences");
    }

    public CipSimilarTab getSimilarTab() {
      return new CipSimilarTab(".tab-content > cip-similar");
    }

    public CipAuditTab getAuditTab() {
      return new CipAuditTab(".tab-content > cip-audit");
    }

    public SelenideElement ownerApplication() {
      return child("h3");
    }

    public SelenideElement latestReportLink() {
      return child("a");
    }

    public SelenideElement innerSourceAlertInfo() {
      return child(".iq-alert--info");
    }

    public InnerSourceProducerReportModal innerSourceProducerReportModal() {
      return new InnerSourceProducerReportModal();
    }

    public InnerSourceProducerPermissionsModal innerSourceProducerPermissionsModal() {
      return new InnerSourceProducerPermissionsModal();
    }
  }

  public static class CipOccurrencesTab
      extends BasicElement<CipOccurrencesTab>
  {
    CipOccurrencesTab(String selector) {
      super(selector);
    }

    public ElementsCollection occurrences() {
      return children("li");
    }
  }

  public static class CipSimilarTab
      extends BasicElement<CipSimilarTab>
  {
    CipSimilarTab(String selector) {
      super(selector);
    }

    public SelenideElement emptyMessage() {
      return child("#cip-similar-empty-message");
    }

    public SelenideElement mostSimilarComponent() {
      return child("#cip-most-similar-component");
    }

    public ElementsCollection otherSimilarComponents() {
      return children("#cip-other-similar-components > li");
    }
  }

  public static class CipAuditTab
      extends BasicElement<CipAuditTab>
  {
    CipAuditTab(String selector) {
      super(selector);
    }

    public ElementsCollection rowWithoutDate(int index) {
      return children(getRowSelector(index) + ":not(:first-child)");
    }

    public SelenideElement dateFromRow(int rowIndex) {
      return child(getRowSelector(rowIndex) + ":first-child");
    }

    public SelenideElement emptyMessage() {
      return child("p");
    }

    public SelenideElement table() {
      return child("table");
    }

    private String getRowSelector(int index) {
      return "tbody > tr:nth-child(" + (index + 1) + ") td";
    }

    public IqSortingHeader dateHeader() {
      return new IqSortingHeader(selector + " thead > tr > th:nth-child(1)");
    }

    public IqSortingHeader userHeader() {
      return new IqSortingHeader(selector + " thead > tr > th:nth-child(2)");
    }

    public IqSortingHeader actionHeader() {
      return new IqSortingHeader(selector + " thead > tr > th:nth-child(3)");
    }

    public IqSortingHeader detailHeader() {
      return new IqSortingHeader(selector + " thead > tr > th:nth-child(4)");
    }

    public IqSortingHeader commentHeader() {
      return new IqSortingHeader(selector + " thead > tr > th:nth-child(5)");
    }
  }

  public static class InnerSourceProducerReportModal
      extends BasicElement<InnerSourceProducerReportModal>
  {
    public static final String ROOT = "#innersource-producer-report-modal";

    public InnerSourceProducerReportModal() {
      super(ROOT);
    }

    public SelenideElement header() {
      return child(".nx-modal-header .nx-h2");
    }

    public SelenideElement content() {
      return child(".nx-modal-content");
    }

    public SelenideElement cancelButton() {
      return child("#innersource-producer-report-modal-cancel");
    }

    public SelenideElement continueToReportButton() {
      return child("#innersource-producer-report-modal-continue-to-report");
    }
  }

  public static class InnerSourceProducerPermissionsModal
      extends BasicElement<InnerSourceProducerPermissionsModal>
  {
    public static final String ROOT = "#innersource-producer-insufficient-permissions-modal";

    public InnerSourceProducerPermissionsModal() {
      super(ROOT);
    }

    public SelenideElement header() {
      return child(".nx-modal-header .nx-h2");
    }

    public SelenideElement content() {
      return child(".nx-modal-content");
    }

    public SelenideElement closeButton() {
      return child("#innersource-producer-insufficient-permissions-modal-close");
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
      return new IqSortingHeader(childSelector(" .iq-cell--application-report-component-display a"));
    }

    public SelenideElement policyNameFilterInput() {
      return child(".iq-cell--application-report-policy-name-filter input");
    }

    public SelenideElement componentNameFilterInput() {
      return child(".iq-cell--application-report-component-name-filter input");
    }
  }

  public SelenideElement policyTypeFilterWarning() {
    return child("#application-report-policy-type-filter-warning");
  }
}
