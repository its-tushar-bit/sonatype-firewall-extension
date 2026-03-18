/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsEditPage.AddMembersForm;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsMappingList.RoleRow;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class AdministratorsTest
    extends AbstractFunctionalTest
{
  private LdapUserMappingDAO ldapUserMappingDAO;

  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(AdministratorsPage.url());
    loginAsAdmin();
  }

  @Before
  public void startup() {
    ldapUserMappingDAO = lookup(LdapUserMappingDAO.class);

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
  public void addAndRemoveMembers() {
    final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";
    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();

    AdministratorsEditPage.AddMembersForm addMembersForm = administratorsEditPage.addMembersForm();

    addMembersForm.searchInput().setValue("*").click();
    addMembersForm.searchResults().shouldHave(size(3));
    addMembersForm.searchResults()
        .shouldHave(texts("John Doe", "Jane Doe", "Authenticated Users (Group)"));
    addMembersForm.searchResults().get(1).click();
    addMembersForm.addedItems().shouldHave(size(2));
    addMembersForm.addedItems().shouldHave(texts("Admin BuiltIn", "Jane Doe"));

    addMembersForm.searchInput().setValue("*").click();
    addMembersForm.searchResults().shouldHave(size(2));
    addMembersForm.searchResults()
        .shouldHave(texts("John Doe", "Authenticated Users (Group)"));
    addMembersForm.searchResults().get(1).click();
    addMembersForm.addedItems().shouldHave(size(3));
    addMembersForm.addedItems()
        .shouldHave(texts("Admin BuiltIn", "Authenticated Users (Group)", "Jane Doe"));

    addMembersForm.addedItems().get(0).click();
    addMembersForm.addedItems().shouldHave(size(2));
    addMembersForm.addedItems().shouldHave(texts("Authenticated Users (Group)", "Jane Doe"));

    addMembersForm.removeAll().click();
    addMembersForm.addedItems().shouldHave(size(0));

    addMembersForm.searchInput().setValue("*").click();
    addMembersForm.searchResults().shouldHave(size(4));
    addMembersForm.searchResults()
        .shouldHave(texts("Admin BuiltIn", "John Doe", "Jane Doe", "Authenticated Users (Group)"));
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

  @Test
  public void testCancelAddMembersFormAndUnsavedChangesModal() {
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

    addMembersForm.cancelBtn().click();
    UnsavedModal unsavedModal = new UnsavedModal();
    unsavedModal.continueButton().click();
    waitUntilUrl(AdministratorsPage.url());

    policyAdministratorRow.shouldBe(visible);
    policyAdministratorRow.role().shouldHave(text("Policy Administrator"));
    policyAdministratorRow.members().shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testGroupSearchAndAdd() {
    // no ldap servers configured
    final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";
    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    AdministratorsEditPage administratorsEditPage = new AdministratorsEditPage();

    AdministratorsEditPage.AddMembersForm addMembersForm = administratorsEditPage.addMembersForm();

    addMembersForm.groupAlert().shouldNotBe(visible);

    // all servers have group search disabled
    String ldap1 = tempEntity.newLdapServer("LDAP1").getId();
    tempEntity.newLdapConnection(ldap1);

    LdapUserMapping ldapUserMapping1 = tempEntity.newLdapUserMapping(ldap1);
    ldapUserMapping1.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping1.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(ldapUserMapping1);

    String ldap2 = tempEntity.newLdapServer("LDAP2").getId();
    tempEntity.newLdapConnection(ldap2);

    LdapUserMapping ldapUserMapping2 = tempEntity.newLdapUserMapping(ldap2);
    ldapUserMapping2.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping2.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(ldapUserMapping2);

    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    addMembersForm.groupAlert().shouldBe(visible).shouldHave(text(AddMembersForm.DISABLED_GROUP_SEARCH_WARNING));
    addMembersForm.addAssociateGroupSublabel().shouldHave(text("Requires an exact match of the LDAP group name"));

    // mix servers have group search disabled and disabled
    ldapUserMapping2.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(ldapUserMapping2);

    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    addMembersForm.groupAlert().shouldBe(visible).shouldHave(text(AddMembersForm.DISABLED_GROUP_SEARCH_WARNING));

    addMembersForm.addAssociateGroupBtn().shouldHave(cssClass("disabled"));
    addMembersForm.addAssociateGroupInput().setValue("test group").click();
    addMembersForm.addAssociateGroupBtn().shouldNotHave(cssClass("disabled")).click();
    addMembersForm.addedItems().shouldHave(size(2));
    addMembersForm.addedItems().shouldHave(texts("Admin BuiltIn", "test group (Group)"));
    addMembersForm.addAssociateGroupInput().setValue("test group").click();
    addMembersForm.addAssociateGroupBtn().shouldHave(cssClass("disabled"));

    // all servers have group search enabled
    ldapUserMapping1.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(ldapUserMapping1);

    prepareToLeavePage();

    refreshOrOpen(AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    addMembersForm.groupAlert().shouldNotBe(visible);
  }
}
