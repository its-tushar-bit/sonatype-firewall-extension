/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxDropdown;
import com.sonatype.clm.testing.functional.elements.NxSortingHeader;
import com.sonatype.clm.testing.functional.elements.NxToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ElementUtils;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ApplicationReportPage
    extends BasicElement<ApplicationReportPage>
{
  public static final String ROOT = "#app-report";

  public static final WebElementCondition DIRECT_DEPENDENCY_CLASS = cssClass("direct");

  public static final WebElementCondition TRANSITIVE_DEPENDENCY_CLASS = cssClass("transitive");

  public static final WebElementCondition INNER_SOURCE_DEPENDENCY_CLASS = cssClass("inner-source");

  private static final String DEPENDENCY_INDICATOR_SELECTOR = ".iq-dependency-indicator";

  private static final String TRANSITIVE_VIOLATIONS_COUNT_SELECTOR =
      ".iq-transitive-violations-count";

  private static final String ROW_SELECTOR = ".nx-table tbody .nx-table-row";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/policy", app.getPublicId(),
        scanId);
  }

  public static String firewallContainerReportUrl(String applicationPublicId, String scanId) {
    return BaseUrl.resolvePageUrl(
        "/firewall/containerReport/{publicId}/{scanId}/policy",
        applicationPublicId,
        scanId);
  }

  public ApplicationReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return child(".nx-page-title .nx-h1");
  }

  public SelenideElement reportApplicationRiskScore() {
    return child(".iq-application-risk-score--risk");
  }

  public SelenideElement reportApplicationRiskScoreDescription() {
    return child(".iq-application-risk-score--desc-title");
  }

  public SelenideElement reportAddContainerImageWaiverButton() {
    return child("#add-container-image-waiver-button");
  }

  public SelenideElement reportDescription() {
    return child(".nx-page-title__description");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public SelenideElement reevaluateButton() {
    return child("#reevaluate-report-button");
  }

  public SelenideElement fullReevaluateButton() {
    return child("#full-reevaluate-report-button");
  }

  public SelenideElement quickReevaluateButton() {
    return child("#quick-reevaluate-report-button");
  }

  public void seeReevaluationStatusModalAndWaitForDismissal() {
    SelenideElement modal = child("#iq-reevaluation-status-modal");
    ElementUtils.seeElementAndWaitForDismissal(modal);
  }

  public NxDropdown optionsDropdown() {
    return new NxDropdown("#iq-report-options-dropdown");
  }

  public IQThreatIndicators threatIndicators() {
    return new IQThreatIndicators(childSelector(".iq-threat-indicators"));
  }

  public IQCoverageIndicator coverageIndicator() {
    return new IQCoverageIndicator(childSelector(".iq-coverage-indicator"));
  }

  public IQLegacyViolationsIndicator legacyViolationsIndicator() {
    return new IQLegacyViolationsIndicator(childSelector(".iq-legacy-violations-indicator"));
  }

  public SelenideElement filterToggle() {
    return child("#filters-toggle-button");
  }

  public SelenideElement goToDependencyTreeButton() {
    return child("#dependency-tree-button");
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

  public SelenideElement getColFromResultRow(int row, int col) {
    return child(ROW_SELECTOR + ":nth-child(" + row + ") .nx-cell:nth-child(" + col + ")");
  }

  public ResultRow lastResultRow() {
    return new ResultRow(childSelector(ROW_SELECTOR + ":last-child"));
  }

  public ElementsCollection getThreatBars(String threatLevel) {
    return children(ROW_SELECTOR, ".nx-threat-indicator--" + threatLevel);
  }

  public ElementsCollection rowsWithDependencyInfo() {
    return children(ROW_SELECTOR).filter(new WebElementCondition("hasDependencyIndicator")
    {
      @Override
      public CheckResult check(Driver driver, WebElement webElement) {
        return new CheckResult(
            !webElement.findElements(By.cssSelector(DEPENDENCY_INDICATOR_SELECTOR)).isEmpty(),
            webElement);
      }
    });
  }

  public CipModal cipModal() {
    return new CipModal("#cip-modal");
  }

  public NxToggle aggregateByComponentToggle() {
    return new NxToggle("#report-aggregate-by-component-toggle");
  }

  public AppReportHeaders headers() {
    return new AppReportHeaders();
  }

  public SelenideElement viewUnscannedComponentsButton() {
    return child("#application-report-unscannable-components-error .nx-btn--error");
  }

  public SelenideElement unscannedComponentsModal() {
    return child("#unscanned-components-modal");
  }

  public SelenideElement closeUnscannedComponentsModalButton() {
    return child("#unscanned-components-modal .nx-btn--secondary");
  }

  public static class ResultRow
      extends BasicElement<ResultRow>
  {
    public ResultRow(String selector) {
      super(selector);
    }

    public SelenideElement threatBar() {
      return child(".nx-threat-indicator");
    }

    public SelenideElement threatNumber() {
      return child(".iq-app-report__threat-cell");
    }

    public SelenideElement policyName() {
      return child(".iq-app-report__policy-name-cell");
    }

    public SelenideElement componentName() {
      return child(".iq-app-report__component-name-cell");
    }

    public SelenideElement legacyViolationIndicator() {
      return child(".iq-text-indicator--legacy-violation");
    }

    public SelenideElement waivedIndicator() {
      return child(".iq-text-indicator--waived");
    }

    public SelenideElement waiverIndicator() {
      return child(".iq-waiver-indicator");
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
      return child(".nx-small-threat-counter--critical");
    }

    public SelenideElement severe() {
      return child(".nx-small-threat-counter--severe");
    }

    public SelenideElement moderate() {
      return child(".nx-small-threat-counter--moderate");
    }

    public SelenideElement critical_old() {
      return child(".iq-threat-indicator.critical");
    }

    public SelenideElement severe_old() {
      return child(".iq-threat-indicator.severe");
    }

    public SelenideElement moderate_old() {
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
      return child("svg");
    }
  }

  public static class IQLegacyViolationsIndicator
      extends BasicElement<IQLegacyViolationsIndicator>
  {
    public IQLegacyViolationsIndicator(String selector) {
      super(selector);
    }

    public SelenideElement caption() {
      return child(".iq-caption__text");
    }
  }

  public static class CipModal
      extends BasicElement<CipModal>
  {
    public static final WebElementCondition ACTIVE_CLASS = cssClass("active");

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

    public NxSortingHeader dateHeader() {
      return new NxSortingHeader(selector + " thead > tr > th:nth-child(1)");
    }

    public NxSortingHeader userHeader() {
      return new NxSortingHeader(selector + " thead > tr > th:nth-child(2)");
    }

    public NxSortingHeader actionHeader() {
      return new NxSortingHeader(selector + " thead > tr > th:nth-child(3)");
    }

    public NxSortingHeader detailHeader() {
      return new NxSortingHeader(selector + " thead > tr > th:nth-child(4)");
    }

    public NxSortingHeader commentHeader() {
      return new NxSortingHeader(selector + " thead > tr > th:nth-child(5)");
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
      super(ROOT, ".nx-table thead");
    }

    public NxSortingHeader threatHeader() {
      return new NxSortingHeader(childSelector(".iq-app-report__threat-cell"));
    }

    public NxSortingHeader policyNameHeader() {
      return new NxSortingHeader(childSelector(".iq-app-report__policy-name-cell"));
    }

    public NxSortingHeader componentNameHeader() {
      return new NxSortingHeader(childSelector(".iq-app-report__component-name-cell"));
    }

    public SelenideElement policyNameFilterInput() {
      return child("#report-policy-name-filter");
    }

    public SelenideElement componentNameFilterInput() {
      return child("#report-component-name-filter");
    }
  }

  public SelenideElement policyTypeFilterWarning() {
    return child("#application-report-policy-type-filter-warning");
  }

  public SelenideElement oldReportWithNoDependencyInfoWarning() {
    return child("#application-report-no-dependency-info-warning");
  }
}
