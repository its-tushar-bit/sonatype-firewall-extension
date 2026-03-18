/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
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
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class RepositoryPolicyEditorActionsOverrideTest
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
    PolicyEditorPage.summarySection().shouldBe(visible);
    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
    PolicyEditorPage.constraintSection().header().shouldBe(visible);
    PolicyEditorPage.actionsSection().header().shouldBe(visible);
    PolicyEditorPage.notificationsSection().header().shouldBe(visible);
  }

  @Test
  public void testOverrideAddAndEdit_RepositoryManager() {
    goToRepositoryManagerSummaryPage(repositoryManager);
    createTrivialPolicy(true);
    PolicyEditorPage.savePolicy();

    PolicyEditorPage.inheritanceSection().policyActionsOverrideCheckbox().input().shouldNotBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyActionsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow action overrides at repository level"));
    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().shouldNotBe(visible);
    PolicyEditorPage.inheritanceSection().specifiedChildrenInheritRadio().shouldNotBe(visible);
    OwnerDetailSidebar.policyGroup().entryItems().shouldHave(size(1));
    OwnerDetailSidebar.policyGroup().entryItems().get(0).click();

    PolicyEditorPage.title().shouldHave(text("Edit Policy"));
    PolicyEditorPage.inheritanceSection().policyActionsOverrideCheckbox().input().shouldBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyActionsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow action overrides at repository level"));
  }

  @Test
  public void testOverrideAddAndEdit_RepositoryContainer() {
    goToRepositoryContainerSummaryPage();
    createTrivialPolicy(true);
    PolicyEditorPage.savePolicy();

    PolicyEditorPage.inheritanceSection().policyActionsOverrideCheckbox().input().shouldNotBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyActionsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow action overrides at repository manager and repository levels"));
    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().shouldNotBe(visible);
    PolicyEditorPage.inheritanceSection().specifiedChildrenInheritRadio().shouldNotBe(visible);
    OwnerDetailSidebar.policyGroup().entryItems().shouldHave(size(1));
    OwnerDetailSidebar.policyGroup().entryItems().get(0).click();

    PolicyEditorPage.title().shouldHave(text("Edit Policy"));
    PolicyEditorPage.inheritanceSection().policyActionsOverrideCheckbox().input().shouldBe(checked);
    PolicyEditorPage.inheritanceSection()
        .policyActionsOverrideCheckbox()
        .label()
        .shouldHave(text("Allow action overrides at repository manager and repository levels"));
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
    PolicyEditorPage.savePolicy();

    goToRepositoryManagerSummaryPage(repositoryManager);
    RepositoriesSummaryPage.policyTile().policyLists().get(policyListIndex).shouldHave(text("Test Policy"));
    RepositoriesSummaryPage.policyTile().policyLists().get(policyListIndex).click();

    PolicyEditorPage.title().shouldHave(text("View Policy"));
    PolicyEditorPage.actionsSection().overrideParentActions().shouldBe(enableOverride ? enabled : disabled);
    PolicyEditorPage.actionsSection().proxy().noActionRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().proxy().warnRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().proxy().failRadio().shouldBe(disabled);

    if (enableOverride) {
      PolicyEditorPage.actionsSection().overrideParentActions().click();
      PolicyEditorPage.actionsSection().proxy().noActionRadio().shouldBe(enabled);
      PolicyEditorPage.actionsSection().proxy().warnRadio().shouldBe(enabled);
      PolicyEditorPage.actionsSection().proxy().failRadio().shouldBe(enabled);
      PolicyEditorPage.actionsSection().proxy().failRadio().click();
      PolicyEditorPage.actionsSection().proxy().failRadio().input().shouldBe(checked);
      PolicyEditorPage.savePolicy();
      refresh();
      PolicyEditorPage.actionsSection().proxy().failRadio().input().shouldBe(checked);
      PolicyEditorPage.actionsSection().proxy().noActionRadio().shouldBe(enabled);
      PolicyEditorPage.actionsSection().proxy().warnRadio().shouldBe(enabled);
      PolicyEditorPage.actionsSection().proxy().failRadio().shouldBe(enabled);
    }
    else {
      PolicyEditorPage.actionsSection().proxy().noActionRadio().shouldBe(disabled);
      PolicyEditorPage.actionsSection().proxy().warnRadio().shouldBe(disabled);
      PolicyEditorPage.actionsSection().proxy().failRadio().shouldBe(disabled);
    }

    PolicyEditorPage.actionsSection().build().noActionRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().build().warnRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().build().failRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().operate().noActionRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().operate().warnRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().operate().failRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().develop().noActionRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().develop().warnRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().develop().failRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().release().noActionRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().release().warnRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().release().failRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().source().noActionRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().source().warnRadio().shouldBe(disabled);
    PolicyEditorPage.actionsSection().source().failRadio().shouldBe(disabled);
  }

  private static void createTrivialPolicy(boolean enableOverride) {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();
    PolicyEditorPage.title().shouldHave(text("New Policy"));
    PolicyEditorPage.summarySection().policyName().input().setValue("Test Policy");
    PolicyEditorPage.constraintSection().constraintEditor(0).name().setValue("Test Constraint");
    PolicyEditorPage.constraintSection().constraintEditor(0).ageCondition(0).value().age().setValue("1");
    if (enableOverride) {
      PolicyEditorPage.inheritanceSection().policyActionsOverrideCheckbox().click();
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
