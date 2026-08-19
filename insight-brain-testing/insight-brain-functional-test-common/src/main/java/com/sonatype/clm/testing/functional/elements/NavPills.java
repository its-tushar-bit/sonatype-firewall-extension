/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$;

public class NavPills
    extends BasicElement<NavPills>
{
  private static final String NAV_PILLS_LIST = ".iq-nav-pills-menu__list";

  public NavPills() {
    super(NAV_PILLS_LIST);
  }

  public ElementsCollection pills() {
    return children(".iq-nav-pills-menu__pill");
  }

  public SelenideElement appCategory() {
    return $("#owner-pill-app-categories-button");
  }

  public SelenideElement policy() {
    return $("#owner-pill-policy-button");
  }

  public SelenideElement legacyViolations() {
    return $("#owner-pill-legacy-violations-button");
  }

  public SelenideElement continuousMonitoring() {
    return $("#owner-pill-continuous-monitoring-button");
  }

  public SelenideElement proprietaryComponents() {
    return $("#owner-pill-component-configuration-button");
  }

  public SelenideElement labels() {
    return $("#owner-pill-comp-labels-button");
  }

  public SelenideElement ltg() {
    return $("#owner-pill-ltgs-button");
  }

  public SelenideElement retention() {
    return $("#owner-pill-retention-button");
  }

  public SelenideElement sourceControl() {
    return $("#owner-pill-source-control-button");
  }

  public SelenideElement innerSource() {
    return $("#owner-pill-innersource-repository-button");
  }

  public SelenideElement access() {
    return $("#owner-pill-access-button");
  }

  public SelenideElement autoWaivers() {
    return $("#owner-pill-auto-waivers-configuration-button");
  }

  public SelenideElement publicDataSources() {
    return $("#owner-pill-public-data-sources-button");
  }
}
