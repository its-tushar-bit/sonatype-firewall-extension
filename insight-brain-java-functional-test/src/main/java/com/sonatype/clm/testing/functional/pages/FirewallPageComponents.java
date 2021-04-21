/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class FirewallPageComponents
{
  public static class FirewallStatus
      extends BasicElement<FirewallStatus>
  {
    public FirewallStatus(String rootSelector) {
      super(rootSelector, "#firewall-status");
    }

    public SelenideElement title() {
      return child(".nx-h1");
    }
  }

  public static class FirewallQuarantineStatus
      extends BasicElement<FirewallQuarantineStatus>
  {
    public FirewallQuarantineStatus(String rootSelector) {
      super(rootSelector, "#firewall-quarantine-status");
    }

    public SelenideElement header() {
      return child(FirewallPage.CHILD_HEADER_CSS_CLASS);
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
      return child(".nx-text-link");
    }

    public Button retryButton() {
      return new Button(childSelector(".nx-load-error__retry"));
    }
  }

  public static class FirewallQuarantine
      extends BasicElement<FirewallQuarantine>
  {
    public FirewallQuarantine(String rootSelector) {
      super(rootSelector, "#firewall-quarantine");
    }

    public SelenideElement header() {
      return child(FirewallPage.CHILD_HEADER_CSS_CLASS);
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
      return child(".nx-text-link");
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

    public SelenideElement quarantineTimeHeader() {
      return child("#quarantineTime-header");
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

    public SelenideElement quarantineTimeHeader() {
      return child("#quarantineTime-header");
    }

    public SelenideElement releaseQuarantineTimeHeader() {
      return child("#releaseQuarantineTime-header");
    }
  }
}
