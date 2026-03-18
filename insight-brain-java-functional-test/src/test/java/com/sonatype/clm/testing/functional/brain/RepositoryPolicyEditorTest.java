/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.allRadioText;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.specifiedRadioText;

public class RepositoryPolicyEditorTest
    extends AbstractFunctionalTest
{
  private Repository repository;

  private RepositoryManager repositoryManager;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    repositoryManager = tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639");
    repository = tempEntity.newProxyRepository(repositoryManager, "npm-proxy", "npm", true, true);
  }

  @Test
  public void testEditPolicy_inheritanceSection() {
    Policy policy = tempEntity.newPolicy(repository.getId(), "policyAtRootOrgLevel");

    refreshOrOpen(PolicyEditorPage.urlToEdit(repository, policy.getId()));
    refresh(); // because the page has already loaded the store the policy doesn't exist

    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.shouldNotBe(visible);
  }

  @Test
  public void testViewPolicy_inheritanceSectionForRootOrg() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policyAtRootOrgLevel");

    refreshOrOpen(PolicyEditorPage.urlToEdit(repository, policy.getId()));
    refresh(); // because the page has already loaded the store the policy doesn't exist

    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.shouldNotHave(text("This Policy Inherits to All Repository Managers and Repositories within Them"));
    inheritance.shouldNotHave(text("This policy inherits to all Repositories in Root Organization"));

    inheritance.allChildrenInheritRadio().shouldBe(visible, disabled);
    inheritance.allChildrenInheritRadio().shouldHave(allRadioText("Root Organization"));
    inheritance.specifiedChildrenInheritRadio().shouldBe(visible, disabled);
    inheritance.specifiedChildrenInheritRadio().shouldHave(specifiedRadioText("Root Organization"));
    inheritance.associationEditor().shouldBe(hidden);

    inheritance.policyActionsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow action overrides at organization, application and repositories levels"));
    inheritance.policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow notification overrides at organization, application and repositories levels"));

    eyesWatcher.eyesCheck("Policy Editor Inheritance section at repository level for root org policy");
  }

  @Test
  public void testViewPolicy_inheritanceSectionForRepoContainer() {
    Policy policy = tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, "policyAtRepoContainerLevel");

    refreshOrOpen(PolicyEditorPage.urlToEdit(repository, policy.getId()));
    refresh(); // because the page has already loaded the store the policy doesn't exist

    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.shouldHave(text("This Policy Inherits to All Repository Managers and Repositories within Them"));

    inheritance.allChildrenInheritRadio().shouldBe(hidden);
    inheritance.specifiedChildrenInheritRadio().shouldBe(hidden);
    inheritance.associationEditor().shouldBe(hidden);

    inheritance.policyActionsOverrideCheckbox().shouldBe(disabled);
    inheritance.policyActionsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow action overrides at repository manager and repository level"));
    inheritance.policyNotificationsOverrideCheckbox().shouldBe(disabled);
    inheritance.policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow notification overrides at repository manager and repository level"));

    eyesWatcher.eyesCheck("Policy Editor Inheritance section at repository level for repo container policy");
  }

  @Test
  public void testViewPolicy_inheritanceSectionForRepoManager() {
    Policy policy = tempEntity.newPolicy(repositoryManager.getId(), "policyAtRepoMangerLevel");

    refreshOrOpen(PolicyEditorPage.urlToEdit(repository, policy.getId()));
    refresh(); // because the page has already loaded the store the policy doesn't exist

    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.shouldHave(text("This policy inherits to all Repositories in " + repositoryManager.getName()));

    inheritance.allChildrenInheritRadio().shouldBe(hidden);
    inheritance.specifiedChildrenInheritRadio().shouldBe(hidden);
    inheritance.associationEditor().shouldBe(hidden);

    inheritance.policyActionsOverrideCheckbox().shouldBe(disabled);
    inheritance.policyActionsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow action overrides at repository level"));
    inheritance.policyNotificationsOverrideCheckbox().shouldBe(disabled);
    inheritance.policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow notification overrides at repository level"));

    eyesWatcher.eyesCheck("Policy Editor Inheritance section at repository level for repo manager policy");
  }
}
