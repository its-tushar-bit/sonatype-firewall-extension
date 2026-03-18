/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList.RoleRow;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class MultiTenantAdministratorsTest
    extends AbstractMtiqFunctionalTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void startup() {
    refreshOrOpen(AdministratorsPage.url());
    loginAsAdmin();

    enableSsoWithSaml();
    tempEntity.newSamlUser("a-john@doe.net", "John", "Doe", "a-john@doe.net");
    tempEntity.newSamlUser("b-jane@doe.net", "Jane", "Doe", "b-jane@doe.net");
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

  // TODO MTIQ should not really have the built-in admin user. After CLM-26430 and CLM-26391 adjust these tests to use
  // a SAML-based admin and to expect not to see "Admin Builtin" in the lists
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
    addMembersForm.searchResults().shouldHave(size(3));
    addMembersForm.searchResults().get(1).click();

    // TODO Adjust here and below in CLM-26430
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
    policyAdministratorRow.members().shouldHave(text("Admin BuiltIn"), text("Authenticated Users"), text("Jane Doe"));

    policyAdministratorRow.click();

    waitUntilUrl(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    addMembersForm.addedItems().get(1).click();
    addMembersForm.addedItems().get(1).click();
    addMembersForm.addedItems().shouldHave(texts("Admin BuiltIn"));

    addMembersForm.submitBtn().click();
    waitUntilUrl(AdministratorsPage.url());

    policyAdministratorRow.shouldBe(visible);
    policyAdministratorRow.role().shouldHave(text("Policy Administrator"));
    policyAdministratorRow.members().shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testGroupSearchAndAdd() {
    // Third party IdP - Associate Group text box should be present
    final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";
    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();

    AdministratorsEditPage.AddMembersForm addMembersForm = administratorsEditPage.addMembersForm();

    addMembersForm.groupAlert().shouldNotBe(visible);

    addMembersForm.addAssociateGroupSublabel().shouldHave(text("Requires an exact match of the SAML group name"));
    addMembersForm.addAssociateGroupBtn().shouldHave(cssClass("disabled"));
    addMembersForm.addAssociateGroupInput().setValue("test group").click();
    addMembersForm.addAssociateGroupBtn().shouldNotHave(cssClass("disabled"));

    eyesWatcher.eyesCheck("with external group text box");

    addMembersForm.addAssociateGroupBtn().click();
    addMembersForm.addedItems().shouldHave(size(2));
    addMembersForm.addedItems().shouldHave(texts("Admin BuiltIn", "test group (Group)")); // TODO CLM-26430
    addMembersForm.addAssociateGroupInput().setValue("test group");
    addMembersForm.addAssociateGroupBtn().shouldHave(cssClass("disabled"));

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    addMembersForm.cancelBtn().click();
    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    addMembersForm.groupAlert().shouldNotBe(visible);
    addMembersForm.addAssociateGroupSublabel();

    eyesWatcher.eyesCheck("without external group text box");
  }

  /*
   * TODO add test that an explicitly added group grants appropriate permissions to newly-logged-in user in that group.
   * This depends on the Auth0 mock server to be implemented in CLM-26391
   */
}
