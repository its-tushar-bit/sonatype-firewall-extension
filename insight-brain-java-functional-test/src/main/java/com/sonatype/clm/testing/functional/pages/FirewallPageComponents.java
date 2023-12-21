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
}
