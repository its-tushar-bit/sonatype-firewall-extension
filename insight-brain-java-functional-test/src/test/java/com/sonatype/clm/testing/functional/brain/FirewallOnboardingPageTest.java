/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.FirewallOnboardingPage;

import com.codeborne.selenide.Condition;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;

public class FirewallOnboardingPageTest
    extends AbstractFunctionalTest
{
  private final FirewallOnboardingPage page = new FirewallOnboardingPage();

  @BeforeClass
  public static void before() {
    refreshOrOpen(FirewallOnboardingPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallOnboardingPageLayout() {
    refreshOrOpen(FirewallOnboardingPage.url());

    SidebarNavigation.container().shouldBe(hidden);
    page.shouldBe(Condition.visible);
    page.steps().shouldBe(Condition.visible);
    page.actionsFooter().shouldBe(Condition.visible);
    page.shouldHave(Condition.text("content"));
  }

  @Test
  public void testFirewallOnboardingPageNavigation() {
    refreshOrOpen(FirewallOnboardingPage.url());

    SidebarNavigation.container().shouldBe(hidden);
    
    page.continueButton().shouldBe(Condition.visible);
    page.previousButton().shouldNotBe(Condition.visible);
    page.launchFirewallButton().shouldNotBe(Condition.visible);
    page.selectedStepShouldBe("1. Select");

    page.continueButton().click();
    page.selectedStepShouldBe("2. Protect");
    page.previousButton().shouldBe(Condition.visible);
    page.continueButton().shouldNotBe(Condition.visible);
    page.launchFirewallButton().shouldBe(Condition.visible);
  }
}
