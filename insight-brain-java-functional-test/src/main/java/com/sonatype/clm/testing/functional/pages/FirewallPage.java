/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class FirewallPage
    extends BasicElement<FirewallPage>
{
  public static final String ROOT = "#firewall-page";

  public static final String CHILD_HEADER_CSS_CLASS = ".nx-h3";

  public FirewallPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewall");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public FirewallStatus firewallStatus() {
    return new FirewallStatus();
  }

  public FirewallQuarantineStatus firewallQuarantineStatus() {
    return new FirewallQuarantineStatus();
  }

  public FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus() {
    return new FirewallAutoUnquarantineStatus();
  }

  public FirewallAutoUnquarantine firewallQuarantine() {
    return new FirewallAutoUnquarantine();
  }

  public FirewallAutoUnquarantine firewallAutoUnquarantine() {
    return new FirewallAutoUnquarantine();
  }

  public FirewallQuarantineTable firewallQuarantineTable() {
    return new FirewallQuarantineTable();
  }

  public static class FirewallStatus
      extends BasicElement<FirewallStatus>
  {
    public FirewallStatus() {
      super(ROOT, "#firewall-status");
    }

    public SelenideElement title() {
      return child(".nx-h1");
    }
  }

  public static class FirewallQuarantineStatus
      extends BasicElement<FirewallQuarantineStatus>
  {
    public FirewallQuarantineStatus() {
      super(ROOT, "#firewall-quarantine-status");
    }

    public SelenideElement header() {
      return child(CHILD_HEADER_CSS_CLASS);
    }
  }

  public static class FirewallAutoUnquarantineStatus
      extends BasicElement<FirewallAutoUnquarantineStatus>
  {
    public FirewallAutoUnquarantineStatus() {
      super(ROOT, "#firewall-auto-unquarantine-status");
    }

    public SelenideElement header() {
      return child(CHILD_HEADER_CSS_CLASS);
    }
  }

  public static class FirewallQuarantine
      extends BasicElement<FirewallQuarantine>
  {
    public FirewallQuarantine() {
      super(ROOT, "#firewall-quarantine");
    }

    public SelenideElement header() {
      return child(CHILD_HEADER_CSS_CLASS);
    }
  }

  public static class FirewallAutoUnquarantine
      extends BasicElement<FirewallAutoUnquarantine>
  {
    public FirewallAutoUnquarantine() {
      super(ROOT, "#firewall-auto-unquarantine");
    }

    public SelenideElement header() {
      return child(CHILD_HEADER_CSS_CLASS);
    }
  }

  public static class FirewallQuarantineTable
      extends BasicElement<FirewallQuarantineTable>
  {
    public FirewallQuarantineTable() {
      super(ROOT, "#firewall-quarantine-table");
    }

    public SelenideElement header() {
      return child(CHILD_HEADER_CSS_CLASS);
    }
  }
}
