/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.*;

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

import org.junit.BeforeClass;
import org.junit.Test;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Condition.*;

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
      tempEntity.newRepositoryManager();
      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());
      SidebarNavigation.container().shouldBe(Condition.visible);
      page.getStartedButton().click();
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
      tempEntity.newRepositoryManager();
      loginAsAdmin();

      waitUntilUrl(FirewallOnboardingPage.url());

      page.getStartedButton().click();
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
      tempEntity.newRepositoryManager("instanceId1", "nexusTest");
      tempEntity.newRepositoryManager("instanceId2", "OtherRepoManager");
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
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instanceId3", "nexusTest3");

      List<Repository> maven2Repositories = createProxyRepositories(5, repositoryManager, "maven2");
      List<Repository> pypiRepositories = createProxyRepositories(4, repositoryManager, "pypi");
      List<Repository> npmRepositories = createProxyRepositories(3, repositoryManager, "npm");
      List<Repository> goRepositories = createProxyRepositories(2, repositoryManager, "go");
      List<Repository> swiftRepositories = createProxyRepositories(2, repositoryManager, "swift");
      List<Repository> otherRepositories = new ArrayList<Repository>(goRepositories);
      createProxyRepositories(2, repositoryManager,null); // add a repository with null format
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

      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(1), pypiRepositories);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(2), npmRepositories);
      checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(3), otherRepositories);

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
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instanceId4", "nexus");
      createProxyRepositories(5, repositoryManager, "maven2");

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
  public void testLastStepDisplaysCorrectNumberOfEnabledProxyRepositories() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instanceId3", "nexusTest3");

      List<Repository> maven2Repositories = createProxyRepositories(5, repositoryManager, "maven2");
      createProxyRepositories(4, repositoryManager, "pypi");
      createProxyRepositories(3, repositoryManager, "npm");
      List<Repository> goRepositories = createProxyRepositories(2, repositoryManager, "go");
      new ArrayList<>(goRepositories).addAll(maven2Repositories);

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
      page.shouldHave(Condition.text("Inspect and complete onboarding"));
      page.shouldHave(Condition.text("Congratulations, you’re all set!"));
      page.shouldHave(
          Condition.text("Once you launch Firewall, malicious blocking will be enabled for 8 proxy repositories.")
      );

      eyesWatcher.eyesCheck("Firewall onboarding: inspect and complete onboarding step");

      page.previousButton().click();
      firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
      page.continueButton().click();
      page.shouldHave(
          Condition.text("Once you launch Firewall, malicious blocking will be enabled for 10 proxy repositories.")
      );

      page.previousButton().click();
      firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
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
  public void testLastStepDisplaysNumberOfSupportedEnabledProxyRepositoriesOnly() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);

    try {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instanceId3", "nexusTest3");

      createProxyRepositories(4, repositoryManager, "maven2");
      createProxyRepositories(4, repositoryManager, "pypi");
      createProxyRepositories(4, repositoryManager, "npm");

      // Unsupported repositories, should not be counted:
      createProxyRepositories(4, repositoryManager, "nugget");

      List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm");
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

      page.firewallRepositoryLists();

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
          List<Repository> repositoriesListByFormat)
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

    checkRepositoriesProtectionRuleAndSort(firewallRepositoryList, repositoriesListByFormat);

    firewallRepositoryList.nameHeaderColumn().nxAnchor().click();
    firewallRepositoryList.nameHeaderColumn().name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("name descending"));

    Collections.reverse(repositoriesListByFormat);
    checkRepositoriesProtectionRuleAndSort(firewallRepositoryList, repositoriesListByFormat);
  }

  private void checkRepositoriesProtectionRuleAndSort(
          FirewallRepositoryList firewallRepositoryList,List<Repository> repositoriesListByFormat)
  {
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      firewallRepositoryList.row(i).name().shouldHave(text(repositoriesListByFormat.get(i).getName()));
      if (repositoriesListByFormat.get(i).isQuarantineEnabled()) {
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

  private List<Repository> createProxyRepositories(
          int totalRepositories,
          RepositoryManager repositoryManager,
          String format)
  {
    List<Repository> result = new ArrayList<>();

    for (int i = 0; i < totalRepositories; i++) {
      String publicId = format + "publicId" + i;

      Repository repository = tempEntity.newRepository(
              repositoryManager,
              publicId,
              RepositoryType.proxy,
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
}
