/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxTableHeader;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class FirewallPageComponents
{
  public static final String TEXT_LINK = ".nx-text-link";

  public static class FirewallStatus
      extends BasicElement<FirewallStatus>
  {
    public FirewallStatus(String rootSelector) {
      super(rootSelector, "#firewall-status");
    }

    public SelenideElement title() {
      return child(".nx-h1");
    }

    public SelenideElement statusPartiallyProtected() {
      return child(".iq-firewall-status__status-indicator.nx-status-indicator--intermediate");
    }

    public SelenideElement statusFullyProtected() {
      return child(".iq-firewall-status__status-indicator.nx-status-indicator--positive");
    }

    public SelenideElement componentsMonitored() {
      return child(".iq-firewall-status__components-monitored");
    }
  }

  public static class FirewallMetrics
      extends BasicElement<FirewallMetrics>
  {
    public FirewallMetrics(String rootSelector) {
      super(rootSelector, "#firewall-metrics");
    }
  }

  public static class FirewallMetricsContent
      extends BasicElement<FirewallMetricsContent>
  {
    public FirewallMetricsContent(String rootSelector, String id) {
      super(rootSelector, id);
    }

    public SelenideElement value() {
      return child(".iq-firewall-metrics-content__values");
    }

    public SelenideElement link() {
      return child(TEXT_LINK);
    }
  }

  public static class FirewallAutoUnquarantineStatus
      extends BasicElement<FirewallAutoUnquarantineStatus>
  {
    public FirewallAutoUnquarantineStatus(String rootSelector) {
      super(rootSelector, "#firewall-auto-unquarantine-status");
    }

    public SelenideElement header() {
      return child(FirewallPage.CHILD_HEADER_CSS_CLASS);
    }

    public SelenideElement statusIndicatorIcon() {
      return child(".iq-status-indicator-icon");
    }

    public SelenideElement statusIndicatorIconActive() {
      return child(".iq-status-indicator-icon--active");
    }

    public SelenideElement statusLabel() {
      return child(".iq-status-indicator span");
    }

    public SelenideElement statusDescription() {
      return child(".nx-card__text");
    }

    public SelenideElement configureLink() {
      return child(TEXT_LINK);
    }

    public Button retryButton() {
      return new Button(childSelector(".nx-load-error__retry"));
    }
  }

  public static class FirewallAutoUnquarantine
      extends BasicElement<FirewallAutoUnquarantine>
  {
    public FirewallAutoUnquarantine(String rootSelector) {
      super(rootSelector, "#firewall-auto-release-quarantine");
    }

    public SelenideElement header() {
      return child(FirewallPage.CHILD_HEADER_CSS_CLASS);
    }

    public SelenideElement cardContent() {
      return child(".nx-card__call-out");
    }

    public SelenideElement autoUnquarantineLink() {
      return child(TEXT_LINK);
    }
  }

  public static class FirewallQuarantineTable
      extends BasicElement<FirewallQuarantineTable>
  {
    public FirewallQuarantineTable() {
      super(FirewallPage.ROOT, "#firewall-quarantine-table");
    }

    public SelenideElement header() {
      return child(FirewallPage.CHILD_HEADER_CSS_CLASS);
    }

    public SelenideElement tableBody() {
      return child("#iq-firewall-quarantine-table-body");
    }

    public ElementsCollection tableBodyRows() {
      return tableBody().findAll("tr");
    }

    public ElementsCollection tableBodyCellsFromRow(int rowIndex) {
      return tableBody().findAll("tr:nth-child(" + (rowIndex + 1) + ") td");
    }

    public SelenideElement getComponentDetailsPageLinkFromRow(int rowIndex) {
      return tableBody().find("tr:nth-child(" + (rowIndex + 1) + ") td:nth-child(4) .nx-text-link");
    }

    public NxTableHeader quarantineTimeHeader() {
      return new NxTableHeader("#quarantineTime-header");
    }

    public NxTableHeader policyNameHeader() {
      return new NxTableHeader("#policyName-header");
    }

    public NxTableHeader componentHeader() {
      return new NxTableHeader("#component-header");
    }

    public NxTableHeader repositoryHeader() {
      return new NxTableHeader("#repository-header");
    }

    public SelenideElement policyNameSelect() {
      return child("#firewall-quarantine-table--select-policy");
    }

    public ElementsCollection policyNameCheckboxes() {
      return children("#firewall-quarantine-table--select-policy .nx-radio-checkbox");
    }

    public SelenideElement policyFilterReset() {
      return child("#firewall-quarantine-table--select-policy .nx-filter-dropdown__reset");
    }

    public SelenideElement componentNameInput() {
      return child("#firewall-quarantine-table--component-name");
    }

    public SelenideElement repositoryPublicIdInput() {
      return child("#firewall-quarantine-table--repository-public-id");
    }

    public ElementsCollection quarantineTimeOptions() {
      return children("#firewall-quarantine-table--select-quarantine-time .nx-dropdown-button");
    }

    public SelenideElement quarantineTimeInput() {
      return child("#firewall-quarantine-table--select-quarantine-time");
    }
  }

  public static class FirewallAutoUnquarantineYtd
      extends BasicElement<FirewallAutoUnquarantineYtd>
  {
    public FirewallAutoUnquarantineYtd(String rootSelector) {
      super(rootSelector, "#firewall-auto-release-quarantine-ytd");
    }

    public SelenideElement header() {
      return child(FirewallAutoUnquarantinePage.CHILD_HEADER_CSS_CLASS);
    }

    public SelenideElement cardContent() {
      return child(".nx-card__call-out");
    }
  }

  public static class FirewallAutoUnquarantineMtd
      extends BasicElement<FirewallAutoUnquarantineMtd>
  {
    public FirewallAutoUnquarantineMtd(String rootSelector) {
      super(rootSelector, "#firewall-auto-release-quarantine-mtd");
    }

    public SelenideElement header() {
      return child(FirewallAutoUnquarantinePage.CHILD_HEADER_CSS_CLASS);
    }

    public SelenideElement cardContent() {
      return child(".nx-card__call-out");
    }
  }

  public static class FirewallUnquarantineTable
      extends BasicElement<FirewallUnquarantineTable>
  {
    public FirewallUnquarantineTable(String rootSelector) {
      super(rootSelector, ".nx-table-container");
    }

    public SelenideElement header() {
      return child(FirewallAutoUnquarantinePage.CHILD_HEADER_CSS_CLASS);
    }

    public SelenideElement tableBody() {
      return child("#iq-firewall-auto-unquarantine-table-body");
    }

    public ElementsCollection tableBodyRows() {
      return tableBody().findAll("tr");
    }

    public NxTableHeader quarantineTimeHeader() {
      return new NxTableHeader("#quarantineTime-header");
    }

    public NxTableHeader releaseQuarantineTimeHeader() {
      return new NxTableHeader("#releaseQuarantineTime-header");
    }
  }

  public static CipModal cipModal() {
    return new CipModal("#cip-modal");
  }

  public static class FirewallWaiversTable
      extends BasicElement<FirewallWaiversTable>
  {
    public FirewallWaiversTable(String rootSelector) {
      super(rootSelector, "#firewall-waivers-tab-panel");
    }
  }

  public static class RoiFirewallMetricsTab
      extends BasicElement<RoiFirewallMetricsTab>
  {
    public RoiFirewallMetricsTab(String rootSelector) {
      super(rootSelector, "#firewall-roi-tab");
    }
  }

  public static class RoiFirewallMetrics
      extends BasicElement<RoiFirewallMetrics>
  {
    public RoiFirewallMetrics(String rootSelector) {
      super(rootSelector, "#roi-firewall-metrics");
    }

    public SelenideElement title() {
      return child(".roi-firewall-metrics__title");
    }

    public SelenideElement description() {
      return child(".roi-firewall-metrics__description");
    }

    public SelenideElement total() {
      return child(".roi-firewall-metrics__total");
    }

    public SelenideElement contentHeader(String id) {
      String selector = "h2[data-testid='roi-firewall-metrics-content__title__" + id + "']";
      return child(selector);
    }

    public SelenideElement contentHeaderTooltipIcon(String id) {
      String selector = "svg[data-testid='roi-firewall-metrics-content__tool-tip-title__" + id + "']";
      return child(selector);
    }

    public SelenideElement contentValue(String id) {
      String selector = "div[data-testid='roi-firewall-metrics-content__value__" + id + "']";
      return child(selector);
    }
  }

  public static class FirewallPageTabs
      extends BasicElement<FirewallPageTabs>
  {
    public FirewallPageTabs(String rootSelector) {
      super(rootSelector, "#firewall-page-tabs");
    }

    public SelenideElement tab(String id) {
      return child("#firewall-" + id + "-tab");
    }

    public SelenideElement tabPanel(String id) {
      return child("#firewall-" + id + "-tab-panel");
    }
  }

  public static class FirewallContainerWaiversTabContent
      extends BasicElement<FirewallContainerWaiversTabContent>
  {
    public FirewallContainerWaiversTabContent(String rootSelector) {
      super(rootSelector, "#firewall-container-waiver-tab-content");
    }

    public SelenideElement refreshButton() {
      return child(".refresh-button");
    }

    public SelenideElement waiverTableTitle() {
      return child(".nx-h2");
    }

    public SelenideElement waiverTable() {
      return child("#firewall-container-waiver-table");
    }

    public ElementsCollection waivers() {
      return children(".firewall-container-waiver");
    }

    public ContainerWaiverTile waiver(int index) {
      return new ContainerWaiverTile(childSelector(createSelector(".firewall-container-waiver", nthChild(index + 1))));
    }

    public SelenideElement previousPageButton() {
      return childXpath("//button[@aria-label='goto previous page']");
    }

    public SelenideElement nextPageButton() {
      return childXpath("//button[@aria-label='goto next page']");
    }

    public ElementsCollection paginationButtons() {
      return children(".nx-btn--pagination.nx-btn");
    }
  }

  public static class ContainerWaiverTile
      extends BasicElement<ContainerWaiverTile>
  {
    public ContainerWaiverTile(String selector) {
      super(selector);
    }

    public SelenideElement threatIndicator() {
      return child(".waiver-threat-cell .nx-threat-indicator");
    }

    public SelenideElement threatNumber() {
      return child(".waiver-threat-cell .nx-threat-number");
    }

    public SelenideElement createTime() {
      return child(createSelector(".nx-cell", nthChild(2)));
    }

    public SelenideElement expiryTime() {
      return child(createSelector(".nx-cell", nthChild(3)));
    }

    public SelenideElement policy() {
      return child(createSelector(".nx-cell", nthChild(4)));
    }

    public SelenideElement scope() {
      return child(createSelector(".nx-cell", nthChild(5)));
    }

    public SelenideElement component() {
      return child(createSelector(".nx-cell", nthChild(6)));
    }
  }

  public static class FirewallContainerQuarantineTabContent
      extends BasicElement<FirewallContainerQuarantineTabContent>
  {
    public FirewallContainerQuarantineTabContent(String rootSelector) {
      super(rootSelector, "#firewall-container-quarantine-table");
    }

    public SelenideElement quarantineTableTitle() {
      return child(".nx-h2");
    }

    public SelenideElement refreshButton() {
      return child("#firewall-container-quarantine-table--refresh-button");
    }

    public SelenideElement quarantineTable() {
      return child("#pagination-firewall-container-quarantine-table");
    }

    public ElementsCollection quarantinedContainers() {
      return children(".firewall-container-quarantine");
    }

    public ContainerQuarantineTile quarantinedContainer(int index) {
      return new ContainerQuarantineTile(
          childSelector(createSelector(".firewall-container-quarantine", nthChild(index + 1))));
    }

    public SelenideElement previousPageButton() {
      return childXpath("//button[@aria-label='goto previous page']");
    }

    public SelenideElement nextPageButton() {
      return childXpath("//button[@aria-label='goto next page']");
    }

    public ElementsCollection paginationButtons() {
      return children(".nx-btn--pagination.nx-btn");
    }
  }

  public static class ContainerQuarantineTile
      extends BasicElement<ContainerQuarantineTile>
  {
    public ContainerQuarantineTile(String selector) {
      super(selector);
    }

    public SelenideElement threatIndicator() {
      return child(".quarantine-threat-cell .nx-threat-indicator");
    }

    public SelenideElement threatNumber() {
      return child(".quarantine-threat-cell .nx-threat-number");
    }

    public SelenideElement policy() {
      return child(createSelector(".nx-cell", nthChild(2)));
    }

    public SelenideElement quarantineTime() {
      return child(createSelector(".nx-cell", nthChild(3)));
    }

    public SelenideElement container() {
      return child(createSelector(".nx-cell", nthChild(4)));
    }

    public SelenideElement containerReportPageLink() {
      return container().find(".nx-text-link");
    }

    public SelenideElement repository() {
      return child(createSelector(".nx-cell", nthChild(5)));
    }

    public SelenideElement repositoryResultsPageLink() {
      return repository().find(".nx-text-link");
    }
  }
}
