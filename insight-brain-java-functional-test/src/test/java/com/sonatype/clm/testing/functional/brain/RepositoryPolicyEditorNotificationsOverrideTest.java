/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class RepositoryPolicyEditorNotificationsOverrideTest
    extends AbstractFunctionalTest
{
  private RepositoryManager repositoryManager;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    repositoryManager = tempEntity.newRepositoryManager();
  }

  @Test
  public void testOverrideAddAndEdit_Repository() {
    Repository repository = tempEntity.newProxyRepository(repositoryManager, "npm-proxy", "npm",
        true, true);
    goToRepositorySummaryPage(repository);
    createTrivialPolicy(false);
    PolicyEditorPage.savePolicy();

    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
    OwnerDetailSidebar.policyGroup().entryItems().shouldHave(size(1));
    OwnerDetailSidebar.policyGroup().entryItems().get(0).click();

    PolicyEditorPage.title().shouldHave(text("Edit Policy"));
    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
  }

  @Test
  public void testOverrideAddAndEdit_RepositoryManager() {
    goToRepositoryManagerSummaryPage(repositoryManager);
    createTrivialPolicy(true);
    PolicyEditorPage.savePolicy();

    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().input().shouldNotBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow notification overrides at repository level"));
    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().shouldNotBe(visible);
    PolicyEditorPage.inheritanceSection().specifiedChildrenInheritRadio().shouldNotBe(visible);
    OwnerDetailSidebar.policyGroup().entryItems().shouldHave(size(1));
    OwnerDetailSidebar.policyGroup().entryItems().get(0).click();

    PolicyEditorPage.title().shouldHave(text("Edit Policy"));
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().input().shouldBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow notification overrides at repository level"));
  }

  @Test
  public void testOverrideAddAndEdit_RepositoryContainer() {
    goToRepositoryContainerSummaryPage();
    createTrivialPolicy(true);
    PolicyEditorPage.savePolicy();

    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().input().shouldNotBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow notification overrides at repository manager and repository levels"));
    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().shouldNotBe(visible);
    PolicyEditorPage.inheritanceSection().specifiedChildrenInheritRadio().shouldNotBe(visible);
    OwnerDetailSidebar.policyGroup().entryItems().shouldHave(size(1));
    OwnerDetailSidebar.policyGroup().entryItems().get(0).click();

    PolicyEditorPage.title().shouldHave(text("Edit Policy"));
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().input().shouldBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow notification overrides at repository manager and repository levels"));
  }

  @Test
  public void testOverride_inheritsFromContainerPolicyTrue() {
    goToRepositoryContainerSummaryPage();
    testOverride_inheritsFromPolicy(true, 1);
  }

  @Test
  public void testOverride_inheritsFromContainerPolicyFalse() {
    goToRepositoryContainerSummaryPage();
    testOverride_inheritsFromPolicy(false, 1);
  }

  @Test
  public void testOverride_inheritsFromRootPolicyTrue() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    testOverride_inheritsFromPolicy(true, 2);
  }

  @Test
  public void testOverride_inheritsFromRootPolicyFalse() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    testOverride_inheritsFromPolicy(false, 2);
  }

  /**
   * Tests that the enableOverride checkbox enables the UI in the child to override the parent policy actions.
   *
   * @param enableOverride whether to enable the override checkbox
   * @param policyListIndex 0: local policy, 1: inherited policy from container, 2: inherited policy from root org
   */
  private void testOverride_inheritsFromPolicy(boolean enableOverride, int policyListIndex) {
    createTrivialPolicy(enableOverride);
    createEmailNotification("parent@example.com");
    PolicyEditorPage.savePolicy();

    goToRepositoryManagerSummaryPage(repositoryManager);
    RepositoriesSummaryPage.policyTile().policyLists().get(policyListIndex).shouldHave(text("Test Policy"));
    RepositoriesSummaryPage.policyTile().policyLists().get(policyListIndex).click();

    PolicyEditorPage.title().shouldHave(text("View Policy"));

    NotificationsSection.notificationFor("parent@example.com").proxy().input().shouldBe(checked);
    NotificationsSection.notificationFor("parent@example.com").proxy().shouldBe(disabled);

    if (enableOverride) {
      PolicyEditorPage.notificationsSection().overrideParentNotifications().click();
      NotificationsSection.notificationFor("parent@example.com").proxy().input().shouldBe(checked);
      NotificationsSection.notificationFor("parent@example.com").proxy().shouldNotBe(disabled);
      createEmailNotification("user@example.com");
      PolicyEditorPage.savePolicy();
      NotificationsSection.notifications().shouldHave(size(2));
      NotificationsSection.notificationFor("user@example.com").proxy().input().shouldBe(checked);
      NotificationsSection.notificationFor("user@example.com").proxy().shouldNotBe(disabled);
    }
    else {
      PolicyEditorPage.notificationsSection().overrideParentNotifications().shouldBe(disabled);
      NotificationsSection.notificationFor("parent@example.com").proxy().shouldBe(disabled);
    }

    NotificationsSection.notificationFor("parent@example.com").build().shouldBe(disabled);
    NotificationsSection.notificationFor("parent@example.com").operate().shouldBe(disabled);
    NotificationsSection.notificationFor("parent@example.com").develop().shouldBe(disabled);
    NotificationsSection.notificationFor("parent@example.com").release().shouldBe(disabled);
    NotificationsSection.notificationFor("parent@example.com").continuousMonitoring().shouldBe(disabled);
    NotificationsSection.notificationFor("parent@example.com").source().shouldBe(disabled);
  }

  private void createEmailNotification(String email) {
    NotificationsSection.addNotification().email().setValue(email);
    NotificationsSection.addNotification().addButton().click();
    NotificationsSection.notificationFor(email).proxy().click();
  }

  private static void createTrivialPolicy(boolean enableOverride) {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();
    PolicyEditorPage.title().shouldHave(text("New Policy"));
    PolicyEditorPage.summarySection().policyName().input().setValue("Test Policy");
    PolicyEditorPage.constraintSection().constraintEditor(0).name().setValue("Test Constraint");
    PolicyEditorPage.constraintSection().constraintEditor(0).ageCondition(0).value().age().setValue("1");
    if (enableOverride) {
      PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().click();
    }
  }

  private void goToRepositoryContainerSummaryPage() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text("Repository Managers"));
  }

  private void goToRepositoryManagerSummaryPage(RepositoryManager repositoryManager) {
    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(repositoryManager.getName()));
  }

  private void goToRepositorySummaryPage(Repository repository) {
    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(repository.getName()));
  }
}
