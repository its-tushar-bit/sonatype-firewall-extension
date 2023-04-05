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
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;

public class FirewallOnboardingPageTest
    extends AbstractFunctionalTest
{
  private final FirewallOnboardingPage page = new FirewallOnboardingPage();

  @Before
  public void before() {
    refreshOrOpen(FirewallOnboardingPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallOnboardingPage_DisplayText() {
    refreshOrOpen(FirewallOnboardingPage.url());

    SidebarNavigation.container().shouldBe(hidden);
    page.shouldBe(Condition.visible);
    page.shouldBe(Condition.text("Firewall Onboarding Page"));
  }
}
