/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class RepositoryPolicyEditorNotificationsOverrideTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void boot() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    tempEntity.newRepositoryManager();
  }

  @Test
  public void testOverrideAddAndEdit_RepositoryContainer() {
    refreshOrOpen(RepositoriesSummaryPage.url());

    RepositoriesSummaryPage.summaryTile().name().shouldHave(text("Repository Managers"));
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();
    PolicyEditorPage.title().shouldHave(text("New Policy"));
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().input().shouldNotBe(checked);
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().label()
        .shouldHave(text("Allow notification overrides at repository manager and repository levels"));
    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().shouldNotBe(visible);
    PolicyEditorPage.inheritanceSection().specifiedChildrenInheritRadio().shouldNotBe(visible);

    PolicyEditorPage.summarySection().policyName().input().setValue("Test Policy");
    PolicyEditorPage.constraintSection().constraintEditor(0).name().setValue("Test Constraint");
    PolicyEditorPage.constraintSection().constraintEditor(0).ageCondition(0).value().age().setValue("1");
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().click();
    PolicyEditorPage.savePolicy();

    OwnerDetailSidebar.policyGroup().entryItems().shouldHaveSize(1);
    OwnerDetailSidebar.policyGroup().entryItems().get(0).click();

    PolicyEditorPage.title().shouldHave(text("Edit Policy"));
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().input().shouldBe(checked);
    PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox().label()
        .shouldHave(text("Allow notification overrides at repository manager and repository levels"));
  }
}
