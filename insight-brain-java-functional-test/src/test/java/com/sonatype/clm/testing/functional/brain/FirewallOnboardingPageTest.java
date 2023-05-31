/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallOnboardingPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Condition;
import org.junit.BeforeClass;
import org.junit.Test;

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

    SidebarNavigation.container().shouldBe(Condition.hidden);
    page.shouldBe(Condition.visible);
    page.steps().shouldBe(Condition.visible);
    page.actionsFooter().shouldBe(Condition.visible);
    page.shouldHave(Condition.text("Select proxy repositories"));
  }

  @Test
  public void testFirewallOnboardingPageNavigation() {
    refreshOrOpen(FirewallOnboardingPage.url());

    SidebarNavigation.container().shouldBe(Condition.hidden);

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

  @Test
  public void testLaunchFirewallButtonRedirectsToFirewallPageAndOpensWelcomeModal() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.continueButton().click();
    page.launchFirewallButton().shouldBe(Condition.visible).click();

    waitUntilUrl(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    firewallPage.shouldBe(Condition.visible);
    firewallPage.firewallWelcomeModal().shouldBe(Condition.visible);

    eyesWatcher.eyesCheck("Firewall Welcome Modal");

    firewallPage.firewallWelcomeModal().closeButton().click();
    firewallPage.firewallWelcomeModal().shouldBe(Condition.hidden);
  }

  @Test
  public void testRedirectToFirewallOnboardingPage() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      tempEntity.newRepositoryManager();
      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());
      page.shouldBe(Condition.visible);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testNoRedirectWhenNoUnconfiguredRepoManagersExist() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      loginAsAdmin();

      waitUntilUrl(DashboardPage.urlToViolations());
      page.shouldBe(Condition.hidden);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testRedirectWhenMultipleUnconfiguredRepoManagersExist() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      tempEntity.newRepositoryManager("instanceId1","nexusTest");
      tempEntity.newRepositoryManager("instanceId2","OtherRepoManager");
      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());
      page.shouldBe(Condition.visible);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testNoRedirectWhenUserHaveReadPermissionOnly() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      User user = tempEntity.newUser();
      grantPermissions(user.getUsername(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

      login(user.getUsername(),user.getPassword());

      waitUntilUrl(DashboardPage.urlToViolations());
      page.shouldBe(Condition.hidden);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }
}
