/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList.RoleRow;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class AdministratorsTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(AdministratorsPage.url());
    loginAsAdmin();
  }

  @Before
  public void startup() {
    // create two users
    tempEntity.newUser("test-a", "secret", "John", "Doe", "john@doe.net");
    tempEntity.newUser("test-b", "secret", "Jane", "Doe", "jane@doe.net");

    refreshOrOpen(AdministratorsPage.url());
  }

  @After
  public void prepareToLeavePage() {
    AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();

    AdministratorsEditPage.AddMembersForm addMembersForm = administratorsEditPage.addMembersForm();

    SelenideElement cancelBtn = addMembersForm.cancelBtn();

    if (cancelBtn.isDisplayed()) {
      cancelBtn.click();

      UnsavedModal unsavedModal = new UnsavedModal();

      // dismiss unsaved changes
      SelenideElement modalBtn = unsavedModal.continueButton();
      if (modalBtn.exists()) {
        modalBtn.click();
        waitUntilUrl(AdministratorsPage.url());
      }
    }
  }

  @Test
  public void testDefaultRolesAndBuiltinUsers() {
    refreshOrOpen(AdministratorsPage.url());

    AdministratorsMappingList mapping = AdministratorsPage.administratorsMappingList();
    mapping.rows().shouldHave(size(2));

    RoleRow firstRoleRow = mapping.row(0);

    firstRoleRow.shouldBe(visible);
    firstRoleRow.role().shouldHave(text("Policy Administrator"));
    firstRoleRow.members().shouldHave(text("Admin BuiltIn"));
    firstRoleRow.chevron().shouldBe(visible);

    RoleRow secondRoleRow = mapping.row(1);

    secondRoleRow.shouldBe(visible);
    secondRoleRow.role().shouldHave(text("System Administrator"));
    secondRoleRow.members().shouldHave(text("Admin BuiltIn"));
    secondRoleRow.chevron().shouldBe(visible);
  }

  @Test
  public void testUsageViewerRoleAppears_whenConsumptionReportingEnabled() {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);
    try {
      refreshOrOpen(AdministratorsPage.url());

      AdministratorsMappingList mapping = AdministratorsPage.administratorsMappingList();
      mapping.rows().shouldHave(size(3));

      RoleRow usageViewerRow = mapping.row(2);
      usageViewerRow.shouldBe(visible);
      usageViewerRow.role().shouldHave(text("Usage Viewer"));
      usageViewerRow.chevron().shouldBe(visible);
    }
    finally {
      SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    }
  }

  @Test
  public void testClickEdit() {
    refreshOrOpen(AdministratorsPage.url());
    RoleRow policyAdministratorRow = AdministratorsPage.administratorsMappingList().row(0);

    policyAdministratorRow.shouldBe(visible);

    policyAdministratorRow.click();

    // roleId's can be found in Role.java
    final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";
    waitUntilUrl(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();

    AdministratorsEditPage.AddMembersForm addMembersForm = administratorsEditPage.addMembersForm();

    administratorsEditPage.roleDetails().shouldBe(visible);
    administratorsEditPage.roleDetails().name().shouldHave(text("Policy Administrator"));
    administratorsEditPage.roleDetails()
        .description()
        .shouldHave(text("Manages all organizations, applications, policies, and policy violations."));
    addMembersForm.shouldBe(visible);
    addMembersForm.addedItems().shouldHave(size(1));
    addMembersForm.addedItems().shouldHave(texts("Admin BuiltIn"));
    eyesWatcher.eyesCheck("Edit Administrators");
  }

  @Test
  public void testSubmitAddMembersForm() {
    refreshOrOpen(AdministratorsPage.url());

    RoleRow policyAdministratorRow = AdministratorsPage.administratorsMappingList().row(0);

    policyAdministratorRow.shouldBe(visible);
    policyAdministratorRow.role().shouldHave(text("Policy Administrator"));
    policyAdministratorRow.members().shouldHave(text("Admin BuiltIn"));

    policyAdministratorRow.click();

    final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";
    waitUntilUrl(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();

    AdministratorsEditPage.AddMembersForm addMembersForm = administratorsEditPage.addMembersForm();

    addMembersForm.searchInput().setValue("*").click();
    addMembersForm.searchResults().get(1).click();
    addMembersForm.addedItems().shouldHave(size(2));
    addMembersForm.addedItems().shouldHave(texts("Admin BuiltIn", "Jane Doe"));

    addMembersForm.searchInput().setValue("*").click();
    addMembersForm.searchResults().get(1).click();
    addMembersForm.addedItems().shouldHave(size(3));
    addMembersForm.addedItems()
        .shouldHave(texts("Admin BuiltIn", "Authenticated Users (Group)", "Jane Doe"));

    addMembersForm.submitBtn().click();
    waitUntilUrl(AdministratorsPage.url());

    policyAdministratorRow.shouldBe(visible);
    policyAdministratorRow.role().shouldHave(text("Policy Administrator"));
    policyAdministratorRow.members().shouldHave(text("Authenticated Users, Admin BuiltIn, Jane Doe"));

    policyAdministratorRow.click();

    waitUntilUrl(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    addMembersForm.addedItems().get(1).click();
    addMembersForm.addedItems().get(1).click();
    addMembersForm.addedItems()
        .shouldHave(texts("Admin BuiltIn"));

    addMembersForm.submitBtn().click();
    waitUntilUrl(AdministratorsPage.url());

    policyAdministratorRow.shouldBe(visible);
    policyAdministratorRow.role().shouldHave(text("Policy Administrator"));
    policyAdministratorRow.members().shouldHave(text("Admin BuiltIn"));
  }

}
