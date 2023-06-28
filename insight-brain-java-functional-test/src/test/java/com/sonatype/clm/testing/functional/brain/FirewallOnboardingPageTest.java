/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FirewallRepositoryList;
import com.sonatype.clm.testing.functional.elements.FirewallRepositoryList.HeaderColumn;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallOnboardingPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

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
  public void testFirewallOnboardingPageWelcomeLayout() {
    String welcomeDescription =
        "Protect against 3rd party malicious attacks, dependency confusion and investigate existing threats and risks"
            +  " in your repositories.";
    refreshOrOpen(FirewallOnboardingPage.url());

    eyesWatcher.eyesCheck("Firewall onboarding: Welcome Screen");

    page.welcomeTitle().shouldHave(Condition.text("Welcome to Repository Firewall"));
    page.welcomeSubtitle().shouldHave(Condition.text("Start step-by-step configuration"));
    page.welcomeDescription().shouldHave(Condition.text(welcomeDescription));
    page.getStartedButton().shouldHave(Condition.text("Get Started"));
  }

  @Test
  public void testFirewallOnboardingPageOnboardingLayout() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.getStartedButton().click();
    page.shouldBe(Condition.visible);
    page.steps().shouldBe(Condition.visible);
    page.actionsFooter().shouldBe(Condition.visible);
    page.shouldHave(Condition.text("Select proxy repositories"));
  }

  @Test
  public void testConfirmProxyRepositoryContainersAreAsWideAsParentContainer() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId1");

      createRepositories(1, repositoryManager, "maven2", RepositoryType.proxy);
      List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();
      waitUntilUrl(FirewallOnboardingPage.url());
      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.shouldHave(Condition.text("Select proxy repositories"));
      eyesWatcher.eyesCheck("Firewall onboarding: 1 repository container is as wide as in its parent container," +
          " with space its sides");

      createRepositories(1, repositoryManager, "pypi", RepositoryType.proxy);
      refreshOrOpen(FirewallOnboardingPage.url());
      waitUntilUrl(FirewallOnboardingPage.url());
      page.getStartedButton().click();
      eyesWatcher.eyesCheck("Firewall onboarding: 2 repository containers combined are as wide as its parent " +
          "container, and have space around them");

      createRepositories(1, repositoryManager, "npm", RepositoryType.proxy);
      refreshOrOpen(FirewallOnboardingPage.url());
      waitUntilUrl(FirewallOnboardingPage.url());
      page.getStartedButton().click();
      eyesWatcher.eyesCheck("Firewall onboarding: 3 repository containers combined are as wide as its parent " +
          "container, and have space around them");

      createRepositories(1, repositoryManager, "go", RepositoryType.proxy);
      refreshOrOpen(FirewallOnboardingPage.url());
      waitUntilUrl(FirewallOnboardingPage.url());
      page.getStartedButton().click();
      eyesWatcher.eyesCheck("Firewall onboarding: 4 repository containers combined are as wide as its parent " +
          "container, and have space around them");
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }
  
  @Test
  public void testHelpButtonShouldBeDisplayedWithCorrectAttributes() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.shouldBe(Condition.visible);
    page.getStartedButton().click();
    page.helpButton()
      .shouldBe(attribute("href", "http://links.sonatype.com/products/nxiq/doc/firewall-onboarding"))
      .shouldHave(attribute("target", "_blank"));
  }

  @Test
  public void testClickingSidebarNavigationOpensIncompleteConfigurationModalOnWelcomeScreen() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    SidebarNavigation.container().shouldBe(Condition.visible);
    SidebarNavigation.legalNavigationButton().click();
    page.incompleteConfigurationModal().shouldBe(Condition.visible);

    eyesWatcher.eyesCheck("Incomplete Configuration Modal: Welcome Screen");

    page.incompleteConfigurationModal().continueButton().click();
    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    page.shouldBe(Condition.visible);

    SidebarNavigation.dashboardNavigationButton().click();
    page.incompleteConfigurationModal().exitButton().click();

    waitUntilUrl(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldBe(Condition.visible);
  }

  @Test
  public void testClickingSidebarNavigationOpensIncompleteConfigurationModalOnOnboardingScreen() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    page.getStartedButton().click();
    SidebarNavigation.container().shouldBe(Condition.visible);
    SidebarNavigation.legalNavigationButton().click();
    page.incompleteConfigurationModal().shouldBe(Condition.visible);

    eyesWatcher.eyesCheck("Incomplete Configuration Modal: Onboarding Screen");

    page.incompleteConfigurationModal().continueButton().click();
    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    page.shouldBe(Condition.visible);

    SidebarNavigation.dashboardNavigationButton().click();
    page.incompleteConfigurationModal().exitButton().click();

    waitUntilUrl(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldBe(Condition.visible);
  }

  @Test
  public void testCancelButtonOpensIncompleteConfigurationModal() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.getStartedButton().click();
    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    page.cancelButton().shouldBe(Condition.visible).click();

    page.incompleteConfigurationModal().shouldBe(Condition.visible);
    page.incompleteConfigurationModal().continueButton().click();
    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    page.shouldBe(Condition.visible);

    page.cancelButton().shouldBe(Condition.visible).click();
    page.incompleteConfigurationModal().shouldBe(Condition.visible);
    page.incompleteConfigurationModal().exitButton().click();
    page.incompleteConfigurationModal().shouldNotBe(Condition.visible);

    waitUntilUrl(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    firewallPage.shouldBe(Condition.visible);
  }

  @Test
  public void testFirewallOnboardingPageNavigation() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      createUnconfiguredRepositoryManager();
      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());
      SidebarNavigation.container().shouldBe(Condition.visible);
      page.getStartedButton().click();

      page.selectedStepShouldBe("1. Select");
      page.previousButton().shouldNotBe(Condition.visible);
      page.launchFirewallButton().shouldNotBe(Condition.visible);
      page.continueButton().shouldBe(Condition.visible);
      page.continueButton().click();

      page.selectedStepShouldBe("2. Select");
      page.previousButton().shouldBe(Condition.visible);
      page.launchFirewallButton().shouldNotBe(Condition.visible);
      page.continueButton().shouldBe(Condition.visible);
      page.continueButton().click();

      page.selectedStepShouldBe("3. Protect");
      page.previousButton().shouldBe(Condition.visible);
      page.continueButton().shouldNotBe(Condition.visible);
      page.launchFirewallButton().shouldBe(Condition.visible);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testLaunchFirewallButtonRedirectsToFirewallPageAndOpensWelcomeModal() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      createUnconfiguredRepositoryManager();
      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());

      page.getStartedButton().click();
      page.continueButton().click();
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
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testRedirectToFirewallOnboardingPage() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      createUnconfiguredRepositoryManager();
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
      createUnconfiguredRepositoryManager("instanceId1");
      createUnconfiguredRepositoryManager("instanceId2");
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

  @Test
  public void testSelectProxyRepositoriesStep() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");

      List<Repository> maven2Repositories = createRepositories(5, repositoryManager, "maven2", RepositoryType.proxy);
      List<Repository> pypiRepositories = createRepositories(4, repositoryManager, "pypi", RepositoryType.proxy);
      List<Repository> npmRepositories = createRepositories(3, repositoryManager, "npm", RepositoryType.proxy);
      List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", RepositoryType.proxy);
      List<Repository> swiftRepositories = createRepositories(2, repositoryManager, "swift", RepositoryType.proxy);
      List<Repository> otherRepositories = new ArrayList<Repository>(goRepositories);
      createRepositories(2, repositoryManager, null, RepositoryType.proxy); // add a repository with null format
      otherRepositories.addAll(swiftRepositories);

      List<String> supportedFormats = Arrays.asList("maven2", "swift", "pypi", "npm", "go");
      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());

      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.shouldHave(Condition.text("Select proxy repositories"));
      page.shouldHave(Condition.text(
              "Choose which proxy repositories you would like to apply your protection rules to."));

      ElementsCollection repositoriesLists = page.repositoriesList();
      repositoriesLists.shouldHaveSize(4);
      repositoriesLists.get(0).shouldHave(Condition.text("maven2\n" + "3 of 5"));
      repositoriesLists.get(1).shouldHave(Condition.text("pypi\n" + "2 of 4"));
      repositoriesLists.get(2).shouldHave(Condition.text("npm\n" + "2 of 3"));
      repositoriesLists.get(3).shouldHave(Condition.text("other\n" + "2 of 4"));

      eyesWatcher.eyesCheck("Firewall onboarding: select proxy repositories step");

      List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories, RepositoryType.proxy);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(1), pypiRepositories, RepositoryType.proxy);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(2), npmRepositories, RepositoryType.proxy);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(3), otherRepositories, RepositoryType.proxy);

      for (FirewallRepositoryList firewallRepositoryList: firewallRepositoryLists) {
        selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryList);
      }
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testSelectHostedRepositoriesStep() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");

      List<Repository> maven2Repositories = createRepositories(5, repositoryManager, "maven2", RepositoryType.hosted);
      List<Repository> pypiRepositories = createRepositories(4, repositoryManager, "pypi", RepositoryType.hosted);
      List<Repository> npmRepositories = createRepositories(3, repositoryManager, "npm", RepositoryType.hosted);
      List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", RepositoryType.hosted);
      List<Repository> swiftRepositories = createRepositories(2, repositoryManager, "swift", RepositoryType.hosted);
      List<Repository> otherRepositories = new ArrayList<Repository>(goRepositories);
      createRepositories(2, repositoryManager, null, RepositoryType.hosted); // add a repository with null form at
      otherRepositories.addAll(swiftRepositories);

      List<String> supportedFormats = Arrays.asList("maven2", "swift", "pypi", "npm", "go");
      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());

      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.continueButton().click();
      page.shouldHave(Condition.text("Select hosted repositories"));

      ElementsCollection repositoriesLists = page.repositoriesList();
      repositoriesLists.shouldHaveSize(4);
      repositoriesLists.get(0).shouldHave(Condition.text("maven2\n" + "3 of 5"));
      repositoriesLists.get(1).shouldHave(Condition.text("pypi\n" + "2 of 4"));
      repositoriesLists.get(2).shouldHave(Condition.text("npm\n" + "2 of 3"));
      repositoriesLists.get(3).shouldHave(Condition.text("other\n" + "2 of 4"));

      eyesWatcher.eyesCheck("Firewall onboarding: select hosted repositories step");

      List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories, RepositoryType.hosted);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(1), pypiRepositories, RepositoryType.hosted);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(2), npmRepositories, RepositoryType.hosted);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(3), otherRepositories, RepositoryType.hosted);

      for (FirewallRepositoryList firewallRepositoryList: firewallRepositoryLists) {
        selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryList);
      }
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testSelectProxyRepositoriesStepWithNoSupportedFormat() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
      createRepositories(5, repositoryManager, "maven2", RepositoryType.proxy);

      List<String> supportedFormats = Arrays.asList("otherFormat");
      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());
      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.shouldHave(Condition.text("Select proxy repositories"));
      page.shouldHave(text("There are no proxy repositories to apply your protection rules."));

      eyesWatcher.eyesCheck(
              "Firewall onboarding: select proxy repositories step with no format supported");

      ElementsCollection repositoriesLists = page.repositoriesList();
      repositoriesLists.shouldHaveSize(0);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testSelectHostedRepositoriesStepWithNoSupportedFormat() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
      createRepositories(5, repositoryManager, "maven2", RepositoryType.proxy);

      List<String> supportedFormats = Arrays.asList("otherFormat");
      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());
      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.continueButton().click();
      page.shouldHave(Condition.text("Select hosted repositories"));
      page.shouldHave(text("There are no hosted repositories to apply your protection rules."));

      eyesWatcher.eyesCheck(
              "Firewall onboarding: select hosted repositories step with no format supported");

      ElementsCollection repositoriesLists = page.repositoriesList();
      repositoriesLists.shouldHaveSize(0);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testLastStepDisplaysCorrectNumberOfEnabledProxyAndHostedRepositories() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");

      // Proxy Repositories
      List<Repository> maven2ProxyRepositories = createRepositories(
          5, repositoryManager, "maven2", RepositoryType.proxy
      );
      createRepositories(4, repositoryManager, "pypi", RepositoryType.proxy);
      createRepositories(3, repositoryManager, "npm", RepositoryType.proxy);
      List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", RepositoryType.proxy);
      new ArrayList<>(goRepositories).addAll(maven2ProxyRepositories);

      // Hosted Repositories
      List<Repository> maven2HostedRepositories = createRepositories(
          5, repositoryManager, "maven2", RepositoryType.hosted
      );
      createRepositories(4, repositoryManager, "pypi", RepositoryType.hosted);
      createRepositories(3, repositoryManager, "npm", RepositoryType.hosted);
      List<Repository> goHostedRepositories = createRepositories(2, repositoryManager, "go", RepositoryType.hosted);
      new ArrayList<>(goHostedRepositories).addAll(maven2HostedRepositories);

      List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());

      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.shouldHave(Condition.text("Select proxy repositories"));
      page.shouldHave(Condition.text(
              "Choose which proxy repositories you would like to apply your protection rules to."));

      ElementsCollection repositoriesLists = page.repositoriesList();
      repositoriesLists.shouldHaveSize(4);
      repositoriesLists.get(0).shouldHave(Condition.text("maven2\n" + "3 of 5"));
      repositoriesLists.get(1).shouldHave(Condition.text("pypi\n" + "2 of 4"));
      repositoriesLists.get(2).shouldHave(Condition.text("npm\n" + "2 of 3"));
      repositoriesLists.get(3).shouldHave(Condition.text("other\n" + "1 of 2"));

      List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

      page.continueButton().click();
      page.shouldHave(Condition.text("Select hosted repositories"));

      page.continueButton().click();

      // Hosted Repositories count will be implemented: CLM-25614
      page.shouldHave(Condition.text("Inspect and complete onboarding"));
      page.shouldHave(Condition.text("Congratulations, you’re all set!"));
      page.shouldHave(
          Condition.text("Once you launch Firewall, malicious blocking will be enabled for 8 proxy repositories.")
      );

      eyesWatcher.eyesCheck("Firewall onboarding: inspect and complete onboarding step");

      page.previousButton().click();
      page.previousButton().click();
      firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
      page.continueButton().click();
      page.continueButton().click();
      page.shouldHave(
          Condition.text("Once you launch Firewall, malicious blocking will be enabled for 10 proxy repositories.")
      );

      page.previousButton().click();
      page.previousButton().click();
      firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
      page.continueButton().click();
      page.continueButton().click();
      page.shouldHave(
          Condition.text("Once you launch Firewall, malicious blocking will be enabled for 5 proxy repositories.")
      );
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  @Test
  public void testLastStepDisplaysNumberOfSupportedEnabledProxyAndHostedRepositoriesOnly() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
      List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm");

      // Proxy Repositories
      createRepositories(4, repositoryManager, "maven2", RepositoryType.proxy);
      createRepositories(4, repositoryManager, "pypi", RepositoryType.proxy);
      createRepositories(4, repositoryManager, "npm", RepositoryType.proxy);
      // Unsupported repositories, should not be counted:
      createRepositories(4, repositoryManager, "nugget", RepositoryType.proxy);
      
      // Hosted Repositories
      createRepositories(4, repositoryManager, "maven2", RepositoryType.hosted);
      createRepositories(4, repositoryManager, "pypi", RepositoryType.hosted);
      createRepositories(4, repositoryManager, "npm", RepositoryType.hosted);
      // Unsupported repositories, should not be counted:
      createRepositories(4, repositoryManager, "nugget", RepositoryType.hosted);

      mockComponentSupportedFormats(supportedFormats);

      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());

      page.shouldBe(Condition.visible);
      page.getStartedButton().click();
      page.shouldHave(Condition.text("Select proxy repositories"));

      ElementsCollection repositoriesLists = page.repositoriesList();
      repositoriesLists.shouldHaveSize(3);
      repositoriesLists.get(0).shouldHave(Condition.text("maven2\n" + "2 of 4"));
      repositoriesLists.get(1).shouldHave(Condition.text("npm\n" + "2 of 4"));
      repositoriesLists.get(2).shouldHave(Condition.text("pypi\n" + "2 of 4"));

      page.continueButton().click();

      page.shouldHave(Condition.text("Select hosted repositories"));

      ElementsCollection hostedRepositoriesLists = page.repositoriesList();
      hostedRepositoriesLists.shouldHaveSize(3);
      hostedRepositoriesLists.get(0).shouldHave(Condition.text("maven2\n" + "2 of 4"));
      hostedRepositoriesLists.get(1).shouldHave(Condition.text("npm\n" + "2 of 4"));
      hostedRepositoriesLists.get(2).shouldHave(Condition.text("pypi\n" + "2 of 4"));

      page.continueButton().click();
      page.shouldHave(Condition.text("Inspect and complete onboarding"));
      page.shouldHave(
          Condition.text("Once you launch Firewall, malicious blocking will be enabled for 6 proxy repositories.")
      );
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    }
  }

  private void checkFirewallRepositoryListByFormat(
          FirewallRepositoryList firewallRepositoryList,
          List<Repository> repositoriesListByFormat,
          RepositoryType repositoryType)

  {
    firewallRepositoryList.checkAllHeaderColumn().selectAllCheckbox().shouldNotBe(checked);

    firewallRepositoryList.nameHeaderColumn().name().shouldHave(text("name"));
    firewallRepositoryList.nameHeaderColumn().name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("name unsorted"));

    firewallRepositoryList.nameHeaderColumn().nxAnchor().click();
    firewallRepositoryList.nameHeaderColumn().name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("name ascending"));

    firewallRepositoryList.nameHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldBe(visible);
    firewallRepositoryList.nameHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldBe(visible);

    checkRepositoriesProtectionRuleAndSort(firewallRepositoryList, repositoriesListByFormat, repositoryType);

    firewallRepositoryList.nameHeaderColumn().nxAnchor().click();
    firewallRepositoryList.nameHeaderColumn().name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("name descending"));

    Collections.reverse(repositoriesListByFormat);
    checkRepositoriesProtectionRuleAndSort(firewallRepositoryList, repositoriesListByFormat, repositoryType);
  }

  private void checkRepositoriesProtectionRuleAndSort(
          FirewallRepositoryList firewallRepositoryList,
          List<Repository> repositoriesListByFormat,
          RepositoryType repositoryType)
  {
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      firewallRepositoryList.row(i).name().shouldHave(text(repositoriesListByFormat.get(i).getName()));

      Boolean checkCondition = false;

      if (repositoryType == RepositoryType.proxy) {
        checkCondition = repositoriesListByFormat.get(i).isQuarantineEnabled();
      }
      else if (repositoryType == RepositoryType.hosted) {
        checkCondition = repositoriesListByFormat.get(i).isNamespaceConfusionProtectionEnabled();
      }

      if (checkCondition) {
        firewallRepositoryList.row(i).checkbox().shouldBe(selected);
      }
      else {
        firewallRepositoryList.row(i).checkbox().shouldNotBe(selected);
      }
    }
  }

  private void selectAndUnselectAllRepositoriesFromGroupedList(FirewallRepositoryList firewallRepositoryList) {
    firewallRepositoryList.checkAllHeaderColumn().selectAllCheckbox().click();
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      firewallRepositoryList.row(i).checkbox().shouldBe(selected);
    }

    firewallRepositoryList.checkAllHeaderColumn().selectAllCheckbox().click();
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      firewallRepositoryList.row(i).checkbox().shouldNotBe(selected);
    }
  }

  private List<Repository> createRepositories(
          int totalRepositories,
          RepositoryManager repositoryManager,
          String format,
          RepositoryType repositoryType)
  {
    List<Repository> result = new ArrayList<>();

    for (int i = 0; i < totalRepositories; i++) {
      String publicId = format + "publicId" + i;
      
      if (repositoryType == RepositoryType.hosted) {
        publicId = format + "hostedPublicId" + i;
      }

      Repository repository = tempEntity.newRepository(
              repositoryManager,
              publicId,
              repositoryType,
              format,
              (i % 2) == 0);
      result.add(repository);
    }

    return result;
  }

  private void mockComponentSupportedFormats(List<String> supportedFormats) {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    for (String format : supportedFormats) {
      hdsResult.regexpsByRepositoryFormat.put(format, Collections.singletonList(""));
    }

    testCLMServer.getHdsServer()
            .respondWith(hdsResult)
            .atUri(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);
  }

  private void createUnconfiguredRepositoryManager() {
    createUnconfiguredRepositoryManager("testInstanceId");
  }

  private RepositoryManager createUnconfiguredRepositoryManager(String repositoryManagerInstanceId) {
    return tempEntity.newRepositoryManager(repositoryManagerInstanceId,
        "Nexus/3.56.0-SNAPSHOT (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
  }
}
