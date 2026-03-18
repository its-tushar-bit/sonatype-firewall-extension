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
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FirewallRepositoryList;
import com.sonatype.clm.testing.functional.elements.FirewallRepositoryList.HeaderColumn;
import com.sonatype.clm.testing.functional.elements.HelpMenu;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallOnboardingPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.dto.model.repository.RepositoryType.hosted;
import static com.sonatype.clm.dto.model.repository.RepositoryType.proxy;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallOnboardingPageTest
    extends AbstractFunctionalTest
{
  private final FirewallOnboardingPage page = new FirewallOnboardingPage();

  private PolicyDAO policyDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  @BeforeClass
  public static void before() {
    refreshOrOpen(FirewallOnboardingPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
    // the firewall onboarding feature has been disabled by default. In order for this test to exercise the feature
    // we have to turn it on first
    systemConfigurationPropertyDAO.set(
        SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.getPropertyName(),
        Boolean.TRUE.toString());
  }

  @Test
  public void testFirewallOnboardingPageWelcomeLayout() {
    String welcomeDescription =
        "Protect against 3rd party malicious attacks, dependency confusion and investigate existing threats and risks"
            + " in your repositories.";
    refreshOrOpen(FirewallOnboardingPage.url());

    page.welcomeTitle().shouldHave(Condition.text("Welcome to Repository Firewall"));
    page.welcomeSubtitle().shouldHave(Condition.text("Start step-by-step configuration"));
    page.welcomeDescription().shouldHave(Condition.text(welcomeDescription));
    page.getStartedButton().shouldHave(Condition.text("Get Started"));

    // Wait for the sidebar to close.
    Selenide.sleep(1000);
    eyesWatcher.eyesCheck("Firewall onboarding: Welcome Screen");
  }

  @Test
  public void testFirewallOnboardingPageOnboardingLayout() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.getStartedButton().click();
    page.shouldBe(visible);
    page.steps().shouldBe(visible);
    page.actionsFooter().shouldBe(visible);
    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());
  }

  @Test
  public void testConfirmProxyRepositoryContainersAreAsWideAsParentContainer1Repo() {
    refreshOrOpen(FirewallOnboardingPage.url());

    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId1");

    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);
    createRepositories(1, repositoryManager, "maven2", proxy, supportedFormats);

    loginAsAdmin();
    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());
    eyesWatcher.eyesCheck(
        "Firewall onboarding: 1 repository container is as wide as in its parent container," + " with space its sides");

  }

  @Test
  public void testConfirmProxyRepositoryContainersAreAsWideAsParentContainer2Repos() {
    refreshOrOpen(FirewallOnboardingPage.url());

    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId1");

    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);
    createRepositories(1, repositoryManager, "maven2", proxy, supportedFormats);
    createRepositories(1, repositoryManager, "pypi", proxy, supportedFormats);

    loginAsAdmin();
    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    eyesWatcher.eyesCheck("Firewall onboarding: 2 repository containers combined are as wide as its parent "
        + "container, and have space around them");
  }

  @Test
  public void testConfirmProxyRepositoryContainersAreAsWideAsParentContainer3Repos() {
    refreshOrOpen(FirewallOnboardingPage.url());

    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId1");

    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);
    createRepositories(1, repositoryManager, "maven2", proxy, supportedFormats);
    createRepositories(1, repositoryManager, "pypi", proxy, supportedFormats);
    createRepositories(1, repositoryManager, "npm", proxy, supportedFormats);

    loginAsAdmin();
    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    eyesWatcher.eyesCheck("Firewall onboarding: 3 repository containers combined are as wide as its parent "
        + "container, and have space around them");
  }

  @Test
  public void testConfirmProxyRepositoryContainersAreAsWideAsParentContainer4Repos() {
    refreshOrOpen(FirewallOnboardingPage.url());

    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId1");

    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);
    createRepositories(1, repositoryManager, "maven2", proxy, supportedFormats);
    createRepositories(1, repositoryManager, "pypi", proxy, supportedFormats);
    createRepositories(1, repositoryManager, "npm", proxy, supportedFormats);
    createRepositories(1, repositoryManager, "go", proxy, supportedFormats);

    loginAsAdmin();
    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());
    eyesWatcher.eyesCheck("Firewall onboarding: 4 repository containers combined are as wide as its parent "
        + "container, and have space around them");
  }

  @Test
  public void testHelpButtonShouldBeDisplayedWithCorrectAttributes() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.helpButton()
        .shouldBe(attribute("href", "http://links.sonatype.com/products/nxiq/doc/firewall-onboarding"))
        .shouldHave(attribute("target", "_blank"));
  }

  @Test
  public void testMaliciousComponentsDocumentationLinkShouldBeDisplayedWithCorrectAttributes() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.maliciousComponentsDocumentationLink()
        .shouldBe(attribute(
            "href",
            "http://links.sonatype.com/nexus-firewall/protection-from-pending-and-suspicious-components"))
        .shouldHave(attribute("target", "_blank"));
  }

  @Test
  public void testNamespaceAttacksDocumentationLinkShouldBeDisplayedWithCorrectAttributes() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.namespaceAttacksDocumentationLink()
        .shouldBe(attribute("href", "http://links.sonatype.com/products/nxiq/doc/preventing-namespace-confusion"))
        .shouldHave(attribute("target", "_blank"));
  }

  @Test
  public void testMainHeaderHelpMenuItemIsInterceptedAndOpensIncompleteConfigurationModal() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.incompleteConfigurationModal().shouldNotBe(visible);

    HelpMenu help = MainHeader.helpMenu();

    help.dropdownToggle().click();
    help.gettingStartedLink().shouldBe(visible).click();

    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().cancelButton().click();
    page.incompleteConfigurationModal().shouldNotBe(visible);
    page.shouldBe(visible);

    help.dropdownToggle().click();
    help.gettingStartedLink().shouldBe(visible).click();

    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().continueButton().click();

    waitUntilUrl(GettingStartedPage.url());
    new GettingStartedPage().shouldBe(visible);
  }

  @Test
  public void testMainHeaderSystemPreferenceItemIsInterceptedAndOpensIncompleteConfigurationModal() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.incompleteConfigurationModal().shouldNotBe(visible);

    SystemConfigMenu sysConfigMenu = MainHeader.systemConfigMenu();

    sysConfigMenu.dropdownToggle().click();
    sysConfigMenu.users().shouldBe(visible).click();

    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().cancelButton().click();
    page.incompleteConfigurationModal().shouldNotBe(visible);
    page.shouldBe(visible);

    sysConfigMenu.dropdownToggle().click();
    sysConfigMenu.users().shouldBe(visible).click();

    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().continueButton().click();

    waitUntilUrl(UserManagementPage.firewallUrl());
    new UserManagementPage().shouldBe(visible);
  }

  @Test
  public void testCancelButtonOpensIncompleteConfigurationModal() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.getStartedButton().click();
    page.incompleteConfigurationModal().shouldNotBe(visible);

    page.cancelButton().shouldBe(visible).click();

    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().cancelButton().click();
    page.incompleteConfigurationModal().shouldNotBe(visible);

    page.shouldBe(visible);

    page.cancelButton().shouldBe(visible).click();
    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().continueButton().click();
    page.incompleteConfigurationModal().shouldNotBe(visible);

    waitUntilUrl(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    firewallPage.shouldBe(visible);
  }

  @Test
  public void testLogoutOpensIncompleteConfigurationModalAndClickCancelButton() {
    refreshOrOpen(FirewallOnboardingPage.url());

    page.incompleteConfigurationModal().shouldNotBe(visible);

    logoutAndDontIgnoreUnsavedChangesModal();

    page.incompleteConfigurationModal().shouldBe(visible);
    page.incompleteConfigurationModal().cancelButton().click();
    page.incompleteConfigurationModal().shouldNotBe(visible);

    page.shouldBe(visible);
  }

  @Test
  public void testFirewallOnboardingPageNavigation() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    createUnconfiguredRepositoryManager();
    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());
    SidebarNavigation.container().shouldBe(visible);

    page.getStartedButton().click();
    page.continueButton().shouldBe(visible);
    page.previousButton().shouldNotBe(visible);
    page.launchFirewallButton().shouldNotBe(visible);
    page.selectedStepShouldBe("1. Protection Rules Selection");

    page.continueButton().click();
    page.continueButton().shouldBe(visible);
    page.previousButton().shouldBe(visible);
    page.launchFirewallButton().shouldNotBe(visible);
    page.selectedStepShouldBe("2. Enabling Protection (open source components)");

    page.continueButton().click();
    page.continueButton().shouldBe(visible);
    page.previousButton().shouldBe(visible);
    page.launchFirewallButton().shouldNotBe(visible);
    page.selectedStepShouldBe("3. Enabling Protection (proprietary components)");

    page.continueButton().shouldBe(visible);
    page.continueButton().click();
    page.selectedStepShouldBe("4. Review");
    page.previousButton().shouldBe(visible);
    page.continueButton().shouldNotBe(visible);
    page.launchFirewallButton().shouldBe(visible);
  }

  @Test
  public void testLaunchFirewallButtonRedirectsToFirewallPageAndOpensWelcomeModal() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);
    createRepositories(1, repositoryManager, "maven2", proxy, supportedFormats);
    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.getStartedButton().click();
    page.continueButton().click();
    page.continueButton().click();
    page.continueButton().click();
    page.launchFirewallButton().shouldBe(visible).click();

    waitUntilUrl(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    firewallPage.shouldBe(visible);
    firewallPage.firewallWelcomeModal().shouldBe(visible);

    eyesWatcher.eyesCheck("Firewall Welcome Modal");

    firewallPage.firewallWelcomeModal().closeButton().click();
    firewallPage.firewallWelcomeModal().shouldBe(Condition.hidden);
  }

  @Test
  public void testRedirectToFirewallOnboardingPage() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    createUnconfiguredRepositoryManager();
    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
  }

  @Test
  public void testNoRedirectWhenNoUnconfiguredRepoManagersExist() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    loginAsAdmin();

    waitUntilUrl(DashboardPage.urlToViolations());
    page.shouldBe(Condition.hidden);
  }

  @Test
  public void testRedirectWhenMultipleUnconfiguredRepoManagersExist() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    createUnconfiguredRepositoryManager("instanceId1");
    createUnconfiguredRepositoryManager("instanceId2");
    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
  }

  @Test
  public void testNoRedirectWhenUserHaveReadPermissionOnly() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    User user = tempEntity.newUser();
    grantPermissions(user.getUsername(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.READ);

    login(user.getUsername(), user.getPassword());

    waitUntilUrl(DashboardPage.urlToViolations());
    page.shouldBe(Condition.hidden);
  }

  @Test
  public void testSelectProtectionRulesStep() {
    refreshOrOpen(FirewallOnboardingPage.url());

    createUnconfiguredRepositoryManager("instanceId");

    List<String> supportedFormats = Arrays.asList("maven2", "swift", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);

    page.shouldBe(Condition.visible);
    page.getStartedButton().click();

    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());
    page.shouldHave(Condition.text("Select a core set of policies that enable a default set of protection rules. "
        + "You can modify these protection rules again later."));

    page.supplyChainAttacksProtectionRuleCheckbox().shouldNotBe(selected);
    page.supplyChainAttacksProtectionRuleCheckbox().click();
    page.supplyChainAttacksProtectionRuleCheckbox().shouldBe(selected);

    page.namespaceConfusionProtectionRuleCheckbox().shouldNotBe(selected);
    page.namespaceConfusionProtectionRuleCheckbox().click();
    page.namespaceConfusionProtectionRuleCheckbox().shouldBe(selected);

    eyesWatcher.eyesCheck("Firewall onboarding: select protection rules step");

    page.continueButton().click();

    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorTitle());
  }

  @Test
  public void testProxyAndHostedContextualTextsBasedOnSelectedProtectionRules() {
    refreshOrOpen(FirewallOnboardingPage.url());

    createUnconfiguredRepositoryManager("instanceId");

    List<String> supportedFormats = Arrays.asList("maven2", "swift", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);

    page.shouldBe(Condition.visible);
    page.getStartedButton().click();

    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());

    page.supplyChainAttacksProtectionRuleCheckbox().shouldNotBe(selected);
    page.supplyChainAttacksProtectionRuleCheckbox().click();
    page.supplyChainAttacksProtectionRuleCheckbox().shouldBe(selected);

    page.namespaceConfusionProtectionRuleCheckbox().shouldNotBe(selected);
    page.namespaceConfusionProtectionRuleCheckbox().click();
    page.namespaceConfusionProtectionRuleCheckbox().shouldBe(selected);

    page.continueButton().click();
    page.shouldHave(Condition.text("Enable protection from malicious components"));
    page.shouldHave(Condition.text("The selected proxy repositories will have supply chain attacks "
        + "protection and namespace confusion protection enabled."));
    page.continueButton().click();
    page.shouldHave(Condition.text("Protect your internal components from namespace attacks"));
    page.shouldHave(Condition.text("The component names from the selected hosted repositories will be used to protect "
        + "against namespace confusion attacks against your proxy repositories."));
    page.shouldHave(
        Condition.text("This capability should only be turned on for repositories with proprietary components only."
            + " Enabling it on hosted repositories containing open-source components will cause those namespaces "
            + "to be quarantined."));

    page.previousButton().click();
    page.previousButton().click();

    page.supplyChainAttacksProtectionRuleCheckbox().click();
    page.supplyChainAttacksProtectionRuleCheckbox().shouldNotBe(selected);
    page.namespaceConfusionProtectionRuleCheckbox().shouldBe(selected);

    page.continueButton().click();
    page.shouldHave(Condition.text("You have not enabled recommended protection"));
    page.shouldHave(Condition.text("The selected proxy repositories will have namespace confusion protection enabled. "
        + "You can also enable supply chain attacks protection by going back to the previous step."));
    page.continueButton().click();
    page.shouldHave(Condition.text("Protect your internal components from namespace attacks"));
    page.shouldHave(Condition.text("The component names from the selected hosted repositories will be used to protect "
        + "against namespace confusion attacks against your proxy repositories."));
    page.shouldHave(
        Condition.text("This capability should only be turned on for repositories with proprietary components only."
            + " Enabling it on hosted repositories containing open-source components will cause those namespaces "
            + "to be quarantined."));

    page.previousButton().click();
    page.previousButton().click();

    page.supplyChainAttacksProtectionRuleCheckbox().click();
    page.namespaceConfusionProtectionRuleCheckbox().click();
    page.supplyChainAttacksProtectionRuleCheckbox().shouldBe(selected);
    page.namespaceConfusionProtectionRuleCheckbox().shouldNotBe(selected);

    page.continueButton().click();
    page.shouldHave(Condition.text("Enable protection from malicious components"));
    page.shouldHave(Condition.text("The selected proxy repositories will have supply chain attacks protection enabled. "
        + "You can also enable namespace confusion protection by going back to the previous step."));
    page.continueButton().click();
    page.shouldHave(Condition.text("Protect your internal components from namespace attacks"));
    page.shouldHave(
        Condition.text("The component names from the selected hosted repositories will not be used to protect against "
            + "namespace confusion attacks against your proxy repositories. "
            + "You can enable namespace confusion protection by going back to the previous step."));
    page.shouldHave(
        Condition.text("This capability should only be turned on for repositories with proprietary components only."
            + " Enabling it on hosted repositories containing open-source components will cause those namespaces "
            + "to be quarantined."));

    page.previousButton().click();
    page.previousButton().click();

    page.supplyChainAttacksProtectionRuleCheckbox().click();
    page.supplyChainAttacksProtectionRuleCheckbox().shouldNotBe(selected);
    page.namespaceConfusionProtectionRuleCheckbox().shouldNotBe(selected);

    page.continueButton().click();
    page.shouldHave(Condition.text("You have not enabled recommended protection"));
    page.shouldHave(Condition.text("The selected proxy repositories will not have supply chain attacks "
        + "protection or namespace confusion protection enabled. "
        + "You can enable protection by going back to the previous step."));
    page.continueButton().click();
    page.shouldHave(Condition.text("Protect your internal components from namespace attacks"));
    page.shouldHave(
        Condition.text("The component names from the selected hosted repositories will not be used to protect "
            + "against namespace confusion attacks against your proxy repositories. "
            + "You can enable namespace confusion protection by going back to the previous step."));
    page.shouldHave(
        Condition.text("This capability should only be turned on for repositories with proprietary components only."
            + " Enabling it on hosted repositories containing open-source components will cause those namespaces "
            + "to be quarantined."));
  }

  @Test
  public void testSelectProxyRepositoriesStep() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");

    List<String> supportedFormats = Arrays.asList("maven2", "swift", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);

    List<Repository> maven2Repositories = createRepositories(5, repositoryManager, "maven2", proxy, supportedFormats);
    List<Repository> pypiRepositories = createRepositories(4, repositoryManager, "pypi", proxy, supportedFormats);
    List<Repository> npmRepositories = createRepositories(3, repositoryManager, "npm", proxy, supportedFormats);
    List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", proxy, supportedFormats);
    List<Repository> swiftRepositories = createRepositories(2, repositoryManager, "swift", proxy, supportedFormats);
    List<Repository> otherRepositories = new ArrayList<>(goRepositories);
    createRepositories(2, repositoryManager, null, proxy, supportedFormats); // add a repository with null format
    otherRepositories.addAll(swiftRepositories);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());

    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());
    page.shouldHave(text("The selected proxy repositories will not have supply chain attacks "
        + "protection or namespace confusion protection enabled. "
        + "You can enable protection by going back to the previous step."));

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(4));
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "3 of 5"));
    repositoriesLists.get(1).shouldHave(text("pypi\n" + "2 of 4"));
    repositoriesLists.get(2).shouldHave(text("npm\n" + "2 of 3"));
    repositoriesLists.get(3).shouldHave(text("other\n" + "2 of 4"));

    eyesWatcher.eyesCheck("Firewall onboarding: select proxy repositories step");

    List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories, proxy, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(1), pypiRepositories, proxy, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(2), npmRepositories, proxy, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(3), otherRepositories, proxy, supportedFormats);

    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(0), maven2Repositories,
        supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(1), pypiRepositories, supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(2), npmRepositories, supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(3), otherRepositories,
        supportedFormats);
  }

  @Test
  public void testSelectHostedRepositoriesStep() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    List<String> supportedFormats = Arrays.asList("maven2", "swift", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");

    List<Repository> maven2Repositories = createRepositories(5, repositoryManager, "maven2", hosted, supportedFormats);
    List<Repository> pypiRepositories = createRepositories(4, repositoryManager, "pypi", hosted, supportedFormats);
    List<Repository> npmRepositories = createRepositories(3, repositoryManager, "npm", hosted, supportedFormats);
    List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", hosted, supportedFormats);
    List<Repository> swiftRepositories = createRepositories(2, repositoryManager, "swift", hosted, supportedFormats);
    List<Repository> otherRepositories = new ArrayList<>(goRepositories);
    createRepositories(2, repositoryManager, null, hosted, supportedFormats); // add a repository with null form at
    otherRepositories.addAll(swiftRepositories);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    page.continueButton().click();
    page.shouldHave(text("Protect your internal components from namespace attacks"));

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(4));
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "3 of 5"));
    repositoriesLists.get(1).shouldHave(text("pypi\n" + "2 of 4"));
    repositoriesLists.get(2).shouldHave(text("npm\n" + "2 of 3"));
    repositoriesLists.get(3).shouldHave(text("other\n" + "2 of 4"));

    eyesWatcher.eyesCheck("Firewall onboarding: select hosted repositories step");

    List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories, hosted, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(1), pypiRepositories, hosted, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(2), npmRepositories, hosted, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(3), otherRepositories, hosted, supportedFormats);

    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(0), maven2Repositories,
        supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(1), pypiRepositories, supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(2), npmRepositories, supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(3), otherRepositories,
        supportedFormats);
  }

  @Test
  public void testSelectHostedRepositoriesStep_unsupportedRepositories() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    List<String> supportedFormats = Arrays.asList("maven2", "npm");
    mockComponentSupportedFormats(supportedFormats);

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");

    List<Repository> maven2Repositories = createRepositories(5, repositoryManager, "maven2", hosted, supportedFormats);
    List<Repository> pypiRepositories = createRepositories(4, repositoryManager, "pypi", hosted, supportedFormats);
    List<Repository> npmRepositories = createRepositories(3, repositoryManager, "npm", hosted, supportedFormats);
    List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", hosted, supportedFormats);
    List<Repository> swiftRepositories = createRepositories(2, repositoryManager, "swift", hosted, supportedFormats);
    List<Repository> otherRepositories = new ArrayList<>(goRepositories);
    createRepositories(2, repositoryManager, null, hosted, supportedFormats); // add a repository with null format
    otherRepositories.addAll(swiftRepositories);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    page.continueButton().click();
    page.shouldHave(text("Protect your internal components from namespace attacks"));

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(4));
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "3 of 5"));
    repositoriesLists.get(1).shouldHave(text("npm\n" + "2 of 3"));
    repositoriesLists.get(2).shouldHave(text("pypi\n" + "0 of 4"));
    repositoriesLists.get(3).shouldHave(text("other\n" + "0 of 4"));

    List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories, hosted, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(1), npmRepositories, hosted, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(2), pypiRepositories, hosted, supportedFormats);
    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(3), otherRepositories, hosted, supportedFormats);

    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(0), maven2Repositories,
        supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(1), npmRepositories, supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(2), pypiRepositories, supportedFormats);
    selectAndUnselectAllRepositoriesFromGroupedList(firewallRepositoryLists.get(3), otherRepositories,
        supportedFormats);
  }

  @Test
  public void testSelectProxyRepositoriesStepWithNoSupportedFormat() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    List<String> supportedFormats = Arrays.asList("otherFormat");
    mockComponentSupportedFormats(supportedFormats);

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId4");
    List<Repository> maven2Repositories = createRepositories(5, repositoryManager, "maven2", proxy, supportedFormats);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(1));
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "0 of 5"));

    eyesWatcher.eyesCheck("Firewall onboarding: select proxy repositories step with no format supported");

    List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

    checkFirewallRepositoryListByFormat(firewallRepositoryLists.get(0), maven2Repositories, proxy, supportedFormats);

    // and all repositories are disabled
    expectAllRepositoriesToBeDisabled(firewallRepositoryLists, selenideElement -> true);
  }

  @Test
  public void testSelectProxyRepositoriesStep_onlyUnsupportedReposAreDisabled() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    // given that only the maven2 format is supported
    List<String> supportedFormats = Collections.singletonList("maven2");
    mockComponentSupportedFormats(supportedFormats);

    // and a repository manager with 5 maven2, 4 helm repositories and 6 apt repositories
    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId5");
    createRepositories(4, repositoryManager, "helm", proxy, supportedFormats);
    createRepositories(5, repositoryManager, "maven2", proxy, supportedFormats);
    createRepositories(6, repositoryManager, "apt", proxy, supportedFormats);

    // when I open the firewall onboarding page
    loginAsAdmin();
    waitUntilUrl(FirewallOnboardingPage.url());

    // then I should see the select protection rules step
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());

    // then I should see the select proxy repositories step
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());

    // and I should see 3 lists of repositories
    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(3));

    // and the supported formats are sorted before the unsupported ones even if the unsupported ones have more repos
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "3 of 5"));
    repositoriesLists.get(1).shouldHave(text("apt\n" + "0 of 6"));
    repositoriesLists.get(2).shouldHave(text("helm\n" + "0 of 4"));

    // and all helm repos should be disabled and all maven2 repos should be enabled
    List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();
    expectAllRepositoriesToBeDisabled(firewallRepositoryLists,
        where -> where.has(text("apt")) || where.has(text("helm")));
  }

  private static void expectAllRepositoriesToBeDisabled(
      final List<FirewallRepositoryList> firewallRepositoryLists,
      final Predicate<SelenideElement> where)
  {
    firewallRepositoryLists.forEach(firewallRepositoryList -> {
      for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
        NxCheckbox checkbox = firewallRepositoryList.row(i).checkbox();
        if (where.test(firewallRepositoryList.row(i).column(1))) {
          checkbox.input().shouldBe(disabled);
        }
        else {
          checkbox.input().shouldBe(enabled);
        }
      }
    });
  }

  @Test
  public void testSelectHostedRepositoriesStepWithNoSupportedFormat() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    List<String> supportedFormats = Arrays.asList("otherFormat");
    mockComponentSupportedFormats(supportedFormats);

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
    createRepositories(5, repositoryManager, "maven2", proxy, supportedFormats);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());
    page.shouldBe(visible);
    page.getStartedButton().click();
    page.continueButton().click();
    page.continueButton().click();
    page.shouldHave(text("Protect your internal components from namespace attacks"));
    page.shouldHave(text("There are no hosted repositories to apply your protection rules."));

    eyesWatcher.eyesCheck("Firewall onboarding: select hosted repositories step with no format supported");

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(0));
  }

  @Test
  public void testLastStepDisplaysCorrectNumberOfEnabledProxyAndHostedRepositories() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);

    // Proxy Repositories
    List<Repository> maven2ProxyRepositories =
        createRepositories(5, repositoryManager, "maven2", proxy, supportedFormats);
    createRepositories(4, repositoryManager, "pypi", proxy, supportedFormats);
    createRepositories(3, repositoryManager, "npm", proxy, supportedFormats);
    List<Repository> goRepositories = createRepositories(2, repositoryManager, "go", proxy, supportedFormats);
    new ArrayList<>(goRepositories).addAll(maven2ProxyRepositories);

    // Hosted Repositories
    List<Repository> maven2HostedRepositories =
        createRepositories(5, repositoryManager, "maven2", hosted, supportedFormats);
    createRepositories(4, repositoryManager, "pypi", hosted, supportedFormats);
    createRepositories(3, repositoryManager, "npm", hosted, supportedFormats);
    List<Repository> goHostedRepositories = createRepositories(2, repositoryManager, "go", hosted, supportedFormats);
    new ArrayList<>(goHostedRepositories).addAll(maven2HostedRepositories);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(4));
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "3 of 5"));
    repositoriesLists.get(1).shouldHave(text("pypi\n" + "2 of 4"));
    repositoriesLists.get(2).shouldHave(text("npm\n" + "2 of 3"));
    repositoriesLists.get(3).shouldHave(text("go\n" + "1 of 2"));

    List<FirewallRepositoryList> firewallRepositoryLists = page.firewallRepositoryLists();

    page.continueButton().click();
    page.shouldHave(text("Protect your internal components from namespace attacks"));

    page.continueButton().click();

    // Hosted Repositories count will be implemented: CLM-25614
    page.shouldHave(text("Inspect and complete configuration"));
    page.shouldHave(text("Congratulations, you’ve reached the final step."));
    page.shouldHave(text(
        "After you complete, Firewall configuration will run in the background and populate " +
            "data related to all your\n enabled repositories."));
    page.shouldHave(text(
        "Time taken to complete this process depends on the number of enabled repositories and " +
            "size of each individual\nrepository."));
    page.shouldHave(text(
        "Malicious blocking will be enabled for 8 out of 14 proxy repositories."));
    page.shouldHave(text(
        "Namespace confusion protection will block selected namespaces from 8 out of 14 hosted repositories."));

    eyesWatcher.eyesCheck("Firewall onboarding: inspect and complete onboarding step");

    page.previousButton().click();
    page.previousButton().click();
    firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
    page.continueButton().click();
    firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
    page.continueButton().click();

    page.shouldHave(text(
        "Malicious blocking will be enabled for 10 out of 14 proxy repositories."));
    page.shouldHave(text(
        "Namespace confusion protection will block selected namespaces from 10 out of 14 hosted repositories."));

    page.previousButton().click();
    page.previousButton().click();
    firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
    page.continueButton().click();
    firewallRepositoryLists.get(0).checkAllHeaderColumn().selectAllCheckbox().click();
    page.continueButton().click();
    page.shouldHave(text(
        "Malicious blocking will be enabled for 5 out of 14 proxy repositories."));
    page.shouldHave(text(
        "Namespace confusion protection will block selected namespaces from 5 out of 14 hosted repositories."));
  }

  @Test
  public void testLastStepDisplaysNumberOfSupportedEnabledProxyAndHostedRepositoriesOnly() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm");

    // Proxy Repositories
    createRepositories(4, repositoryManager, "maven2", proxy, supportedFormats);
    createRepositories(4, repositoryManager, "pypi", proxy, supportedFormats);
    createRepositories(4, repositoryManager, "npm", proxy, supportedFormats);
    // Unsupported repositories, should not be counted:
    createRepositories(4, repositoryManager, "nuget", proxy, supportedFormats);

    // Hosted Repositories
    createRepositories(4, repositoryManager, "maven2", hosted, supportedFormats);
    createRepositories(4, repositoryManager, "pypi", hosted, supportedFormats);
    createRepositories(4, repositoryManager, "npm", hosted, supportedFormats);
    // Unsupported repositories, should not be counted:
    createRepositories(4, repositoryManager, "nuget", hosted, supportedFormats);

    mockComponentSupportedFormats(supportedFormats);

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.shouldBe(visible);
    page.getStartedButton().click();
    page.shouldHave(FirewallOnboardingPage.protectionRulesSelectorTitle());
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorNoProtectionRulesTitle());

    ElementsCollection repositoriesLists = page.repositoriesList();
    repositoriesLists.shouldHave(size(4));
    repositoriesLists.get(0).shouldHave(text("maven2\n" + "2 of 4"));
    repositoriesLists.get(1).shouldHave(text("npm\n" + "2 of 4"));
    repositoriesLists.get(2).shouldHave(text("pypi\n" + "2 of 4"));
    repositoriesLists.get(3).shouldHave(text("nuget\n" + "0 of 4"));

    page.continueButton().click();

    page.shouldHave(text("Protect your internal components from namespace attacks"));

    ElementsCollection hostedRepositoriesLists = page.repositoriesList();
    hostedRepositoriesLists.shouldHave(size(4));
    hostedRepositoriesLists.get(0).shouldHave(text("maven2\n" + "2 of 4"));
    hostedRepositoriesLists.get(1).shouldHave(text("npm\n" + "2 of 4"));
    hostedRepositoriesLists.get(2).shouldHave(text("pypi\n" + "2 of 4"));
    hostedRepositoriesLists.get(3).shouldHave(text("nuget\n" + "0 of 4"));

    page.continueButton().click();
    page.shouldHave(text("Inspect and complete configuration"));
    page.shouldHave(text(
        "After you complete, Firewall configuration will run in the background and populate data related " +
            "to all your\n enabled repositories."));
    page.shouldHave(text(
        "Time taken to complete this process depends on the number of enabled repositories and " +
            "size of each individual\nrepository."));
    page.shouldHave(text(
        "Malicious blocking will be enabled for 6 out of 16 proxy repositories."));
    page.shouldHave(text(
        "Namespace confusion protection will block selected namespaces from 6 out of 16 hosted repositories."));
  }

  @Test
  public void testLaunchFirewallConfiguresProtectionRules() {
    refreshOrOpen(FirewallOnboardingPage.url());
    logout();

    RepositoryManager repositoryManager = createUnconfiguredRepositoryManager("instanceId");
    List<String> supportedFormats = Arrays.asList("maven2", "pypi", "npm", "go");
    mockComponentSupportedFormats(supportedFormats);
    createRepositories(1, repositoryManager, "maven2", proxy, supportedFormats);
    Policy securityMaliciousPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious");
    Policy securityNamespaceConflictPolicy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
    Policy integrityRatingPolicy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Security-Namespace Conflict");

    assertThat(securityMaliciousPolicy.getActions()).isEmpty();
    assertThat(integrityRatingPolicy.getActions()).isEmpty();
    assertThat(securityNamespaceConflictPolicy.getActions()).isEmpty();

    loginAsAdmin();

    waitUntilUrl(FirewallOnboardingPage.url());

    page.shouldBe(Condition.visible);
    page.getStartedButton().click();
    page.shouldHave(Condition.text("Enable Repository Firewall features"));
    page.supplyChainAttacksProtectionRuleCheckbox().click();
    page.namespaceConfusionProtectionRuleCheckbox().click();
    page.continueButton().click();
    page.shouldHave(FirewallOnboardingPage.proxyRepositoriesSelectorTitle());
    page.continueButton().click();
    page.shouldHave(Condition.text("Protect your internal components from namespace attacks"));
    page.continueButton().click();
    page.launchFirewallButton().click();
    page.closeButton().click();

    Policy policy = policyDAO.getById(securityMaliciousPolicy.getId());
    assertThat(policy.getActions().get(StageTypes.PROXY.getId())).isEqualTo(Action.ID_FAIL);

    policy = policyDAO.getById(securityNamespaceConflictPolicy.getId());
    assertThat(policy.getActions().get(StageTypes.PROXY.getId())).isEqualTo(Action.ID_FAIL);

    policy = policyDAO.getById(integrityRatingPolicy.getId());
    assertThat(policy.getActions().get(StageTypes.PROXY.getId())).isEqualTo(Action.ID_FAIL);
  }

  private void checkFirewallRepositoryListByFormat(
      FirewallRepositoryList firewallRepositoryList,
      List<Repository> repositoriesListByFormat,
      RepositoryType repositoryType,
      List<String> supportedFormats)

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

    checkRepositoriesProtectionRuleAndSort(firewallRepositoryList, repositoriesListByFormat, repositoryType,
        supportedFormats);

    firewallRepositoryList.nameHeaderColumn().nxAnchor().click();
    firewallRepositoryList.nameHeaderColumn().name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("name descending"));

    Collections.reverse(repositoriesListByFormat);
    checkRepositoriesProtectionRuleAndSort(firewallRepositoryList, repositoriesListByFormat, repositoryType,
        supportedFormats);
  }

  private void checkRepositoriesProtectionRuleAndSort(
      FirewallRepositoryList firewallRepositoryList,
      List<Repository> repositoriesListByFormat,
      RepositoryType repositoryType,
      List<String> supportedFormats)
  {
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      firewallRepositoryList.row(i).name().shouldHave(text(repositoriesListByFormat.get(i).getName()));

      boolean checkCondition;

      if (!supportedFormats.contains(repositoriesListByFormat.get(i).getFormat())) {
        checkCondition = false;
      }
      else if (repositoryType == proxy) {
        checkCondition = repositoriesListByFormat.get(i).isQuarantineEnabled();
      }
      else if (repositoryType == hosted) {
        checkCondition = repositoriesListByFormat.get(i).isNamespaceConfusionProtectionEnabled();
      }
      else {
        throw new IllegalArgumentException("Unsupported repository type: " + repositoryType);
      }

      if (checkCondition) {
        firewallRepositoryList.row(i).checkbox().shouldBe(selected);
      }
      else {
        firewallRepositoryList.row(i).checkbox().shouldNotBe(selected);
      }
    }
  }

  private void selectAndUnselectAllRepositoriesFromGroupedList(
      FirewallRepositoryList firewallRepositoryList,
      List<Repository> otherRepositories,
      List<String> supportedFormats)
  {
    firewallRepositoryList.checkAllHeaderColumn().selectAllCheckbox().click();
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      WebElementCondition expectedCondition = supportedFormats.contains(otherRepositories.get(i).getFormat())
          ? selected
          : disabled;
      firewallRepositoryList.row(i).checkbox().input().shouldBe(expectedCondition);
    }

    firewallRepositoryList.checkAllHeaderColumn().selectAllCheckbox().click();
    for (int i = 0; i < firewallRepositoryList.rows().size(); i++) {
      WebElementCondition expectedCondition = supportedFormats.contains(otherRepositories.get(i).getFormat())
          ? not(selected)
          : disabled;
      firewallRepositoryList.row(i).checkbox().input().shouldBe(expectedCondition);
    }
  }

  private List<Repository> createRepositories(
      int totalRepositories,
      RepositoryManager repositoryManager,
      String format,
      RepositoryType repositoryType,
      List<String> supportedFormats)
  {
    List<Repository> result = new ArrayList<>();

    for (int i = 0; i < totalRepositories; i++) {
      String publicId = format + "publicId" + i;

      if (repositoryType == hosted) {
        publicId = format + "hostedPublicId" + i;
      }

      Repository repository = tempEntity.newRepository(
          repositoryManager,
          publicId,
          repositoryType,
          format,
          (i % 2) == 0 && supportedFormats.contains(format));
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
    RepositoryManager repoManager = tempEntity.newRepositoryManager(repositoryManagerInstanceId,
        "Nexus/3.60.0-01 (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    repoManager.setProductName("Nexus");
    repoManager.setProductVersion("3.60.0-01");
    repositoryManagerDAO.update(repoManager);
    return repoManager;
  }
}
