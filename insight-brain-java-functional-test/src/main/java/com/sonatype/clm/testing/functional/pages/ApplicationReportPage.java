/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.elements.IqCheckbox;
import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.elements.IqSortingHeader;
import com.sonatype.clm.testing.functional.elements.IqTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.elements.PolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ApplicationReportPage
    extends BasicElement<ApplicationReportPage>
{
  public static final String ROOT = "#application-report";

  private final String ROW_SELECTOR = ".iq-table--application-report tbody .iq-table-row";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/policy", app.getPublicId(), scanId);
  }

  public ApplicationReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return child(".iq-tile--application-report .iq-tile-header__title");
  }

  public SelenideElement reportDate() {
    return child(".iq-tile--application-report .iq-tile-header__subtitle");
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

  public CipModal cipModal() {
    return new CipModal("#cip-modal");
  }

  public IqRadio showAggregatedViolationsRadio() {
    return new IqRadio(child("#aggregate-by-component-radio"));
  }

  public IqRadio showAllViolationsRadio() {
    return new IqRadio(child("#no-aggregation-radio"));
  }

  public ProprietaryFilter proprietaryFilter() {
    return new ProprietaryFilter("#proprietary-filter");
  }

  public MatchStateFilter matchStateFilter() {
    return new MatchStateFilter("#match-state-filter");
  }

  public ViolationStateFilter violationStateFilter() {
    return new ViolationStateFilter("#violation-state-filter");
  }

  public PolicyTypeFilter policyTypeFilter() {
    return new PolicyTypeFilter("#policy-type-filter");
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

    public CipOccurrencesTab getOccurrencesTab() {
      return new CipOccurrencesTab(".tab-content > cip-occurrences");
    }

    public CipSimilarTab getSimilarTab() {
      return new CipSimilarTab(".tab-content > cip-similar");
    }

    public CipAuditTab getAuditTab() {
      return new CipAuditTab(".tab-content > cip-audit");
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

  public static class ProprietaryFilter
      extends IqTreeViewMultiSelect
  {
    public ProprietaryFilter(String selector) {
      super(selector);
    }

    public IqCheckbox nonProprietary() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(2)));
    }

    public IqCheckbox proprietary() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(3)));
    }
  }

  public static class MatchStateFilter
      extends IqTreeViewMultiSelect
  {
    public MatchStateFilter(String selector) {
      super(selector);
    }

    public IqCheckbox exact() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(2)));
    }

    public IqCheckbox similar() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(3)));
    }

    public IqCheckbox unknown() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(4)));
    }
  }

  public static class ViolationStateFilter
      extends IqTreeViewMultiSelect
  {
    public ViolationStateFilter(String selector) {
      super(selector);
    }

    public IqCheckbox notViolating() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(2)));
    }

    public IqCheckbox open() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(3)));
    }

    public IqCheckbox waived() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(4)));
    }

    public IqCheckbox grandfathered() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(5)));
    }
  }

  public static class PolicyTypeFilter
      extends IqTreeViewMultiSelect
  {
    public PolicyTypeFilter(String selector) {
      super(selector);
    }

    public IqCheckbox security() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(2)));
    }

    public IqCheckbox license() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(3)));
    }

    public IqCheckbox quality() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(4)));
    }

    public IqCheckbox other() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(5)));
    }
  }

  public static PolicyThreatLevelFilter policyThreatLevelFilter() {
    return new PolicyThreatLevelFilter("#threat-level-filter");
  }
}
