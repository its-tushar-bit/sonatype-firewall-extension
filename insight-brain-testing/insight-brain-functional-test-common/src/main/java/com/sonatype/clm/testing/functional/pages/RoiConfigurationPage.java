/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class RoiConfigurationPage
    extends BasicElement<RoiConfigurationPage>
{
  public static final String ROOT = "#roi-configuration-page";

  public RoiConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/roiConfiguration");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/roiConfiguration");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public Button editButton() {
    return new Button(childSelector("#roi-configuration-page__button__edit"));
  }

  public SelenideElement lifecycleTitle() {
    return child("#roi-configuration-page__lifecycle-title");
  }

  public SelenideElement baselineDaysToResolveViolation() {
    return child("#roi-configuration-page__numeric-value__baseline-days-to-resolve-violation");
  }

  public SelenideElement dailyRiskCostOfUnfixedViolation() {
    return child("#roi-configuration-page__numeric-value__daily-risk-cost-of-unfixed-violation");
  }

  public SelenideElement firewallTitle() {
    return child("#roi-configuration-page__firewall-title");
  }

  public SelenideElement malwareAttacksPrevented() {
    return child("#roi-configuration-page__numeric-value__malware-attacks-prevented");
  }

  public SelenideElement namespaceAttacksPrevented() {
    return child("#roi-configuration-page__numeric-value__namespace-attacks-prevented");
  }

  public SelenideElement safeComponentsAutoSelected() {
    return child("#roi-configuration-page__numeric-value__safe-components-auto-selected");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }
}
