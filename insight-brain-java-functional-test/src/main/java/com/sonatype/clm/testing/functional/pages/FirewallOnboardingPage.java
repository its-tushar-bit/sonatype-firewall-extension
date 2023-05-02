/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class FirewallOnboardingPage
    extends BasicElement<FirewallOnboardingPage>
{
  public static final String ROOT = "#firewall-onboarding-page";

  public FirewallOnboardingPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewallOnboarding");
  }

  public Button continueButton() {
    return new Button(childSelector("#continue-button"));
  }

  public Button previousButton() {
    return new Button(childSelector("#previous-button"));
  }

  public Button launchFirewallButton() {
    return new Button(childSelector("#launch-button"));
  }

  public SelenideElement selectedStepShouldBe(String step) {
    return child(".step.selected").shouldHave(Condition.text(step));
  }

  public SelenideElement steps() {
    return child("#onboarding-steps");
  }

  public SelenideElement actionsFooter() {
    return child("#actions-footer");
  }
}
